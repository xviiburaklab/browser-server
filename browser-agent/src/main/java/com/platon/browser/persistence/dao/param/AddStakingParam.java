package com.platon.browser.persistence.dao.param;

import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * @Auther: dongqile
 * @Date: 2019/10/31
 * @Description:
 */
@Data
public class AddStakingParam {

    /**
     * 节点Id
     */
    private String nodeId;

    /**
     * 增持金额
     */
    private BigDecimal amount;


    /**
     *  交易Hash
     */
    private String Hash;

    /**
     * 交易快高
     */
    private BigInteger bNum;

    /**
     * 质押交易所在块高
     */
    private BigInteger stakingBlockNum;

    /**
     * 时间
     */
    private Date time;


}