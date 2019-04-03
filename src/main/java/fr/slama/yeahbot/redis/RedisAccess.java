package fr.slama.yeahbot.redis;

import fr.slama.yeahbot.YeahBot;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 16/11/2018.
 */
public class RedisAccess {

    private static final Logger logger = LoggerFactory.getLogger(RedisAccess.class);
    static RedisAccess INSTANCE;
    private RedissonClient redissonClient;

    private RedisAccess(RedisCredentials credentials) {
        INSTANCE = this;
        this.redissonClient = initRedisson(credentials);
    }

    public static void init() {
        logger.info("Connection to Redis...");
        new RedisAccess(new RedisCredentials(YeahBot.CONFIG.redis.host, YeahBot.CONFIG.redis.password, YeahBot.CONFIG.redis.port));
    }

    public static void close() {
        logger.info("Closing Redis connection...");
        RedisAccess.INSTANCE.getRedissonClient().shutdown();
    }

    private RedissonClient initRedisson(RedisCredentials credentials) {
        final Config config = new Config();

        config.setCodec(new JsonJacksonCodec());
        config.setThreads(2);
        config.setNettyThreads(2);
        config.useSingleServer()
                .setAddress(credentials.toRedisURL())
                .setPassword(credentials.getPassword())
                .setDatabase(0)
                .setClientName(credentials.getClientName());

        logger.info("Successfully connected to Redis !");
        return Redisson.create(config);
    }

    RedissonClient getRedissonClient() {
        return redissonClient;
    }
}
