package com.platon.browser.common.complement.cache;

import com.platon.browser.dao.entity.Proposal;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 参数提案缓存
 * 缓存结构: Map<提案生效块号,List<参数提案ID>>
 */
@Component
public class ParamProposalCache {
    private static final Map<Long, Set<String>> cache = new HashMap<>();

    public void add(Long activeBlockNumber,String proposalId)
    {
        Set<String> proposalIdList = cache.computeIfAbsent(activeBlockNumber, k -> new HashSet<>());
        proposalIdList.add(proposalId);
    }

    public Set<String> get(Long activeBlockNumber){
        return cache.get(activeBlockNumber);
    }

    public void init(List<Proposal> proposalList) {
        if(proposalList.isEmpty()) return;
        proposalList.forEach(p->add(p.getActiveBlock(),p.getPipId()));
    }
}
