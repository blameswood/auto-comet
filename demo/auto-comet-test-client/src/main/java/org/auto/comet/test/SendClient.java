package org.auto.comet.test;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * @author xiaohanghu
 * */
public class SendClient {

	static String URL = "http://localhost:8080/auto-comet-demo/testConcurrent.do?method=sendMessageToAll";

	// static String URL =
	// "http://192.168.12.164:8080/auto-comet-demo/testConcurrent.do?method=sendMessageToAll";
	public static void main(String[] args) {
		 sendAndClose();
	}

	public static void sendMessage() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("message", "test");
		String result = HttpClientUtils.doPostMethod(URL, params, "UTF-8");
		System.out.println(result);
	}

	public static void sendAndClose() {
		String url = "http://localhost:8080/auto-comet-demo/testConcurrent.do?method=sendAndClose";

		Map<String, String> params = new HashMap<String, String>();
		params.put("message", "test");
		params.put("id", "user0");
		String result = HttpClientUtils.doPostMethod(url, params, "UTF-8");
		System.out.println(result);
	}

}
