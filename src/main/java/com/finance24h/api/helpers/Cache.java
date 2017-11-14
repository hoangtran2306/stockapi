package com.finance24h.api.helpers;

import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.spy.memcached.MemcachedClient;
import redis.clients.jedis.Jedis;

@Component
public class Cache {
	
	private static MemcachedClient memcachedClient;
	
	private static Jedis redisClient;
	
	static JsonParser jsonParser = new JsonParser();
	static LogStashHelper logger;
	
	// 300 -> 5 min
	// 3600 -> 1 hour
	// 86400 -> 1 day
	// 432000 -> 5 days
	// 2592000 -> 1 month
	
	public static int TIME_RELATED_ARTICLE = 300;
	
	public static int TIME_STOCK_STATISTICS = 86400;
	
	public static int TIME_COMPANY_INFO = 86400;
	
	public static int TIME_SCFF_DETAIL = 3600;
	
	public static int TIME_VNDC_DETAIL = 300;
	
	public static int TIME_GRAPH_DAILY = 300;
	
	public static int TIME_GRAPH_MONTHLY = 86400;
	
	public static int TIME_GRAPH_YEARLY = 432000;
	
	public static int TIME_COMMUNITY_POSTS = 300;
	
	public static int TIME_ALL_COMPANY = 86400;
	
	public static int TIME_ARTICLE_DETAIL = 3600;
	
	public static int TIME_NEWS_LIST = 300;
	
	public static int TIME_NEWS_HOME = 300;
	
	public static int TIME_FLOOR_HEADER = 300;
	
	public static int TIME_FLOOR_STATISTICS = 300;
	
	public static int TIME_FLOOR_INDEX = 2592000;
	
	public static int TIME_DEVICE = 300;
	
	
	public static void setMemcachedClient(MemcachedClient memcachedClient) {
		Cache.memcachedClient = memcachedClient;
	}
	
	public static void setRedisClient(Jedis redisClient) {
		Cache.redisClient = redisClient;
	}

	public static void setLogger(LogStashHelper logger) {
		Cache.logger = logger;
	}
	
	public static synchronized void set(String key, int time, Object val) {
		setRedis(key, time, val);
	}
	
	public static synchronized Object getObject(String key) {
		return getRedisObject(key);
	}
	
	public static synchronized JsonObject getJsonObject(String key) {
		return getRedisJsonObject(key);
	}
	
	public static synchronized JsonArray getJsonArray(String key) {
		return getRedisJsonArray(key);
	}

	public static synchronized void setMemcached(String key, int time, Object val) {
		try {
			if (val != null) {
				memcachedClient.set(key, time, val.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static synchronized Object getMemcachedObject(String key) {
		try {
			Object obj = memcachedClient.get(key);
			if (obj != null) {
				logger.logInfo("get from memcached key= " + key);
				return obj;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static synchronized JsonObject getMemcachedJsonObject(String key) {
		try {
			Object obj = memcachedClient.get(key);
			if (obj != null) {
				logger.logInfo("get from memcached key= " + key);
				return jsonParser.parse(obj.toString()).getAsJsonObject();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static synchronized JsonArray getMemcachedJsonArray(String key) {
		try {
			Object obj = memcachedClient.get(key);
			if (obj != null) {
				logger.logInfo("get from memcached key= " + key);
				return jsonParser.parse(obj.toString()).getAsJsonArray();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static synchronized void setRedis(String key, int time, Object val) {
		try {
			if (val != null) {
				redisClient.setex(key, time, val.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static synchronized Object getRedisObject(String key) {
		try {
			Object obj = redisClient.get(key);
			if (obj != null) {
				logger.logInfo("get from redis key= " + key);
				return obj;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static synchronized JsonObject getRedisJsonObject(String key) {
		try {
			Object obj = redisClient.get(key);
			if (obj != null) {
				logger.logInfo("get from redis key= " + key);
				return jsonParser.parse(obj.toString()).getAsJsonObject();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static synchronized JsonArray getRedisJsonArray(String key) {
		try {
			Object obj = redisClient.get(key);
			if (obj != null) {
				logger.logInfo("get from redis key= " + key);
				return jsonParser.parse(obj.toString()).getAsJsonArray();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
