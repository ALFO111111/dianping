package com.alfo.shop.mapper;

import com.alfo.shop.domain.po.ShopType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface ShopTypeMapper extends BaseMapper<ShopType> {

    @Select("select * from tb_shop_type")
    List<ShopType> getShopType();
}
