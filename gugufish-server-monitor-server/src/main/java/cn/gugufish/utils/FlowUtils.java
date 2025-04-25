package cn.gugufish.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 限流通用工具
 * 针对于不同的情况进行限流操作，支持限流升级
 */
@Slf4j
@Component
public class FlowUtils {

    @Resource
    StringRedisTemplate template;

    /**
     * 针对于单次频率限制，请求成功后，在冷却时间内不得再次进行请求，如3秒内不能再次发起请求
     * @param key 键
     * @param blockTime 限制时间
     * @return 是否通过限流检查
     */
    public boolean limitOnceCheck(String key, int blockTime){
        return this.internalCheck(key, 1, blockTime, (overclock) -> false);
    }

    /**
     * 针对于单次频率限制，请求成功后，在冷却时间内不得再次进行请求
     * 如3秒内不能再次发起请求，如果不听劝阻继续发起请求，将限制更长时间
     * @param key 键
     * @param frequency 请求频率
     * @param baseTime 基础限制时间
     * @param upgradeTime 升级限制时间
     * @return 是否通过限流检查
     */
    public boolean limitOnceUpgradeCheck(String key, int frequency, int baseTime, int upgradeTime){
        return this.internalCheck(key, frequency, baseTime, (overclock) -> {
                    if (overclock)
                        template.opsForValue().set(key, "1", upgradeTime, TimeUnit.SECONDS);
                    return false;
                });
    }

    /**
     * 针对于在时间段内多次请求限制，如3秒内限制请求20次，超出频率则封禁一段时间
     * @param counterKey 计数键
     * @param blockKey 封禁键
     * @param blockTime 封禁时间
     * @param frequency 请求频率
     * @param period 计数周期
     * @return 是否通过限流检查
     */
    public boolean limitPeriodCheck(String counterKey, String blockKey, int blockTime, int frequency, int period){
        return this.internalCheck(counterKey, frequency, period, (overclock) -> {
                    if (overclock)
                        template.opsForValue().set(blockKey, "", blockTime, TimeUnit.SECONDS);
                    return !overclock;
                });
    }

    /**
     * 内部使用的限流检查核心方法
     * 该方法实现了基于Redis的计数器限流算法，支持多种限流策略
     * 
     * @param key 在Redis中用于计数的键名，包含用户标识（如IP地址）
     * @param frequency 允许的最大请求频率/次数
     * @param period 计数周期，单位为秒，在该周期内累计请求次数
     * @param action 限流策略回调接口，根据是否超出频率限制执行不同操作
     * @return 是否通过限流检查，true表示允许请求，false表示请求被限制
     */
    private boolean internalCheck(String key, int frequency, int period, LimitAction action){
        // 从Redis中获取当前计数值
        String count = template.opsForValue().get(key);
        if (count != null) {
            // 键已存在，表示不是首次请求，将计数器值加1
            // 使用increment原子操作确保并发安全，如果操作失败则返回0
            long value = Optional.ofNullable(template.opsForValue().increment(key)).orElse(0L);
            int c = Integer.parseInt(count);
            // 如果返回值不等于预期值(c+1)，说明可能发生了异常情况
            // 重新设置过期时间，确保计数器会在period秒后过期
            if(value != c + 1)
                template.expire(key, period, TimeUnit.SECONDS);
            // 调用限流策略接口，根据是否超出频率限制执行对应操作
            // 将value > frequency作为参数传入，表示是否超出限制
            return action.run(value > frequency);
        } else {
            // 键不存在，表示是首次请求，设置初始计数为1，并设置过期时间
            template.opsForValue().set(key, "1", period, TimeUnit.SECONDS);
            // 首次请求默认通过限流检查
            return true;
        }
    }

    /**
     * 内部使用，限制行为与策略
     */
    private interface LimitAction {
        boolean run(boolean overclock);
    }
}
