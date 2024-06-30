package com.alfo.voucher.service;


import com.alfo.common.domain.dto.Result;
import com.alfo.voucher.domain.po.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {
    Result seckillVoucher(Long voucherId) throws InterruptedException;

    Result createVoucherOrder(VoucherOrder voucherOrder);
}
