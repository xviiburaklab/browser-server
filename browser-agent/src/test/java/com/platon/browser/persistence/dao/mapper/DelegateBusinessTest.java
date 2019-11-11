package com.platon.browser.persistence.dao.mapper;

import com.platon.browser.AgentApplication;
import com.platon.browser.TestBase;
import com.platon.browser.complement.dao.mapper.*;
import com.platon.browser.complement.dao.param.delegate.DelegateCreate;
import com.platon.browser.complement.dao.param.delegate.DelegateExit;
import com.platon.browser.complement.dao.param.epoch.Consensus;
import com.platon.browser.complement.dao.param.epoch.Election;
import com.platon.browser.complement.dao.param.epoch.NewBlock;
import com.platon.browser.complement.dao.param.epoch.Settle;
import com.platon.browser.complement.dao.param.proposal.ProposalText;
import com.platon.browser.complement.dao.param.proposal.ProposalUpgrade;
import com.platon.browser.complement.dao.param.proposal.ProposalVote;
import com.platon.browser.complement.dao.param.restricting.RestrictingCreate;
import com.platon.browser.complement.dao.param.slash.Report;
import com.platon.browser.complement.dao.param.stake.StakeCreate;
import com.platon.browser.complement.dao.param.stake.StakeExit;
import com.platon.browser.complement.dao.param.stake.StakeIncrease;
import com.platon.browser.complement.dao.param.stake.StakeModify;
import com.platon.browser.complement.dao.param.statistic.AddressStatChange;
import com.platon.browser.complement.dao.param.statistic.NetworkStatChange;
import com.platon.browser.dao.entity.*;
import com.platon.browser.dao.mapper.*;
import com.platon.browser.elasticsearch.dto.NodeOpt;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @Auther: dongqile
 * @Date: 2019/10/31
 * @Description:
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AgentApplication.class, value = "spring.profiles.active=test")
@SpringBootApplication
public class DelegateBusinessTest extends TestBase {

    @Autowired
    private StakeBusinessMapper stakeBusinessMapper;

    @Autowired
    private SlashBusinessMapper slashBusinessMapper;

    @Autowired
    private NewBlockMapper newBlockMapper;

    @Autowired
    private EpochBusinessMapper epochBusinessMapper;

    @Autowired
    private DelegateBusinessMapper delegateBusinessMapper;

    @Autowired
    private ProposalBusinessMapper proposalBusinessMapper;

    @Autowired
    private StatisticBusinessMapper statisticBusinessMapper;

    @Autowired
    private RestrictingBusinessMapper restrictingBusinessMapper;

    @Autowired
    private NOptBakMapper nOptBakMapper;

    @Autowired
    private StakingMapper stakingMapper;

    @Autowired
    private NodeMapper nodeMapper;

    @Autowired
    private DelegationMapper delegationMapper;

    @Autowired
    private SlashMapper slashMapper;

    @Autowired
    private CustomProposalMapper customProposalMapper;


    @Test
    public void createStakingMapper () {
        StakeCreate createStakingParam = stakingParam();
        //删除数据
        deleteCreateStaking(createStakingParam);
        //数据插入
        stakeBusinessMapper.create(createStakingParam);
        //staking数据插入验证
        Staking staking = getStaking(createStakingParam.getNodeId(), createStakingParam.getStakingBlockNum().longValue());
        assertEquals(createStakingParam.getNodeId(), staking.getNodeId());
        //node数据插入验证
        Node node = nodeMapper.selectByPrimaryKey(createStakingParam.getNodeId());
        assertEquals(createStakingParam.getStakingBlockNum(), new BigInteger(node.getStakingBlockNum().toString()));
        //opt数据插入验证
        NOptBakExample nodeOptExample = new NOptBakExample();
        nodeOptExample.createCriteria().andNodeIdEqualTo(createStakingParam.getNodeId())
                .andBNumEqualTo(createStakingParam.getStakingBlockNum().longValue());
        List <NOptBak> nodeOptList = nOptBakMapper.selectByExample(nodeOptExample);
        assertEquals(nodeOptList.get(0).getNodeId(), createStakingParam.getNodeId());
    }

    public void deleteCreateStaking ( StakeCreate param ) {
        //删除staking数据
        StakingKey stakingKey = new StakingKey();
        stakingKey.setNodeId(param.getNodeId());
        stakingKey.setStakingBlockNum(param.getStakingBlockNum().longValue());
        stakingMapper.deleteByPrimaryKey(stakingKey);
        //删除node数据
        nodeMapper.deleteByPrimaryKey(param.getNodeId());
        //删除opt数据
        NOptBakExample nodeOptExample = new NOptBakExample();
        nodeOptExample.createCriteria().andNodeIdEqualTo(param.getNodeId()).andBNumEqualTo(param.getStakingBlockNum().longValue());
        nOptBakMapper.deleteByExample(nodeOptExample);
    }

    @Test
    public void modifyStakingMapper () {
        StakeModify modifyStakingParam = modifyStakingParam();
        stakeBusinessMapper.modify(modifyStakingParam);
        //staking数据更新验证
        Staking staking = getStaking(modifyStakingParam.getNodeId(), modifyStakingParam.getStakingBlockNum().longValue());
        assertEquals(modifyStakingParam.getNodeName(), staking.getNodeName());
        assertEquals(modifyStakingParam.getExternalId(), staking.getExternalId());
        assertEquals(modifyStakingParam.getBenefitAddr(), staking.getBenefitAddr());
        assertEquals(modifyStakingParam.getWebSite(), staking.getWebSite());
        assertEquals(modifyStakingParam.getDetails(), staking.getDetails());
        assertEquals(modifyStakingParam.getIsInit(), staking.getIsInit().intValue());
        //node数据更新验证
        Node node = nodeMapper.selectByPrimaryKey(modifyStakingParam.getNodeId());
        assertEquals(modifyStakingParam.getNodeName(), node.getNodeName());
        assertEquals(modifyStakingParam.getExternalId(), node.getExternalId());
        assertEquals(modifyStakingParam.getBenefitAddr(), node.getBenefitAddr());
        assertEquals(modifyStakingParam.getDetails(), node.getDetails());
        assertEquals(modifyStakingParam.getWebSite(), node.getWebSite());
        assertEquals(modifyStakingParam.getIsInit(), node.getIsInit().intValue());
        //opt插入数据验证
        NOptBakExample nodeOptExample = new NOptBakExample();
        nodeOptExample.createCriteria().andNodeIdEqualTo(modifyStakingParam.getNodeId())
                .andBNumEqualTo(modifyStakingParam.getStakingBlockNum().longValue());
        List <NOptBak> nodeOptList = nOptBakMapper.selectByExample(nodeOptExample);
        assertEquals(modifyStakingParam.getNodeId(), nodeOptList.get(0).getNodeId());
    }

    @Test
    public void addStakingMapper () {
        StakeIncrease addStakingParam = addStakingParam();
        Staking stakingAfter = getStaking(addStakingParam.getNodeId(), addStakingParam.getStakingBlockNum().longValue());
        Node nodeAfter = nodeMapper.selectByPrimaryKey(addStakingParam.getNodeId());

        stakeBusinessMapper.increase(addStakingParam);
        //staking数据更新验证
        Staking staking = getStaking(addStakingParam.getNodeId(), addStakingParam.getStakingBlockNum().longValue());
        assertEquals(stakingAfter.getStakingHes(), staking.getStakingHes().subtract(addStakingParam.getAmount()));
        //node数据更新验证

        Node nodeBefore = nodeMapper.selectByPrimaryKey(addStakingParam.getNodeId());
        assertEquals(nodeAfter.getTotalValue().longValue(), nodeBefore.getTotalValue().subtract(addStakingParam.getAmount()).longValue());
        assertEquals(nodeAfter.getStakingHes().longValue(), nodeBefore.getStakingHes().subtract(addStakingParam.getAmount()).longValue());

        //opt插入数据验证
        NOptBakExample nodeOptExample = new NOptBakExample();
        nodeOptExample.createCriteria().andNodeIdEqualTo(addStakingParam.getNodeId())
                .andBNumEqualTo(addStakingParam.getStakingBlockNum().longValue());
        List <NOptBak> nodeOptList = nOptBakMapper.selectByExample(nodeOptExample);
        assertEquals(addStakingParam.getNodeId(), nodeOptList.get(0).getNodeId());
    }

    @Test
    public void withdrewStakingMapper () {
        StakeExit withdrewStakingParam = withdrewStakingParam();
        stakeBusinessMapper.exit(withdrewStakingParam);
        //delegation数据更新验证
        DelegationKey delegationKey = new Delegation();
        delegationKey.setNodeId(withdrewStakingParam.getNodeId());
        delegationKey.setStakingBlockNum(withdrewStakingParam.getStakingBlockNum().longValue());
        Delegation delegation = delegationMapper.selectByPrimaryKey(delegationKey);
        assertEquals(delegation.getDelegateHes().longValue(), 0);
        assertEquals(delegation.getDelegateLocked().longValue(), 0);
        assertEquals(delegation.getDelegateReleased().longValue(), 0);
        //node数据更新验证
        Node node = nodeMapper.selectByPrimaryKey(withdrewStakingParam.getNodeId());
        assertEquals(withdrewStakingParam.getStakingReductionEpoch(), node.getStakingReductionEpoch().intValue());
        //staking数据验证
        Staking staking = getStaking(withdrewStakingParam.getNodeId(), withdrewStakingParam.getStakingBlockNum().longValue());
        assertEquals(withdrewStakingParam.getStakingReductionEpoch(), staking.getStakingReductionEpoch().intValue());
        //opt数据验证
        NOptBakExample nodeOptExample = new NOptBakExample();
        nodeOptExample.createCriteria().andNodeIdEqualTo(withdrewStakingParam.getNodeId())
                .andBNumEqualTo(withdrewStakingParam.getStakingBlockNum().longValue());
        List <NOptBak> nodeOptList = nOptBakMapper.selectByExample(nodeOptExample);
        assertEquals(withdrewStakingParam.getNodeId(), nodeOptList.get(0).getNodeId());
    }

    @Test
    public void reportDuplicateSignMapper () {
        Report reportDuplicateSignParam = reportDuplicateSignParam();
        slashBusinessMapper.report(reportDuplicateSignParam);
        //node更新数据验证
        Node node = nodeMapper.selectByPrimaryKey(reportDuplicateSignParam.getNodeId());
        assertEquals(node.getStatus().intValue(), reportDuplicateSignParam.getCodeStatus());
        assertEquals(node.getStakingReductionEpoch().intValue(), reportDuplicateSignParam.getSettingEpoch());
        assertEquals(node.getStakingReduction(), reportDuplicateSignParam.getCodeCurStakingLocked());
        //staking更新数据验证
        Staking staking = getStaking(reportDuplicateSignParam.getNodeId(), reportDuplicateSignParam.getStakingBlockNum().longValue());
        assertEquals(staking.getStatus().intValue(), reportDuplicateSignParam.getCodeStatus());
        assertEquals(staking.getStakingReductionEpoch().intValue(), reportDuplicateSignParam.getSettingEpoch());
        assertEquals(staking.getStakingReduction(), reportDuplicateSignParam.getCodeCurStakingLocked());
        //slash插入数据验证
        Slash slash = slashMapper.selectByPrimaryKey(reportDuplicateSignParam.getTxHash());
        assertTrue(!StringUtils.isEmpty(slash));
        //opt插入验证
        NOptBakExample nodeOptExample = new NOptBakExample();
        nodeOptExample.createCriteria().andNodeIdEqualTo(reportDuplicateSignParam.getNodeId())
                .andBNumEqualTo(reportDuplicateSignParam.getStakingBlockNum().longValue());
        List <NOptBak> nodeOptList = nOptBakMapper.selectByExample(nodeOptExample);
        assertEquals(reportDuplicateSignParam.getNodeId(), nodeOptList.get(0).getNodeId());
    }

    /**
     * 新增区块
     */
    @Test
    public void newBlockMapper () {
        NewBlock blockParam = blockParam();
        Staking stakingBefore = getStaking(blockParam.getNodeId(), blockParam.getStakingBlockNum().longValue());
        Node nodeBefore = nodeMapper.selectByPrimaryKey(blockParam.getNodeId());
        newBlockMapper.newBlock(blockParam);
        Staking stakingAfter = getStaking(blockParam.getNodeId(), blockParam.getStakingBlockNum().longValue());
        //staking更新数据验证
        assertEquals(stakingBefore.getBlockRewardValue(), stakingAfter.getBlockRewardValue().subtract(blockParam.getBlockRewardValue()));
        assertEquals(stakingBefore.getFeeRewardValue(), stakingAfter.getFeeRewardValue().subtract(blockParam.getFeeRewardValue()));
        //node更新数据验证
        Node nodeAfter = nodeMapper.selectByPrimaryKey(blockParam.getNodeId());
        assertEquals(nodeBefore.getStatBlockRewardValue(), nodeAfter.getStatBlockRewardValue().subtract(blockParam.getBlockRewardValue()));
        assertEquals(nodeBefore.getStatFeeRewardValue(), nodeAfter.getStatFeeRewardValue().subtract(blockParam.getFeeRewardValue()));
    }


    /**
     * 共识周期切换
     */
    @Test
    public void newConsensusEpochMapper () {
        Consensus newConsensusParam = consensusParam();
        epochBusinessMapper.consensus(newConsensusParam);
        StakingExample stakingExample = new StakingExample();
        stakingExample.createCriteria()
                .andStatusEqualTo(1);
        List <Staking> stakingList = stakingMapper.selectByExample(stakingExample);
        //staking更新数据验证
        assertEquals(0, stakingList.get(0).getCurConsBlockQty().longValue());
    }

    /**
     * 结算周期切换
     */
    @Test
    public void newReductionEpochMapper () {
        Settle settleParam = settleParam();
        epochBusinessMapper.settle(settleParam);
    }


    /**
     * 选举周期切换-查询待踢出验证人
     */
    @Test
    public void newElectionEpochMapperQuerySlashNode() {
        List<String> nodeList = electionQuerySlashNodeParam();
        epochBusinessMapper.querySlashNode(nodeList);
        
    }
    
    /**
     * 选举周期切换
     */
    @Test
    public void newElectionEpochMapper () {
        Election electionParam = electionSlashNodeParam();
        epochBusinessMapper.slashNode(electionParam);
    }

    /**
     * 创建委托
     */
    @Test
    public void delegationCreateMapper(){
        DelegateCreate delegateCreate = delegateCreateParam();
        delegateBusinessMapper.create(delegateCreate);
    }

    /**
     * 退出委托
     */
    @Test
    public void delegationExitMapper(){
        DelegateExit delegateExit = delegateExitParam();
        delegateBusinessMapper.exit(delegateExit);
    }

    public Staking getStaking ( String nodeId, long stakingBlockNumer ) {
        StakingKey stakingKey = new StakingKey();
        stakingKey.setNodeId(nodeId);
        stakingKey.setStakingBlockNum(stakingBlockNumer);
        Staking staking = stakingMapper.selectByPrimaryKey(stakingKey);
        return staking;
    }

    /**
     * 创建文本提案
     */
    @Test
    public void proposalTestMapper(){
        ProposalText proposalText = proposalTextParam();
        proposalBusinessMapper.text(proposalText);
    }

    /**
     * 创建升级提案
     */
    @Test
    public void proposalUpgradeMapper(){
        ProposalUpgrade proposalUpgradeOrCancel = proposalUpgradeOrCancelParam();
        proposalBusinessMapper.upgrade(proposalUpgradeOrCancel);
    }

    /**
     * 取消提案
     */
    @Test
    public void ProposalCancelMapper(){
        ProposalUpgrade proposalUpgradeOrCancel = proposalUpgradeOrCancelParam();
        proposalBusinessMapper.upgrade(proposalUpgradeOrCancel);
    }

    /**
     * 投票
     */
    @Test
    public void proposalVoteMapper(){
        ProposalVote proposalVote = proposalVoteParam();
        proposalBusinessMapper.vote(proposalVote);
    }

    /**
     * 地址数据统计
     */
    @Test
    public void addressChangeMapper(){
        AddressStatChange addressStatChange = addressStatChangeParam();
        statisticBusinessMapper.addressChange(addressStatChange);
    }

    /**
     * 创建锁仓计划
     */
    @Test
    public void restrcingCrateMapper(){
        RestrictingCreate restrictingCreate = restrictingCreateParam();
        restrictingBusinessMapper.create(restrictingCreate);
    }


     /**
     *  其他数据统计
     */
    @Test
    public void netWorkChangeMapper(){
        NetworkStat networkStatChange = networkStatChangeParam();
        statisticBusinessMapper.networkChange(networkStatChange);
    }

   /* @Test
    public void test(){
        List<Proposal> list = new ArrayList <>();
        Proposal proposal = new Proposal();
        proposal.setHash("0x09ffb5916c2f40f86ab3d395957fb6b0d5881be5e61fe20c408b4300a811f232");
        proposal.setTopic("inquiry1");
        list.add(proposal);
        Proposal proposal2 = new Proposal();
        proposal2.setHash("0x31d3ecb0e65e598c66d3d02cd7c8785c7f4405b59c31351089e86438d8b4f4c7");
        proposal2.setNays(1l);
        list.add(proposal2);
        customProposalMapper.updateProposalList(list);
    }*/
}