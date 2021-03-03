package com.xxxx.seckill.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wf.captcha.ArithmeticCaptcha;
import com.xxxx.seckill.config.AccessLimit;
import com.xxxx.seckill.exception.GlobalException;
import com.xxxx.seckill.pojo.Order;
import com.xxxx.seckill.pojo.SeckillMessage;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.rabbitmq.MQSender;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IOrderService;
import com.xxxx.seckill.service.ISeckillOrderService;
import com.xxxx.seckill.utils.JsonUtil;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.RespBean;
import com.xxxx.seckill.vo.RespBeanEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀
 * <p>
 * 乐字节：专注线上IT培训
 * 答疑老师微信：lezijie
 *
 * @author zhoubin
 * @since 1.0.0
 */
@Slf4j
@Controller
@RequestMapping("/seckill")
public class SecKillController implements InitializingBean {

	@Autowired
	private IGoodsService goodsService;
	@Autowired
	private ISeckillOrderService seckillOrderService;
	@Autowired
	private IOrderService orderService;
	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private MQSender mqSender;
	@Autowired
	private RedisScript<Long> script;

	private Map<Long, Boolean> EmptyStockMap = new HashMap<>();


	/**
	 * 功能描述: 秒杀
	 * windows优化前QPS：785
	 * Linux优化前QPS：170
	 *
	 * @param:
	 * @return: 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	@RequestMapping("/doSeckill2")
	public String doSeckill2(Model model, User user, Long goodsId) {
		if (user == null) {
			return "login";
		}
		model.addAttribute("user", user);
		GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
		//判断库存
		if (goods.getStockCount() < 1) {
			model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
			return "secKillFail";
		}
		//判断是否重复抢购
		SeckillOrder seckillOrder =
				seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().eq("user_id", user.getId()).eq("goods_id",
						goodsId));
		if (seckillOrder != null) {
			model.addAttribute("errmsg", RespBeanEnum.REPEATE_ERROR.getMessage());
			return "secKillFail";
		}
		Order order = orderService.seckill(user, goods);
		model.addAttribute("order", order);
		model.addAttribute("goods", goods);
		return "orderDetail";
	}


	/**
	 * 功能描述: 秒杀
	 * windows优化前QPS：785
	 * 缓存QPS：1356
	 * 优化QPS：2454
	 *
	 * @param:
	 * @return: 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	@RequestMapping(value = "/{path}/doSeckill", method = RequestMethod.POST)
	@ResponseBody
	public RespBean doSeckill(@PathVariable String path, User user, Long goodsId) {
		if (user == null) {
			return RespBean.error(RespBeanEnum.SESSION_ERROR);
		}
		ValueOperations valueOperations = redisTemplate.opsForValue();
		boolean check = orderService.checkPath(user, goodsId, path);
		if (!check) {
			return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
		}
		//判断是否重复抢购
		SeckillOrder seckillOrder =
				(SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
		if (seckillOrder != null) {
			return RespBean.error(RespBeanEnum.REPEATE_ERROR);
		}
		//内存标记，减少Redis的访问
		if (EmptyStockMap.get(goodsId)) {
			return RespBean.error(RespBeanEnum.EMPTY_STOCK);
		}
		//预减库存
		// Long stock = valueOperations.decrement("seckillGoods:" + goodsId);
		Long stock = (Long) redisTemplate.execute(script, Collections.singletonList("seckillGoods:" + goodsId),
				Collections.EMPTY_LIST);
		if (stock < 0) {
			EmptyStockMap.put(goodsId, true);
			valueOperations.increment("seckillGoods:" + goodsId);
			return RespBean.error(RespBeanEnum.EMPTY_STOCK);
		}
		SeckillMessage seckillMessage = new SeckillMessage(user, goodsId);
		mqSender.sendSeckillMessage(JsonUtil.object2JsonStr(seckillMessage));
		return RespBean.success(0);


		/*
		GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
		//判断库存
		if (goods.getStockCount() < 1) {
			return RespBean.error(RespBeanEnum.EMPTY_STOCK);
		}
		//判断是否重复抢购
		// SeckillOrder seckillOrder =
		// 		seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().eq("user_id", user.getId()).eq("goods_id",
		// 				goodsId));
		SeckillOrder seckillOrder =
				(SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
		if (seckillOrder != null) {
			return RespBean.error(RespBeanEnum.REPEATE_ERROR);
		}
		Order order = orderService.seckill(user, goods);
		return RespBean.success(order);
		 */
	}


	/**
	 * 功能描述: 获取秒杀结果
	 *
	 * @param:
	 * @return: orderId:成功，-1：秒杀失败，0：排队中
	 * <p>
	 * 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	@RequestMapping(value = "/result", method = RequestMethod.GET)
	@ResponseBody
	public RespBean getResult(User user, Long goodsId) {
		if (user == null) {
			return RespBean.error(RespBeanEnum.SESSION_ERROR);
		}
		Long orderId = seckillOrderService.getResult(user, goodsId);
		return RespBean.success(orderId);
	}


	/**
	 * 功能描述: 获取秒杀地址
	 *
	 * @param:
	 * @return: 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	@AccessLimit(second = 5, maxCount = 5, needLogin = true)
	@RequestMapping(value = "/path", method = RequestMethod.GET)
	@ResponseBody
	public RespBean getPath(User user, Long goodsId, String captcha, HttpServletRequest request) {
		if (user == null) {
			return RespBean.error(RespBeanEnum.SESSION_ERROR);
		}
		boolean check = orderService.checkCaptcha(user, goodsId, captcha);
		if (!check) {
			return RespBean.error(RespBeanEnum.ERROR_CAPTCHA);
		}
		String str = orderService.createPath(user, goodsId);
		return RespBean.success(str);
	}


	@RequestMapping(value = "/captcha", method = RequestMethod.GET)
	public void verifyCode(User user, Long goodsId, HttpServletResponse response) {
		if (user == null || goodsId < 0) {
			throw new GlobalException(RespBeanEnum.REQUEST_ILLEGAL);
		}
		//设置请求头为输出图片的类型
		response.setContentType("image/jpg");
		response.setHeader("Pargam", "No-cache");
		response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires", 0);
		//生成验证码，将结果放入Redis
		ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32, 3);
		redisTemplate.opsForValue().set("captcha:" + user.getId() + ":" + goodsId, captcha.text(), 300,
				TimeUnit.SECONDS);
		try {
			captcha.out(response.getOutputStream());
		} catch (IOException e) {
			log.error("验证码生成失败", e.getMessage());
		}
	}


	/**
	 * 系统初始化，把商品库存数量加载到Redis
	 *
	 * @throws Exception
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		List<GoodsVo> list = goodsService.findGoodsVo();
		if (CollectionUtils.isEmpty(list)) {
			return;
		}
		list.forEach(goodsVo -> {
					redisTemplate.opsForValue().set("seckillGoods:" + goodsVo.getId(), goodsVo.getStockCount());
					EmptyStockMap.put(goodsVo.getId(), false);
				}
		);
	}
}