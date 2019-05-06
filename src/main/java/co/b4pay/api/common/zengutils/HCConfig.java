package co.b4pay.api.common.zengutils;

import co.b4pay.api.common.kit.Prop;
import co.b4pay.api.common.kit.PropKit;

/**
 * 获取配置信息工具类. 
 * @date 2015年12月29日
 */
public class HCConfig {	
	
	private static Prop config = PropKit.use("config.properties");
//	private static Prop dbConfig = PropKit.use("db.properties");
	
	public static String get(String key){
		return config.get(key);
	}
	
	public static boolean getBoolean(String key){
		return config.getBoolean(key);
	}
	public static boolean getBoolean(String key,Boolean defaultValue){
		return config.getBoolean(key,defaultValue);
	}
	
//	public static boolean getDBBoolean(String key){
//		return dbConfig.getBoolean(key);
//	}
	
	public static int getInt(String key){
		return config.getInt(key);
	}
	
	public static long getLong(String key){
		return config.getLong(key);
	}
	
//	public static boolean getDBBoolean(String key,Boolean defaultValue){
//		return dbConfig.getBoolean(key,defaultValue);
//	}
//	public static String getDB(String key){
//		return dbConfig.get(key);
//	}
//	public static int getDbInt(String key,int defaultValue){
//		return dbConfig.getInt(key,defaultValue);
//	}
	
}
