package com.hmdp.mapper;

import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {

    @Select("select * from tb_voucher where id = #{voucherId};")
    Voucher getVoucherById(Long voucherId);

    @Select("select * from tb_seckill_voucher where voucher_id = #{voucherId}")
    SeckillVoucher getSeckillVoucherById(Long voucherId);

    @Update("update tb_seckill_voucher set stock = stock - 1 where voucher_id = #{voucherId} and stock > 0")
    boolean subStockById(Long voucherId);

    @Insert("insert into tb_voucher_order(id, user_id, voucher_id) " +
            "values (#{id}, #{userId}, #{voucherId})")
    void createVoucherOrder(VoucherOrder voucherOrder);

    @Select("select * from tb_voucher_order where voucher_id = #{voucherId} and user_id = #{userId}")
    Object getVoucherOrder(Long voucherId, Long userId);
}
