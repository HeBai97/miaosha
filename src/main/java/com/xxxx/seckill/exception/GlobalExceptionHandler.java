package com.xxxx.seckill.exception;

import com.xxxx.seckill.vo.RespBean;
import com.xxxx.seckill.vo.RespBeanEnum;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * 全局异常处理类
 * 乐字节：专注线上IT培训
 * 答疑老师微信：lezijie
 *
 * @author zhoubin
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(Exception.class)
	public RespBean ExceptionHandler(Exception e) {
		if (e instanceof GlobalException) {
			GlobalException ex = (GlobalException) e;
			return RespBean.error(ex.getRespBeanEnum());
		} else if (e instanceof BindException) {
			BindException ex = (BindException) e;
			RespBean respBean = RespBean.error(RespBeanEnum.BIND_ERROR);
			respBean.setMessage("参数校验异常：" + ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
			return respBean;
		}
		return RespBean.error(RespBeanEnum.ERROR);
	}

}