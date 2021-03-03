package com.xxxx.seckill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxxx.seckill.pojo.Order;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.OrderDetailVo;

/**
 * <p>
 *  服务类
 * </p>
 *
 * 乐字节：专注线上IT培训
 * 答疑老师微信：lezijie
 *
 * @author zhoubin
 *
 */
public interface IOrderService extends IService<Order> {

	/**
	 * 功能描述: 秒杀
	 *
	 * @param:
	 * @return:
	 *
	 * 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	Order seckill(User user, GoodsVo goods);


	/**
	 * 功能描述: 订单详情
	 *
	 * @param:
	 * @return:
	 *
	 * 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	OrderDetailVo detail(Long orderId);

	/**
	 * 功能描述: 获取秒杀地址
	 *
	 * @param:
	 * @return:
	 *
	 * 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	String createPath(User user, Long goodsId);

	/**
	 * 校验秒杀地址
	 * @param user
	 * @param goodsId
	 * @param path
	 * @return
	 */
	boolean checkPath(User user, Long goodsId, String path);


	/**
	 * 功能描述: 校验验证码
	 *
	 * @param:
	 * @return:
	 *
	 * 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	boolean checkCaptcha(User user, Long goodsId, String captcha);
}
