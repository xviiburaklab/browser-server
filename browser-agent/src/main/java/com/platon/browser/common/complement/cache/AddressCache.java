package com.platon.browser.common.complement.cache;

import com.platon.browser.common.enums.AddressTypeEnum;
import com.platon.browser.complement.dao.param.delegate.DelegateExit;
import com.platon.browser.complement.dao.param.delegate.DelegateRewardClaim;
import com.platon.browser.dao.entity.Address;
import com.platon.browser.elasticsearch.dto.Transaction;
import com.platon.browser.enums.ContractDescEnum;
import com.platon.browser.enums.InnerContractAddrEnum;
import com.platon.browser.param.claim.Reward;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 地址统计缓存
 */
@Component
public class AddressCache {
    private Map<String,Address> addressMap = new HashMap<>();
	/**
	 * 普通合约地址缓存
	 */
	private Set<String> generalContractAddressCache = new HashSet<>();
	public Set<String> getGeneralContractAddressCache(){return generalContractAddressCache;}
	public boolean isGeneralContractAddress(String address){
		return generalContractAddressCache.contains(address);
	}
    
    public void update(Transaction tx) {
    	String from = tx.getFrom();
    	String to = tx.getTo();
    	String contractAddress = tx.getContractAddress();
    	updateAddress(tx,from,false);
		updateAddress(tx,to,false);
		updateAddress(tx,contractAddress,true);

    }
    
    public Collection<Address> getAll(){
    	return addressMap.values();
    }
    
    public void cleanAll() {
    	addressMap.clear();
    }
    
    private void updateAddress(Transaction tx, String addr,boolean isContractCreateAddress) {
    	if(addr==null) return;
    	Address address = addressMap.get(addr);
    	if(address == null) {
    		address = createDefaultAddress(addr);
    		addressMap.put(addr, address);
    	}

		address.setTxQty(address.getTxQty() + 1);
		switch (tx.getTypeEnum()){
		    case TRANSFER: // 转账交易
			 address.setTransferQty(address.getTransferQty() + 1);
		    break;
		case STAKE_CREATE:// 创建验证人
		case STAKE_INCREASE:// 增加自有质押
		case STAKE_MODIFY:// 编辑验证人
		case STAKE_EXIT:// 退出验证人
		case REPORT:// 举报验证人
			 address.setStakingQty(address.getStakingQty()+1);
		    break;
		case DELEGATE_CREATE:// 发起委托
		case DELEGATE_EXIT:// 撤销委托
			case CLAIM_REWARDS:// 领取委托奖励
			 address.setDelegateQty(address.getDelegateQty()+1);
		    break;
		case PROPOSAL_TEXT:// 创建文本提案
		case PROPOSAL_UPGRADE:// 创建升级提案
		case PROPOSAL_PARAMETER:// 创建参数提案
		case PROPOSAL_VOTE:// 提案投票
		case PROPOSAL_CANCEL:// 取消提案
		case VERSION_DECLARE:// 版本声明
		   	 address.setProposalQty(address.getProposalQty()+1);
		   	 break;
		case CONTRACT_CREATE:
			if(isContractCreateAddress){
				// 如果地址是创建合约的回执里返回的合约地址
				address.setContractCreatehash(tx.getHash());
				address.setContractCreate(tx.getFrom());
				// 覆盖createDefaultAddress()中设置的值
				address.setType(AddressTypeEnum.CONTRACT.getCode());
				address.setContractBin(tx.getBin());
				generalContractAddressCache.add(addr);
			}
			break;
		    default:
		}
    }
    
    private Address createDefaultAddress(String addr) {
    	Address address = new Address();
    	address.setAddress(addr);
        // 设置地址类型
        if(InnerContractAddrEnum.getAddresses().contains(addr)){
            // 内置合约地址
        	address.setType(AddressTypeEnum.INNER_CONTRACT.getCode());
        }else{
            // 先默认置为账户地址，具体是什么类型，由调用此方法的后续逻辑决定并设置
        	address.setType(AddressTypeEnum.ACCOUNT.getCode());
        }
        
        ContractDescEnum cde = ContractDescEnum.getMap().get(addr);
        if(cde!=null){
        	address.setContractName(cde.getContractName());
        	address.setContractCreate(cde.getCreator());
        	address.setContractCreatehash(cde.getContractHash());
        } else {
        	address.setContractName("");
        	address.setContractCreate("");
        	address.setContractCreatehash("");
		}    	
    	
        address.setTxQty(0);
        address.setTransferQty(0);
        address.setStakingQty(0);
        address.setDelegateQty(0);
        address.setProposalQty(0);
        address.setHaveReward(BigDecimal.ZERO);
    	return address;
    }

	/**
	 * 初始化
	 * @param addressList 地址实体列表
	 */
	public void initGeneralContractAddressCache(List<Address> addressList) {
		if(addressList.isEmpty()) return;
		generalContractAddressCache.clear();
		addressList.forEach(address -> {
			if(address.getType()==AddressTypeEnum.CONTRACT.getCode()){
				generalContractAddressCache.add(address.getAddress());
			}
		});
	}
	
	/**
	 * 第一次启动初始化
	 */
	public void initOnFirstStart() {
		for(ContractDescEnum contractDescEnum : ContractDescEnum.values()) {
			addressMap.put(contractDescEnum.getAddress(), createDefaultAddress(contractDescEnum.getAddress()));
		}
	}

	/**
	 * 领取奖励交易更新已领取的委托奖励字段
	 * @param drc
	 */
	public void update(DelegateRewardClaim drc) {
		// 统计当前交易from地址的【已领取委托奖励】
		BigDecimal totalAmount = BigDecimal.ZERO;
		for (Reward reward : drc.getRewardList()) {
			totalAmount = totalAmount.add(reward.getReward());
		}
		update(drc.getAddress(),totalAmount);
	}

	/**
	 * 撤销委托交易更新已领取的委托奖励字段
	 * @param de
	 */
    public void update(DelegateExit de) {
		update(de.getTxFrom(),de.getDelegateReward());
    }

    private void update(String address,BigDecimal amount){
		Address cache = addressMap.get(address);
		if(cache == null) {
			cache = createDefaultAddress(address);
			addressMap.put(address, cache);
		}
		cache.setHaveReward(cache.getHaveReward().add(amount));
	}
}
