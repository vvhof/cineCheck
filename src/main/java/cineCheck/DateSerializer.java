package cineCheck;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class DateSerializer implements JsonSerializer<Date> {
	protected static final String TAG = DateSerializer.class.getName();

	public JsonElement serialize(Date src, Type typeOfSrc,
			JsonSerializationContext context) {
		if (src != null){
			return new JsonPrimitive(new SimpleDateFormat().format(src));
		}
		return null;
	}
	
}
