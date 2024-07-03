package com.alfo.voucher.controller;


import com.alfo.common.domain.dto.Result;
import com.alfo.voucher.service.impl.VoucherOrderServiceImpl;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.events.Event;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
@Slf4j
public class VoucherOrderController {

    @Resource
    private VoucherOrderServiceImpl voucherOrderService;

    @PostMapping("seckill/{id}")
    @ApiOperation("秒杀券管理")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) throws InterruptedException {
        log.info("seckillVoucher:{}",voucherId);
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
