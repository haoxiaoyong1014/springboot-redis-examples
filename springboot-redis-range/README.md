#### SpringBoot 使用 Redis Geo 实现查找附近的位置


**6个操作命令**

Redis 命令  | 描述
------------- | -------------
GEOADD  | 增加某个地理位置的坐标
GEOPOS  | 获取某个地理位置的坐标
GEODIST | 获取两个地理位置的距离
GEORADIUS | 根据给定地理位置坐标获取指定范围内的地理位置集合
GEORADIUSBYMEMBERl  | 根据给定地理位置获取指定范围内的地理位置集合
GEOHASH  | 获取某个地理位置的 geohash 值

**GEOADD**

该命令格式：

`geoadd key longitude latitude member [longitude latitude member ...]`

对应例子：
```
redis> GEOADD cities 13.361389 38.115556 "shanghai" 15.087269 37.502669 "hangzhou"
(integer) 2
redis> GEODIST cities shanghai hangzhou
"166274.1516"
redis> GEORADIUS cities 15 37 100 km
1) "hangzhou"
redis> GEORADIUS cities 15 37 200 km
1) "shanghai"
2) "hangzhou"
redis> 
```
`geoadd`命令意思是将经度为13.361389纬度为38.115556的地点shanghai和经度为15.087269纬度为37.502669的地点hangzhou加入key为cities的 sorted set集合中。可以添加一到多个位置。

> 有效的经度从-180度到180度。有效的纬度从-85.05112878度到85.05112878度。当坐标位置超出上述指定范围时，该命令将会返回一个错误。

**GEODIST**

该命令格式：
`GEODIST key member1 member2 [m|km|ft|mi]`

对应例子：
`GEODIST cities shanghai hangzhou`

返回两点之间的距离，默认为米；
可选值：
* m for meters.
* km for kilometers.
* mi for miles.
* ft for feet.

例如：`GEODIST cities shanghai hangzhou km`

**GEOPOS**

该命令格式：
`GEOPOS key member [member ...]`

对应例子：
```yaml
redis> GEOPOS cities shanghai hangzhou 
1) 1) "13.36138933897018433"
   2) "38.11555639549629859"
2) 1) "15.08726745843887329"
   2) "37.50266842333162032"
```
获取某个地理位置的坐标
对于不存在的城市返回null;
```yaml
redis> GEOPOS cities nonExisting
(nil)
```
**GEORADIUS**

该命令格式：
`GEORADIUS key longitude latitude radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count] [ASC|DESC] [STORE key] [STOREDIST key]`


* radius  半径长度，必选项。后面的m、km、ft、mi、是长度单位选项，四选一。
* WITHCOORD  将位置元素的经度和维度也一并返回，非必选。
* WITHDIST 在返回位置元素的同时， 将位置元素与中心点的距离也一并返回。 距离的单位和查询单位一致，非必选。
* WITHHASH 返回位置的52位精度的Geohash值，非必选。这个我反正很少用，可能其它一些偏向底层的LBS应用服务需要这个。
* COUNT 返回符合条件的位置元素的数量，非必选。比如返回前10个，以避免出现符合的结果太多而出现性能问题。
* ASC|DESC 排序方式，非必选。默认情况下返回未排序，但是大多数我们需要进行排序。参照中心位置，从近到远使用ASC ，从远到近使用DESC。

对应例子：

```yaml
redis> GEOADD cities 13.361389 38.115556 "shanghai" 15.087269 37.502669 "hangzhou"
(integer) 2
redis> GEORADIUS cities 15 37 200 km WITHDIST
1) 1) "shanghai"
   2) "190.4424"
2) 1) "hangzhou"
   2) "56.4413"
redis> GEORADIUS cities 15 37 200 km WITHCOORD
1) 1) "shanghai"
   2) 1) "13.36138933897018433"
      2) "38.11555639549629859"
2) 1) "hangzhou"
   2) 1) "15.08726745843887329"
      2) "37.50266842333162032"
redis> GEORADIUS cities 15 37 200 km WITHDIST WITHCOORD
1) 1) "shanghai"
   2) "190.4424"
   3) 1) "13.36138933897018433"
      2) "38.11555639549629859"
2) 1) "hangzhou"
   2) "56.4413"
   3) 1) "15.08726745843887329"
      2) "37.50266842333162032"
redis> 

```

**GEORADIUSBYMEMBERl**

该命令格式：
`GEORADIUSBYMEMBER key member radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count] [ASC|DESC] [STORE key] [STOREDIST key]`

对应例子：
```yaml
redis> GEOADD cities 13.583333 37.316667 "shenzhen"
(integer) 1
redis> GEOADD cities 13.361389 38.115556 "shanghai" 15.087269 37.502669 "hangzhou"
(integer) 2
redis> GEORADIUSBYMEMBER cities shenzhen 100 km
1) "shenzhen"
2) "shanghai"
redis> 
```
该命令与GEORADIUS完全相同，唯一的区别在于，该查询不使用经度和纬度值作为要查询的区域的中心，而是使用已存在于由排序集表示的地理空间索引中的成员的名称。

#### SpringBoot 使用 Redis Geo（实例）

**springboot版本**
```xml
 <parent>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-parent</artifactId>
      <version>2.1.8.RELEASE</version>
  </parent>
```

**redis starter**
```xml
 <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```
**vo 对象定义**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityInfo {

    /** 城市 */
    private String city;

    /** 经度 */
    private Double longitude;

    /** 纬度 */
    private Double latitude;
}
```
**服务接口定义**
```java
/**
 * @author haoxiaoyong
 * @date created at 下午3:47 on 2020/9/18
 * @github https://github.com/haoxiaoyong1014
 * @blog www.haoxiaoyong.cn
 */
public interface IGeoService {

    /**
     * 把城市信息保存到 Redis 中
     * @param cityInfos {@link CityInfo}
     * @return 成功保存的个数
     * */
    Long saveCityInfoToRedis(Collection<CityInfo> cityInfos);

    /**
     * 获取给定城市的坐标
     * @param cities 给定城市 key
     * @return {@link Point}s
     * */
    List<Point> getCityPos(String[] cities);

    /**
     * 获取两个城市之间的距离
     * @param city1 第一个城市
     * @param city2 第二个城市
     * @param metric {@link Metric} 单位信息, 可以是 null
     * @return {@link Distance}
     * */
    Distance getTwoCityDistance(String city1, String city2, Metric metric);

    /**
     * 根据给定地理位置坐标获取指定范围内的地理位置集合
     * @param within {@link Circle} 中心点和距离
     * @param args {@link RedisGeoCommands.GeoRadiusCommandArgs} 限制返回的个数和排序方式, 可以是 null
     * @return {@link RedisGeoCommands.GeoLocation}
     * */
    GeoResults<RedisGeoCommands.GeoLocation<String>> getPointRadius(
            Circle within, RedisGeoCommands.GeoRadiusCommandArgs args);

    /**
     * 根据给定地理位置获取指定范围内的地理位置集合
     * */
    GeoResults<RedisGeoCommands.GeoLocation<String>> getMemberRadius(
            String member, Distance distance, RedisGeoCommands.GeoRadiusCommandArgs args);

    /**
     * 获取某个地理位置的 geohash 值
     * @param cities 给定城市 key
     * @return city geohashs
     * */
    List<String> getCityGeoHash(String[] cities);
}

```

**服务接口实现**

```java
/**
 * @author haoxiaoyong
 * @date created at 下午4:06 on 2020/9/18
 * @github https://github.com/haoxiaoyong1014
 * @blog www.haoxiaoyong.cn
 */
@Service
@Slf4j
public class GeoServiceImpl implements IGeoService {

    private final String GEO_KEY = "ah-cities";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Long saveCityInfoToRedis(Collection<CityInfo> cityInfos) {
        log.info("start to save city info: {}.", JSON.toJSONString(cityInfos));

        GeoOperations<String, String> ops = redisTemplate.opsForGeo();
        Set<RedisGeoCommands.GeoLocation<String>> locations = new HashSet<>();
        cityInfos.forEach(ci -> locations.add(new RedisGeoCommands.GeoLocation<String>(
                ci.getCity(), new Point(ci.getLongitude(), ci.getLatitude())
        )));

        log.info("done to save city info.");

        return ops.add(GEO_KEY, locations);
    }

    @Override
    public List<Point> getCityPos(String[] cities) {
        GeoOperations<String, String> ops = redisTemplate.opsForGeo();

        return ops.position(GEO_KEY, cities);
    }

    @Override
    public Distance getTwoCityDistance(String city1, String city2, Metric metric) {
        GeoOperations<String, String> ops = redisTemplate.opsForGeo();

        return metric == null ?
                ops.distance(GEO_KEY, city1, city2) : ops.distance(GEO_KEY, city1, city2, metric);
    }

    @Override
    public GeoResults<RedisGeoCommands.GeoLocation<String>> getPointRadius(Circle within, RedisGeoCommands.GeoRadiusCommandArgs args) {
        GeoOperations<String, String> ops = redisTemplate.opsForGeo();

        return args == null ?
                ops.radius(GEO_KEY, within) : ops.radius(GEO_KEY, within, args);
    }

    @Override
    public GeoResults<RedisGeoCommands.GeoLocation<String>> getMemberRadius(String member, Distance distance, RedisGeoCommands.GeoRadiusCommandArgs args) {

        GeoOperations<String, String> ops = redisTemplate.opsForGeo();

        return args == null ?
                ops.radius(GEO_KEY, member, distance) : ops.radius(GEO_KEY, member, distance, args);
    }

    @Override
    public List<String> getCityGeoHash(String[] cities) {
        GeoOperations<String, String> ops = redisTemplate.opsForGeo();

        return ops.hash(GEO_KEY, cities);
    }
}

```
**测试用例**
```java

/**
 * @author haoxiaoyong
 * @date created at 下午4:22 on 2020/9/18
 * @github https://github.com/haoxiaoyong1014
 * @blog www.haoxiaoyong.cn
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RangeApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class GeoServiceTest {

    /** fake some cityInfos */
    private List<CityInfo> cityInfos;

    @Autowired
    private IGeoService geoService;

    @Before
    public void init() {

        cityInfos = new ArrayList<>();

        cityInfos.add(new CityInfo("hefei", 117.17, 31.52));
        cityInfos.add(new CityInfo("anqing", 117.02, 30.31));
        cityInfos.add(new CityInfo("huaibei", 116.47, 33.57));
        cityInfos.add(new CityInfo("suzhou", 116.58, 33.38));
        cityInfos.add(new CityInfo("fuyang", 115.48, 32.54));
        cityInfos.add(new CityInfo("bengbu", 117.21, 32.56));
        cityInfos.add(new CityInfo("huangshan", 118.18, 29.43));
    }
    /**
     * <h2>测试 saveCityInfoToRedis 方法</h2>
     * */
    @Test
    public void testSaveCityInfoToRedis() {

        System.out.println(geoService.saveCityInfoToRedis(cityInfos));
    }

    /**
     * <h2>测试 getCityPos 方法</h2>
     * 如果传递的 city 在 Redis 中没有记录, 会返回什么呢 ? 例如, 这里传递的 xxx
     * */
    @Test
    public void testGetCityPos() {

        System.out.println(JSON.toJSONString(geoService.getCityPos(
                Arrays.asList("anqing", "suzhou", "xxx").toArray(new String[3])
        )));
    }

    /**
     * <h2>测试 getTwoCityDistance 方法</h2>
     * */
    @Test
    public void testGetTwoCityDistance() {

        System.out.println(geoService.getTwoCityDistance("hefei", "anqing", null).getValue());
        System.out.println(geoService.getTwoCityDistance("hefei", "anqing", Metrics.KILOMETERS).getValue());
    }

    /**
     * <h2>测试 getPointRadius 方法</h2>
     * */
    @Test
    public void testGetPointRadius() {

        Point center = new Point(cityInfos.get(0).getLongitude(), cityInfos.get(0).getLatitude());
        Distance radius = new Distance(140, Metrics.KILOMETERS);
        Circle within = new Circle(center, radius);

        System.out.println(JSON.toJSONString(geoService.getPointRadius(within, null)));

        // order by 距离 limit 2, 同时返回距离中心点的距离
        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().limit(2).sortAscending();
        System.out.println(JSON.toJSONString(geoService.getPointRadius(within, args)));
    }

    /**
     * <h2>测试 getMemberRadius 方法</h2>
     * */
    @Test
    public void testGetMemberRadius() {

        Distance radius = new Distance(200, Metrics.KILOMETERS);

        System.out.println(JSON.toJSONString(geoService.getMemberRadius("suzhou", radius, null)));

        // order by 距离 limit 2, 同时返回距离中心点的距离
        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().limit(2).sortAscending();
        System.out.println(JSON.toJSONString(geoService.getMemberRadius("suzhou", radius, args)));
    }

    /**
     * <h2>测试 getCityGeoHash 方法</h2>
     * */
    @Test
    public void testGetCityGeoHash() {

        System.out.println(JSON.toJSONString(geoService.getCityGeoHash(
                Arrays.asList("anqing", "suzhou", "xxx").toArray(new String[3])
        )));
    }
}

```

