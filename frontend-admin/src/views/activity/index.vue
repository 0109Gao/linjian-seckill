<template>
  <div class="page-container">
    <div class="page-title">活动管理</div>

    <div class="card">
      <div class="filter-bar">
        <el-radio-group v-model="status" @change="handleSearch">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button :value="0">待开始</el-radio-button>
          <el-radio-button :value="1">进行中</el-radio-button>
          <el-radio-button :value="2">已结束</el-radio-button>
          <el-radio-button :value="3">已取消</el-radio-button>
        </el-radio-group>
        <div class="spacer" />
        <el-button type="primary" :icon="Plus" @click="router.push('/activities/create')">新建活动</el-button>
      </div>

      <el-table v-loading="loading" :data="list" empty-text="暂无活动">
        <el-table-column prop="id" label="ID" width="196" show-overflow-tooltip />
        <el-table-column label="活动" min-width="240">
          <template #default="{ row }">
            <div class="act-title">{{ row.title }}</div>
            <div class="act-desc">{{ row.description || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="goodsCount" label="商品数" width="90" align="center" />
        <el-table-column label="活动时间" width="300">
          <template #default="{ row }">
            <div class="time-line">{{ formatTime(row.startTime) }}</div>
            <div class="time-line muted">至 {{ formatTime(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="row.status !== 0"
              @click="router.push(`/activities/${row.id}/edit`)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="success"
              :disabled="row.status === 2 || row.status === 3"
              :loading="preheatingId === row.id"
              @click="handlePreheat(row)"
            >
              预热
            </el-button>
            <el-button
              link
              type="warning"
              :disabled="row.status === 2 || row.status === 3"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
            <el-button link @click="router.push(`/stock?activityId=${row.id}`)">库存</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="handleSearch"
        />
      </div>
    </div>

    <div class="hint">
      提示：活动未开始时编辑才有效（后端只允许改待开始的活动）；预热接口用于演示与排查，
      实际由定时任务在开始前 5 分钟自动完成。
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getActivityPage, preheatActivity, cancelActivity } from '@/api'

const router = useRouter()

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref(null)
const loading = ref(false)
const preheatingId = ref(null)

onMounted(load)

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

function handleSearch() {
  pageNum.value = 1
  load()
}

async function handlePreheat(row) {
  preheatingId.value = row.id
  try {
    const res = await preheatActivity(row.id)
    if (res.data > 0) {
      ElMessage.success(`预热完成，写入 ${res.data} 个商品`)
    } else {
      ElMessage.info('这些商品的库存键已存在，本次未重复写入（预热是幂等的）')
    }
    await load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    preheatingId.value = null
  }
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm(
      `确认取消活动「${row.title}」？取消后会清理 Redis 中的库存键，用户将无法再抢购。`,
      '取消活动',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '再想想' }
    )
  } catch (e) {
    return
  }
  try {
    await cancelActivity(row.id)
    ElMessage.success('活动已取消')
    await load()
  } catch (e) {
    // 拦截器已提示
  }
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
</script>

<style scoped>
.act-title {
  font-size: 14px;
  font-weight: 500;
}

.act-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.time-line {
  font-size: 12px;
  line-height: 1.6;
}

.hint {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  line-height: 1.7;
}
</style>
