<template>
  <section>
    <div class="page-heading"><div><h1>用户管理</h1><p class="muted">管理普通用户、会议室管理员和系统管理员账号</p></div><el-button type="primary" :icon="Plus" @click="edit()">新增用户</el-button></div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索用户编号、用户名、姓名或邮箱" style="width: 310px" :prefix-icon="Search" @input="page = 1" />
        <el-select v-model="role" clearable placeholder="全部角色" style="width: 170px" @change="page = 1"><el-option v-for="item in roles" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom: 16px" />
      <el-alert v-if="users.length >= 1000" title="当前接口最多加载 1000 条用户，筛选和分页仅针对已加载的记录。" type="info" :closable="false" style="margin-bottom: 16px" />
      <el-table v-loading="loading" :data="visibleUsers" empty-text="暂无匹配用户" stripe>
        <el-table-column prop="userId" label="编号" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="110" />
        <el-table-column label="邮箱" min-width="210" show-overflow-tooltip><template #default="{ row }"><el-tag v-if="row.emailUnavailable" type="warning">历史邮箱无法解密</el-tag><span v-else>{{ row.email || '未填写' }}</span></template></el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column label="角色" min-width="140"><template #default="{ row }"><el-tag :type="row.userType === 2 ? 'warning' : 'primary'">{{ roleName(row.userType) }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'danger' : 'success'">{{ row.status === 1 ? '冻结' : '正常' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="150" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="edit(row)">编辑</el-button><el-button link type="danger" :disabled="busy" @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :page-sizes="[10, 20, 50]" :total="filtered.length" layout="total, sizes, prev, pager, next" @size-change="page = 1" />
    </el-card>
    <el-dialog v-model="dialog" :title="editingId == null ? '新增用户' : '编辑用户'" width="min(560px, 94vw)" :close-on-click-modal="false">
      <el-alert v-if="editingEmailUnavailable" title="历史邮箱无法解密。留空保留原数据，填写正确邮箱后保存可修复该账号邮箱。" type="warning" :closable="false" show-icon style="margin-bottom: 16px" />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" maxlength="20" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" maxlength="50" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model="form.email" :placeholder="editingId == null ? '请输入邮箱' : '留空保留原邮箱'" /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" maxlength="20" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" :placeholder="editingId == null ? '至少 8 位' : '留空保留原密码'" /></el-form-item>
        <el-form-item label="角色" prop="userType"><el-select v-model="form.userType"><el-option v-for="item in roles" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="账号状态"><el-radio-group v-model="form.status"><el-radio :value="0">正常</el-radio><el-radio :value="1">冻结</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
defineOptions({ name: 'SystemAdminUsers' })
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { listUsers, createUser, updateUser, deleteUser } from '@/apis/systemAdmin'
const roles = [{ value: 1, label: '普通用户' }, { value: 0, label: '会议室管理员' }, { value: 2, label: '系统管理员' }]
const roleName = value => roles.find(item => item.value === value)?.label || '未知角色'
const users = ref([]), loading = ref(false), error = ref(''), keyword = ref(''), role = ref(''), page = ref(1), size = ref(10)
const dialog = ref(false), editingId = ref(null), saving = ref(false), busy = ref(false), formRef = ref()
const editingEmailUnavailable = ref(false)
const form = reactive({ username: '', realName: '', email: '', phone: '', password: '', userType: 1, status: 0 })
const rules = {
  username: [{ required: true, whitespace: true, message: '请输入用户名', trigger: 'blur' }, { min: 2, max: 20, message: '用户名为 2—20 位', trigger: 'blur' }],
  email: [{ validator: (_rule, value, callback) => callback(editingId.value == null && !value.trim() ? new Error('请输入邮箱') : undefined), trigger: 'blur' }, { type: 'email', message: '请输入正确邮箱', trigger: 'blur' }],
  password: [{ validator: (_rule, value, callback) => callback((editingId.value == null || value) && value.length < 8 ? new Error('密码至少 8 位') : undefined), trigger: 'blur' }],
  userType: [{ required: true, message: '请选择角色', trigger: 'change' }],
}
const filtered = computed(() => users.value.filter(user => (role.value === '' || user.userType === role.value) && [user.userId, user.username, user.realName, user.email].some(value => String(value ?? '').toLowerCase().includes(keyword.value.trim().toLowerCase()))))
const visibleUsers = computed(() => filtered.value.slice((page.value - 1) * size.value, page.value * size.value))
const failure = err => err.response?.data?.message || err.message || '请求失败，请重试'
async function load() {
  loading.value = true; error.value = ''
  try { users.value = await listUsers(); page.value = Math.min(page.value, Math.max(1, Math.ceil(filtered.value.length / size.value))) }
  catch (err) { users.value = []; error.value = failure(err) }
  finally { loading.value = false }
}
async function edit(user) {
  editingId.value = user?.userId ?? null
  editingEmailUnavailable.value = Boolean(user?.emailUnavailable)
  Object.assign(form, { username: user?.username || '', realName: user?.realName || '', email: user?.email || '', phone: user?.phone || '', password: '', userType: user?.userType ?? 1, status: user?.status ?? 0 })
  dialog.value = true; await nextTick(); formRef.value?.clearValidate()
}
async function save() {
  if (saving.value) return
  try { await formRef.value.validate() } catch { return }
  saving.value = true
  try {
    const payload = { ...form, username: form.username.trim(), email: form.email.trim() }
    if (editingId.value != null && !payload.password) delete payload.password
    if (editingId.value != null && !payload.email) delete payload.email
    if (editingId.value == null) await createUser(payload); else await updateUser(editingId.value, payload)
    ElMessage.success('用户已保存'); dialog.value = false; await load()
  } catch (err) { ElMessage.error(failure(err)) } finally { saving.value = false }
}
async function remove(user) {
  try { await ElMessageBox.confirm(`确定删除用户“${user.username}”？此操作不可撤销。`, '删除用户', { type: 'warning' }) } catch { return }
  busy.value = true
  try { await deleteUser(user.userId); ElMessage.success('用户已删除'); await load() }
  catch (err) { ElMessage.error(failure(err)) } finally { busy.value = false }
}
onMounted(load)
</script>
