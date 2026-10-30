<template>
  <div class="page-container">
    <div class="page-title">商品管理</div>

    <div class="card">
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="按商品名搜索"
          clearable
          style="width: 240px"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <div class="spacer" />
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增商品</el-button>
      </div>

      <el-table v-loading="loading" :data="list" empty-text="暂无商品">
        <el-table-column prop="id" label="ID" width="190" show-overflow-tooltip />
        <el-table-column label="商品" min-width="240">
          <template #default="{ row }">
            <div class="goods-cell">
              <div class="thumb">
                <img v-if="row.imageUrl" :src="row.imageUrl" :alt="row.name" />
                <span v-else>{{ row.name.charAt(0) }}</span>
              </div>
              <div>
                <div class="name">{{ row.name }}</div>
                <div class="sub">{{ row.subtitle || '—' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="原价" width="110" align="right">
          <template #default="{ row }">¥{{ row.originPrice }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="520px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="商品名" prop="name">
          <el-input v-model="form.name" maxlength="64" show-word-limit placeholder="例如：林间手冲咖啡豆 250g" />
        </el-form-item>
        <el-form-item label="副标题" prop="subtitle">
          <el-input v-model="form.subtitle" maxlength="128" placeholder="例如：中浅烘焙 · 柑橘尾韵" />
        </el-form-item>
        <el-form-item label="主图地址" prop="imageUrl">
          <el-input v-model="form.imageUrl" maxlength="255" placeholder="可留空，前端会用首字占位" />
        </el-form-item>
        <el-form-item label="原价" prop="originPrice">
          <el-input-number v-model="form.originPrice" :min="0.01" :precision="2" :step="10" style="width: 180px" />
          <span class="muted" style="margin-left: 8px">元</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { getGoodsPage, createGoods, updateGoods, deleteGoods } from '@/api'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  subtitle: '',
  imageUrl: '',
  originPrice: 99,
  status: 1
})

const rules = {
  name: [
    { required: true, message: '请输入商品名', trigger: 'blur' },
    { max: 64, message: '最长 64 字', trigger: 'blur' }
  ],
  originPrice: [{ required: true, message: '请输入原价', trigger: 'blur' }]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await getGoodsPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    })
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

function openDialog(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      subtitle: row.subtitle || '',
      imageUrl: row.imageUrl || '',
      originPrice: Number(row.originPrice),
      status: row.status
    })
  } else {
    resetForm()
  }
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    id: null,
    name: '',
    subtitle: '',
    imageUrl: '',
    originPrice: 99,
    status: 1
  })
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      name: form.name,
      subtitle: form.subtitle || null,
      imageUrl: form.imageUrl || null,
      originPrice: form.originPrice,
      status: form.status
    }
    if (form.id) {
      await updateGoods(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await createGoods(payload)
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除商品「${row.name}」？已被活动引用的商品不允许删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteGoods(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    // 拦截器已提示（例如「商品已被活动引用」）
  }
}

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.goods-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  font-weight: 600;
  overflow: hidden;
  flex-shrink: 0;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.name {
  font-size: 14px;
}

.sub {
  font-size: 12px;
  color: #909399;
}
</style>
