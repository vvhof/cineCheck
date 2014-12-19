package cineCheck;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class DateDeserializer implements JsonDeserializer<Date> {
	
	protected static final String TAG = DateDeserializer.class.getName();

	public Date deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		String dateString;
		try {
			dateString = json.getAsString();
		} catch (Exception e) {
			return null;
		}
		if (dateString != null){
			Date date = null;
			try {
				date = new SimpleDateFormat("dd.mm.yyyy hh:mm").parse(dateString);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return date;
		}
		return null;
	}
	
}