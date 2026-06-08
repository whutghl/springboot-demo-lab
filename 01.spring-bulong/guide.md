# 布隆过滤器演示模块 — 使用说明

## 一、启动后 curl 验证

```bash
# 1. 启动项目
mvn spring-boot:run

# 2. 查预填数据（user_1 ~ user_5 已在启动时写入）
curl "http://localhost:10000/bloom/contains?value=user_1"
# → {"mightExist":true, "hint":"布隆过滤器返回true：元素可能存在（有小概率是误判）"}

# 3. 查不存在的数据
curl "http://localhost:10000/bloom/contains?value=user_999"
# → {"mightExist":false, "hint":"布隆过滤器返回false：元素一定不存在（绝不会漏判）"}

# 4. 动态添加新元素
curl "http://localhost:10000/bloom/add?value=user_100"
curl "http://localhost:10000/bloom/contains?value=user_100"
# → {"mightExist":true}
```

## 二、如何人为制造「误判存在（false positive）」

布隆过滤器的 fpp=0.01，所以大约每 100 次对不存在的元素查询，会有 1 次误判为 true。要明显观察到误判：

```bash
# 用脚本批量添加大量元素，让位数组越来越"拥挤"
for i in $(seq 1 10000); do
  curl -s "http://localhost:10000/bloom/add?value=item_$i" > /dev/null
done

# 然后查询一个从未添加过的元素，有较高概率返回 true
curl "http://localhost:10000/bloom/contains?value=never_added_abc"
```

原理：插入元素越多，位数组中 1 的比例越高，新查询的 k 个哈希位置碰巧全为 1 的概率就越大，误判率上升。这也是为什么 `expectedInsertions` 必须合理预估——超容量后 fpp 会急剧恶化。

## 三、离生产级还差哪几步

| 关键点 | 说明 |
|---|---|
| **1. 数据不持久化** | 当前是内存 Bean，重启丢失。生产需启动时从 DB 全量加载，或用 Redis Bitmap / RedisBloom |
| **2. 不支持删除** | 布隆过滤器只能置 1 不能置 0，删数据后会产生 false positive。生产可用 Counting Bloom Filter 解决 |
| **3. 单机不共享** | 多实例部署时各节点布隆过滤器数据不一致。需 Redis 集中式布隆过滤器 |
| **4. 容量超限无告警** | 插入数超过 expectedInsertions 后 fpp 会恶化，需监控实际插入量并告警 |
| **5. 预热时效性** | 启动预热和运行时写入之间有间隙，期间新数据可能漏入 DB。需要双写保障 |

## 四、核心原理

布隆过滤器能挡缓存穿透，靠的就是 **"返回 false 则一定不存在"** 这个数学保证——只要布隆过滤器说没有，就不用去查缓存和数据库了，直接挡掉。
