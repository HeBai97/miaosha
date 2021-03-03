package com.xxxx.seckill.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 测试
 *
 * 乐字节：专注线上IT培训
 * 答疑老师微信：lezijie
 * @author zhoubin
 * @since 1.0.0
 */
@Controller
@RequestMapping("/demo")
public class DemoController {


	/**
	 * 功能描述: 测试页面跳转
	 *
	 * @param:
	 * @return:
	 * 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author: zhoubin
	 */
	@RequestMapping("/hello")
	public String hello(Model model){
		model.addAttribute("name","xxxx");
		return "hello";
	}

}