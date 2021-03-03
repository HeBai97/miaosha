package com.xxxx.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxxx.seckill.mapper.SeckillOrderMapper;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.ISeckillOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 * 乐字节：专注线上IT培训
 * 答疑老师微信：lezijie
 *
 * @author zhoubin
 */
@Service
public class SeckillOrderServiceImpl extends ServiceImpl<SeckillOrderMapper, SeckillOrder> implements ISeckillOrderService {

	@Autowired
	private SeckillOrderMapper seckillOrderMapper;
	@Autowired
	private RedisTemplate redisTemplate;

	/**
	 * 功能描述: 获取秒杀结果
	 *
	 * @param:
	 * @return:orderId:成功，-1：秒杀失败，0：排队中 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	@Override
	public Long getResult(User user, Long goodsId) {
		SeckillOrder seckillOrder = seckillOrderMapper.selectOne(new QueryWrapper<SeckillOrder>().eq("user_id",
				user.getId()).eq("goods_id",
				goodsId));
		if (null != seckillOrder) {
			return seckillOrder.getOrderId();
		} else if (redisTemplate.hasKey("isStockEmpty:" + goodsId)) {
			return -1L;
		} else {
			return 0L;
		}
	}
}
