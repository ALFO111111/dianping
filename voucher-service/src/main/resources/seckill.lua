-- 1.判断库存是否充足
local voucherId = ARGV[1]
local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 1.1不充足，返回1，结束
-- redis返回的是字符串，我们这里需要进行转化到数字再比较
if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end
-- 2.充足，判断用户是否下单
-- sismember可以判断是否在
local userId = ARGV[2]
-- 2.1已经下单，返回2，结束
if (redis.call('sismember', orderKey, userId) == 1) then
    return 2
end
-- 3.没下过单，扣减优惠券库存
-- incrby key -1
redis.call('incrby', stockKey, -1)
-- 3.1将userId存入当前优惠券的set集合
-- sadd orderKey userId
redis.call('sadd', orderKey, userId)
-- 3.2返回0
-- 获取订单id
local orderId = ARGV[3]
-- 发送消息到队列： XADD stream.orders * k1 v1...
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)

return 0