/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.common.util

import java.lang._
import java.util.Set
import redis.clients.jedis.{Jedis, JedisCluster, Pipeline, ScanParams}

import scala.collection.immutable
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


/**
 *  <<出征>>
 *
 * 让我扭过头决绝地走
 * 擦干泪水松开了母亲的手
 * 敌忾同仇神州在紧要关头
 * 一声大吼 同志们全体都有
 *
 * 请接受我的请命
 * 就为了咱的百姓
 * 就为我姊妹兄弟
 * 请让我按下手印
 * 就为这天下太平
 * 就为这多少生命
 * 为这医者仁心
 * 就为我中华大地
 *
 * 他敬礼风中飘的旗
 * 那冲锋号也吹的急
 * 我连夜向疫区行进
 * 你誓要灭掉这场病
 * 多少的英雄在集结
 * 就让我为你冲在前
 * 所有的心都紧相连
 * 就会让我们更团结
 *
 * 怒发冲冠凭阑处
 * 潇潇雨歇出征夜
 * 抬望眼仰天长啸
 * 壮怀激烈我魂不灭
 * 三十功名尘与土
 * 八千里路云和月
 * 长江水不可断绝
 * 卫我同胞万万千
 * 岂曰无衣 与子同袍
 * 与子同泽 与子同裳
 *
 * 苟利国家生死以
 * 岂因祸福避趋之
 * 身袭白衣问自己
 * 是否能悬壶济世
 * 我念着先辈的意志
 * 救死扶伤的义士
 * 留取丹心照汗青
 * 那是我民族的气质
 *
 * 一定要阻断 这苦难 就顶住 武汉
 * 让我为请战 的同伴 再擦干 血汗
 * 拯救那病患
 * 内心充满胜算
 * 就算惊涛拍岸
 * 且看我如何应战
 *
 * 看这一次
 * 中华儿女决不会低下头
 * 活这一世
 * 我与你共患难风雨同舟
 * 看这一次
 * 在出发之前唱起满江红
 * 看这一次
 * 无数的双手撑起黄鹤楼
 *
 */

/**
 * @author benjobs
 */
object RedisUtils extends Logger {

  /**
   * exists
   *
   * @param key
   * @return
   */
  def exists(key: String)(implicit endpoint: RedisEndpoint): Boolean = doRedis(_.exists(key))

  /**
   * get
   *
   * @param key
   * @return
   */
  def get(key: String)(implicit endpoint: RedisEndpoint): String = doRedis(_.get(key))


  def hget(key: String, field: String)(implicit endpoint: RedisEndpoint): String = doRedis(_.hget(key, field))

  /**
   * get
   *
   * @param key
   * @return
   */
  def setex(key: String, seconds: Int, value: String)(implicit endpoint: RedisEndpoint): String = doRedis(_.setex(key, seconds, value))


  def hsetnx(key: String, field: String, value: String)(implicit endpoint: RedisEndpoint): Long = doRedis(_.hsetnx(key, field, value))
  /**
   * mget
   *
   * @param keys
   * @param endpoint
   * @return
   */
  def mget(keys: Array[String])(implicit endpoint: RedisEndpoint): Array[String] = doRedis(_.mget(keys: _*).asScala.toArray)


  /**
   * del
   *
   * @param key
   * @param endpoint
   * @return
   */
  def del(key: String)(implicit endpoint: RedisEndpoint): Long = doRedis(_.del(key))

  def set(key: String, value: String, ttl: Int = 0)(implicit endpoint: RedisEndpoint): String = doRedis(r => {
    val s = r.set(key, value)
    if (ttl > 0) {
      r.expire(key, ttl)
    }
    s
  })

  /**
   * hset
   *
   * @param key
   * @param field
   * @param value
   * @return
   */
  def hset(key: String, field: String, value: String, ttl: Int = 0)(implicit endpoint: RedisEndpoint): Long = doRedis(r => {
    val s = r.hset(key, field, value)
    if (ttl > 0) {
      r.expire(key, ttl)
    }
    s
  })


  /**
   *
   * @param key
   * @param hash
   * @param ttl
   * @param endpoint
   * @return
   */
  def hmset(key: String, hash: Map[String, String], ttl: Int = 0)(implicit endpoint: RedisEndpoint): String = doRedis(r => {
    val s = r.hmset(key, hash.asJava)
    if (ttl > 0) {
      r.expire(key, ttl)
    }
    s
  })

  /**
   *
   * @param key
   * @param fields
   * @param endpoint
   * @return
   */
  def hmget(key: String, fields: List[String])(implicit endpoint: RedisEndpoint): List[String] = doRedis(_.hmget(key, fields: _*).asScala.toList)

  /**
   *
   * @param key
   * @param endpoint
   * @return
   */
  def hgetAll(key: String)(implicit endpoint: RedisEndpoint): Map[String, String] = doRedis(_.hgetAll(key).asScala.toMap)

  /**
   *
   * @param key
   * @param fields
   * @param endpoint
   * @return
   */
  def hdel(key: String, fields: immutable.List[String])(implicit endpoint: RedisEndpoint): Long = {
    if (key == null || fields == null || fields.isEmpty) 0L
    else doRedis(_.hdel(key, fields.toArray: _*))
  }

  /**
   *
   * @param key
   * @param members
   * @param endpoint
   * @return
   */
  def sadd(key: String, members: immutable.List[String])(implicit endpoint: RedisEndpoint): Long = doRedis(_.sadd(key, members.toArray: _*))

  def smembers(key: String)(implicit endpoint: RedisEndpoint): Set[String] = doRedis(_.smembers(key))

  def srem(key: String, members: immutable.List[String])(implicit endpoint: RedisEndpoint): Long = doRedis(_.srem(key, members.toArray: _*))

  /**
   * 验证是否存在,如果不存在则插入(安装传入的过期时间和value)
   *
   * @param key
   * @param seconds
   * @param value
   * @return
   */
  def existsElseSetex(key: String, seconds: Int, value: String)(implicit endpoint: RedisEndpoint): Boolean = doRedis(x => {
    if (x.exists(key)) true else {
      x.setex(key, seconds, value)
      false
    }
  })

  def getOrElseSetex(key: String, seconds: Int, value: String)(implicit endpoint: RedisEndpoint): String = doRedis(x => {
    val v = x.getSet(key,value)
    if(v == null) {
      x.setex(key, seconds, value)
    }
    v
  })


  def existsElseSetnx(key: String, value: String)(implicit endpoint: RedisEndpoint): Boolean = doRedis(x => {
    if (x.exists(key)) true else {
      x.setnx(key, value)
      false
    }
  })

  def getOrElseSetnx(key: String, seconds: Int, value: String)(implicit endpoint: RedisEndpoint): String = doRedis(x => {
    val v = x.getSet(key,value)
    if(v == null) {
      x.setnx(key, value)
    }
    v
  })

  def existsElseHSetnx(key: String, field: String, value: String, ttl: Int = 0)(implicit endpoint: RedisEndpoint): Boolean = doRedis(x => {
    if (x.hexists(key, field)) true else {
      x.hsetnx(key, value, value)
      if (ttl > 0) {
        x.expire(key, ttl)
      }
      false
    }
  })

  def getOrElseHSetnx(key: String, field: String, value: String, ttl: Int = 0)(implicit endpoint: RedisEndpoint): String = doRedis(x => {
    val v = x.hget(key,field)
    if(v == null) {
      x.hsetnx(key, value, value)
      if (ttl > 0) {
        x.expire(key, ttl)
      }
    }
    v
  })

  /**
   *
   * @param key
   * @param value
   */
  def hexistsElseHset(key: String, field: String, value: String)(implicit endpoint: RedisEndpoint): Boolean = doRedis(x => {
    if (x.hexists(key, field)) true else {
      x.hset(key, field, value)
      false
    }
  })


  def getOrElseHset(key: String, field: String, value: String)(implicit endpoint: RedisEndpoint): String = doRedis(x => {
    val v = x.hget(key,field)
    if(v == null) {
      x.hset(key, field, value)
    }
    v
  })

  def hincrBy(key: String, field: String, value: Long, ttl: Int = 0)(implicit endpoint: RedisEndpoint): Long = doRedis(x => {
    val ret = x.hincrBy(key, field, value)
    if (ret == value && ttl > 0) x.expire(key, ttl)
    ret
  })

  def hincrByFloat(key: String, field: String, value: Double, ttl: Int = 0)(implicit endpoint: RedisEndpoint): Double = doRedis(x => {
    val ret = x.hincrByFloat(key, field, value)
    if (ret == value && ttl > 0) x.expire(key, ttl)
    ret
  })

  def incrBy(key: String, value: Long, ttl: Int = 0)(implicit endpoint: RedisEndpoint): Long = doRedis(x => {
    val ret = x.incrBy(key, value)
    if (ret == value && ttl > 0) x.expire(key, ttl)
    ret
  })

  def incrByFloat(key: String, value: Double, ttl: Int = 0)(implicit endpoint: RedisEndpoint): Double = doRedis(x => {
    val ret = x.incrByFloat(key, value)
    if (ret == value && ttl > 0) x.expire(key, ttl)
    ret
  })

  /**
   * 批量写入
   *
   * @param kvs
   * @param ttl
   */
  def mSets(kvs: Seq[(String, String)], ttl: Int = 0)(implicit endpoint: RedisEndpoint): Long = doRedis(x => {
    val start = System.currentTimeMillis()
    val pipe = x.pipelined()
    kvs.foreach { case (k, v) =>
      pipe.mset(k, v)
      if (ttl > 0) pipe.expire(k, ttl)
    }
    pipe.sync()
    System.currentTimeMillis() - start
  })

  def mSetex(kvs: Seq[(String, String)], ttl: Int = 0)(implicit endpoint: RedisEndpoint): Long = doRedis(x => {
    val start = System.currentTimeMillis()
    val pipe = x.pipelined()
    kvs.foreach { case (k, v) =>
      pipe.setex(k, ttl, v)
    }
    pipe.sync()
    System.currentTimeMillis() - start
  })

  def expire(key: String, s: Int)(implicit endpoint: RedisEndpoint) = doRedis(_.expire(key, s))

  def delByPattern(key: String)(implicit endpoint: RedisEndpoint) = doRedis(r => {
    val scanParams = new ScanParams
    scanParams.`match`(key)
    scanParams.count(10000)
    var cursor = ScanParams.SCAN_POINTER_START
    do {
      val scanResult = r.scan(cursor, scanParams)
      cursor = scanResult.getStringCursor()
      scanResult.getResult().asScala.foreach(x => {
        r.del(x)
      })
    } while (cursor != "0")
  })

  def hlen(key: String)(implicit endpoint: RedisEndpoint): Long = doRedis(_.hlen(key))

  def doRedis[R](f: Jedis => R)(implicit endpoint: RedisEndpoint): R = {
    val redis = RedisClient.connect(endpoint)
    val result = f(redis)
    Try {
      redis.close()
    } match {
      case Success(_) => logger.debug("jedis.close successful.")
      case Failure(_) => logger.error("jedis.close failed.")
    }
    result
  }

  def doCluster[R](f: JedisCluster => R)(implicit endpoint: RedisEndpoint*): R = {
    val cluster = RedisClient.connectCluster(endpoint: _*)
    val result = f(cluster)
    Try {
      cluster.close()
    } match {
      case Success(_) => logger.debug("jedis.close successful.")
      case Failure(_) => logger.error("jedis.close failed.")
    }
    result
  }

  def doPipe[R](f: Pipeline => R)(implicit endpoint: RedisEndpoint): R = {
    val redis = RedisClient.connect(endpoint)
    val pipe = redis.pipelined()
    val result = f(pipe)
    Try {
      pipe.sync()
      pipe.close()
      redis.close()
    } match {
      case Success(_) => logger.debug("pipe.close successful.")
      case Failure(_) => logger.error("pipe.close failed.")
    }
    result
  }
}