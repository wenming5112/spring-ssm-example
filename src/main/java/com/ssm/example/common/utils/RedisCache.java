package com.ssm.example.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * redis 缓存工具类
 *
 * @author ming
 * @version 1.0.0
 * @since 2019/8/22 17:27
 **/
@Slf4j
@Component
public class RedisCache {

    // 注： 这里不能用Autowired按类型装配注入,必须用@Resource
    // StringRedisTemplate默认采用的是String的序列化策略,
    // RedisTemplate默认采用的是JDK的序列化策略，保存的key和value都是采用此策略序列化保存的
    /**
     * 注入redis
     */
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 生成业务查询使用的缓存key , 带分页
     *
     * @param username 用户名
     * @param obj      业务相关命名
     * @return key
     */
    public String generateCachePageKey(String username, String obj, Integer pageNo, Integer pageSize) {
        return generateCacheKey(username, obj) + "_page[" + pageNo + "," + pageSize + "]";
    }

    /**
     * 生成业务查询使用的缓存key
     *
     * @param username 用户名
     * @param obj      业务相关命名
     * @return key
     */
    public String generateCacheKey(String username, String obj) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(obj)) {
            return null;
        }
        return username + "_" + obj;
    }

    /**
     * 指定缓存失效时间
     *
     * @param key  键
     * @param time 时间(秒)
     */
    private void expire(String key, long time) {
        if (time > 0) {
            redisTemplate.expire(key, time, TimeUnit.SECONDS);
        }
    }

    /**
     * 根据key 获取过期时间
     *
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 可以传一个值 或多个
     */
    public void delete(String... key) {
        redisTemplate.delete(Arrays.asList(key));
    }

    // ============================ String =============================

    /**
     * 获取字符串
     *
     * @param key key
     * @return String value
     */
    public String getString(String key) {
        Object obj = redisTemplate.opsForValue().get(key);
        return obj.toString();
    }

    /**
     * 保存字符串
     *
     * @param key   键
     * @param value 值
     */
    public void setString(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key        键
     * @param value      值
     * @param expireTime 时间(秒) time要大于0 如果time小于等于0 将设置无限期
     */
    public void setString(String key, Object value, long expireTime) {
        if (expireTime > 0) {
            redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
        } else {
            setString(key, value);
        }
    }

    // ============================ Object =============================

    /**
     * 获取对象
     *
     * @param key key
     * @return obj
     */
    public Object getObject(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     */
    public void setObject(String key, Object value, long time) {
        //JsonUtils.toJson
        if (time > 0) {
            redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
        } else {
            setObject(key, value);
        }
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     */
    public void setObject(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return long
     */
    public Long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Integer getNumber(String key, Integer expire) {
        ValueOperations<String, Object> opsfv = redisTemplate.opsForValue();
        if (!hasKey(key)) {
            RedisAtomicLong redisAtomicLong = new RedisAtomicLong(key, Objects.requireNonNull(redisTemplate.getConnectionFactory()));
            // 设置初值
            redisAtomicLong.set(0);
            // 多久之后过期.设置24s后过期
            redisAtomicLong.expire(expire, TimeUnit.HOURS);
            // 设置到什么时间过期.例：当前时间的24h后过期
            // redisAtomicLong.expireAt(new Date(System.currentTimeMillis() + 60 * 1000))
        }
        // 第二个参数为递增因子
        return Objects.requireNonNull(opsfv.increment(key,1L)).intValue();
    }

    public Integer getIncrement(String key) {
        return Integer.valueOf(Objects.requireNonNull(redisTemplate.boundValueOps(key).get(0, -1)));
    }

    public void zAdd(String queueName, String value, int expire) {
        Calendar calendar = Calendar.getInstance();
        // default 30min
        calendar.add(Calendar.MINUTE, expire);
        redisTemplate.opsForZSet().add(queueName, value, calendar.getTimeInMillis());
    }

    public void consume(String queueName) {
        while (true) {
            Set<Object> set = redisTemplate.opsForZSet().rangeByScore(queueName, 0, System.currentTimeMillis(), 0, 1);
            if (set == null || set.isEmpty()) {
                try {
                    //log.info("没有任务");
                    TimeUnit.MICROSECONDS.sleep(500);
                } catch (InterruptedException e) {
                    log.error("InterruptedException", e);
                }
                continue;
            }
            String orderId = (String) set.iterator().next();
            if (redisTemplate.opsForZSet().remove(queueName, orderId) > 0) {
                log.info("order id:{} handle success", orderId);
                // 看是不是取完了
                long length = redisTemplate.opsForZSet().size(queueName);
                log.info("[consume]{} length:{}", queueName, length);
            }
        }
    }

    public void consume2(String queueName) {
        Executors.newSingleThreadExecutor().submit((Runnable) () -> {
            while (true) {
                try {
                    Set<Object> taskIdSet = redisTemplate.opsForZSet().rangeByScore(queueName, 0, System.currentTimeMillis(), 0, 1);
                    if (taskIdSet == null || taskIdSet.isEmpty()) {
                        log.info("没有任务");
                    } else {
                        taskIdSet.forEach(value -> {
                            long result = redisTemplate.opsForZSet().remove(queueName, value);
                            // todo: 执行检查逻辑，判断是否需要升级
                            if (result > 0) {
                                log.info("从延时队列中获取到任务，服务单ID:" + value + " , 当前时间：" + LocalDateTime.now());
                            }
                        });
                    }
                } catch (Exception e) {
                    // redis 操作的地方需要进行try catch处理，防止发生异常导致线程中断
                }
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几(小于0)
     * @return long
     */
    public Long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }

    // ================================ Map =================================

    /**
     * HashGet
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     */
    public void hmset(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * HashSet 并设置时间
     *
     * @param key  键
     * @param map  对应多个键值
     * @param time 时间(秒)
     */
    public void hmset(String key, Map<String, Object> map, long time) {
        redisTemplate.opsForHash().putAll(key, map);
        if (time > 0) {
            expire(key, time);
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     */
    public void hset(String key, String item, Object value) {
        redisTemplate.opsForHash().put(key, item, value);
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
     */
    public void hset(String key, String item, Object value, long time) {
        redisTemplate.opsForHash().put(key, item, value);
        if (time > 0) {
            expire(key, time);
        }
    }

    /**
     * 删除hash表中的值
     *
     * @param key  键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断hash表中是否有该项的值
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key  键
     * @param item 项
     * @param by   要增加几(大于0)
     * @return double
     */
    public double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * hash递减
     *
     * @param key  键
     * @param item 项
     * @param by   要减少记(小于0)
     * @return double
     */
    public double hdecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    // ============================ set =============================

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 键
     * @return Set
     */
    public Set<Object> sGet(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 根据value从一个set中查询,是否存在
     *
     * @param key   键
     * @param value 值
     * @return Boolean
     */
    public Boolean sHasKey(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 将数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return Long 成功个数
     */
    public Long sSet(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * 将set数据放入缓存
     *
     * @param key    键
     * @param time   时间(秒)
     * @param values 值可以是多个
     * @return 成功个数
     */
    public Long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Throwable e) {
            return 0L;
        }
    }

    /**
     * 获取set缓存的长度
     *
     * @param key 键
     * @return Long
     */
    public Long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Throwable e) {
            return 0L;
        }
    }

    /**
     * 移除值为value的
     *
     * @param key    键
     * @param values 值可以是多个
     * @return 移除的个数
     */
    public Long setRemove(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Throwable e) {
            return 0L;
        }
    }

    // ===============================list=================================

    /**
     * 通过索引 获取list中的值
     *
     * @param key   键
     * @param index 索引 index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return Obj
     */
    public Object lGetIndex(String key, long index) {
        try {
            //redisTemplate.opsForList().rightPop(key,index);
            return redisTemplate.opsForList().index(key, index);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     */
    public void lSet(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     */
    public void lSet(String key, Object value, long time) {
        redisTemplate.opsForList().rightPush(key, value);
        if (time > 0) {
            expire(key, time);
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     */
    public void lSet(String key, List<Object> value) {
        redisTemplate.opsForList().rightPushAll(key, value);
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     */
    public void lSet(String key, List<Object> value, long time) {
        redisTemplate.opsForList().rightPushAll(key, value);
        if (time > 0) {
            expire(key, time);
        }
    }

    /**
     * 根据索引修改list中的某条数据
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return boolean
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 移除N个值为value
     *
     * @param key   键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public Long lRemove(String key, long count, Object value) {
        try {
            return redisTemplate.opsForList().remove(key, count, value);
        } catch (Throwable e) {
            return 0L;
        }
    }

    /**
     * Set 添加元素
     *
     * @param key     集合名
     * @param timeout 过期时间（默认：7，单位：天）
     * @param obj     存入的值
     */
    public void saveSet(String key, Integer timeout, Integer len, Object... obj) {
        //TimeUnit.SECONDS
        // todo: 超过len个元素则自动丢弃，不进行添加操作
        if (hasKey(key)) {
            // 默认Set集合长度为10 超过长度则丢弃元素
            if (getSetMembers(key).size() < len) {
                redisTemplate.opsForSet().add(key, obj);
            } else {
                log.warn("Redis Set 添加元素失败，超过设定长度：{},则丢弃该元素：{}", len, obj);
            }
        } else {
            // 首次创建的时候设置过期时间
            redisTemplate.opsForSet().add(key, obj);
            redisTemplate.expire(key, timeout, TimeUnit.DAYS);
        }
    }

    /**
     * 获取集合所有成员
     *
     * @param key 集合名
     * @return 集合
     */
    public Set<Object> getSetMembers(String key) {
        //TimeUnit.SECONDS
        return redisTemplate.opsForSet().members(key);
    }

}
