import request from './request'

// ===== 认证 =====
export function login(data) {
  return request.post('/auth/login', data)
}

export function logout() {
  return request.post('/auth/logout')
}

export function getCurrentUser() {
  return request.get('/auth/me')
}

// ===== 商品管理 =====
export function getGoodsPage(params) {
  return request.get('/admin/goods', { params })
}

export function getGoods(id) {
  return request.get(`/admin/goods/${id}`)
}

export function createGoods(data) {
  return request.post('/admin/goods', data)
}

export function updateGoods(id, data) {
  return request.put(`/admin/goods/${id}`, data)
}

export function deleteGoods(id) {
  return request.delete(`/admin/goods/${id}`)
}

// ===== 活动管理 =====
export function getActivityPage(params) {
  return request.get('/admin/activities', { params })
}

export function getActivityDetail(id) {
  return request.get(`/activities/${id}`)
}

export function createActivity(data) {
  return request.post('/admin/activities', data)
}

export function updateActivity(id, data) {
  return request.put(`/admin/activities/${id}`, data)
}

export function cancelActivity(id) {
  return request.post(`/admin/activities/${id}/cancel`)
}

export function preheatActivity(id) {
  return request.post(`/admin/activities/${id}/preheat`)
}

// ===== 订单查询 =====
export function getOrderPage(params) {
  return request.get('/admin/orders', { params })
}

// ===== 库存管理 =====
export function getStockByActivity(activityId) {
  return request.get(`/admin/stock/activity/${activityId}`)
}

export function adjustStock(data) {
  return request.post('/admin/stock/adjust', data)
}

// ===== 看板 =====
export function getDashboard(params) {
  return request.get('/admin/dashboard/overview', { params })
}
