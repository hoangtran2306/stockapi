package com.finance24h.api;

import net.spy.memcached.MemcachedClient;

public class SpyMemcachedThread implements Runnable {
    private MemcachedClient memcachedClient;
    private int interval;
    
    public SpyMemcachedThread(MemcachedClient memcachedClient, int interval) {
		this.memcachedClient = memcachedClient;
		this.interval = interval;
	}

    @Override
    public void run() {
        try {
            while (true) {
            	try {
                    Thread.sleep(interval);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                memcachedClient.get("test");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
