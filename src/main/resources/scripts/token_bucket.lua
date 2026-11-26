-- KEYS[1] = Global bucket key
-- KEYS[2] = Endpoint bucket key

-- ARGV[1] = Global capacity
-- ARGV[2] = Global refill rate
-- ARGV[3] = Endpoint capacity
-- ARGV[4] = Endpoint refill rate

-- Use Redis server time.
local time = redis.call('TIME')

local now = tonumber(time[1]) * 1000
           + math.floor(tonumber(time[2]) / 1000)

local function loadBucket(key, capacity, refillRate)

    local values = redis.call(
        'HMGET', key, 'tokens', 'last_refill_ms'
    )

    local tokens = tonumber(values[1])
    local lastRefill = tonumber(values[2])

    -- Initialize missing buckets as full.
    if tokens == nil or lastRefill == nil then
        tokens = capacity
        lastRefill = now
    end

    local elapsed = math.max(0, now - lastRefill) / 1000

    tokens = math.min(
        capacity,
        tokens + elapsed * refillRate
    )

    return tokens
end

local globalCapacity = tonumber(ARGV[1])
local globalRefill = tonumber(ARGV[2])

local endpointCapacity = tonumber(ARGV[3])
local endpointRefill = tonumber(ARGV[4])

-- Validate configuration before performing writes.
if not globalCapacity or not globalRefill
    or not endpointCapacity or not endpointRefill
    or globalCapacity < 1 or endpointCapacity < 1
    or globalRefill <= 0 or endpointRefill <= 0 then
    return redis.error_reply('Invalid bucket configuration')
end

-- Refill both buckets.
local globalTokens = loadBucket(
    KEYS[1], globalCapacity, globalRefill
)

local endpointTokens = loadBucket(
    KEYS[2], endpointCapacity, endpointRefill
)

local allowed = (
    globalTokens >= 1 and endpointTokens >= 1
)

local retryAfterMs = 0

if allowed then

    globalTokens = globalTokens - 1
    endpointTokens = endpointTokens - 1

else

    if globalTokens < 1 then
        retryAfterMs = math.max(
            retryAfterMs,
            math.ceil((1 - globalTokens) / globalRefill * 1000)
        )
    end

    if endpointTokens < 1 then
        retryAfterMs = math.max(
            retryAfterMs,
            math.ceil((1 - endpointTokens) / endpointRefill * 1000)
        )
    end

end

-- Persist updated bucket states.
redis.call(
    'HSET', KEYS[1],
    'tokens', tostring(globalTokens),
    'last_refill_ms', tostring(now)
)

redis.call(
    'HSET', KEYS[2],
    'tokens', tostring(endpointTokens),
    'last_refill_ms', tostring(now)
)

-- Expire keys after enough inactivity for a bucket
-- to have fully refilled, with an additional margin.
local globalTtl = math.max(
    60000,
    math.ceil(globalCapacity / globalRefill * 2000)
)

local endpointTtl = math.max(
    60000,
    math.ceil(endpointCapacity / endpointRefill * 2000)
)

redis.call('PEXPIRE', KEYS[1], globalTtl)
redis.call('PEXPIRE', KEYS[2], endpointTtl)

return {
    allowed and 1 or 0,
    tostring(globalTokens),
    tostring(endpointTokens),
    retryAfterMs
}