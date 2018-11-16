package ranking.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ranking.entity.DynamicInfo;
import ranking.entity.Phone;
import ranking.entity.PhoneInfo;
import ranking.service.PhoneService;
import ranking.utils.Constants;
import ranking.utils.StringUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Created by haoxy on 2018/7/26.
 * E-mail:hxyHelloWorld@163.com
 * github:https://github.com/haoxiaoyong1014
 */
@Service
public class PhoneServiceImpl implements PhoneService {

    List<Phone> phones = Arrays.asList(
            new Phone(1, "苹果"),
            new Phone(2, "小米"),
            new Phone(3, "华为"),
            new Phone(4, "oppo"),
            new Phone(5, "vivo"));

    Jedis jedis = null;
    @PostConstruct
    public void initRedisClient () {
        // 注：保持代码简介，未使用 JedisPool 生产环境 应使用连接池
        jedis = new Jedis("127.0.0.1", 6379);

    }

    @Override
    public void buyPhone(int phoneId) {
        // 购买成功则对排行榜中该手机的销量进行加1
        jedis.zincrby(Constants.SALES_LIST, 1, String.valueOf(phoneId));
        /*redisTemplate.opsForZSet().*/
        // 添加购买动态
        long currentTimeMillis = System.currentTimeMillis();
        String msg = currentTimeMillis + Constants.separator + phones.get(phoneId - 1).getName();
        //将字符串添加到指定的key中存储列表的头部（LPUSH）或尾部（RPUSH）
        jedis.lpush(Constants.BUY_DYNAMIC, msg);

    }

    @Override
    public List<PhoneInfo> getPhbList() {
        // 按照销量多少排行，取出前五名
        Set<Tuple> tuples = jedis.zrevrangeWithScores(Constants.SALES_LIST, 0, 4);
        List<PhoneInfo> list = new ArrayList<PhoneInfo>();
        for (Tuple tuple : tuples) {
            PhoneInfo vo = new PhoneInfo();
            // 取出对应 phoneId 的手机名称
            int phoneId = Integer.parseInt(tuple.getElement());
            vo.setName(phones.get(phoneId - 1).getName());
            vo.setSales((int) tuple.getScore());
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<DynamicInfo> getBuyDynamic() {
        List<DynamicInfo> dynamicList = new ArrayList<DynamicInfo>();
        for (int i = 0; i < 3; i++) {
           /* jedis.lindex(String key,int index)
            * 返回存储在指定键中的列表的指定元素。0是第一个元素，1是第二个元素
            */
            String result = jedis.lindex(Constants.BUY_DYNAMIC, i);
            if (StringUtils.isEmpty(result)) {
                break;
            }
            String[] arr = result.split(Constants.separator);
            long time = Long.valueOf(arr[0]);
            String phone = arr[1];
            DynamicInfo vo = new DynamicInfo();
            vo.setPhone(phone);
            vo.setTime(StringUtil.showTime(new Date(time)));
            dynamicList.add(vo);
        }

        // 只保留队列中20个销售动态
        jedis.ltrim(Constants.BUY_DYNAMIC, 0, 19);
        return dynamicList;
    }

    @Override
    public int phoneRank(int phoneId) {
        // 如果是排名第一， 返回的是0 ，因此如果为null 即不在排行榜上则返回-1
        Long zrank = jedis.zrevrank(Constants.SALES_LIST, String.valueOf(phoneId));
        return zrank == null ? -1 : zrank.intValue();
    }

    @Override
    public void clear() {
        jedis.del(Constants.SALES_LIST);
        jedis.del(Constants.BUY_DYNAMIC);
    }

    @Override
    public void initCache() {
        Map<String, Double> map = new HashMap<String, Double>();
        map.put("1", 4.0);
        map.put("2", 2.0);
        map.put("3", 3.0);
        jedis.zadd(Constants.SALES_LIST, map);
    }

}
