<template>
  <div class="page-wrap">
    <div class="result-card">
      <!-- 排队中 -->
      <template v-if="phase === 'pending'">
        <div class="spinner">
          <el-icon class="is-loading" :size="42"><Loading /></el-icon>
        </div>
        <h2 class="title">正在为你排队</h2>
        <p class="desc">
          Redis 库存已预扣成功，订单正在异步落库。已轮询 {{ polls }} 次 /
          最长 {{ maxPoll }} 次
        </p>
        <el-progress
          :percentage="pollPercent"
          :show-text="false"
          :stroke-width="6"
          color="#e93b3d"
          class="bar"
        />
        <p class="tip">请勿关闭页面，通常 1 秒内完成</p>
      </template>

      <!-- 成功 -->
      <template v-else-if="phase === 'success'">
        <div class="icon-wrap success">
          <el-icon :size="40"><CircleCheckFilled /></el-icon>
        </div>
        <h2 class="title">抢购成功</h2>
        <p class="desc">订单已生成，请在 {{ formatTime(order?.expireTime) || '30 分钟内' }} 前完成支付</p>

        <div class="order-box">
          <div class="order-row">
            <span>订单号</span>
            <strong>{{ result.orderNo }}</strong>
          </div>
          <div class="order-row" v-if="order">
            <span>商品</span>
            <strong>{{ order.goodsName }}</strong>
          </div>
          <div class="order-row" v-if="order">
            <span>金额</span>
            <strong class="price-now">¥{{ order.price }}</strong>
          </div>
          <div class="order-row" v-if="order">
            <span>状态</span>
            <el-tag :type="orderStatusType(order.status)" size="small">{{ order.statusDesc }}</el-tag>
          </div>
        </div>

        <div class="actions">
          <el-button v-if="order && order.status === 0" type="primary" size="large" round :loading="paying" @click="handlePay">
            立即支付 ¥{{ order.price }}
          </el-button>
          <el-button size="large" round @click="router.push('/orders')">查看我的订单</el-button>
          <el-button size="large" round text @click="router.push('/activities')">继续逛</el-button>
        </div>
      </template>

      <!-- 失败 -->
      <template v-else-if="phase === 'failed'">
        <div class="icon-wrap failed">
          <el-icon :size="40"><CircleCloseFilled /></el-icon>
        </div>
        <h2 class="title">{{ failTitle }}</h2>
        <p class="desc">{{ failMessage }}</p>
        <div class="actions">
          <el-button type="primary" size="large" round @click="router.push('/activities')">返回活动列表</el-button>
          <el-button size="large" round @click="goBack">返回上一页</el-button>
        </div>
      </template>

      <!-- 超时未出结果 -->
      <template v-else>
        <div class="icon-wrap timeout">
          <el-icon :size="40"><WarningFilled /></el-icon>
        </div>
        <h2 class="title">结果确认中</h2>
        <p class="desc">
          排队时间超出预期，订单可能已在后台生成。请到「我的订单」确认，不要重复抢购。
        </p>
        <div class="actions">
          <el-button type="primary" size="large" round @click="router.push('/orders')">去我的订单看看</el-button>
          <el-button size="large" round :loading="retrying" @click="restart">再查一次</el-button>
        </div>
      </template>

      <p class="request-id">requestId: {{ requestId }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSeckillResult, getMyOrders, payOrder } from '@/api'

const route = useRoute()
const router = useRouter()

const requestId = route.params.requestId
const phase = ref('pending') // pending | success | failed | timeout
const result = ref({})
const order = ref(null)
const polls = ref(0)
const maxPoll = 30
const intervalMs = 600
const paying = ref(false)
const retrying = ref(false)

let timer = null
let stopped = false

const pollPercent = computed(() => Math.min(100, Math.round((polls.value / maxPoll) * 100)))

const failTitle = computed(() => {
  const code = result.value.code
  if (code === 4001) return '手慢了，已被抢光'
  if (code === 4003) return '你已参与过本场秒杀'
  if (code === 4002) return '活动已结束'
  if (code === 4006) return '库存还没准备好'
  return '抢购未成功'
})

const failMessage = computed(() => result.value.message || '请稍后重试')

onMounted(() => {
  poll()
})

onBeforeUnmount(() => {
  stopped = true
  if (timer) clearTimeout(timer)
})

function schedule() {
  if (stopped) return
  timer = setTimeout(poll, intervalMs)
}

async function poll() {
  if (stopped) return
  polls.value += 1
  try {
    const res = await getSeckillResult(requestId)
    const data = res.data || {}
    result.value = data

    if (data.status === 'SUCCESS') {
      phase.value = 'success'
      await loadOrder(data.orderNo)
      return
    }
    if (data.status === 'FAILED') {
      phase.value = 'failed'
      return
    }
    if (polls.value >= maxPoll) {
      phase.value = 'timeout'
      return
    }
    schedule()
  } catch (e) {
    // 单次查询失败（网络抖动）不终止轮询，下次继续；连续失败由拦截器提示
    if (polls.value >= maxPoll) {
      phase.value = 'timeout'
      return
    }
    schedule()
  }
}

async function loadOrder(orderNo) {
  if (!orderNo) return
  try {
    const res = await getMyOrders({ pageNum: 1, pageSize: 20 })
    order.value = res.data.list.find((o) => o.orderNo === orderNo) || null
  } catch (e) {
    order.value = null
  }
}

async function handlePay() {
  paying.value = true
  try {
    const res = await payOrder(result.value.orderNo)
    order.value = res.data
    ElMessage.success('支付成功')
  } catch (e) {
    // 拦截器已提示，这里刷新一次拿到最新状态（可能已被超时关单）
    await loadOrder(result.value.orderNo)
  } finally {
    paying.value = false
  }
}

function restart() {
  retrying.value = true
  stopped = false
  polls.value = 0
  phase.value = 'pending'
  poll()
  setTimeout(() => {
    retrying.value = false
  }, 400)
}

/**
 * 返回上一页。
 * 结果是允许直接打开/分享的页面，此时 history 里没有站内上一条，
 * 裸用 router.back() 会退到 about:blank，这里兜底回活动列表。
 */
function goBack() {
  if (window.history.state && window.history.state.back) {
    router.back()
  } else {
    router.push('/activities')
  }
}

function formatTime(value) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

function orderStatusType(status) {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  return 'info'
}
</script>

<style scoped>
.result-card {
  max-width: 560px;
  margin: 40px auto;
  background: #fff;
  border-radius: 16px;
  padding: 44px 36px 28px;
  text-align: center;
  box-shadow: 0 10px 36px rgba(0, 0, 0, 0.07);
}

.spinner {
  color: var(--sk-primary);
  margin-bottom: 18px;
}

.icon-wrap {
  width: 74px;
  height: 74px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 18px;
}

.icon-wrap.success {
  background: #e8f7ed;
  color: #52c41a;
}

.icon-wrap.failed {
  background: #fff1f0;
  color: var(--sk-primary);
}

.icon-wrap.timeout {
  background: #fff7e6;
  color: #fa8c16;
}

.title {
  font-size: 21px;
  font-weight: 700;
  margin-bottom: 10px;
}

.desc {
  font-size: 14px;
  color: var(--sk-text-sub);
  line-height: 1.6;
  margin-bottom: 22px;
}

.bar {
  max-width: 320px;
  margin: 0 auto 14px;
}

.tip {
  font-size: 12px;
  color: #a8abb2;
}

.order-box {
  background: #fafafa;
  border-radius: 10px;
  padding: 16px 18px;
  text-align: left;
  margin-bottom: 24px;
}

.order-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  padding: 7px 0;
}

.order-row span {
  color: var(--sk-text-sub);
}

.actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}

.request-id {
  margin-top: 26px;
  font-size: 11px;
  color: #c8cbd0;
}
</style>
