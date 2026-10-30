import request from './request'

// ===== 认证 =====
export function register(data) {
  return request.post('/auth/register', data)
}

export function login(data) {
  return request.post('/auth/login', data)
}

export function logout() {
  return request.post('/auth/logout')
}

export function getCurrentUser() {
  return request.get('/auth/me')
}

// ===== 活动（无需登录）=====
export function getActivityPage(params) {
  return request.get('/activities', { params })
}

export function getActivityDetail(id) {
  return request.get(`/activities/${id}`)
}

// ===== 秒杀（需登录）=====
export function doSeckill(activityId, goodsId) {
  return request.post(`/seckill/${activityId}/${goodsId}`)
}

export function getSeckillResult(requestId) {
  return request.get(`/seckill/result/${requestId}`)
}

export function getMyOrders(params) {
  return request.get('/seckill/orders', { params })
}

export function payOrder(orderNo) {
  return request.post(`/seckill/orders/${orderNo}/pay`)
}
