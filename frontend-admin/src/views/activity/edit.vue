<template>
  <div class="page-container" v-loading="loading">
    <div class="page-title">{{ isEdit ? '编辑活动' : '新建活动' }}</div>

    <div class="card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="act-form">
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="form.title" maxlength="64" show-word-limit placeholder="例如：周五晚八点 · 咖啡专场" />
        </el-form-item>

        <el-form-item label="活动说明" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            maxlength="255"
            show-word-limit
            placeholder="例如：限量秒杀，每人限购 1 件"
          />
        </el-form-item>

        <el-form-item label="活动时间" prop="timeRange">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :default-time="defaultTime"
          />
          <div class="field-tip">
            库存会在活动开始前 5 分钟由定时任务自动预热；活动结束 1 小时后 Redis 库存键自动过期。
          </div>
        </el-form-item>
      </el-form>

      <div class="items-head">
        <span class="items-title">活动商品</span>
        <el-button type="primary" plain size="small" :icon="Plus" @click="addItem">添加商品</el-button>
      </div>

      <el-table :data="form.items" empty-text="请至少添加一个商品" size="small">
        <el-table-column label="商品" min-width="260">
          <template #default="{ row, $index }">
            <el-select
              v-model="row.goodsId"
              placeholder="选择商品"
              filterable
              style="width: 100%"
              @change="(val) => onGoodsChange(val, $index)"
            >
              <el-option
                v-for="goods in goodsOptions"
                :key="goods.id"
                :label="`${goods.name}（原价 ¥${goods.originPrice}）`"
                :value="goods.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="秒杀价" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.seckillPrice" :min="0.01" :precision="2" :step="1" size="small" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="库存" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.stock" :min="1" :step="10" size="small" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="每人限购" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.limitPerUser" :min="1" :max="100" size="small" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeItem($index)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="actions">
        <el-button @click="router.push('/activities')">返回列表</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '创建活动' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createActivity, updateActivity, getActivityDetail, getGoodsPage } from '@/api'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const goodsOptions = ref([])

const defaultTime = [new Date(2000, 0, 1, 20, 0, 0), new Date(2000, 0, 1, 22, 0, 0)]

const form = reactive({
  title: '',
  description: '',
  timeRange: [],
  items: []
})

const rules = {
  title: [
    { required: true, message: '请输入活动标题', trigger: 'blur' },
    { max: 64, message: '最长 64 字', trigger: 'blur' }
  ],
  timeRange: [
    {
      validator: (rule, value, callback) => {
        if (!value || value.length !== 2) {
          callback(new Error('请选择活动时间'))
        } else if (new Date(value[0]) >= new Date(value[1])) {
          callback(new Error('结束时间必须晚于开始时间'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

onMounted(async () => {
  await loadGoods()
  if (isEdit.value) {
    await loadDetail()
  }
})

async function loadGoods() {
  try {
    const res = await getGoodsPage({ pageNum: 1, pageSize: 100 })
    // 只让选上架商品：下架商品挂到活动上，用户端看到的是一个点不动的按钮
    goodsOptions.value = res.data.list.filter((g) => g.status === 1)
  } catch (e) {
    goodsOptions.value = []
  }
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getActivityDetail(route.params.id)
    const detail = res.data
    form.title = detail.title
    form.description = detail.description || ''
    form.timeRange = [normalize(detail.startTime), normalize(detail.endTime)]
    form.items = (detail.items || []).map((item) => ({
      goodsId: item.goodsId,
      seckillPrice: Number(item.seckillPrice),
      stock: item.stock,
      limitPerUser: item.limitPerUser
    }))
  } catch (e) {
    ElMessage.error('活动加载失败')
    router.push('/activities')
  } finally {
    loading.value = false
  }
}

/** 后端返回 ISO 风格（2026-10-31T20:00:00），日期组件要空格分隔 */
function normalize(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}

function addItem() {
  form.items.push({ goodsId: null, seckillPrice: 9.9, stock: 100, limitPerUser: 1 })
}

function removeItem(index) {
  form.items.splice(index, 1)
}

function onGoodsChange(goodsId, index) {
  const goods = goodsOptions.value.find((g) => g.id === goodsId)
  if (!goods) return
  // 秒杀价默认按原价 5 折，比让人从 1 分钱开始填更接近实际操作
  const suggested = Number((Number(goods.originPrice) * 0.5).toFixed(2))
  form.items[index].seckillPrice = Math.max(0.01, suggested)
}

async function handleSubmit() {
  await formRef.value.validate()

  if (form.items.length === 0) {
    ElMessage.warning('请至少添加一个商品')
    return
  }
  const emptyRow = form.items.find((item) => !item.goodsId)
  if (emptyRow) {
    ElMessage.warning('有商品未选择，请先选择或移除该行')
    return
  }
  const duplicated = form.items.length !== new Set(form.items.map((i) => i.goodsId)).size
  if (duplicated) {
    ElMessage.warning('同一商品不能重复添加')
    return
  }

  submitting.value = true
  try {
    const payload = {
      title: form.title,
      description: form.description || null,
      startTime: form.timeRange[0],
      endTime: form.timeRange[1],
      items: form.items.map((item) => ({
        goodsId: item.goodsId,
        seckillPrice: item.seckillPrice,
        stock: item.stock,
        limitPerUser: item.limitPerUser
      }))
    }
    if (isEdit.value) {
      await updateActivity(route.params.id, payload)
      ElMessage.success('已保存')
    } else {
      await createActivity(payload)
      ElMessage.success('已创建')
    }
    router.push('/activities')
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.act-form {
  max-width: 780px;
}

.field-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  margin-top: 4px;
}

.items-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 18px 0 10px;
}

.items-title {
  font-size: 14px;
  font-weight: 600;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}
</style>
