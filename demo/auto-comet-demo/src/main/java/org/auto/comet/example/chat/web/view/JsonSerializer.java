package org.auto.comet.example.chat.web.view;

import java.text.DateFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 */
public class JsonSerializer implements ResourceSerializer {

	private Gson gson;

	{
		GsonBuilder gsonBuilder = new GsonBuilder();
		com.google.gson.JsonSerializer dateSerializer = new NumberDateSerializer();
		gsonBuilder.registerTypeAdapter(java.util.Date.class, dateSerializer);
		gsonBuilder.registerTypeAdapter(java.sql.Date.class, dateSerializer);
		gsonBuilder.setDateFormat(DateFormat.LONG);
		gson = gsonBuilder.create();
	}

	public String serialization(Object resource) {
		if (null == resource)
			return null;
		return gson.toJson(resource);
	}

}
