---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by 13347.
--- DateTime: 2024/5/10 11:49
---
-- 判断是否和指定的标识一致
if (ARGV[1] == redis.call('get', KEYS[1])) then
    -- 释放锁lock
    return redis.call('del', KEYS[1])
end
return 0