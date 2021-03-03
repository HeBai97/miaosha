package com.xxxx.seckill.rabbitmq;

import com.xxxx.seckill.pojo.SeckillMessage;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IOrderService;
import com.xxxx.seckill.utils.JsonUtil;
import com.xxxx.seckill.vo.GoodsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 消息消费者
 * <p>
 * 乐字节：专注线上IT培训
 * 答疑老师微信：lezijie
 *
 * @author zhoubin
 * @since 1.0.0
 */
@Service
@Slf4j
public class MQReceiver {

	// @RabbitListener(queues = "queue")
	// public void receive(Object msg) {
	// 	log.info("接收消息：" + msg);
	// }
	//
	//
	// @RabbitListener(queues = "queue_fanout01")
	// public void receive01(Object msg) {
	// 	log.info("QUEUE01接收消息：" + msg);
	// }
	//
	// @RabbitListener(queues = "queue_fanout02")
	// public void receive02(Object msg) {
	// 	log.info("QUEUE02接收消息：" + msg);
	// }
	//
	//
	// @RabbitListener(queues = "queue_direct01")
	// public void receive03(Object msg) {
	// 	log.info("QUEUE01接收消息：" + msg);
	// }
	//
	// @RabbitListener(queues = "queue_direct02")
	// public void receive04(Object msg) {
	// 	log.info("QUEUE02接收消息：" + msg);
	// }
	//
	// @RabbitListener(queues = "queue_topic01")
	// private void receive05(Object msg) {
	// 	log.info("QUEUE01接收消息：" + msg);
	// }
	//
	// @RabbitListener(queues = "queue_topic02")
	// private void receive06(Object msg) {
	// 	log.info("QUEUE02接收消息：" + msg);
	// }
	//
	// @RabbitListener(queues = "queue_header01")
	// public void receive07(Message message) {
	// 	log.info("QUEUE01接收Message对象：" + message);
	// 	log.info("QUEUE01接收消息：" + new String(message.getBody()));
	// }
	//
	// @RabbitListener(queues = "queue_header02")
	// public void receive08(Message message) {
	// 	log.info("QUEUE02接收Message对象：" + message);
	// 	log.info("QUEUE02接收消息：" + new String(message.getBody()));
	// }

	@Autowired
	private IGoodsService goodsService;
	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private IOrderService orderService;

	/**
	 * 下单操作
	 */
	@RabbitListener(queues = "seckillQueue")
	public void receive(String message) {
		log.info("接收的消息：" + message);
		SeckillMessage seckillMessage = JsonUtil.jsonStr2Object(message, SeckillMessage.class);
		Long goodId = seckillMessage.getGoodId();
		User user = seckillMessage.getUser();
		//判断库存
		GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodId);
		if (goodsVo.getStockCount() < 1) {
			return;
		}
		//判断是否重复抢购
		SeckillOrder seckillOrder =
				(SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodId);
		if (seckillOrder != null) {
			return;
		}
		//下单操作
		orderService.seckill(user, goodsVo);
	}
}
