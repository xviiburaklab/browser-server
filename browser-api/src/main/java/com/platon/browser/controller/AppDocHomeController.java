package com.platon.browser.controller;

import com.platon.browser.enums.I18nEnum;
import com.platon.browser.enums.RetEnum;
import com.platon.browser.exception.BusinessException;
import com.platon.browser.exception.ResponseException;
import com.platon.browser.now.service.HomeService;
import com.platon.browser.req.home.QueryNavigationRequest;
import com.platon.browser.res.BaseResp;
import com.platon.browser.res.home.*;
import com.platon.browser.util.I18nUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 *  首页模块Contract。定义使用方法
 *  @file AppDocHomeController.java
 *  @description 
 *	@author zhangrj
 *  @data 2019年8月31日
 */
@RestController
public class AppDocHomeController implements AppDocHome {

	@Autowired
    private I18nUtil i18n;
	
	@Autowired
    private HomeService homeService;
	
	@Override
	public BaseResp<QueryNavigationResp> queryNavigation(@Valid QueryNavigationRequest req) {
        try{
        	QueryNavigationResp queryNavigationResp = homeService.queryNavigation(req);
            return BaseResp.build(RetEnum.RET_SUCCESS.getCode(),i18n.i(I18nEnum.SUCCESS),queryNavigationResp);
        }catch (BusinessException be){
            throw new ResponseException(be.getErrorMessage());
        }
	}

	@Override
	public BaseResp<BlockStatisticNewResp> blockStatisticNew() {
		BlockStatisticNewResp blockStatisticNewResp = homeService.blockStatisticNew();
		return BaseResp.build(RetEnum.RET_SUCCESS.getCode(),i18n.i(I18nEnum.SUCCESS),blockStatisticNewResp);
	}

	@Override
	public BaseResp<ChainStatisticNewResp> chainStatisticNew() {
		ChainStatisticNewResp chainStatisticNewResp = homeService.chainStatisticNew();
		return BaseResp.build(RetEnum.RET_SUCCESS.getCode(),i18n.i(I18nEnum.SUCCESS),chainStatisticNewResp);
	}

	@Override
	public BaseResp<List<BlockListNewResp>> blockListNew() {
		List<BlockListNewResp> lists = homeService.blockListNew();
		/**
		 * 第一次返回都设为true
		 */
		if(lists !=null && lists.size() > 0) {
			lists.get(0).setIsRefresh(true);
		}
		return BaseResp.build(RetEnum.RET_SUCCESS.getCode(),i18n.i(I18nEnum.SUCCESS),lists);
	}

	@Override
	public BaseResp<StakingListNewResp> stakingListNew() {
		StakingListNewResp stakingListNewResp = homeService.stakingListNew();
		/**
		 * 第一次返回都设为true
		 */
		stakingListNewResp.setIsRefresh(true);
		return BaseResp.build(RetEnum.RET_SUCCESS.getCode(),i18n.i(I18nEnum.SUCCESS),stakingListNewResp);
	}

}
