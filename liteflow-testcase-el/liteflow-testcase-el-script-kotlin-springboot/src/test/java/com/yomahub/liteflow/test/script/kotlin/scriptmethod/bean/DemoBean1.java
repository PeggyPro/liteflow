package com.yomahub.liteflow.test.script.kotlin.scriptmethod.bean;

import com.yomahub.liteflow.script.annotation.ScriptMethod;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class DemoBean1 {

	@Resource
	private DemoBean2 demoBean2;

	@ScriptMethod("demo")
	public String getDemoStr1() {
		return "hello";
	}

	@ScriptMethod("demo2")
	public String getDemoStr2(String name) {
		return demoBean2.getDemoStr2(name);
	}

}
