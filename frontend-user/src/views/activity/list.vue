<template>
  <div class="page-wrap">
    <div class="page-title">全部活动</div>

    <div class="filter-bar">
      <el-radio-group v-model="status" @change="handleFilterChange">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="1">进行中</el-radio-button>
        <el-radio-button :value="0">即将开始</el-radio-button>
        <el-radio-button :value="2">已结束</el-radio-button>
      </el-radio-group>
      <span class="muted">共 {{ total }} 场活动</span>
    </div>

    <div v-loading="loading" class="grid">
      <el-empty v-if="!loading && list.length === 0" description="暂无活动" class="empty" />

      <div v-for="item in list" :key="item.id" class="card" @click="goDetail(item.id)">
        <div class="card-status">
          <el-tag :type="statusType(item.status)" effect="dark" size="small" round>
            {{ item.statusDesc }}
          </el-tag>
        </div>

        <h3 class="card-title">{{ item.title }}</h3>
        <p class="card-desc">{{ item.description || '暂无活动说明' }}</p>

        <div class="card-meta">
          <span><el-icon><Goods /></el-icon> {{ item.goodsCount }} 件商品</span>
          <span><el-icon><Clock /></el-icon> {{ countdownText(item) }}</span>
        </div>

        <div class="card-time">
          {{ formatTime(item.startTime) }} — {{ formatTime(item.endTime) }}
        </div>

        <el-button
          class="card-btn"
          :type="item.status === 1 ? 'primary' : 'default'"
          :disabled="item.status === 2 || item.status === 3"
          round
        >
          {{ item.status === 1 ? '立即抢购' : item.status === 0 ? '查看预告' : '已结束' }}
        </el-button>
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
import { getActivityPage } from '@/api'

const router = useRouter()

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(9)
const status = ref(null)
const loading = ref(false)

// 一个 setInterval 驱动全部卡片倒计时，避免每张卡片各起一个定时器
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
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (status.value !== null) {
      params.status = status.value
    }
    const res = await getActivityPage(params)
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  pageNum.value = 1
  load()
}

function goDetail(id) {
  router.push(`/activities/${id}`)
}

function statusType(code) {
  if (code === 1) return 'danger'
  if (code === 0) return 'warning'
  if (code === 3) return 'info'
  return 'info'
}

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

/** 解析后端返回的 yyyy-MM-ddTHH:mm:ss，返回毫秒时间戳 */
function toTs(value) {
  if (!value) return 0
  return new Date(value.replace(' ', 'T')).getTime()
}

function countdownText(item) {
  const start = toTs(item.startTime)
  const end = toTs(item.endTime)
  const t = now.value

  if (item.status === 1) {
    return '剩余 ' + humanize(end - t)
  }
  if (item.status === 0) {
    const diff = start - t
    return diff > 0 ? humanize(diff) + ' 后开始' : '即将开始'
  }
  return '活动已结束'
}

function humanize(ms) {
  if (ms <= 0) return '00:00:00'
  const totalSec = Math.floor(ms / 1000)
  const d = Math.floor(totalSec / 86400)
  const h = Math.floor((totalSec % 86400) / 3600)
  const m = Math.floor((totalSec % 3600) / 60)
  const s = totalSec % 60
  const pad = (n) => String(n).padStart(2, '0')
  return d > 0 ? `${d} 天 ${pad(h)}:${pad(m)}:${pad(s)}` : `${pad(h)}:${pad(m)}:${pad(s)}`
}
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  gap: 12px;
  flex-wrap: wrap;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.empty {
  grid-column: 1 / -1;
}

.card {
  position: relative;
  background: #fff;
  border-radius: var(--sk-radius);
  padding: 20px 18px 18px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.18s;
  display: flex;
  flex-direction: column;
}

.card:hover {
  border-color: var(--sk-primary);
  box-shadow: 0 8px 24px rgba(233, 59, 61, 0.12);
  transform: translateY(-2px);
}

.card-status {
  position: absolute;
  top: 14px;
  right: 14px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  padding-right: 76px;
  margin-bottom: 8px;
  line-height: 1.4;
}

.card-desc {
  font-size: 13px;
  color: var(--sk-text-sub);
  line-height: 1.5;
  min-height: 38px;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  gap: 14px;
  font-size: 13px;
  color: var(--sk-text-sub);
  margin-bottom: 6px;
}

.card-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.card-time {
  font-size: 12px;
  color: #a8abb2;
  margin-bottom: 14px;
}

.card-btn {
  width: 100%;
  margin-top: auto;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
