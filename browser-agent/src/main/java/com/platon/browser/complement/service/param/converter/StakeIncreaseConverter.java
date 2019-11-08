package com.platon.browser.complement.service.param.converter;

import com.platon.browser.common.collection.dto.CollectionTransaction;
import com.platon.browser.common.complement.param.stake.StakeIncrease;
import com.platon.browser.common.queue.collection.event.CollectionEvent;
import com.platon.browser.param.StakeIncreaseParam;
import com.platon.browser.persistence.dao.mapper.StakeBusinessMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @description: 增持质押业务参数转换器
 * @author: chendongming@juzix.net
 * @create: 2019-11-04 17:58:27
 **/
@Service
public class StakeIncreaseConverter extends BusinessParamConverter<StakeIncrease> {

    @Autowired
    private StakeBusinessMapper stakeBusinessMapper;
    
    @Override
    public StakeIncrease convert(CollectionEvent event, CollectionTransaction tx) {
        // 增持质押
        StakeIncreaseParam txParam = tx.getTxParam(StakeIncreaseParam.class);
        StakeIncrease businessParam= StakeIncrease.builder()        		
        		.nodeId(txParam.getNodeId())
        		.amount(new BigDecimal(txParam.getAmount()))
        		.bNum(BigInteger.valueOf(tx.getNum()))
        		.stakingBlockNum(txParam.getStakingBlockNum())
        		.time(tx.getTime())
        		.txHash(tx.getHash())
                .build();
        
        stakeBusinessMapper.increase(businessParam);
        return businessParam;
    }
}
