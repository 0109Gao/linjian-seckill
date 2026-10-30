# -*- coding: utf-8 -*-
"""演示账号：抢 2 单（1 单支付、1 单待支付），让订单页与结果页有真实数据。"""
import json, time, urllib.request
API='http://localhost/api'
def call(m,p,b=None,t=None):
    d=json.dumps(b,ensure_ascii=False).encode() if b is not None else None
    r=urllib.request.Request(API+p,data=d,method=m)
    r.add_header('Content-Type','application/json; charset=utf-8')
    if t: r.add_header('Authorization','Bearer '+t)
    with urllib.request.urlopen(r,timeout=20) as x: return json.loads(x.read().decode())

U='demo_linjian'
call('POST','/auth/register',{'username':U,'password':'123456','nickname':'林小满'})
lg=call('POST','/auth/login',{'username':U,'password':'123456'})
T=lg['data']['accessToken']
print('演示账号登录 OK, userId=', lg['data']['userId'])

# 两个进行中活动各抢 1 单
targets=[('2116127895112880130','2001','双十一预热'), ('2116127895213543425','2003','器具焕新')]
reqs=[]
for act,gid,label in targets:
    r=call('POST','/seckill/%s/%s'%(act,gid),None,T)
    print('  %s 抢购 code=%s requestId=%s'%(label, r.get('code'), r.get('data',{}).get('requestId')))
    if r.get('data',{}).get('requestId'): reqs.append(r['data']['requestId'])
time.sleep(3)

# 第一单支付，第二单留待支付
orders=call('GET','/seckill/orders?pageNum=1&pageSize=10',None,T)['data']['list']
print('  订单数:', len(orders))
for i,o in enumerate(orders):
    print('   %s status=%s goods=%s'%(o['orderNo'], o['status'], o.get('goodsName')))
if orders:
    pay=call('POST','/seckill/orders/%s/pay'%orders[0]['orderNo'],None,T)
    print('  支付第一单 code=%s'%pay.get('code'))
print('\n结果页可用 requestId:', reqs)
