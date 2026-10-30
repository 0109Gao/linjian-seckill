<template>
  <div class="page-container" v-loading="loading">
    <div class="page-title">监控看板</div>

    <el-row :gutter="12">
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-label">活动</div>
          <div class="stat-value">{{ data.activityTotal || 0 }}</div>
          <div class="stat-sub">进行中 <b class="hl">{{ data.activityRunning || 0 }}</b></div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-label">订单总数</div>
          <div class="stat-value">{{ data.orderTotal || 0 }}</div>
          <div class="stat-sub">
            待支付 <b class="warn">{{ data.orderPending || 0 }}</b>
            · 已关闭 <b>{{ data.orderClosed || 0 }}</b>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-label">累计成交额</div>
          <div class="stat-value money">¥{{ money(data.salesAmount) }}</div>
          <div class="stat-sub">已支付 <b class="ok">{{ data.orderPaid || 0 }}</b> 单</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-label">今日</div>
          <div class="stat-value">{{ data.todayOrders || 0 }} <span class="unit">单</span></div>
          <div class="stat-sub">成交 ¥{{ money(data.todaySalesAmount) }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="card table-card">
      <div class="card-head">
        <span class="card-title">活动明细</span>
        <el-button text :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>

      <el-table :data="data.activities || []" size="default" empty-text="暂无活动">
        <el-table-column prop="activityId" label="ID" width="196" show-overflow-tooltip />
        <el-table-column prop="title" label="活动名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="剩余库存" width="110" align="right">
          <template #default="{ row }">{{ row.dbStock ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="订单数" width="100" align="right">
          <template #default="{ row }">{{ row.orderCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="已支付" width="100" align="right">
          <template #default="{ row }">{{ row.paidCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="成交额" width="130" align="right">
          <template #default="{ row }">
            <span class="money">¥{{ money(row.salesAmount) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card note-card">
      <div class="note-title">口径说明</div>
      <p>
        库存为 DB 中的 <code>sk_activity_goods.stock</code> 汇总。本系统采用「预扣即扣」，
        Redis 预扣成功的那一瞬间 DB 库存不会立刻减，而是在 MQ 消费落单时才减，
        因此看板数字与用户端看到的「剩余」在秒杀瞬间会有短暂差异，属于设计预期。
      </p>
      <p>
        订单数含待支付、已支付、已关闭（取消 + 已回补）。成交额只统计已支付订单的
        <code>price × quantity</code>，不含待支付。
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getDashboard } from '@/api'

const data = ref({})
const loading = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await getDashboard({ activityLimit: 10 })
    data.value = res.data
  } catch (e) {
    data.value = {}
  } finally {
    loading.value = false
  }
}

function money(value) {
  if (value === null || value === undefined) return '0.00'
  return Number(value).toFixed(2)
}

function statusType(status) {
  if (status === 1) return 'danger'
  if (status === 0) return 'warning'
  return 'info'
}
</script>

<style scoped>
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 18px;
  margin-bottom: 12px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 6px;
}

.stat-value .unit {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
}

.stat-sub {
  font-size: 12px;
  color: #909399;
}

.hl {
  color: #409eff;
}

.warn {
  color: #e6a23c;
}

.ok {
  color: #67c23a;
}

.table-card {
  margin-top: 4px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

.note-card {
  margin-top: 12px;
  font-size: 13px;
  line-height: 1.75;
  color: #606266;
}

.note-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}

.note-card p + p {
  margin-top: 6px;
}

code {
  background: #f5f7fa;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 12px;
  color: #c7254e;
}
</style>
