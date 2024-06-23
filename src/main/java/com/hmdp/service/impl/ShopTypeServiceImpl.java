package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private ShopTypeMapper shopTypeMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public List<ShopType> getTypeList() {
        //1.判断TypeList在redis中是否存在
        String shopTypeListKey = RedisConstants.SHOP_TYPE_LIST;
        String shopTypeListJson = stringRedisTemplate.opsForValue().get(shopTypeListKey);
        //2.存在，返回
        if (!StrUtil.isBlank(shopTypeListJson)) {
            return JSONUtil.toList(shopTypeListJson, ShopType.class);
        }
        //3.不存在，查找数据库
        List<ShopType> shopTypeList = shopTypeMapper.getShopType();
        //4.库中不存在，返回错误信息
        if (shopTypeList == null) {
            return new ArrayList<>();
        }
        //5.库中存在，插入redis缓存，返回
        stringRedisTemplate.opsForValue().set(shopTypeListKey, JSONUtil.toJsonStr(shopTypeList));
        return shopTypeList;
    }
}
