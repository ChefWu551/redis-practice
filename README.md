# Redis常见知识梳理

## 1. NoSQL数据简介

​		not only sql，非关系型数据库(key-value模式的存储结构)，打破了传统的关系型数据库的存储方式，根据实际业务场景，性能优先存储。

- 不遵循sql标准
- 不支持acid
- 远超于sql性能
- 多路复用：采用多个socket链接，复用同一个线程

### 1.1. 使用场景

- 高并发读写
- 海量数据的读写
- 对数据高可扩展性

### 1.2. 拓展

- 行存储类型数据库和列存储类型数据库

## 2. Redis6概述和安装

#### 2.1. 特点概述

- key-value存储结构
- 支持存储的value类型相对更多，包括了string（字符串）、list（链表）、set（集合）、zset（sort set 有序集合）、hash（哈希类型）
- 支持push/pop、add/remove、取交集并集差集，且这些操作都具有原子特性（redis本身不支持acid，这里指的是原子操作，单线程+io多路复用操作，线程是不会被打断的）
- 支持各种排序
- 数据都是缓存在内存中
- redis会周期性的把更新的数据写入磁盘或者把修改操作写入追加的记录文件
- 可以实现主从操作

#### 2.2. 多样数据结构存储持久化（场景）

- 通过list实现自然时间排序的数据，获取最新的N个数据
- 利用Zset（有需集合）获取排名靠前的几个
- Expire过期，短信的时效性控制
- 原子性，自增方法increase，decrease，例如计数器，秒杀
- 利用set集合，对数据大量去重
- 利用list集合，构建队列
- pub/sub模式，发布订阅消息系统

#### 2.3. linux下安装redis

- 系统安装C编译环境
- 解压redis压缩包，进入到文件夹中，make 编译解压的文件
- 用make install完成安装
- cd /usr/local/bin 目录下查看安装结果
    - redis-benchmark: 性能测试工具，可运行测试自己的电脑性能
    - redis-check-aof: 修复有问题的AOF文件，rdb和aof后面讲
    - redis-check-dum: 修复有问题的dump.rdb文件
    - redis-sentinel: redis集群使用
    - **redis-server: redis服务器启动命令**
    - **redis-cli: 客户端，操作入口**
- 配置后台启动
    - 修改redis.conf文件
    - 修改daemonize 的值为yes
    - 使用 redis-server启动
    - 使用redis-cli测试链接状态，eg:pint
- 具体操作步骤

```bash
docker pull redis:6.2.1

docker run -d -p 6379:6379 --name myredis redis:6.2.1

winpty docker exec -it myredis bash

cd /usr/local/bin

redis-cli # 直接进入redis-cli测试链接客户端
```



#### 2.4. 默认6379端口

- 默认初始化0号数据库，一共有16个数据库，通过select来切换数据库，例如：select 15
- redis是**单线程**+**多路IO复用技术**
    - 多路复用：使用一个线程来检查多个文件的描述符（socket）就绪状态，比如调用select和poll函数，传入多个文件描述符，如果有一个文件描述符就绪，则返回，否则阻塞直到超时，得到就绪状态后进行真正的操作可以同一个线程里执行，也可以启动线程执行（例如使用线程池）
- **串行 vs 多线程+锁 vs 单线程(memcached)+多路IO复用(redis)**

## 3. 常用五大数据类型

### 3.1. redis键(key)

```bash
# 进入客户端模式
root@7a9c5452cde4:/usr/local/bin# redis-cli 
127.0.0.1:6379> key
(error) ERR unknown command `key`, with args beginning with:
# 查找key操作
127.0.0.1:6379> keys *
(empty array)
# 添加一个key-value
127.0.0.1:6379> set k1 lucy
OK
127.0.0.1:6379> set k2 mary
OK
127.0.0.1:6379> set k3 jack
OK
127.0.0.1:6379> keys *
1) "k3"
2) "k1"
3) "k2"
# 判断是否存在key=k1，若存在返回1，否则返回0
127.0.0.1:6379> exists k1
(integer) 1
127.0.0.1:6379> exists k4
(integer) 0
# 查看key的类型
127.0.0.1:6379> type k2
string
# 删除一个key=k3
127.0.0.1:6379> del k3
(integer) 1
127.0.0.1:6379> keys *
1) "k1"
2) "k2"
# 删除一个key=k2
# unlink 区别与 delete， 根据value选择非阻塞删除
# 仅将keys从keyspace元数据中删除，真正的删除会在后续异步操作中进行
127.0.0.1:6379> unlink k2
(integer) 1
127.0.0.1:6379> keys *
1) "k1"
# 设置key=k1的过期时间为10s
127.0.0.1:6379> expire k1 10
(integer) 1
# 查看还有多少秒过期
127.0.0.1:6379> ttl k1
(integer) 6
# 值是-2表示已经国企， 值是-1表示永不过期
127.0.0.1:6379> ttl k2
(integer) -2
127.0.0.1:6379> set k2 xiaoming
OK
127.0.0.1:6379> ttl k2
(integer) -1
# 切换db，select 1表示切换到名称为1的数据库
127.0.0.1:6379> select 1
OK
127.0.0.1:6379[1]> keys *
(empty array)
127.0.0.1:6379[1]> select 0
OK
127.0.0.1:6379> keys *
1) "k2"
# 查看当前db含有的数据数量
127.0.0.1:6379> dbsize
(integer) 1
# 清空当前库
127.0.0.1:6379> flushdb
OK
# 通杀全部库
127.0.0.1:6379> flushall
OK
127.0.0.1:6379> keys *
(empty array)
127.0.0.1:6379>

```

### 3.1. 字符串string

​		最基本的数据类型，string是二进制安全的（例如jpg），一个redis中字符串的value最多可以是512M

```
127.0.0.1:6379> flushdb
OK
127.0.0.1:6379> keys *
(empty array)
127.0.0.1:6379> set k1 v100
OK
127.0.0.1:6379> set k2 v200
OK
127.0.0.1:6379> keys *
1) "k1"
2) "k2"
127.0.0.1:6379> get k1
"v100"
# 若存在k1覆盖原有的value值，否则set k1对应的value, 相当于 insert into on dumplicate update 操作
127.0.0.1:6379> set k1 v1100
OK
127.0.0.1:6379> get k1
"v1100"
# 在k1后面追加字符串abc
127.0.0.1:6379> append k1 abc
(integer) 8
127.0.0.1:6379> get k1
"v1100abc"
# 测量值为k1的value字符串长度
127.0.0.1:6379> strlen k1
(integer) 8
# 若k1的key已经存在则set失败，只会set进去不存在的key对应的value值(set key not exist)
127.0.0.1:6379> setnx k1 v1
(integer) 0
127.0.0.1:6379> setnx k3 v300
(integer) 1
127.0.0.1:6379> get k3
"v300"
127.0.0.1:6379> set k3 500
OK
# k4的value自增1，只能针对数值类型，返回值就是更新后的值
127.0.0.1:6379> incr k4
(integer) 1
127.0.0.1:6379> get k4
"1"
127.0.0.1:6379> keys *
1) "k4"
2) "k1"
3) "k2"
4) "k3"
127.0.0.1:6379> get k4
"1"
127.0.0.1:6379> incr k4
(integer) 2
127.0.0.1:6379> get k4
"2"
127.0.0.1:6379> decr k4
(integer) 1
127.0.0.1:6379> get k4
"1"
# k4的value增加10（step）
127.0.0.1:6379> incrby k4 10
(integer) 11
# k4的value减去20（step）
127.0.0.1:6379> decrby k4 20
(integer) -9
127.0.0.1:6379> flushdb
OK
# 批量设置key value
127.0.0.1:6379> mset k1 v1 k2 v2 k3 v3
OK
127.0.0.1:6379> keys *
1) "k1"
2) "k2"
3) "k3"
# 批量获取key value
127.0.0.1:6379> mget k1 k2
1) "v1"
2) "v2"
# 批量设置不存在的key对应的value，若其中有个key存在，则都失败
127.0.0.1:6379> msetnx k11 v11 k22 v22 k3 v4
(integer) 0
127.0.0.1:6379> msetnx k11 v11 k22 v22 k33 v33
(integer) 1
127.0.0.1:6379> keys *
1) "k11"
2) "k33"
3) "k1"
4) "k2"
5) "k3"
6) "k22"
127.0.0.1:6379> get k1
"v1"
# 获取key=k1 索引位置从0开始的后100位
127.0.0.1:6379> getrange k1 0 100
"v1"
127.0.0.1:6379> getrange k1 1 100
"1"
# 获取key=k1从第三位开始以后使用字符串填充
127.0.0.1:6379> setrange k1 3 abcdddddd
(integer) 12
127.0.0.1:6379> get k1
"v1\x00abcdddddd"

```



### 3.2. 列表list

​	特点：单键多值

​	redis列表是简单的字符串列表，按照插入顺序排序，你可以添加一个元素到列表的头部或者尾部；

​	他的底层实际是个双链表，对两端的操作性能很高，通过索引下标的操作中间的节点性能会较差。

```
127.0.0.1:6379> flushdb
OK
# l表示left，跟压栈一样，把从左往右压，例如：lpush 1 2 3  实际存储是： 3 2 1
127.0.0.1:6379> lpush k1 k1v1 k1v2 k1v3 k1v4
(integer) 4
127.0.0.1:6379> lrange k1 2
(error) ERR wrong number of arguments for 'lrange' command
127.0.0.1:6379> lrange k1 2 4
1) "k1v2"
2) "k1v1"
# r表示right，跟压栈一样，从右往左压，k2的值存储结构最后为:k2v1 k2v2 k2v3
127.0.0.1:6379> rpush k2 k2v1 k2v2 k2v3
(integer) 3
127.0.0.1:6379> get k2
(error) WRONGTYPE Operation against a key holding the wrong kind of value
127.0.0.1:6379> get k2
(error) WRONGTYPE Operation against a key holding the wrong kind of value
127.0.0.1:6379> lrange k2 0 4
1) "k2v1"
2) "k2v2"
3) "k2v3"
# 左边吐出一个值
127.0.0.1:6379> lpop k2
"k2v1"
# 右边吐出一个值
127.0.0.1:6379> rpop k2
"k2v3"
# 取出k1的右边的第一个值，放入k2的左边
127.0.0.1:6379> rpoplpush k1 k2
"k1v1"
127.0.0.1:6379> lrange k1 0 100
1) "k1v4"
2) "k1v3"
3) "k1v2"
127.0.0.1:6379> lrange k2 0 100
1) "k1v1"
2) "k2v2"
# 查找k1的索引位置为1的值
127.0.0.1:6379> lindex k1 1
"k1v3"
# 查找k2有多少个元素
127.0.0.1:6379> llen k2
(integer) 2
127.0.0.1:6379> lrange k1 0 100
1) "k1v4"
2) "k1v3"
3) "k1v2"
# 在k1的值为 k1v3的左边插入 值 kinsert，原来：k1v4 k1v3 k1v2 ；后来：k1v4 kinsert k1v3 k1v2
127.0.0.1:6379> linsert k1 before k1v3 kinsert
(integer) 4
127.0.0.1:6379> lrange k1 0 100
1) "k1v4"
2) "kinsert"
3) "k1v3"
4) "k1v2"
# 从左边删除n个value（有多少个删除指定的n个，若不够则默认删完即可）
127.0.0.1:6379> lrem k1 3 "k1v3"
(integer) 1
127.0.0.1:6379> lrange k1 0 100
1) "k1v4"
2) "kinsert"
3) "k1v2"
# 将列表key下标为index的值替换成value
127.0.0.1:6379> lset k2 1 kupdate
OK
127.0.0.1:6379> lrange k1 0 -1
1) "k1v4"
2) "kinsert"
3) "k1v2"



```

- 快速链表quicklist
    - 元素较少的情况下没使用ziplist的压缩列表
    - 元素变多后会使用压缩列表（多个ziplist-ziplist-ziplist双向）

### 3.3. 集合set

​	同list的列表功能，但是能主动去重，本身是无序的，底层是一个hashset结构，字典

```
127.0.0.1:6379> flushdb
OK
# 添加集合key=k1 value是 k1 k2 k3 k4
127.0.0.1:6379> sadd k1 k1 k2 k3 k4
(integer) 4
#  查看k1的set集合
127.0.0.1:6379> smembers k1
1) "k4"
2) "k1"
3) "k2"
4) "k3"
127.0.0.1:6379> sismeber k1 v1
(error) ERR unknown command `sismeber`, with args beginning with: `k1`, `v1`,
127.0.0.1:6379> sismeber k1 k2
(error) ERR unknown command `sismeber`, with args beginning with: `k1`, `k2`,
# 查看key=k1中是否存在值k2
127.0.0.1:6379> sismember k1 k2
(integer) 1
127.0.0.1:6379> sismember k1 k5
(integer) 0
# 查看key=k1的set长度
127.0.0.1:6379> scard k1
(integer) 4
# 批量移除key=k1值是k1和k2
127.0.0.1:6379> srem k1 k1 k2
(integer) 2
127.0.0.1:6379> smembers k1
1) "k4"
2) "k3"
# 随意推出一个元素值
127.0.0.1:6379> spop k1
"k3"
127.0.0.1:6379> smembers k1
1) "k4"
# 随意取出两个值，是随机的
127.0.0.1:6379> srandmember k1 2
1) "k4"
127.0.0.1:6379> sset k1 v1 v2 v3
(error) ERR unknown command `sset`, with args beginning with: `k1`, `v1`, `v2`, `v3`,
127.0.0.1:6379> sadd k1 v1 v2 v3
(integer) 3
127.0.0.1:6379> smember k1
(error) ERR unknown command `smember`, with args beginning with: `k1`,
127.0.0.1:6379> smembers k1
1) "k4"
2) "v2"
3) "v3"
4) "v1"
127.0.0.1:6379> sranmember k1 3
(error) ERR unknown command `sranmember`, with args beginning with: `k1`, `3`,
127.0.0.1:6379> sranmember k1 2
(error) ERR unknown command `sranmember`, with args beginning with: `k1`, `2`,
127.0.0.1:6379> sranmembers k1 2
(error) ERR unknown command `sranmembers`, with args beginning with: `k1`, `2`,
127.0.0.1:6379> srandmembers k1 2
(error) ERR unknown command `srandmembers`, with args beginning with: `k1`, `2`,
127.0.0.1:6379> srandmember k1 2
1) "k4"
2) "v2"
127.0.0.1:6379> srandmember k1 2
1) "k4"
2) "v2"
127.0.0.1:6379> srandmember k1 2
1) "v2"
2) "v3"
127.0.0.1:6379> sadd k2 v22 v222
(integer) 2
127.0.0.1:6379> smove k2 k1
(error) ERR wrong number of arguments for 'smove' command
# 移动k2中的值k222到k1中去
127.0.0.1:6379> smove k2 k1 v222
(integer) 1
127.0.0.1:6379> smembers k1
1) "k4"
2) "v2"
3) "v3"
4) "v1"
5) "v222"
127.0.0.1:6379> smembers k2
1) "v22"
# 取k1和k2的交集
127.0.0.1:6379> sinter k1 k2
(empty array)
127.0.0.1:6379> sadd k2 v2 v1
(integer) 2
# 取k1和k2的交集
127.0.0.1:6379> sinter k1 k2
1) "v2"
2) "v1"
# 取k1和k2的并集
127.0.0.1:6379> sunion k1 k2
1) "k4"
2) "v2"
3) "v3"
4) "v222"
5) "v22"
6) "v1"
# 取k1中不包含k2值的差集
127.0.0.1:6379> sdiff k1 k2
1) "k4"
2) "v222"
3) "v3"
127.0.0.1:6379>

```



### 3.4.  hash哈希

​		redis hash是一个键值对集合。是一个string类型给的field和value的映射表，hash特别适合用于存储对象。类似java里面的<String, Object>结构

​		用户id为查找的key，存储的value用户对象包含姓名，年龄，生日等信息，如果用普通的key/value结构来存储;

​		hash类的数据结构是两种：ziplist，hashtable。当field-value长度较短且个数较少时，使用ziplist，否则使用hashtable存储

- 例如存储一个 user1对象，对应的属性值：{id=1,name=zhangsan,age=20}

  key field value格式类型

```
127.0.0.1:6379> flushdb
OK
# 添加一个user对象1001，对应的属性名是id 属性值1
127.0.0.1:6379> hset user:1001 id 1
(integer) 1
# 添加一个user对象1001，对应的属性名是name 属性值zhangsan
127.0.0.1:6379> hset user:1001 name zhangsan
(integer) 1
# 查看user对象1001里面的属性值id
127.0.0.1:6379> hget user:1001 id
"1"
# 查看user对象1001里面的属性值name
127.0.0.1:6379> hget user:1001 name
"zhangsan"
# 添加一个user属性1002，并添加属性值id和name和age，值分别是2和lisi和30
127.0.0.1:6379> hmset user:1002 id 2 name lisi age 30
OK
127.0.0.1:6379> hget user:1002
(error) ERR wrong number of arguments for 'hget' command
127.0.0.1:6379> hget user:1002 id
"2"
127.0.0.1:6379> hget user:1002 age
"30"
# 判断user对象的1002对象是否有age字段，若有返回1，否则返回0
127.0.0.1:6379> hexist user:1002 age
(error) ERR unknown command `hexist`, with args beginning with: `user:1002`, `age`,
127.0.0.1:6379> hexists user:1002 age
(integer) 1
127.0.0.1:6379> hexists user:1002 gender
(integer) 0
127.0.0.1:6379> hkeys
(error) ERR wrong number of arguments for 'hkeys' command
# 查看user对象的1002对象包含的key
127.0.0.1:6379> hkeys user:1002
1) "id"
2) "name"
3) "age"
# 查看user的1002对象包含的属性值
127.0.0.1:6379> hvals user:1002
1) "2"
2) "lisi"
3) "30"
# 指定user的1002对象的属性age值增加2
127.0.0.1:6379> hincrby user:1002 age 2
(integer) 32
127.0.0.1:6379> hvals user:1002
1) "2"
2) "lisi"
3) "32"
# 添加user对象1002的属性，若存在则添加失败0，否则添加成功1
127.0.0.1:6379> hsetnx user:1002 age 40
(integer) 0
127.0.0.1:6379> hsetnx user:1002 gender mail
(integer) 1
127.0.0.1:6379> hvals user:1002
1) "2"
2) "lisi"
3) "32"
4) "mail"

```

### 3.5. 有需集合zset

​		有序的set集合。

ZSET底层使用了两种数据结构：

		- 等价于java的map<string, double>，且安装double类型有序排列，可通过string找到对英国的score值
		- 跳跃表，根据score的值快速获取string的值

```
127.0.0.1:6379> flushdb
OK
# 添加有需集合
127.0.0.1:6379> zadd topn 200 java 300 c++ 400 mysql 500php
(error) ERR syntax error
127.0.0.1:6379> zadd topn 200 java 300 c++ 400 mysql 500 php
(integer) 4
127.0.0.1:6379> keys *
1) "topn"
# 查看key=topn的有序集合，注意，这里返回的结果是有序的
127.0.0.1:6379> zrange topn 0 -1
1) "java"
2) "c++"
3) "mysql"
4) "php"
# 查看key=topn的有序集合，带对应的值
127.0.0.1:6379> zrange topn 0 -1 withscores
1) "java"
2) "200"
3) "c++"
4) "300"
5) "mysql"
6) "400"
7) "php"
8) "500"
# 查询指定范围内符合的集合的值
127.0.0.1:6379> zrangebyscore topn 300 500
1) "c++"
2) "mysql"
3) "php"
127.0.0.1:6379> zrangebyscore topn 300 500 limit 2
(error) ERR syntax error
# 查询指定范围内符合的集合的值，带对应的值
127.0.0.1:6379> zrangebyscore topn 300 500 withscores
1) "c++"
2) "300"
3) "mysql"
4) "400"
5) "php"
6) "500"
# 查看值是500 -200 的集合，这里返回是空，因为500-200是逆序
127.0.0.1:6379> zrangebyscore topn 500 200
(empty array)
# 翻转集合的顺序，查看值是500 -200 的集合，这里返回不为空
127.0.0.1:6379> zrevrangebyscore topn 500 200
1) "php"
2) "mysql"
3) "c++"
4) "java"
127.0.0.1:6379> zrevrangebyscore topn 500 200 withscores
1) "php"
2) "500"
3) "mysql"
4) "400"
5) "c++"
6) "300"
7) "java"
8) "200"
127.0.0.1:6379> zrevrangebyscore topn 500 200 withscores 3
(error) ERR syntax error
127.0.0.1:6379> zrevrangebyscore topn 500 200 withscores 3limit 0 3
(error) ERR syntax error
# limit offset size
127.0.0.1:6379> zrevrangebyscore topn 500 200 withscores limit 0 3
1) "php"
2) "500"
3) "mysql"
4) "400"
5) "c++"
6) "300"
# 指定key=topn的值java的属性值增加50
127.0.0.1:6379> zincrby topn 50 java
"250"
# 移除属性值是c++的属性和值
127.0.0.1:6379> zrem topn c++
(integer) 1
# 统计当前集合的数量，需要添加指定范围
127.0.0.1:6379> zcount topn
(error) ERR wrong number of arguments for 'zcount' command
127.0.0.1:6379> zcount topn 200 300
(integer) 1
# 属性是java的排序的位置，从0开始
127.0.0.1:6379> zrank topn java
(integer) 0
# 铲鲟topn范围内的属性名（0到-1）表示展示所有
127.0.0.1:6379> zrange topn 0 -1
1) "java"
2) "mysql"
3) "php"
127.0.0.1:6379> zrange topn mysql
(error) ERR wrong number of arguments for 'zrange' command
127.0.0.1:6379> zrank topn mysql
(integer) 1
127.0.0.1:6379>

```

## 4. Redis6配置文件详解

- 控制远程访问、端口号
- 设置访问密码
- 最大连接数、内存占用多少

## 5. Redis6的发布和订阅

![订阅channel与发布流程](image/订阅channel与发布流程.png)

## 6. Redis6新数据类型

### 6.1. bitmap

​	位图存储，大大节省了存储空间

​	例如，原来存储一个客户是否访问，使用set

```
127.0.0.1:6379> setbit bitkey 12 1
(integer) 0
127.0.0.1:6379> keys *
1) "k2"
2) "k1"
3) "bitkey"
4) "k3"
127.0.0.1:6379> getbit bitkey
(error) ERR wrong number of arguments for 'getbit' command
127.0.0.1:6379> getbit bitkey 1
(integer) 0
127.0.0.1:6379> getbit bitkey 112
(integer) 0
127.0.0.1:6379> getbit bitkey 12
(integer) 1
127.0.0.1:6379> bitcount bitkey
(integer) 1
127.0.0.1:6379> 
```

### 6.2. hyperLogLog

### 6.3. geospatial

## 7. Jedis操作Redis6

## 8. Redis6整合spring-boot

## 9. Redis的事务操作

## 10. Redis持久化之RDB

## 11. Redis持久化之AOF

## 12. Redis的主从复制

## 13. Redis6集群

## 14. Redis6应用问题解决

## 15. Redis6新功能