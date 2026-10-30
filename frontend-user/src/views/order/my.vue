<template>
  <div class="page-wrap">
    <div class="page-title">我的订单</div>

    <div v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="还没有秒杀订单">
        <el-button type="primary" round @click="router.push('/activities')">去逛逛活动</el-button>
      </el-empty>

      <div v-for="order in list" :key="order.id" class="order-item">
        <div class="order-head">
          <span class="order-no">订单号 {{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)" size="small" effect="dark" round>
            {{ order.statusDesc }}
          </el-tag>
        </div>

        <div class="order-body">
          <div class="order-goods">
            <div class="goods-thumb">{{ order.goodsName ? order.goodsName.charAt(0) : '?' }}</div>
            <div>
              <div class="goods-name">{{ order.goodsName }}</div>
              <div class="muted">创建于 {{ formatTime(order.createTime) }}</div>
            </div>
          </div>

          <div class="order-price">
            <span class="price-now">¥{{ order.price }}</span>
            <span class="muted">× {{ order.quantity }}</span>
          </div>

          <div class="order-action">
            <template v-if="order.status === 0">
              <div class="countdown">剩余 {{ remainText(order) }}</div>
              <el-button type="primary" size="small" round :loading="payingId === order.id" @click="handlePay(order)">
                去支付
              </el-button>
            </template>
            <template v-else-if="order.status === 1">
              <div class="muted">支付于 {{ formatTime(order.payTime) }}</div>
            </template>
            <template v-else>
              <div class="muted">{{ order.status === 3 ? '超时未支付已关闭' : '已取消' }}</div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <div v-if="total > pageSize" class="pager">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyOrders, payOrder } from '@/api'

const router = useRouter()

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const payingId = ref(null)
const now = ref(Date.now())

let timer = null

onMounted(() => {
  load()
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})

async function load() {
  loading.value = true
  try {
    const res = await getMyOrders({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handlePay(order) {
  payingId.value = order.id
  try {
    const res = await payOrder(order.orderNo)
    Object.assign(order, res.data)
    ElMessage.success('支付成功')
  } catch (e) {
    await load()
  } finally {
    payingId.value = null
  }
}

function statusType(status) {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  if (status === 3) return 'info'
  return 'info'
}

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function remainText(order) {
  if (!order.expireTime) return '-'
  const expire = new Date(order.expireTime.replace(' ', 'T')).getTime()
  const ms = expire - now.value
  if (ms <= 0) return '已超时'
  const totalSec = Math.floor(ms / 1000)
  const m = Math.floor(totalSec / 60)
  const s = totalSec % 60
  return `${m} 分 ${String(s).padStart(2, '0')} 秒`
}
</script>

<style scoped>
.order-item {
  background: #fff;
  border-radius: var(--sk-radius);
  padding: 14px 18px 16px;
  margin-bottom: 12px;
}

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--sk-border);
}

.order-no {
  font-size: 13px;
  color: var(--sk-text-sub);
  font-variant-numeric: tabular-nums;
}

.order-body {
  display: grid;
  grid-template-columns: 1fr 140px 180px;
  align-items: center;
  gap: 16px;
  padding-top: 14px;
}

.order-goods {
  display: flex;
  align-items: center;
  gap: 12px;
}

.goods-thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffe8e6, #fff3e0);
  color: var(--sk-primary);
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.goods-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 3px;
}

.order-price {
  display: flex;
  align-items: baseline;
  gap: 6px;
  justify-content: flex-end;
}

.order-price .price-now {
  font-size: 17px;
}

.order-action {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.countdown {
  font-size: 12px;
  color: #f0a020;
  font-variant-numeric: tabular-nums;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 800px) {
  .order-body {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .order-price,
  .order-action {
    justify-content: flex-start;
    align-items: flex-start;
  }
}
</style>
