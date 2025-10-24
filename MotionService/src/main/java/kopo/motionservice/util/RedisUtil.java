package kopo.motionservice.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    private static final Logger log = LoggerFactory.getLogger(RedisUtil.class);
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void set(String key, String value, long timeoutSeconds) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(key, value, timeoutSeconds, TimeUnit.SECONDS);
    }

    public String get(String key) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        return ops.get(key);
    }

    public void delete(String key) {
        Boolean result = redisTemplate.delete(key);
        if (result == null) {
            log.warn("[RedisUtil] key 삭제 결과 알 수 없음: {}", key);
        } else if (result) {
            log.info("[RedisUtil] key 삭제 성공: {}", key);
        } else {
            log.info("[RedisUtil] key 삭제 실패(존재하지 않음): {}", key);
        }
    }
}
