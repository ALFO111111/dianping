package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.utils.*;
import netscape.javascript.JSObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SpringBootTest
class HmDianPingApplicationTests {


    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ShopMapper shopMapper;

    private ExecutorService executorService = Executors.newFixedThreadPool(500);

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Resource
    private VoucherOrderMapper voucherOrderMapper;

    @Resource
    private UserMapper userMapper;



    /**
     * 把id <= 1000的用户存入redis，并设置永久登录
     * token存入到 D:\developProject\redis\tokens.txt中，按行分割
     */
    @Test
    public void writeTokensToFile() throws IOException {
        //1.获取id <= 1000的用户的List<UserDTO>集合
        List<UserDTO> userDTOS = userMapper.getUserDTOS();
        //2.把所有用户保存到redis，并把token保存到 D:\developProject\redis\tokens.txt
        StringBuilder sb = new StringBuilder("");
        for (UserDTO userDTO : userDTOS) {
            //2.1写入redis
            String token = UUID.randomUUID().toString();
            Map<String, Object> userDTOMap = BeanUtil.beanToMap(userDTO,
                    new HashMap<>(), CopyOptions.create()
                            .setIgnoreNullValue(true)
                            .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
            String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
            stringRedisTemplate.opsForHash().putAll(tokenKey, userDTOMap);
            sb.append(token).append("\n");
        }
        //2.2写入文件
        PrintWriter printWriter = new PrintWriter(new FileWriter("D:\\developProject\\redis\\tokens.txt"), true);
        printWriter.write(sb.toString());
    }

    @Test
    public void loadShopData() {
        //1.查询店铺信息
        List<Shop> shopList = shopMapper.queryShops();
        //2.把店铺分组（按照typeId分组）
        Map<Long, List<Shop>> map = shopList.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        //3.分批写入
        map.forEach((typeId, shops) -> {
            String key = RedisConstants.SHOP_GEO_KEY + typeId;
            shops.forEach(shop -> {
                stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
            });
        });
    }

    @Test
    public void testData() {
        String yearMonth = LocalDateTime.now().getYear() + "-" + LocalDateTime.now().getMonthValue();
        System.out.println(yearMonth);
    }

    @Test
    public void testHyperLogLog() {
        //测试插入 一千万 个用户，模拟 一千万 的用户访问量
        // 查看其内存占用和统计数量误差率
        String key = "HyperLogLog1";
        String[] users = new String[10000];
        int index = 0;
        for (int i = 0; i < 10000000; i++) {
            index = i % 10000;
            users[index] = "user_" + i;
            if (index == 9999) {
                stringRedisTemplate.opsForHyperLogLog().add(key, users);
            }
        }
        System.out.println(stringRedisTemplate.opsForHyperLogLog().size(key));
    }

}
