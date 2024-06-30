package com.alfo.shop.service.impl;

import com.alfo.common.domain.dto.Result;
import com.alfo.common.utils.constants.SystemConstants;
import com.alfo.common.utils.redis.CacheClient;
import com.alfo.common.utils.redis.RedisConstants;
import com.alfo.shop.domain.po.Shop;
import com.alfo.shop.mapper.ShopMapper;
import com.alfo.shop.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private CacheClient cacheClient;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Override
    public Result queryById(Long id) throws InterruptedException {
        //解决缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,
//                id2 -> shopMapper.queryShopById(id2), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //逻辑过期击穿
        Shop shop = cacheClient.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,
                id2 -> shopMapper.queryShopById(id2), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);


        return Result.ok(shop);
    }


    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        //1.return error message if shop is null
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("No that shop!");
        }
        //2.update SQL
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        shop.setUpdateTime(LocalDateTime.now());
        shopMapper.updateShop(shop);
        //3.delete Redis
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        return Result.ok(shop);
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        final Integer everyPageShowNums = SystemConstants.DEFAULT_PAGE_SIZE;
        //1.判断是否根据坐标查询，如果不是直接根据店铺typeId返回
        if (x == null || y == null) {
            //select * from table where type_id = #{typeId} limit #{startIndex}, #{everyPageShowNums}
            Integer startIndex = (current - 1) * everyPageShowNums;
            List<Shop> shopList = shopMapper.queryShopsPageByTypeId(typeId, startIndex, everyPageShowNums);
            return Result.ok(shopList);
        }
        //2.计算分页参数
        //redis中实现截取，也就是分页，和数据库不同，SortedSet是获取start、end
        //          对于Geo类型，是limit(len)，然后再手动去截取长度
        Integer start = (current - 1) * everyPageShowNums;
        Integer end = start + everyPageShowNums;
        //3.查询redis，返回商铺ids，distances，并根据距离进行排序
        String key = RedisConstants.SHOP_GEO_KEY + typeId;
        // geosearch key [frommember member]|[fromlonlat longitude latitude] [byradius radius] m|km withdistance
        //   GeoFeference.fromCoordinate(x, y)：fromlonlat longitude latitude
        //   new Distance(5000)：默认单位是m
        //   RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().limit(end)：带参数，要前end份
        GeoResults<RedisGeoCommands.GeoLocation<String>> searchResult = stringRedisTemplate.opsForGeo().search(key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().limit(end));
        if (searchResult == null) {
            return  Result.ok();
        }
        // 根据类型分页查询
        List<Shop> shopResult = new ArrayList<>();
        // stream.skip(nums)：使用stream快速跳过start个元素
        //
        searchResult.getContent().stream().skip(start).forEach(result -> {
            Shop shopTemp = shopMapper.queryShopById(Long.parseLong(result.getContent().getName()));
            shopTemp.setDistance(result.getDistance().getValue());
            shopResult.add(shopTemp);
        });
        //4.返回排序好的shops
        // 返回数据
        return Result.ok(shopResult);
    }
}
