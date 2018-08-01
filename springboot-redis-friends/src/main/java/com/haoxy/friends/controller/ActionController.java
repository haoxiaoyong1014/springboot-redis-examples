package com.haoxy.friends.controller;

import com.haoxy.friends.config.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by haoxy on 2018/7/30.
 * E-mail:hxyHelloWorld@163.com
 * github:https://github.com/haoxiaoyong1014
 */
@RestController
@RequestMapping(value = "set")
public class ActionController {

    private RedisTemplate redisTemplate;

    private SetOperations setOperations;

    @Autowired(required = false)
    public void setRedisTemplate(RedisTemplate redisTemplate) {
        RedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setValueSerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setHashValueSerializer(stringSerializer);
        this.redisTemplate = redisTemplate;
        this.setOperations = redisTemplate.opsForSet();
    }

    /**
     * 好友列表
     *
     * @return
     */
    @RequestMapping(value = "getList", method = RequestMethod.GET)
    public Map getList() {
        Map<String,Object> map = new HashMap();
        Set aFriend = setOperations.members(Constants.A_FRIEND_KEY);
        Set bFriend = setOperations.members(Constants.B_FRIEND_KEY);
        map.put("aFriend", aFriend);
        map.put("bFriend", bFriend);
        return map;
    }

    /**
     * 删除好友
     * @param user
     * @param friend
     * @return
     */
    @RequestMapping(value = "deleFriend",method = RequestMethod.DELETE)
    public Long deleFriend(String user,String friend){
        String currentKey = Constants.A_FRIEND_KEY;
        if ("xiaoming".equals(user)) {
            currentKey = Constants.B_FRIEND_KEY;
        }
        //返回删除成功的条数
        return setOperations.remove(currentKey, friend);
    }
    /**
     * 添加好友
     *
     * @return
     */
    @RequestMapping(value = "/addFriend", method = RequestMethod.POST)
    public Long addFriend(String user, String friend) {
        String currentKey = Constants.A_FRIEND_KEY;
        if ("xiaoming".equals(user)) {
            currentKey = Constants.B_FRIEND_KEY;
        }
        //返回添加成功的条数
        return setOperations.add(currentKey, friend);
    }

    /**
     * 共同好友(交集)
     *
     * @return
     */
    @RequestMapping(value = "intersectFriend", method = RequestMethod.GET)
    public Set intersectFriend() {
        return setOperations.intersect(Constants.A_FRIEND_KEY, Constants.B_FRIEND_KEY);
    }

}
