-- ============================================================
-- 秒杀库存原子预扣 + 用户限购校验
--
-- KEYS[1] = 库存键      sk:stock:{activityId}:{goodsId}
-- KEYS[2] = 用户已购键  sk:bought:{activityId}:{goodsId}   (Hash: userId -> 已购数量)
-- ARGV[1] = 本次购买数量
-- ARGV[2] = 用户 ID
-- ARGV[3] = 每人限购数量
--
-- 返回值约定：
--   -1  库存键不存在，活动未预热（或预热被清掉了）
--   -2  库存不足，本次未扣减
--   -3  超出每人限购，本次未扣减
--   >=0 扣减成功，返回扣减后的剩余库存（可能正好是 0，即抢到了最后一件）
--
-- 为什么不用 0 表示失败：扣完最后一件时 DECRBY 的返回值也是 0，
-- 两个语义撞在一起，最后一件商品会被误判成售罄。失败值一律取负数。
--
-- 为什么必须放在 Lua 里：读取库存、校验限购、扣减、记录用户这四步若分开，
-- 两个请求可以同时读到 1 份库存然后都扣成功；同一用户的两个并发请求也会
-- 双双通过限购校验。Redis 单线程执行脚本保证这四步之间不会插入其它命令。
--
-- 为什么用 Hash 存已购数量而不是 Set：limitPerUser 允许大于 1，
-- Set 只能表达「买过/没买过」，Hash 才能表达「买了几个」。

local stock = redis.call('GET', KEYS[1])
if not stock then
    return -1
end

local want = tonumber(ARGV[1])
local limit = tonumber(ARGV[3])

-- HGET 返回 false 表示该用户从未购买，按 0 处理
local bought = tonumber(redis.call('HGET', KEYS[2], ARGV[2]) or '0')
if bought + want > limit then
    return -3
end

local remain = tonumber(stock)
if remain < want then
    return -2
end

local after = redis.call('DECRBY', KEYS[1], want)
redis.call('HINCRBY', KEYS[2], ARGV[2], want)

-- 用户已购 Hash 的 TTL 跟随库存键，避免活动结束后永久堆积
local ttl = redis.call('TTL', KEYS[1])
if ttl > 0 then
    redis.call('EXPIRE', KEYS[2], ttl)
end

return after
