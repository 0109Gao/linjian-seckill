<template>
  <div class="page-container">
    <div class="page-title">订单查询</div>

    <div class="card">
      <div class="filter-bar">
        <el-input
          v-model="query.orderNo"
          placeholder="订单号"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.status" placeholder="订单状态" clearable style="width: 140px" @change="handleSearch">
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已取消" :value="2" />
          <el-option label="已关闭" :value="3" />
        </el-select>
        <el-input
          v-model="query.activityId"
          placeholder="活动 ID"
          clearable
          style="width: 140px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-input
          v-model="query.userId"
          placeholder="用户 ID"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="list" empty-text="暂无订单">
        <el-table-column prop="orderNo" label="订单号" width="180" show-overflow-tooltip />
        <el-table-column label="用户" width="170">
          <template #default="{ row }">
            <div>{{ row.username }}</div>
            <div class="muted tiny ellipsis" :title="row.userId">{{ row.userId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="活动 / 商品" min-width="220">
          <template #default="{ row }">
            <div class="ellipsis">{{ row.activityTitle }}</div>
            <div class="muted tiny ellipsis">{{ row.goodsName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">
            <span class="money">¥{{ row.price }}</span>
            <div class="muted tiny">× {{ row.quantity }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="支付时间" width="150">
          <template #default="{ row }">
            <span v-if="row.payTime">{{ formatTime(row.payTime) }}</span>
            <span v-else class="muted">—</span>
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
      后台只提供查询，不提供「手工改订单状态」的入口：已支付订单要退款应走退款流程，
      把状态点成「已关闭」会让库存回补逻辑误判，凭空多出一份可售库存。
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOrderPage } from '@/api'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const query = reactive({
  orderNo: '',
  status: null,
  activityId: '',
  userId: ''
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (query.orderNo) params.orderNo = query.orderNo
    if (query.status !== null && query.status !== '') params.status = query.status
    if (query.activityId) params.activityId = query.activityId
    if (query.userId) params.userId = query.userId

    const res = await getOrderPage(params)
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

function handleReset() {
  query.orderNo = ''
  query.status = null
  query.activityId = ''
  query.userId = ''
  handleSearch()
}

function statusType(status) {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  return 'info'
}

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}
</script>

<style scoped>
.tiny {
  font-size: 11px;
}

.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hint {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  line-height: 1.7;
}
</style>
