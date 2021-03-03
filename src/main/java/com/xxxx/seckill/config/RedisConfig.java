package com.xxxx.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * <p>
 * 乐字节：专注线上IT培训
 * 答疑老师微信：lezijie
 *
 * @author zhoubin
 * @since 1.0.0
 */
@Configuration
public class RedisConfig {

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		//key序列化
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		//value序列化
		redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		//hash类型 key序列化
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		//hash类型 value序列化
		redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
		//注入连接工厂
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		return redisTemplate;
	}


	// @Bean
	// public DefaultRedisScript<Boolean> script() {
	// 	DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
	// 	//lock.lua脚本位置和application.yml同级目录
	// 	redisScript.setLocation(new ClassPathResource("lock.lua"));
	// 	redisScript.setResultType(Boolean.class);
	// 	return redisScript;
	// }


	@Bean
	public DefaultRedisScript<Long> script() {
		DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
		//放在和application.yml 同层目录下
		redisScript.setLocation(new ClassPathResource("stock.lua"));
		redisScript.setResultType(Long.class);
		return redisScript;
	}

}