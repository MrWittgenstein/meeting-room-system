<template>
  <section>
    <div class="page-heading"><div><h1>日志查询</h1><p class="muted">按时间、服务和追踪编号检索访问日志与设备操作记录</p></div></div>
    <el-card>
      <el-form label-position="top" class="log-filters" @submit.prevent="search">
        <el-form-item label="时间范围（最多 31 天）" class="time-range"><el-date-picker v-model="range" type="datetimerange" start-placeholder="开始时间" end-placeholder="结束时间" :clearable="false" style="width: 100%" /></el-form-item>
        <el-form-item label="服务名"><el-input v-model="filters.microservice" clearable placeholder="如 meetingroom-main" /></el-form-item>
        <el-form-item label="追踪 ID"><el-input v-model="filters.traceId" clearable /></el-form-item>
        <el-form-item label="用户编号"><el-input v-model="filters.userId" clearable /></el-form-item>
        <el-form-item label="设备编号"><el-input v-model="filters.deviceId" clearable /></el-form-item>
        <el-form-item label="命令编号"><el-input v-model="filters.commandId" clearable /></el-form-item>
        <el-form-item label="HTTP 状态码"><el-input-number v-model="filters.statusCode" :min="100" :max="599" :precision="0" controls-position="right" placeholder="全部" /></el-form-item>
      </el-form>
      <div class="toolbar"><el-button type="primary" :icon="Search" :loading="loading" @click="search">查询日志</el-button><el-button @click="reset">重置</el-button><span v-if="queried" class="muted">共 {{ total }} 条 · 查询耗时 {{ duration }} ms · 时间按本地时区显示</span></div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon style="margin-bottom: 16px" />
      <el-table v-loading="loading" :data="rows" stripe :empty-text="queried ? '该条件下暂无日志' : '选择条件后点击查询日志'" @row-click="showDetail">
        <el-table-column label="时间" width="195"><template #default="{ row }">{{ formatTime(row.timestamp) }}</template></el-table-column>
        <el-table-column prop="microservice" label="服务" min-width="160" />
        <el-table-column prop="http_method" label="方法" width="85" />
        <el-table-column prop="api_endpoint" label="请求路径" min-width="210" show-overflow-tooltip />
        <el-table-column label="HTTP 状态" width="110"><template #default="{ row }"><el-tag :type="Number(row.status_code) >= 400 ? 'danger' : 'success'">{{ row.status_code }}</el-tag></template></el-table-column>
        <el-table-column prop="business_code" label="业务码" width="85" />
        <el-table-column prop="latency_ms" label="耗时(ms)" width="100" />
        <el-table-column prop="client_ip" label="客户端 IP" min-width="140" />
        <el-table-column prop="user_id" label="用户编号" width="100" />
        <el-table-column label="操作" width="90" fixed="right"><template #default="{ row }"><el-button link type="primary" @click.stop="showDetail(row)">详情</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :page-sizes="[20, 50, 100, 200]" :total="total" :disabled="loading" layout="total, sizes, prev, pager, next" @current-change="fetchLogs" @size-change="resize" />
    </el-card>
    <el-drawer v-model="drawer" title="日志详情" size="min(640px, 96vw)">
      <el-descriptions v-if="selected" :column="1" border>
        <el-descriptions-item v-for="field in detailFields" :key="field.key" :label="field.label"><span class="detail-value">{{ field.key === 'timestamp' ? formatTime(selected[field.key]) : (selected[field.key] ?? '—') || '—' }}</span></el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </section>
</template>

<script setup>
defineOptions({ name: 'SystemAdminLogs' })
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { queryLogs } from '@/apis/systemAdmin'
const defaultRange = () => [new Date(Date.now() - 86400000), new Date()]
const range = ref(defaultRange()), filters = reactive({ microservice: '', traceId: '', userId: '', deviceId: '', commandId: '', statusCode: undefined })
const rows = ref([]), total = ref(0), duration = ref(0), page = ref(1), size = ref(20), loading = ref(false), error = ref(''), queried = ref(false), drawer = ref(false), selected = ref(null)
let activeQuery = null
const detailFields = [
  ['timestamp', '时间'], ['event_id', '事件编号'], ['microservice', '服务'], ['api_endpoint', '路径'], ['http_method', '方法'], ['status_code', 'HTTP 状态'], ['business_code', '业务码'], ['latency_ms', '耗时(ms)'], ['trace_id', '追踪 ID'], ['user_id', '用户编号'], ['client_ip', '客户端 IP'], ['user_agent', '客户端'], ['device_id', '设备编号'], ['room_id', '会议室编号'], ['command_id', '命令编号'], ['outcome', '执行结果'], ['exception', '异常类型'],
].map(([key, label]) => ({ key, label }))
function formatTime(value) {
  if (!value) return '—'
  const raw = String(value)
  const normalized = /^\d{4}-\d{2}-\d{2} \d{2}:/.test(raw) ? `${raw.replace(' ', 'T')}Z` : raw
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? raw : date.toLocaleString('zh-CN', { hour12: false })
}
function showDetail(row) { selected.value = row; drawer.value = true }
async function search() {
  if (loading.value) return
  const [start, end] = range.value || []
  if (!start || !end || end <= start || end - start > 31 * 86400000) { ElMessage.warning('请选择起止正确且不超过 31 天的时间范围'); return }
  activeQuery = { startTime: start.toISOString(), endTime: end.toISOString() }
  for (const [key, value] of Object.entries(filters)) if (value !== '' && value != null) activeQuery[key] = typeof value === 'string' ? value.trim() : value
  page.value = 1; await fetchLogs()
}
async function fetchLogs() {
  if (!activeQuery) return
  loading.value = true; error.value = ''
  try {
    const data = await queryLogs({ ...activeQuery, page: page.value, pageSize: size.value })
    rows.value = data.list; total.value = data.total; duration.value = data.queryDurationMs; queried.value = true
  } catch (err) {
    rows.value = []; total.value = 0; queried.value = false
    error.value = err.response?.status === 503 ? '日志服务或存储暂不可用，请检查日志服务与 ClickHouse 后重试' : err.response?.data?.message || err.message || '日志查询失败'
  } finally { loading.value = false }
}
function resize() { page.value = 1; fetchLogs() }
function reset() {
  if (loading.value) return
  range.value = defaultRange(); Object.assign(filters, { microservice: '', traceId: '', userId: '', deviceId: '', commandId: '', statusCode: undefined })
  activeQuery = null; rows.value = []; total.value = 0; page.value = 1; queried.value = false; error.value = ''
}
</script>

<style scoped>
.log-filters { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0 18px; }
.time-range { grid-column: span 2; }
.detail-value { overflow-wrap: anywhere; }
@media (max-width: 1000px) { .log-filters { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) { .log-filters { grid-template-columns: 1fr; } .time-range { grid-column: span 1; } }
</style>
