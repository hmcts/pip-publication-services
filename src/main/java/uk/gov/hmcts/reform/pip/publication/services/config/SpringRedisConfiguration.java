package uk.gov.hmcts.reform.pip.publication.services.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class SpringRedisConfiguration {
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("localhost", 6379);
    }

    @Bean
    public RedisTemplate<String, Integer> redisTemplate() {
        RedisTemplate<String, Integer> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new IntegerRedisSerializer());
        return template;
    }

    private class IntegerRedisSerializer implements RedisSerializer<Integer> {
        @Override
        public byte[] serialize(Integer value) {
            return value == null ? null : String.valueOf(value).getBytes();
        }

        @Override
        public Integer deserialize(byte[] bytes) throws SerializationException {
            try {
                return bytes == null ? null : Integer.valueOf(new String(bytes));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
