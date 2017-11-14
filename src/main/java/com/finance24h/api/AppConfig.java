package com.finance24h.api;

import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import com.finance24h.api.helpers.LogStashHelper;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.finance24h.api.helpers.Cache;
import com.finance24h.api.helpers.Utilities;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import redis.clients.jedis.Jedis;

@Configuration
public class AppConfig {

    @Value("${elasticsearch.host}")
    private String elasticsearchHost;
    @Value("${elasticsearch.port}")
    private int elasticsearchPort;
    @Value("${elasticsearch.table_predix}")
    private String tablePrefix;

    @Bean
    JestClient esClient() {
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder(String.format("http://%s:%d", elasticsearchHost, elasticsearchPort))
                .multiThreaded(true)
                .readTimeout(20000)
                .build());
        JestClient client = factory.getObject();
        Utilities.tablePrefix = tablePrefix == null ? "" : tablePrefix;
        return client;
    }

    @Bean
    BoolQueryBuilder boolQueryBuilder() {
        return new BoolQueryBuilder();
    }

//    @Value("${memcached.host}")
//    private String memcachedHost;
//    @Value("${memcached.port}")
//    private int memcachedPort;
//
//    @Bean
//    MemcachedClient memcachedClient() throws IOException {
//        ConnectionFactory connectionFactory = new ConnectionFactoryBuilder()
//                .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                .setOpTimeout(10000)
//                .setOpQueueMaxBlockTime(1000)
//                .build();
//        MemcachedClient memcached = new MemcachedClient(connectionFactory, AddrUtil.getAddresses(String.format("%s:%d", memcachedHost, memcachedPort)));
//        Runnable memcachedClient = new SpyMemcachedThread(memcached, 5000);
//        Thread memcachedClientThread = new Thread(memcachedClient);
//        memcachedClientThread.start();
//        String log = "Connected to memcached host " + memcachedHost + " port " + memcachedPort;
//        Cache.setMemcachedClient(memcached);
//        logger().logInfo(log);
//        return memcached;
//    }
    
    @Value("${redis.host}")
    private String redisHost;
    @Value("${redis.port}")
    private int redisPort;
    
    @Bean
    Jedis redisClient() throws IOException {
        Jedis jedis = new Jedis(redisHost, redisPort);
//        jedis.select(redisIndex);
        String log = "Connected to redis host " + redisHost + " port " + redisPort;
        Cache.setRedisClient(jedis);
        logger().logInfo(log);
        return jedis;
    }
    
    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("locate/labels");
        source.setUseCodeAsDefaultMessage(true);
        Utilities.messageSource = source;
        return source;
    }

    @Value("${log.path}")
    private String path;
    @Value("${log.access.file}")
    private String accessLogFile;
    @Value("${log.error.file}")
    private String errorLogFile;
    @Value("${log.max_size}")
    private int maxFileSize;
    @Bean
    public LogStashHelper logger() {
    		LogStashHelper logger = new LogStashHelper(path, accessLogFile, errorLogFile, maxFileSize);
    		Cache.setLogger(logger);
        return logger;
    }

    @Value("${locale}")
    private String locale;
    @Value("${timezone}")
    private String timeZone;
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        Utilities.locate = new Locale(locale);
        Utilities.timeZone = timeZone;
        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
    }
}
