package com.xxxx.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxxx.seckill.exception.GlobalException;
import com.xxxx.seckill.mapper.OrderMapper;
import com.xxxx.seckill.pojo.Order;
import com.xxxx.seckill.pojo.SeckillGoods;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IOrderService;
import com.xxxx.seckill.service.ISeckillGoodsService;
import com.xxxx.seckill.service.ISeckillOrderService;
import com.xxxx.seckill.utils.MD5Util;
import com.xxxx.seckill.utils.UUIDUtil;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.OrderDetailVo;
import com.xxxx.seckill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

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
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

	@Autowired
	private ISeckillGoodsService seckillGoodsService;
	@Autowired
	private OrderMapper orderMapper;
	@Autowired
	private ISeckillOrderService seckillOrderService;
	@Autowired
	private IGoodsService goodsService;
	@Autowired
	private RedisTemplate redisTemplate;

	/**
	 * 功能描述: 秒杀
	 *
	 * @param:
	 * @return: 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	@Transactional
	@Override
	public Order seckill(User user, GoodsVo goods) {
		ValueOperations valueOperations = redisTemplate.opsForValue();
		//秒杀商品表减库存
		SeckillGoods seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>().eq("goods_id",
				goods.getId()));
		seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);
		boolean result = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>().setSql("stock_count = " +
				"stock_count-1").eq(
				"goods_id", goods.getId()).gt("stock_count", 0));
		if (seckillGoods.getStockCount() < 1) {
			//判断是否还有库存
			valueOperations.set("isStockEmpty:" + goods.getId(), "0");
			return null;
		}
		//生成订单
		Order order = new Order();
		order.setUserId(user.getId());
		order.setGoodsId(goods.getId());
		order.setDeliveryAddrId(0L);
		order.setGoodsName(goods.getGoodsName());
		order.setGoodsCount(1);
		order.setGoodsPrice(seckillGoods.getSeckillPrice());
		order.setOrderChannel(1);
		order.setStatus(0);
		order.setCreateDate(new Date());
		orderMapper.insert(order);
		//生成秒杀订单
		SeckillOrder seckillOrder = new SeckillOrder();
		seckillOrder.setUserId(user.getId());
		seckillOrder.setOrderId(order.getId());
		seckillOrder.setGoodsId(goods.getId());
		seckillOrderService.save(seckillOrder);
		valueOperations.set("order:" + user.getId() + ":" + goods.getId(), seckillOrder);
		return order;
	}

	/**
	 * 功能描述: 订单详情
	 *
	 * @param:
	 * @return: 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	@Override
	public OrderDetailVo detail(Long orderId) {
		if (orderId == null) {
			throw new GlobalException(RespBeanEnum.ORDER_NOT_EXIST);
		}
		Order order = orderMapper.selectById(orderId);
		GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(order.getGoodsId());
		OrderDetailVo detail = new OrderDetailVo();
		detail.setOrder(order);
		detail.setGoodsVo(goodsVo);
		return detail;
	}


	/**
	 * 获取秒杀地址
	 *
	 * @param user
	 * @param goodsId
	 * @return
	 */
	@Override
	public String createPath(User user, Long goodsId) {
		String str = MD5Util.md5(UUIDUtil.uuid() + "123456");
		redisTemplate.opsForValue().set("seckillPath:" + user.getId() + ":" + goodsId, str, 60, TimeUnit.SECONDS);
		return str;
	}

	/**
	 * 功能描述:校验秒杀地址
	 *
	 * @param:
	 * @return: 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	@Override
	public boolean checkPath(User user, Long goodsId, String path) {
		if (user == null || goodsId < 0 || StringUtils.isEmpty(path)) {
			return false;
		}
		String redisPath = (String) redisTemplate.opsForValue().get("seckillPath:" + user.getId() + ":" + goodsId);
		return path.equals(redisPath);
	}

	/**
	 * 功能描述: 校验验证码
	 *
	 * @param:
	 * @return: 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	@Override
	public boolean checkCaptcha(User user, Long goodsId, String captcha) {
		if (StringUtils.isEmpty(captcha) || user == null || goodsId < 0) {
			return false;
		}
		String redisCaptcha = (String) redisTemplate.opsForValue().get("captcha:" + user.getId() + ":" + goodsId);
		return captcha.equals(redisCaptcha);
	}
}
