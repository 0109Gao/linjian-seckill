<template>
  <div class="page-wrap" v-loading="loading">
    <el-page-header v-if="detail" class="back" @back="goBack">
      <template #content>
        <span class="head-title">{{ detail.title }}</span>
      </template>
    </el-page-header>

    <template v-if="detail">
      <!-- 活动横幅 -->
      <div class="banner">
        <div class="banner-left">
          <div class="banner-status">
            <el-tag :type="statusType(detail.status)" effect="dark" round>{{ detail.statusDesc }}</el-tag>
            <span v-if="detail.status === 0" class="banner-tip">距开始 {{ countdownText(detail) }}</span>
            <span v-else-if="detail.status === 1" class="banner-tip">距结束 {{ countdownText(detail) }}</span>
          </div>
          <h1 class="banner-title">{{ detail.title }}</h1>
          <p class="banner-desc">{{ detail.description || '暂无活动说明' }}</p>
          <p class="banner-time">
            {{ formatTime(detail.startTime) }} — {{ formatTime(detail.endTime) }}
          </p>
        </div>
        <div class="banner-clock">
          <span class="clock-label">{{ clockLabel }}</span>
          <span class="clock-value">{{ clockValue }}</span>
        </div>
      </div>

      <!-- 商品列表 -->
      <div class="page-title" style="margin-top: 24px">本场商品</div>

      <el-empty v-if="detail.items.length === 0" description="本场活动暂无商品" />

      <div class="goods-list">
        <div v-for="item in detail.items" :key="item.activityGoodsId" class="goods">
          <div class="goods-img">
            <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" />
            <span v-else class="goods-img-ph">{{ item.name.charAt(0) }}</span>
          </div>

          <div class="goods-main">
            <h3 class="goods-name">{{ item.name }}</h3>
            <p class="goods-sub">{{ item.subtitle || '—' }}</p>

            <div class="goods-tags">
              <el-tag size="small" type="danger" effect="plain">每人限购 {{ item.limitPerUser }}</el-tag>
              <el-tag v-if="item.remaining === null" size="small" type="info" effect="plain">尚未预热</el-tag>
              <el-tag v-else-if="item.remaining === 0" size="small" type="info" effect="plain">已抢光</el-tag>
            </div>
          </div>

          <div class="goods-stock">
            <div class="stock-row">
              <span class="stock-num">{{ item.remaining === null ? item.stock : item.remaining }}</span>
              <span class="stock-total">/ {{ item.stock }}</span>
            </div>
            <el-progress
              :percentage="soldPercent(item)"
              :show-text="false"
              :stroke-width="6"
              :color="progressColor(item)"
            />
            <span class="muted">剩余库存</span>
          </div>

          <div class="goods-price">
            <div class="price-now">¥{{ item.seckillPrice }}</div>
            <div class="price-origin">¥{{ item.originPrice }}</div>
          </div>

          <div class="goods-action">
            <el-button
              type="primary"
              size="large"
              round
              :loading="submittingId === item.activityGoodsId"
              :disabled="!canBuy(item)"
              @click="handleSeckill(item)"
            >
              {{ buttonText(item) }}
            </el-button>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="活动不存在或已下架">
      <el-button type="primary" @click="router.push('/activities')">返回活动列表</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivityDetail, doSeckill } from '@/api'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const detail = ref(null)
const loading = ref(false)
const submittingId = ref(null)
const now = ref(Date.now())

let timer = null

onMounted(async () => {
  await load()
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})

/**
 * 返回上一页。
 * 直接从外部链接/新标签打开详情页时，history 里只有本站这一条记录，
 * 裸用 router.back() 会退到 about:blank（页面全白，用户以为系统坏了）。
 * 这里判断站内是否有可回退的历史，没有就兜底回活动列表。
 */
function goBack() {
  if (window.history.state && window.history.state.back) {
    router.back()
  } else {
    router.push('/activities')
  }
}

async function load() {
  loading.value = true
  try {
    const res = await getActivityDetail(route.params.id)
    detail.value = res.data
  } catch (e) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

const clockLabel = computed(() => {
  if (!detail.value) return ''
  if (detail.value.status === 1) return '距结束'
  if (detail.value.status === 0) return '距开始'
  return '状态'
})

const clockValue = computed(() => {
  if (!detail.value) return ''
  if (detail.value.status === 2) return '已结束'
  if (detail.value.status === 3) return '已取消'
  return humanize(countdownMs(detail.value))
})

function toTs(value) {
  if (!value) return 0
  return new Date(value.replace(' ', 'T')).getTime()
}

function countdownMs(activity) {
  const target = activity.status === 1 ? toTs(activity.endTime) : toTs(activity.startTime)
  return target - now.value
}

function countdownText(activity) {
  const ms = countdownMs(activity)
  return ms > 0 ? humanize(ms) : '即将刷新'
}

function humanize(ms) {
  if (ms <= 0) return '00:00:00'
  const totalSec = Math.floor(ms / 1000)
  const d = Math.floor(totalSec / 86400)
  const h = Math.floor((totalSec % 86400) / 3600)
  const m = Math.floor((totalSec % 3600) / 60)
  const s = totalSec % 60
  const pad = (n) => String(n).padStart(2, '0')
  return d > 0 ? `${d}天 ${pad(h)}:${pad(m)}:${pad(s)}` : `${pad(h)}:${pad(m)}:${pad(s)}`
}

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function statusType(code) {
  if (code === 1) return 'danger'
  if (code === 0) return 'warning'
  return 'info'
}

function soldPercent(item) {
  const total = item.stock || 0
  if (total <= 0) return 0
  const remain = item.remaining === null ? total : item.remaining
  const sold = Math.max(0, total - remain)
  return Math.min(100, Math.round((sold / total) * 100))
}

function progressColor(item) {
  const p = soldPercent(item)
  if (p >= 90) return '#e93b3d'
  if (p >= 50) return '#f0a020'
  return '#67c23a'
}

function canBuy(item) {
  if (!detail.value) return false
  if (detail.value.status !== 1) return false
  if (item.remaining !== null && item.remaining <= 0) return false
  return true
}

function buttonText(item) {
  if (!detail.value) return '抢购'
  if (detail.value.status === 0) return '未开始'
  if (detail.value.status === 2) return '已结束'
  if (detail.value.status === 3) return '已取消'
  if (item.remaining !== null && item.remaining <= 0) return '已抢光'
  return '立即抢购'
}

async function handleSeckill(item) {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  submittingId.value = item.activityGoodsId
  try {
    // 后端只做 Redis 预扣就返回 PENDING，真正的落单在 MQ 消费端完成，
    // 所以这里拿到 requestId 后必须跳到结果页轮询，不能就地等待。
    const res = await doSeckill(detail.value.id, item.goodsId)
    const result = res.data

    if (result.status === 'SUCCESS') {
      router.push({ path: `/seckill/result/${result.requestId}`, query: { direct: '1' } })
      return
    }
    if (result.status === 'FAILED') {
      ElMessage.error(result.message || '抢购失败')
      await load()
      return
    }
    router.push(`/seckill/result/${result.requestId}`)
  } catch (e) {
    await load()
  } finally {
    submittingId.value = null
  }
}
</script>

<style scoped>
.back {
  margin-bottom: 16px;
}

.head-title {
  font-size: 15px;
  font-weight: 600;
}

.banner {
  background: linear-gradient(120deg, #e93b3d 0%, #ff6b4a 55%, #ffa04a 100%);
  border-radius: 14px;
  padding: 26px 28px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.banner-status {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.banner-tip {
  font-size: 13px;
  opacity: 0.9;
}

.banner-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}

.banner-desc {
  font-size: 14px;
  opacity: 0.92;
  margin-bottom: 10px;
  max-width: 640px;
  line-height: 1.5;
}

.banner-time {
  font-size: 12px;
  opacity: 0.8;
}

.banner-clock {
  text-align: center;
  background: rgba(255, 255, 255, 0.16);
  border-radius: 12px;
  padding: 14px 22px;
  min-width: 170px;
}

.clock-label {
  display: block;
  font-size: 12px;
  opacity: 0.85;
  margin-bottom: 6px;
}

.clock-value {
  font-size: 24px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}

.goods-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.goods {
  background: #fff;
  border-radius: var(--sk-radius);
  padding: 16px 18px;
  display: grid;
  grid-template-columns: 88px 1fr 130px 110px 150px;
  align-items: center;
  gap: 16px;
}

.goods-img {
  width: 88px;
  height: 88px;
  border-radius: 10px;
  overflow: hidden;
  background: linear-gradient(135deg, #ffe8e6, #fff3e0);
  display: flex;
  align-items: center;
  justify-content: center;
}

.goods-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.goods-img-ph {
  font-size: 30px;
  font-weight: 700;
  color: var(--sk-primary);
  opacity: 0.55;
}

.goods-name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.goods-sub {
  font-size: 13px;
  color: var(--sk-text-sub);
  margin-bottom: 8px;
}

.goods-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.goods-stock {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stock-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stock-num {
  font-size: 20px;
  font-weight: 700;
  color: var(--sk-primary);
  font-variant-numeric: tabular-nums;
}

.stock-total {
  font-size: 12px;
  color: #a8abb2;
}

.goods-price {
  text-align: right;
}

.goods-price .price-now {
  font-size: 20px;
  font-weight: 700;
  color: var(--sk-primary);
}

.goods-price .price-origin {
  font-size: 12px;
  margin-left: 0;
}

.goods-action {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .goods {
    grid-template-columns: 72px 1fr;
    grid-template-areas:
      'img main'
      'stock price'
      'action action';
  }
  .goods-img { grid-area: img; width: 72px; height: 72px; }
  .goods-main { grid-area: main; }
  .goods-stock { grid-area: stock; }
  .goods-price { grid-area: price; }
  .goods-action { grid-area: action; }
  .goods-action .el-button { width: 100%; }
}
</style>
