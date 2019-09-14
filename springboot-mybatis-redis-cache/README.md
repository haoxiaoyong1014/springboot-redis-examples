### springboot-mybatis-redis-cache

使用redis做mybatis的二级缓存

**application.properties**

在application.properties文件中配置Redis，Mybatis，开启Mybatis二级缓存等

```properties
server.port=8084
spring.datasource.url=jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
spring.datasource.username=root
spring.datasource.password=yong1014
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
spring.datasource.type=org.apache.tomcat.jdbc.pool.DataSource

#mybatis
mybatis.type-aliases-package=com.haoxy.example.model
mybatis.mapper-locations=classpath*:/mappers/*Mapper.xml
#开启mybatis的二级缓存
mybatis.configuration.cache-enabled=true
#pagehelper
#如果不配置,pagehelper会自动获取连接，检测数据库
pagehelper.helperDialect=mysql
#配置pageNum参数合理化，比如第0页，和超过最后一页，则返会第一页和最后一页。而不是意想不到的数据。
pagehelper.reasonable=true
#支持通过 Mapper 接口参数来传递分页参数
pagehelper.supportMethodsArguments=true
#为了支持PageHelper.startPage(Object params)方法,默认值为pageNum=pageNum;pageSize=pageSize;count=countSql
pagehelper.params=count=countSql

#redis
spring.redis.host=127.0.0.1
#server password
spring.redis.password=adminadmin
#connection port
spring.redis.port=6379
spring.redis.timeout=10000
spring.redis.database=0
spring.redis.pool.max-idle=8
spring.redis.pool.max-wait=-1
spring.redis.pool.min-idle=0
spring.redis.pool.max-active=8
```

**pom.xml**

```xml
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>1.5.6.RELEASE</version>
    </parent>

    <groupId>com.haoxy.example</groupId>
    <artifactId>springboot-mybatis-redis-cache</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.11</version>
        </dependency>

        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>1.3.0</version>
        </dependency>
        <dependency>
            <groupId>com.github.pagehelper</groupId>
            <artifactId>pagehelper-spring-boot-starter</artifactId>
            <version>1.1.1</version>
        </dependency>

        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.31</version>
        </dependency>

        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis-ehcache</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
```
**Redis配置类替换序列化实现方式**

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        // 设置值（value）的序列化采用Jackson2JsonRedisSerializer。
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        // 设置键（key）的序列化采用StringRedisSerializer。
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}

```

**Spring容器获取Bean工具类**

通过Spring Aware（容器感知）来获取到ApplicationContext，然后根据ApplicationContext获取容器中的Bean。

```java
@Component
public class SpringContextHolder implements ApplicationContextAware {

    //以静态变量保存Spring ApplicationContext, 可在任何代码任何地方任何时候中取出ApplicaitonContext
    private static ApplicationContext applicationContext;


    /**
     * 实现ApplicationContextAware接口的context注入函数, 将其存入静态变量.
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringContextHolder.applicationContext = applicationContext; // NOSONAR
    }

    /**
     * 取得存储在静态变量中的ApplicationContext.
     */
    public static ApplicationContext getApplicationContext() {
        checkApplicationContext();
        return applicationContext;
    }

    /**
     * 从静态变量ApplicationContext中取得Bean, 自动转型为所赋值对象的类型.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        checkApplicationContext();
        return (T) applicationContext.getBean(name);
    }

    /**
     * 从静态变量ApplicationContext中取得Bean, 自动转型为所赋值对象的类型.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> clazz) {
        checkApplicationContext();
        return (T) applicationContext.getBeansOfType(clazz);
    }

    /**
     * 清除applicationContext静态变量.
     */
    public static void cleanApplicationContext() {
        applicationContext = null;
    }

    private static void checkApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("applicaitonContext未注入,请在applicationContext.xml中定义SpringContextHolder");
        }
    }
}

```
**自定的Mybatis缓存**

自定义缓存需要实现Mybatis的Cache接口，我这里将使用Redis来作为缓存的容器。

```java
public class RedisCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(RedisCache.class);

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    private RedisTemplate redisTemplate;

    private String id;


    public RedisCache(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("Cache instances require an ID");
        }
        logger.info("Redis Cache id " + id);
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void putObject(Object key, Object value) {
        RedisTemplate redisTemplate = getRedisTemplate();
        if (value != null) {
            //向redis中加入数据，有效期是2天
            redisTemplate.opsForValue().set(key.toString(), value, 2, TimeUnit.DAYS);
        }
    }

    @Override
    public Object getObject(Object key) {
        RedisTemplate redisTemplate = getRedisTemplate();
        try {
            if (key != null) {
                Object object = redisTemplate.opsForValue().get(key.toString());
                return object;
            }
        } catch (Exception e) {
            logger.error("redis exception.....");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object removeObject(Object key) {
        RedisTemplate redisTemplate = getRedisTemplate();
        try {
            if (key != null) {
                redisTemplate.delete(key.toString());
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public void clear() {
        RedisTemplate redisTemplate = getRedisTemplate();
        logger.debug("清空缓存");
        try {
            Set<String> keys = redisTemplate.keys("*:" + this.id + "*");
            if (!CollectionUtils.isEmpty(keys)) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            logger.error("clear exception....");
        }
    }

    @Override
    public int getSize() {
        RedisTemplate redisTemplate = getRedisTemplate();
        Long size = (Long) redisTemplate.execute(new RedisCallback<Long>() {
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.dbSize();
            }
        });
        return size.intValue();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return this.readWriteLock;
    }

    private RedisTemplate getRedisTemplate() {
        if (redisTemplate == null) {
            redisTemplate = SpringContextHolder.getBean("redisTemplate");
        }
        return redisTemplate;
    }
}

```

在使用 springboot-mybatis-ehcache (<a href="https://github.com/haoxiaoyong1014/springboot-examples/tree/master/springboot-mybatis-myehcache">使用EhcacheCache做二级缓存,使用pageHelper做分页插件</a>)
同样有一个名叫EhcacheCache的类实现了Cache接口;并实现了其中的方法;只是这个mybatis帮我们做了,我们只需要引入相应的依赖即可,我们要自定义的话，所以上面我们使用redis一样要实现Cache接口;

![image.png](https://upload-images.jianshu.io/upload_images/15181329-f0037f6333276432.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

**Mapper文件**

```xml
<mapper namespace="com.haoxy.example.mapper.PersonMapper">
    <!--mybatis ehcache缓存配置 -->
    <!-- 以下两个<cache>标签二选一,第一个可以输出日志,第二个不输出日志 -->
    <!--<cache type="org.mybatis.caches.ehcache.LoggingEhcache" />
    <cache type="org.mybatis.caches.ehcache.EhcacheCache"/>-->

    <!--根据需求调整缓存参数：-->
        <cache type="com.haoxy.example.utils.RedisCache">
            <property name="eviction" value="LRU" />
            <property name="flushInterval" value="6000000" />
            <property name="size" value="1024" />
            <property name="readOnly" value="false" />
        </cache>

    <resultMap id="BaseResultMap" type="com.haoxy.example.model.Person">
        <!--
          WARNING - @mbggenerated
          This element is automatically generated by MyBatis Generator, do not modify.
        -->
        <id column="id" property="id" jdbcType="BIGINT"/>
        <result column="name" property="name" jdbcType="VARCHAR"/>
        <result column="age" property="age" jdbcType="INTEGER"/>
        <result column="address" property="address" jdbcType="VARCHAR"/>
    </resultMap>
     <!-- ........ -->
    <!--注意下面两个查询语句的区别-->
    
     <!-- 对这个语句useCache="true"默认是true，可以不写 -->
     <select id="findAll" resultMap="BaseResultMap" useCache="true">
            select
            <include refid="Base_Column_List"/>
            from person
      </select>
        <!-- 对这个语句禁用二级缓存 -->
     <select id="findAllPerson" resultMap="BaseResultMap" useCache="false">
            select
            <include refid="Base_Column_List"/>
            from person
      </select>
        
        
        
```

这里只列出重要的部分; [具体见项目源码](https://github.com/haoxiaoyong1014/springboot-redis-examples/tree/master/springboot-mybatis-redis-cache);

**PersonController**

```java
@RequestMapping("/findAll")
    public String findAll() {
        long begin = System.currentTimeMillis();
        List<Person> persons = personService.findAll();
        long ing = System.currentTimeMillis();
        System.out.println(("请求时间：" + (ing - begin) + "ms"));
        return JSON.toJSONString(persons);
    }
    
      @RequestMapping("/findAllPerson")
        public String findAllPerson() {
            long begin = System.currentTimeMillis();
            List<Person> persons = personService.findAllPerson();
            long ing = System.currentTimeMillis();
            System.out.println(("请求时间：" + (ing - begin) + "ms"));
            return JSON.toJSONString(persons);
        }
        
```
**测试**

1，测试`findAll`方法,测试结构如下:

![image.png](https://upload-images.jianshu.io/upload_images/15181329-a1e28e276e458cc0.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

2,测试`findAllPerson`方法,

![image.png](https://upload-images.jianshu.io/upload_images/15181329-915d994594c03dbd.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以很明显的看到`findAllPerson`方法比`findAll`方法快很多,在mapper.xml文件中`findAllPerson`方法的查询语句`useCache="false"`
这个语句的查询结果是没有放到二级缓存的,竟然比`findAll`方法(在mapper.xml文件中`findAll`方法的查询语句`useCache="true"`放到了二级缓存中)快很多;

这是因为我们使用的是Redis做的二级缓存；这也是为什么mybatis帮我们实现了用EhcacheCache做二级缓存的原因;

下一篇文章我们对两者进行比较;

[本案例地址](https://github.com/haoxiaoyong1014/springboot-redis-examples/tree/master/springboot-mybatis-redis-cache);

对应的sql在项目的根目录下;



