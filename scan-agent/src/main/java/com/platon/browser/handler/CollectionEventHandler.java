package com.platon.browser.handler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.EventHandler;
import com.platon.browser.analyzer.TransactionAnalyzer;
import com.platon.browser.bean.*;
import com.platon.browser.cache.AddressCache;
import com.platon.browser.cache.NetworkStatCache;
import com.platon.browser.cache.NodeCache;
import com.platon.browser.dao.custommapper.CustomNOptBakMapper;
import com.platon.browser.dao.custommapper.CustomTxBakMapper;
import com.platon.browser.dao.entity.NOptBak;
import com.platon.browser.dao.entity.TxBak;
import com.platon.browser.dao.entity.TxBakExample;
import com.platon.browser.dao.mapper.NodeMapper;
import com.platon.browser.dao.mapper.TxBakMapper;
import com.platon.browser.elasticsearch.dto.Block;
import com.platon.browser.elasticsearch.dto.NodeOpt;
import com.platon.browser.elasticsearch.dto.Transaction;
import com.platon.browser.publisher.ComplementEventPublisher;
import com.platon.browser.service.block.BlockService;
import com.platon.browser.service.ppos.PPOSService;
import com.platon.browser.service.statistic.StatisticService;
import com.platon.browser.utils.BakDataDeleteUtil;
import com.platon.browser.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 区块事件处理器
 */
@Slf4j
@Component
public class CollectionEventHandler implements EventHandler<CollectionEvent> {

    @Resource
    private PPOSService pposService;

    @Resource
    private BlockService blockService;

    @Resource
    private StatisticService statisticService;

    @Resource
    private ComplementEventPublisher complementEventPublisher;

    @Resource
    private NetworkStatCache networkStatCache;

    @Resource
    private CustomNOptBakMapper customNOptBakMapper;

    @Resource
    private TxBakMapper txBakMapper;

    @Resource
    private NodeMapper nodeMapper;

    @Resource
    private CustomTxBakMapper customTxBakMapper;

    @Resource
    private AddressCache addressCache;

    @Resource
    private NodeCache nodeCache;

    @Resource
    private TransactionAnalyzer transactionAnalyzer;

    // 交易序号id
    private long transactionId = 0;

    private long txDeleteBatchCount = 0;

    /**
     * 重试次数
     */
    private AtomicLong retryCount = new AtomicLong(0);

    @Transactional(rollbackFor = {Exception.class, Error.class})
    @Retryable(value = Exception.class, maxAttempts = Integer.MAX_VALUE)
    public void onEvent(CollectionEvent event, long sequence, boolean endOfBatch) throws Exception {
        surroundExec(event, sequence, endOfBatch);
    }

    private void surroundExec(CollectionEvent event, long sequence, boolean endOfBatch) throws Exception {
        CommonUtil.putTraceId(event.getTraceId());
        long startTime = System.currentTimeMillis();
        exec(event, sequence, endOfBatch);
        log.debug("处理耗时:{} ms", System.currentTimeMillis() - startTime);
        CommonUtil.removeTraceId();
    }

    private void exec(CollectionEvent event, long sequence, boolean endOfBatch) throws Exception {
        // 确保event是原始副本，重试机制每一次使用的都是copyEvent
        CollectionEvent copyEvent = copyCollectionEvent(event);
        // 之前在BlockEventHandler中的交易分析逻辑挪至当前位置 START
        Map<String, Receipt> receiptMap = copyEvent.getBlock().getReceiptMap();
        List<com.platon.protocol.core.methods.response.Transaction> rawTransactions = copyEvent.getBlock().getOriginTransactions();
        for (com.platon.protocol.core.methods.response.Transaction tr : rawTransactions) {
            CollectionTransaction transaction = transactionAnalyzer.analyze(copyEvent.getBlock(), tr, receiptMap.get(tr.getHash()));
            // 把解析好的交易添加到当前区块的交易列表
            copyEvent.getBlock().getTransactions().add(transaction);
            copyEvent.getTransactions().add(transaction);
            // 设置当前块的erc20交易数和erc721u交易数，以便更新network_stat表
            copyEvent.getBlock().setErc20TxQty(copyEvent.getBlock().getErc20TxQty() + transaction.getErc20TxList().size());
            copyEvent.getBlock().setErc721TxQty(copyEvent.getBlock().getErc721TxQty() + transaction.getErc721TxList().size());
        }
        // 之前在BlockEventHandler中的交易分析逻辑挪至当前位置 END

        // 使用已入库的交易数量初始化交易ID初始值
        if (transactionId == 0) transactionId = networkStatCache.getNetworkStat().getTxQty();

        try {
            List<Transaction> transactions = copyEvent.getTransactions();
            // 确保交易从小到大的索引顺序
            transactions.sort(Comparator.comparing(Transaction::getIndex));
            for (Transaction tx : transactions) {
                tx.setId(++transactionId);
            }

            // 根据区块号解析出业务参数
            List<NodeOpt> nodeOpts1 = blockService.analyze(copyEvent);
            // 根据交易解析出业务参数
            TxAnalyseResult txAnalyseResult = pposService.analyze(copyEvent);
            // 统计业务参数
            statisticService.analyze(copyEvent);
            if (!txAnalyseResult.getNodeOptList().isEmpty()) nodeOpts1.addAll(txAnalyseResult.getNodeOptList());

            txDeleteBatchCount++;

            if (txDeleteBatchCount >= 10) {
                // 删除小于最高ID的交易备份
                TxBakExample txBakExample = new TxBakExample();
                txBakExample.createCriteria().andIdLessThanOrEqualTo(BakDataDeleteUtil.getTxBakMaxId());
                int txCount = txBakMapper.deleteByExample(txBakExample);
                log.debug("清除交易备份记录({})条", txCount);
                txDeleteBatchCount = 0;
            }
            // 交易入库mysql
            if (!transactions.isEmpty()) {
                List<TxBak> baks = new ArrayList<>();
                transactions.forEach(tx -> {
                    TxBak bak = new TxBak();
                    BeanUtils.copyProperties(tx, bak);
                    baks.add(bak);
                });
                customTxBakMapper.batchInsertOrUpdateSelective(baks, TxBak.Column.values());
            }

            // 操作日志入库mysql
            if (!nodeOpts1.isEmpty()) {
                List<NOptBak> baks = new ArrayList<>();
                nodeOpts1.forEach(no -> {
                    NOptBak bak = new NOptBak();
                    BeanUtils.copyProperties(no, bak);
                    baks.add(bak);
                });
                customNOptBakMapper.batchInsertOrUpdateSelective(baks, NOptBak.Column.excludes(NOptBak.Column.id));
            }

            complementEventPublisher.publish(copyEvent.getBlock(), transactions, nodeOpts1, txAnalyseResult.getDelegationRewardList(), event.getTraceId());
            // 释放对象引用
            event.releaseRef();
            retryCount.set(0);
        } catch (Exception e) {
            log.error(StrUtil.format("区块[{}]解析交易异常", copyEvent.getBlock().getNum()), e);
            throw e;
        } finally {
            // 当前事务不管是正常处理结束或异常结束，都需要重置地址缓存，防止代码中任何地方出问题后，缓存中留存脏数据
            // 因为地址缓存是当前事务处理的增量缓存，在 StatisticsAddressAnalyzer 进行数据合并入库时：
            // 1、如果出现异常，由于事务保证，当前事务统计的地址数据不会入库mysql，此时应该清空增量缓存，等待下次重试时重新生成缓存
            // 2、如果正常结束，当前事务统计的地址数据会入库mysql，此时应该清空增量缓存
            addressCache.cleanAll();
        }
    }

    /**
     * 模拟深拷贝
     * 因为CollectionEvent引用了第三方的jar对象，没有实现系列化接口，没法做深拷贝
     *
     * @param event:
     * @return: com.platon.browser.bean.CollectionEvent
     * @date: 2021/11/22
     */
    private CollectionEvent copyCollectionEvent(CollectionEvent event) {
        CollectionEvent copyEvent = new CollectionEvent();
        Block block = new Block();
        BeanUtil.copyProperties(event.getBlock(), block);
        copyEvent.setBlock(block);
        copyEvent.getTransactions().addAll(event.getTransactions());
        EpochMessage epochMessage = EpochMessage.newInstance();
        BeanUtil.copyProperties(event.getEpochMessage(), epochMessage);
        copyEvent.setEpochMessage(epochMessage);
        copyEvent.setTraceId(event.getTraceId());
        if (retryCount.incrementAndGet() > 1) {
            initNodeCache();
            addressCache.cleanAll();
            List<String> txHashList = CollUtil.newArrayList();
            if (CollUtil.isNotEmpty(event.getBlock().getOriginTransactions())) {
                txHashList = event.getBlock().getOriginTransactions().stream().map(com.platon.protocol.core.methods.response.Transaction::getHash).collect(Collectors.toList());
            }
            log.warn("重试次数[{}],节点重新初始化，清除地址缓存，该区块[{}]交易列表{}重复处理，event对象数据为[{}]，copyEvent对象数据为[{}]",
                     retryCount.get(),
                     event.getBlock().getNum(),
                     JSONUtil.toJsonStr(txHashList),
                     JSONUtil.toJsonStr(event),
                     JSONUtil.toJsonStr(copyEvent));
        }
        return copyEvent;
    }

    /**
     * 初始化节点缓存
     *
     * @param :
     * @return: void
     * @date: 2021/11/30
     */
    private void initNodeCache() {
        nodeCache.cleanNodeCache();
        List<com.platon.browser.dao.entity.Node> nodeList = nodeMapper.selectByExample(null);
        nodeCache.init(nodeList);
    }

}
