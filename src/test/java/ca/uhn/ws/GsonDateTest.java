package ca.uhn.ws;

import java.util.Date;

import junit.framework.TestCase;

import com.google.gson.reflect.TypeToken;

public class GsonDateTest  extends TestCase {
	
	public void testSerializeDate() {
		java.util.Date d1 = new java.util.Date(12311123231231l);
		String serialized = JsonService.gson.toJson(d1);
		java.util.Date d2 = JsonService.gson.fromJson(serialized, java.util.Date.class);
		
		assertEquals((d1.getTime() / 1000), (d2.getTime() / 1000));
	}
	
	public void testSerializeSqlDate() {
		@SuppressWarnings("deprecation")
		java.sql.Date date1 = new java.sql.Date(10,10,10);
		java.sql.Date date2 = new java.sql.Date(12321312l);
		java.util.Date date3 = new java.util.Date(12321312l);
		
		
		
		String date1Json = JsonService.gson.toJson(date1);
		String date2Json = JsonService.gson.toJson(date2);
		
		assertEquals(date1, JsonService.gson.fromJson(date1Json, new TypeToken<java.sql.Date>(){}.getType()));
		
		//the serialization percision is to seconds
		java.util.Date deserializedDate = JsonService.gson.fromJson(date2Json, new TypeToken<java.sql.Date>(){}.getType());
		assertEquals((date3.getTime() / 1000), (deserializedDate.getTime() / 1000));
	}

	
	public void testSerialieTimestamp() {
	
		java.sql.Timestamp t1 = new java.sql.Timestamp(12311123231231l);
		
		String serialized = JsonService.gson.toJson(t1);
		Date d1 = JsonService.gson.fromJson(serialized, java.util.Date.class);
		
		assertEquals((t1.getTime() / 1000), (d1.getTime() / 1000));
		
		
	}
}
