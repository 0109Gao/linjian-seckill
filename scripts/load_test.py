#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
林间秒杀 · 并发压测脚本（Python asyncio + aiohttp）

用途：对容器化后的秒杀系统做「高并发抢购」压测，统计吞吐/时延/错误率，
      验证「零超卖、错误率可控」等核心目标。

与 JMeter(.jmx) 的关系：
  - 本脚本是「立即可跑」的轻量压测，适合快速拿基线数据；
  - docs/压测报告.md 的正式数据以 JMeter 聚合报告为准（技术方案 D9 约定）。

用法：
  python scripts/load_test.py \
      --base http://localhost \
      --activity 1001 --goods 2003 \
      --users 500 --duration 30

核心流程（每个虚拟用户）：
  登录（拿 token）→ 抢购（POST /api/seckill/{aid}/{gid}）→ 轮询结果
统计：
  - 抢购接口的 TPS、P50/P95/P99、错误率
  - 最终库存（验证是否超卖）

依赖：pip install aiohttp
"""
import argparse
import asyncio
import json
import time
import statistics
import sys

import aiohttp


def parse_args():
    p = argparse.ArgumentParser(description="林间秒杀并发压测")
    p.add_argument("--base", default="http://localhost", help="后端基地址（经 nginx 或直连 8081）")
    p.add_argument("--activity", type=int, default=1001, help="活动 ID")
    p.add_argument("--goods", type=int, default=2003, help="商品 ID")
    p.add_argument("--users", type=int, default=500, help="并发用户数")
    p.add_argument("--rounds", type=int, default=1, help="每用户抢购次数（默认 1，模拟真实单次抢购）")
    p.add_argument("--duration", type=int, default=30, help="压测持续时间（秒，保留参数，once 模式下忽略）")
    p.add_argument("--prefix", default="load", help="压测用户名前缀")
    p.add_argument("--password", default="123456", help="压测用户密码")
    p.add_argument("--no-register", action="store_true", help="不注册用户（假定已注册）")
    return p.parse_args()


class Result:
    """单次抢购的结果记录"""

    __slots__ = ("ok", "status", "latency_ms", "http_status", "code", "msg")

    def __init__(self, ok, status, latency_ms, http_status=0, code=0, msg=""):
        self.ok = ok
        self.status = status
        self.latency_ms = latency_ms
        self.http_status = http_status
        self.code = code
        self.msg = msg


class LoadStats:
    def __init__(self):
        self.total = 0
        self.ok = 0
        self.failed = 0
        self.latencies = []  # 全部抢购请求的时延
        self.by_status = {}
        self.by_code = {}

    def add(self, r: Result):
        self.total += 1
        self.latencies.append(r.latency_ms)
        if r.ok:
            self.ok += 1
        else:
            self.failed += 1
        self.by_status[r.status] = self.by_status.get(r.status, 0) + 1
        if r.code:
            self.by_code[r.code] = self.by_code.get(r.code, 0) + 1

    def pct(self, p):
        if not self.latencies:
            return 0
        s = sorted(self.latencies)
        idx = int(len(s) * p)
        idx = min(idx, len(s) - 1)
        return s[idx]

    def summary(self, duration):
        tps = self.total / duration if duration > 0 else 0
        err_rate = (self.failed / self.total * 100) if self.total else 0
        lines = []
        lines.append(f"总请求数       : {self.total}")
        lines.append(f"成功(PENDING)  : {self.by_status.get('PENDING', 0)}")
        lines.append(f"成功(SUCCESS)  : {self.by_status.get('SUCCESS', 0)}")
        lines.append(f"失败           : {self.failed}")
        lines.append(f"TPS            : {tps:.1f}")
        lines.append(f"错误率         : {err_rate:.2f}%")
        lines.append(f"时延 P50       : {self.pct(0.50):.0f} ms")
        lines.append(f"时延 P95       : {self.pct(0.95):.0f} ms")
        lines.append(f"时延 P99       : {self.pct(0.99):.0f} ms")
        lines.append(f"时延 max       : {max(self.latencies) if self.latencies else 0:.0f} ms")
        if self.by_code:
            lines.append("错误码分布     : " + ", ".join(f"{k}×{v}" for k, v in sorted(self.by_code.items())))
        return "\n".join(lines)


async def login(session, base, username, password):
    """登录并返回 accessToken，失败返回 None"""
    url = f"{base}/api/auth/login"
    try:
        async with session.post(url, json={"username": username, "password": password}) as resp:
            body = await resp.json()
            if body.get("code") == 200:
                return body["data"]["accessToken"]
            return None
    except Exception:
        return None


async def register(session, base, username, password):
    """注册用户（幂等：已存在则忽略）"""
    url = f"{base}/api/auth/register"
    try:
        async with session.post(url, json={"username": username, "password": password,
                                           "nickname": username}) as resp:
            await resp.read()
    except Exception:
        pass


async def do_seckill(session, base, token, activity, goods):
    """抢购一次，返回 Result"""
    url = f"{base}/api/seckill/{activity}/{goods}"
    headers = {"Authorization": f"Bearer {token}"}
    start = time.perf_counter()
    try:
        async with session.post(url, headers=headers) as resp:
            body = await resp.json()
            latency = (time.perf_counter() - start) * 1000
            code = body.get("code")
            # PENDING / SUCCESS 都算抢购「受理成功」；其他 code 是业务失败（库存空/限购/频控等）
            if code == 200:
                status = body.get("data", {}).get("status", "PENDING")
                return Result(True, status, latency, resp.status, code, "")
            return Result(False, "FAILED", latency, resp.status, code, body.get("message", ""))
    except Exception as e:
        latency = (time.perf_counter() - start) * 1000
        return Result(False, "ERROR", latency, 0, 0, str(e))


async def worker(sid, args, stats, barrier, session, token):
    """单个虚拟用户：等所有用户登录完毕后，在 barrier 上同时发起抢购。

    每个用户抢 args.rounds 次（默认 1），两次之间不等待——真实场景里
    用户抢不到就立刻重试，直到库存空或活动结束。
    """
    await barrier.wait()
    for _ in range(args.rounds):
        r = await do_seckill(session, args.base, token, args.activity, args.goods)
        stats.add(r)
        if r.status in ("FAILED",) and r.code == 4001:
            # 已售罄，停止重试（库存没了再抢也无意义）
            break


async def main():
    args = parse_args()
    stats = LoadStats()

    connector = aiohttp.TCPConnector(limit=args.users * 2, force_close=False)
    timeout = aiohttp.ClientTimeout(total=10)

    print(f"压测参数：base={args.base} activity={args.activity} goods={args.goods} "
          f"users={args.users} rounds={args.rounds}")
    print("预热：注册并登录所有用户...")

    async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
        # 先注册 + 登录所有用户，拿到各自的 token
        sem = asyncio.Semaphore(50)

        async def _prepare(sid):
            username = f"{args.prefix}{sid:04d}"
            async with sem:
                if not args.no_register:
                    await register(session, args.base, username, args.password)
                return await login(session, args.base, username, args.password)

        tokens = await asyncio.gather(*[_prepare(i) for i in range(args.users)])
        valid = [(i, t) for i, t in enumerate(tokens) if t]
        print(f"登录成功 {len(valid)}/{args.users} 个用户")

        # 用 barrier 让所有用户同时发起第一波抢购（模拟活动开始瞬间的尖峰）
        barrier = asyncio.Barrier(len(valid))
        print(f"开始压测：{len(valid)} 用户同时抢购，每用户 {args.rounds} 次 ...")
        start = time.perf_counter()

        tasks = [asyncio.create_task(worker(i, args, stats, barrier, session, t))
                 for i, t in valid]
        await asyncio.gather(*tasks, return_exceptions=True)
        elapsed = time.perf_counter() - start

    print("\n===== 抢购压测结果 =====")
    print(stats.summary(elapsed))
    print(f"实际耗时       : {elapsed:.1f} s")

    # 打印示例错误（前 5 条）
    return stats


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        sys.exit(130)
