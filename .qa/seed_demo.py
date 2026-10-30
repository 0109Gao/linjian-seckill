#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
通过管理端真实接口构造一套演示数据（商品 + 活动），用于项目截图。
只用 HTTP API，不直接改库 —— 这样顺便验证了后台写接口的完整链路。
"""
import json
import urllib.request

API = 'http://localhost/api'

GOODS = [
    # (name, subtitle, imageUrl, originPrice)
    ('林间手冲咖啡豆 250g', '中浅烘焙 · 柑橘尾韵 · 埃塞俄比亚水洗',
     'https://images.unsplash.com/photo-1447933601403-0c6688de566e?w=800&q=80&fm=jpg', '128.00'),
    ('冷萃咖啡液 6 瓶装', '零糖零添加 · 冷藏直达 · 10 秒出一杯',
     'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800&q=80&fm=jpg', '96.00'),
    ('手摇磨豆机', '六星刀盘 · 便携装 · 30 档研磨度',
     'https://images.unsplash.com/photo-1587734195503-904fca47e0e9?w=800&q=80&fm=jpg', '239.00'),
    ('挂耳咖啡礼盒 20 片', '单一产地 · 氮气锁鲜 · 独立包装',
     'https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=800&q=80&fm=jpg', '168.00'),
    ('陶瓷手冲滤杯 V60', '01 号 · 白瓷釉面 · 螺旋导流',
     'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=800&q=80&fm=jpg', '89.00'),
    ('咖啡专用电子秤', '0.1g 精度 · 自带计时 · 手冲模式',
     'https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?w=800&q=80&fm=jpg', '199.00'),
]

ACTIVITIES = [
    # (title, description, startTime, endTime, [(goodsIdx, seckillPrice, stock, limit)])
    ('双十一预热 · 咖啡豆专场', '精选单一产地豆源，整点开抢，每人限购 1 件',
     '2026-10-30 08:00:00', '2026-11-01 23:59:59',
     [(0, '79.00', 120, 1), (1, '59.00', 80, 2), (3, '99.00', 60, 1)]),
    ('器具焕新 · 手冲装备场', '磨豆机、滤杯、电子秤一站式配齐，低至 6 折',
     '2026-10-30 00:00:00', '2026-10-31 23:59:59',
     [(2, '159.00', 50, 1), (4, '49.00', 100, 2), (5, '119.00', 40, 1)]),
    ('周末限定 · 冷萃专场', '周六晚八点准时开抢，冷藏直发，售完即止',
     '2026-11-02 20:00:00', '2026-11-02 22:00:00',
     [(1, '39.00', 200, 3)]),
    ('国庆特惠 · 挂耳礼盒场', '节日限定礼盒，已结束，可查看历史成交',
     '2026-10-01 20:00:00', '2026-10-03 22:00:00',
     [(3, '89.00', 150, 2)]),
]


def call(method, path, body=None, token=None):
    data = json.dumps(body, ensure_ascii=False).encode('utf-8') if body is not None else None
    req = urllib.request.Request(API + path, data=data, method=method)
    req.add_header('Content-Type', 'application/json; charset=utf-8')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    with urllib.request.urlopen(req, timeout=20) as resp:
        return json.loads(resp.read().decode('utf-8'))


def main():
    login = call('POST', '/auth/login', {'username': 'admin', 'password': '123456'})
    token = login['data']['accessToken']
    print('admin 登录 OK')

    # 1. 复用已有商品（按名字匹配），缺的创建
    exist = call('GET', '/admin/goods?pageNum=1&pageSize=100', token=token)['data']['list']
    by_name = {g['name']: g['id'] for g in exist}
    goods_ids = []
    for name, subtitle, img, price in GOODS:
        if name in by_name:
            gid = by_name[name]
            call('PUT', '/admin/goods/%s' % gid,
                 {'name': name, 'subtitle': subtitle, 'imageUrl': img,
                  'originPrice': price, 'status': 1}, token=token)
            print('  更新商品 %-22s id=%s' % (name, gid))
        else:
            r = call('POST', '/admin/goods',
                     {'name': name, 'subtitle': subtitle, 'imageUrl': img,
                      'originPrice': price, 'status': 1}, token=token)
            gid = r['data']
            print('  新建商品 %-22s id=%s' % (name, gid))
        goods_ids.append(gid)

    # 2. 创建活动（同名活动已存在则跳过，避免重复）
    exist_act = call('GET', '/admin/activities?pageNum=1&pageSize=100', token=token)['data']['list']
    act_titles = {a['title'] for a in exist_act}
    for title, desc, st, et, items in ACTIVITIES:
        if title in act_titles:
            print('  活动已存在，跳过: %s' % title)
            continue
        payload = {
            'title': title,
            'description': desc,
            'startTime': st,
            'endTime': et,
            'items': [
                {'goodsId': goods_ids[i], 'seckillPrice': p, 'stock': s, 'limitPerUser': l}
                for (i, p, s, l) in items
            ],
        }
        r = call('POST', '/admin/activities', payload, token=token)
        print('  新建活动 %-24s id=%s items=%d' % (title, r['data'], len(items)))

    print('\n完成。当前活动：')
    page = call('GET', '/activities?pageNum=1&pageSize=20')['data']['list']
    for a in page:
        print('  %-26s %-6s 商品 %s 件' % (a['title'], a['statusDesc'], a['goodsCount']))


if __name__ == '__main__':
    main()
