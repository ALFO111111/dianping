package com.alfo.voucher.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alfo.common.domain.dto.Result;
import com.alfo.common.utils.UserDTOHolder;
import com.alfo.common.utils.redis.RedisIdWorker;
import com.alfo.voucher.domain.po.VoucherOrder;
import com.alfo.voucher.mapper.VoucherOrderMapper;
import com.alfo.voucher.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private VoucherOrderMapper voucherOrderMapper;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private IVoucherOrderService proxy;
    //单线程池
    private final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    //Spring中提供的：对象创建时首先执行该方法
    @PostConstruct
    private void init() {
//        proxy = (IVoucherOrderService) AopContext.currentProxy();
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    private class VoucherOrderHandler implements Runnable {
        String queueName = "stream.orders";
        @Override
        public void run() {
            System.out.println(queueName);
            while (true) {
                try {
                    //1.获取消息队列中的订单信息（用户id、订单id、商品id）
                    //XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS streams.order >
                    List<MapRecord<String, Object, Object>> read = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    //2.判断是否获取成功
                    //2.1获取失败，说明没有消息，继续下一次循环
                    if (read == null || read.isEmpty()) {
                        continue;
                    }
                    System.out.println("----------------");
                    //2.2获取成功，可以下单
                    //MapRecord内部封装的就是Map
                    MapRecord<String, Object, Object> record = read.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                    //3.下单完成，ACK下单确认:SACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());

                } catch (Exception e) {
                    log.error("处理订单异常：" + e);
                    handlePendingList();
                }
            }
        }
        private void handlePendingList() {
            while (true) {
                try {
                    //1.获取消息队列中订单信息 XREADGROUP GROUP g1 c1 COUNT 1 STREAMS streams.order 0（0读的是pending-list）
                    List<MapRecord<String, Object, Object>> read = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    //2.判断是否获取成功
                    if (read == null || read.isEmpty()) {
                        break;
                    }
                    //3.解析消息中的订单信息
                    MapRecord<String, Object, Object> record = read.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    //4.获取成功下单：
                    handleVoucherOrder(voucherOrder);

                } catch (InterruptedException e) {
                    log.error("pending-list订单处理异常", e);
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) throws InterruptedException {
        Long userId = voucherOrder.getUserId();
        //获取锁
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
        //没获取到，返回提示信息
        if (!isLock) {
            log.error("不允许重复下单");
            return;
        }
        try {
            //获取到执行下面逻辑
            proxy.createVoucherOrder(voucherOrder);
        }catch (Exception e) {
            log.error("下单失败！");
        } finally {
          lock.unlock();
        }

    }


    @Override
    public Result seckillVoucher(Long voucherId) throws InterruptedException {
        //获取用户
        Long userId = UserDTOHolder.getUserDTO().getId();
        //获取订单id
        long orderId = redisIdWorker.nextId("order");
        // 1.执行Lua脚本
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId));
        // 2.判断脚本是否为0
        int r = result.intValue();
        // 2.1不为0，没有购买资格
        if (r != 0) {
            return Result.fail(r == 1 ? "库存不足" : "已经下过单了");
        }
        // 获取当前类的代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        return Result.ok(voucherId);
    }

    @Transactional
    public Result createVoucherOrder(VoucherOrder voucherOrder) {
        //一人一单
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();

        //3.库存充足
        //3.1根据优惠券id和用户id查询订单
        Object isGot = voucherOrderMapper.getVoucherOrder(voucherId, userId);
        //3.2如果存在，返回异常结果
        if (isGot != null) {
            return Result.fail("该用户已领取过");
        }
        //3.3不存在，扣除库存
        boolean success = voucherOrderMapper.subStockById(voucherId);
        if (!success) {
            return Result.fail("更新失败，stock数量不符");
        }
        voucherOrderMapper.createVoucherOrder(voucherOrder);
        //3.2返回订单id
        return Result.ok(voucherOrder.getId());
    }
}