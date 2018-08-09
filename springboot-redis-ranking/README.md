# springboot-redis-ranking
基于Redis实现商品排行榜

**前言**

排行榜作为互联网应用中几乎必不可少的一个元素，其能够勾起人类自身对比的欲望，从而来增加商品的销量。排行榜的实现方式基本大同小异，大部分都基于 Redis 的有序集合 sorted set 来实现。本文通过了商品销售排行榜这一模型，来进行演示,同时您还可以根据本文章实现
文章的点赞排行,
积分排行等..
项目Github地址：https://github.com/haoxiaoyong1014/springboot-redis-examples/tree/master/springboot-redis-ranking
**需求**

1,按照商品销量进行排行
2,可以获得指定商品的排名
3,显示实时销售动态情况


**一,首先看一下效果图:**
![image](https://github.com/haoxiaoyong1014/springboot-redis-examples/raw/master/springboot-redis-ranking//src/main/java/ranking/image/redis-ranking.gif)

**准备工作:  定义两个 key值**

` public static String SALES_LIST = "phone:sales:list";
    public static String BUY_DYNAMIC = "phone:buy:dynamic"
    public static String separator = "#";`

二,添加依赖:
```
<dependency>
     <groupId>redis.clients</groupId>
     <artifactId>jedis</artifactId>
     <version>2.7.3</version>
</dependency>
```
**三,本案例中使用的所有 redis命令: **

### 获得销售排行榜上该商品的排名
| 命令  | 实例 | 描述|
| :----: | :---: | :---: | 
| ZREVRANK  |ZREVRANK key member|  返回有序集 key 中成员 member 的排名。其中有序集成员按 score 值递减(从大到小)排序。排名以 0 为底，也就是说， score 值最大的成员排名为 0  |  

实例:
```
redis 127.0.0.1:6379> ZRANGE salary 0 -1 WITHSCORES     # 测试数据
1) "jack"
2) "2000"
3) "peter"
4) "3500"
5) "tom"
6) "5000"

redis> ZREVRANK salary peter     # peter 的工资排第二
(integer) 1

redis> ZREVRANK salary tom       # tom 的工资最高
(integer) 0
```
返回值:
```
如果 member 是有序集 key 的成员，返回 member 的排名。
如果 member 不是有序集 key 的成员，返回 null 。
```
项目中所用实例:
```
 @Override
    public int phoneRank(int phoneId) {
        // 如果是排名第一， 返回的是0 ，因此如果为null 即不在排行榜上则返回-1
        Long zrank = jedis.zrevrank(Constants.SALES_LIST, String.valueOf(phoneId));
        return zrank == null ? -1 : zrank.intValue();
    }
```
### 获得销售动态:

| 命令  | 实例 | 描述|
| :----: | :---: | :---: | 
| LINDEX  |LINDEX key index| 返回列表 key 中，下标为 index 的元素。 |  

实例:
```
redis> LPUSH mylist "World"
(integer) 1

redis> LPUSH mylist "Hello"
(integer) 2

redis> LINDEX mylist 0
"Hello"

redis> LINDEX mylist -1
"World"

redis> LINDEX mylist 3        # index不在 mylist 的区间范围内
(nil)
```
返回值:
```
列表中下标为 index 的元素。
如果 index 参数的值不在列表的区间范围内(out of range)，返回 nil 。
```
项目中的实例:
```
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
```
### 购买商品

| 命令  | 实例 | 描述|
| :----: | :---: | :---: | 
|ZINCRBY  |ZINCRBY key increment member| 为有序集 key 的成员 member 的 score 值加上增量 increment ,当 key 不存在，或 member 不是 key 的成员时， ZINCRBY key increment member 等同于 ZADD key increment member|  

实例: 
```
redis> ZSCORE salary tom
"2000"

redis> ZINCRBY salary 2000 tom   # tom 加薪啦！
"4000"
```
返回值:
```
member 成员的新 score 值，以字符串形式表示
```
项目中实例:
```
@Override
    public void buyPhone(int phoneId) {
        // 购买成功则对排行榜中该手机的销量进行加1
        jedis.zincrby(Constants.SALES_LIST, 1, String.valueOf(phoneId));
        // 添加购买动态
        long currentTimeMillis = System.currentTimeMillis();
        String msg = currentTimeMillis + Constants.separator + phones.get(phoneId - 1).getName();
        //将字符串添加到指定的key中存储列表的头部（LPUSH）或尾部（RPUSH）
        jedis.lpush(Constants.BUY_DYNAMIC, msg);
    }
```
### 添加购买动态:

| 命令  | 实例 | 描述|
| :----: | :---: | :---: | 
|LPUSH  |LPUSH key value [value ...]| 将一个或多个值 value 插入到列表 key 的表头 |  

实例:

```
# 加入单个元素

redis> LPUSH languages python
(integer) 1


# 加入重复元素

redis> LPUSH languages python
(integer) 2

redis> LRANGE languages 0 -1     # 列表允许重复元素
1) "python"
2) "python"


# 加入多个元素

redis> LPUSH mylist a b c
(integer) 3

redis> LRANGE mylist 0 -1
1) "c"
2) "b"
3) "a"
```
返回值:
```
执行 LPUSH 命令后，列表的长度。
```
项目中的实例:

```
 String msg = currentTimeMillis + Constants.separator + phones.get(phoneId - 1).getName();
        //将字符串添加到指定的key中存储列表的头部（LPUSH）或尾部（RPUSH）
        jedis.lpush(Constants.BUY_DYNAMIC, msg);
```
### 获取销量排行榜

| 命令  | 实例 | 描述|
| :----: | :---: | :---: | 
|zrevrangeWithScores  |zrevrangeWithScores key start,end|取出下标start到 end 的数据|  

返回值:
```
Set<Tuple> 集合
```
项目中的实例:
```
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
```
### 重置排名:

| 命令  | 实例 | 描述|
| :----: | :---: | :---: | 
|DEL  |DEL key [key ...]|删除给定的一个或多个 key,不存在的 key 会被忽略。|  

实例: 
```
#  删除单个 key

redis> SET name huangz
OK

redis> DEL name
(integer) 1


# 删除一个不存在的 key

redis> EXISTS phone
(integer) 0

redis> DEL phone # 失败，没有 key 被删除
(integer) 0


# 同时删除多个 key

redis> SET name "redis"
OK

redis> SET type "key-value store"
OK

redis> SET website "redis.com"
OK

redis> DEL name type website
(integer) 3
```
返回值: 

```
被删除 key 的数量。
```
项目中实例:
```
@Override
    public void clear() {
        jedis.del(Constants.SALES_LIST);
        jedis.del(Constants.BUY_DYNAMIC);
    }
```
至此项目中所有用到的命令都介绍了一下: 更多的命令请参考: http://doc.redisfans.com/ ,建议大家练习一下,熟能生巧
**本案例项目地址: [点击查看](https://github.com/haoxiaoyong1014/springboot-redis-examples)**
> 纸上得来终觉浅，绝知此事要躬行。————出自《冬夜读书示子聿》