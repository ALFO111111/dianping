package com.hmdp.mapper;

import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface ShopMapper extends BaseMapper<Shop> {

    @Select("select * from tb_shop where id = #{id}")
    Shop queryShopById(Long id);

    @Update("update tb_shop set name = #{name}, type_id = #{typeId}, images = #{images}, area = #{area}," +
            "address = #{address}, x = #{x}, y = #{y}, avg_price = #{avgPrice}, sold = #{sold}, comments = #{comments}," +
            "score = #{score}, open_hours = #{openHours}, create_time = #{createTime}, update_time = #{updateTime}" +
            " where id = #{id}")
    void updateShop(Shop shop);

    @Select("select * from tb_shop")
    List<Shop> queryShops();

    //select * from table where type_id = #{typeId} limit #{startIndex}, #{everyPageShowNums}
    @Select("select * from tb_shop where type_id = #{type_id} limit #{startIndex}, #{everyPageShowNums}")
    List<Shop> queryShopsPageByTypeId(Integer typeId, Integer startIndex, Integer everyPageShowNums);
}
