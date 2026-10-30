<template>
  <div class="page-container">
    <div class="page-title">库存调整</div>

    <div class="card">
      <div class="filter-bar">
        <span class="label">选择活动</span>
        <el-select
          v-model="activityId"
          placeholder="请选择活动"
          filterable
          clearable
          style="width: 320px"
          @change="loadDetail"
        >
          <el-option
            v-for="act in activities"
            :key="act.id"
            :label="`#${act.id} ${act.title}（${act.statusDesc}）`"
            :value="act.id"
          />
        </el-select>
        <el-button :icon="Refresh" :disabled="!activityId" @click="loadDetail">刷新</el-button>
      </div>

      <template v-if="detail">
        <el-alert type="info" :closable="false" show-icon class="tip">
          <template #title>
            剩余库存有两份：<b>DB 库存</b> 是最终事实，<b>Redis 余量</b> 是抢购时真正被扣的值。
            调整会同时改两边；若 Redis 未预热（显示「—」），只改 DB，等预热任务按新值写入。
          </template>
        </el-alert>

        <el-table v-loading="loading" :data="detail.items" empty-text="该活动没有商品">
          <el-table-column prop="goodsId" label="商品 ID" width="180" />
          <el-table-column label="商品" min-width="220">
            <template #default="{ row }">
              <div>{{ row.name }}</div>
              <div class="muted tiny">{{ row.subtitle || '—' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="秒杀价" width="110" align="right">
            <template #default="{ row }"><span class="money">¥{{ row.seckillPrice }}</span></template>
          </el-table-column>
          <el-table-column label="DB 库存" width="110" align="right">
            <template #default="{ row }">
              <span class="num">{{ row.stock }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Redis 余量" width="120" align="right">
            <template #default="{ row }">
              <span v-if="row.remaining === null" class="muted">—</span>
              <span v-else class="num redis">{{ row.remaining }}</span>
            </template>
          </el-table-column>
          <el-table-column label="每人限购" width="100" align="center">
            <template #default="{ row }">{{ row.limitPerUser }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openAdjust(row)">调整库存</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <el-empty v-else description="先选择一个活动" />
    </div>

    <el-dialog v-model="dialogVisible" title="调整库存" width="460px">
      <div v-if="current" class="adjust-box">
        <div class="adjust-goods">{{ current.name }}</div>
        <div class="adjust-now">
          <span>DB 库存 <b>{{ current.stock }}</b></span>
          <span>Redis 余量 <b>{{ current.remaining === null ? '未预热' : current.remaining }}</b></span>
        </div>

        <el-form label-width="90px" class="adjust-form">
          <el-form-item label="调整数量">
            <el-input-number v-model="delta" :step="10" :precision="0" style="width: 160px" />
            <span class="muted" style="margin-left: 8px">正数加、负数减</span>
          </el-form-item>
          <el-form-item label="调整后">
            <span class="preview">DB：{{ current.stock }} → {{ previewStock }}</span>
          </el-form-item>
          <el-form-item label="原因">
            <el-input v-model="remark" maxlength="64" placeholder="例如：运营补货（可选）" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!delta" @click="handleAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getActivityPage, getStockByActivity, adjustStock } from '@/api'

const route = useRoute()

const activities = ref([])
const activityId = ref(null)
const detail = ref(null)
const loading = ref(false)

const dialogVisible = ref(false)
const current = ref(null)
const delta = ref(10)
const remark = ref('')
const submitting = ref(false)

const previewStock = computed(() => {
  if (!current.value) return 0
  return Math.max(0, current.value.stock + (delta.value || 0))
})

onMounted(async () => {
  await loadActivities()
  const preset = route.query.activityId
  if (preset) {
    // 不能 Number(preset)：雪花 ID 超出 JS 安全整数，转数字会丢精度（…425 变 …400），
    // 导致下拉匹配不上、后端报「活动不存在」。路由 query 本就是字符串，直接用。
    activityId.value = String(preset)
    await loadDetail()
  }
})

async function loadActivities() {
  try {
    const res = await getActivityPage({ pageNum: 1, pageSize: 100 })
    activities.value = res.data.list
  } catch (e) {
    activities.value = []
  }
}

async function loadDetail() {
  if (!activityId.value) {
    detail.value = null
    return
  }
  loading.value = true
  try {
    const res = await getStockByActivity(activityId.value)
    detail.value = res.data
  } catch (e) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

function openAdjust(row) {
  current.value = row
  delta.value = 10
  remark.value = ''
  dialogVisible.value = true
}

async function handleAdjust() {
  if (!delta.value) {
    ElMessage.warning('调整数量不能为 0')
    return
  }
  submitting.value = true
  try {
    const res = await adjustStock({
      activityGoodsId: current.value.activityGoodsId,
      delta: delta.value,
      remark: remark.value || undefined
    })
    const data = res.data
    dialogVisible.value = false

    // 同步结果必须显式告知：DB 改了 Redis 没改，是「补了货却抢不到」的直接原因
    if (data.redisSynced) {
      ElMessage.success(`已调整：DB ${data.stockBefore} → ${data.stockAfter}，Redis 余量 ${data.redisRemaining}`)
    } else {
      ElMessage.warning(data.message || 'DB 已调整，但 Redis 未同步')
    }
    await loadDetail()
  } catch (e) {
    // 拦截器已提示（例如「调整后库存会变成负数」）
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.label {
  font-size: 14px;
  color: #606266;
}

.tip {
  margin-bottom: 14px;
}

.num {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

.num.redis {
  color: #409eff;
}

.tiny {
  font-size: 11px;
}

.adjust-goods {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}

.adjust-now {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 16px;
}

.adjust-now b {
  color: #303133;
}

.adjust-form {
  margin-top: 8px;
}

.preview {
  font-size: 13px;
  color: #e6a23c;
}
</style>
