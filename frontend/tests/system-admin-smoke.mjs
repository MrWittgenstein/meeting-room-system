import { pathToFileURL } from 'node:url'
import assert from 'node:assert/strict'
import process from 'node:process'
const { chromium } = await import(pathToFileURL(process.argv[2]).href)
const browser = await chromium.launch({ channel: 'msedge', headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
const failures = []
page.on('pageerror', error => failures.push(error.message))
let role = 'super_admin', logError = false, writes = [], users = [{ userId: 1, username: 'admin', email: 'admin@example.com', userType: 2, status: 0 }]
await page.route('http://127.0.0.1:8080/**', async route => {
  const req = route.request(), path = new URL(req.url()).pathname
  if (req.method() === 'OPTIONS') return route.fulfill({ status: 204, headers: { 'Access-Control-Allow-Origin': 'http://127.0.0.1:5175', 'Access-Control-Allow-Credentials': 'true', 'Access-Control-Allow-Headers': '*', 'Access-Control-Allow-Methods': '*' } })
  let data = { code: 1, message: 'success', data: {} }, status = 200
  if (path === '/user/session') data.data = { userId: 1, userName: 'admin', role, permissions: ['user:manage'] }
  if (path === '/user/login/password') data.data = { sessionId: 'smoke', role: 'super_admin', userType: '超级管理员' }
  if (path === '/api/user/all') data = users
  if (path === '/api/user/add') { const payload = req.postDataJSON(); writes.push(payload); users.push({ ...payload, userId: 2 }); data = true }
  if (path === '/api/user/update/2') { const payload = req.postDataJSON(); writes.push(payload); users[1] = { ...users[1], ...payload } }
  if (path === '/api/user/2' && req.method() === 'DELETE') { users = users.filter(user => user.userId !== 2); data = true }
  if (path === '/api/v1/logs/query') {
    const payload = req.postDataJSON()
    assert.ok(payload.startTime.endsWith('Z'))
    if (logError) { status = 503; data = { message: 'unavailable' } }
    else data.data = { list: [{ timestamp: '2026-11-09 00:01:00.000', microservice: 'meetingroom-main', status_code: 200, business_code: 1, trace_id: 'trace-smoke', api_endpoint: '/user/info' }], total: 1, queryDurationMs: 2 }
  }
  await route.fulfill({ status, headers: { 'Access-Control-Allow-Origin': 'http://127.0.0.1:5175', 'Access-Control-Allow-Credentials': 'true' }, contentType: 'application/json', body: JSON.stringify(data) })
})
try {
  await page.goto('http://127.0.0.1:5175/system-admin/users')
  await page.waitForURL('http://127.0.0.1:5175/')
  await page.getByPlaceholder('请输入邮箱', { exact: true }).fill('admin@example.com')
  await page.getByPlaceholder('请输入密码', { exact: true }).fill('password123')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL('**/system-admin/users')
  await page.getByText('admin@example.com', { exact: true }).waitFor()
  await page.screenshot({ path: 'run-logs/system-admin-users.png', fullPage: true })
  await page.getByRole('button', { name: '新增用户' }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('用户名', { exact: true }).fill('testuser')
  await dialog.getByLabel('邮箱', { exact: true }).fill('test@example.com')
  await dialog.getByLabel('密码', { exact: true }).fill('password123')
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await page.getByText('test@example.com', { exact: true }).waitFor()
  await page.getByRole('row').filter({ hasText: 'testuser' }).getByRole('button', { name: '编辑' }).click()
  await dialog.getByLabel('用户名', { exact: true }).fill('editeduser')
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await page.getByText('editeduser', { exact: true }).waitFor()
  assert.equal('password' in writes.at(-1), false)
  await page.getByRole('row').filter({ hasText: 'editeduser' }).getByRole('button', { name: '删除' }).click()
  await page.getByRole('button', { name: '确定', exact: true }).click()
  await page.getByText('用户已删除', { exact: true }).waitFor()
  await page.getByRole('menuitem', { name: '日志查询' }).click()
  await page.getByRole('button', { name: '查询日志' }).click()
  await page.getByRole('button', { name: '详情' }).click()
  await page.getByText('trace-smoke', { exact: true }).waitFor()
  await page.keyboard.press('Escape')
  await page.screenshot({ path: 'run-logs/system-admin-logs-success.png', fullPage: true })
  logError = true
  await page.getByRole('button', { name: '查询日志' }).click()
  await page.getByText('日志服务或存储暂不可用，请检查日志服务与 ClickHouse 后重试').waitFor()
  await page.screenshot({ path: 'run-logs/system-admin-logs.png', fullPage: true })
  role = 'user'
  await page.goto('http://127.0.0.1:5175/system-admin/users')
  await page.waitForURL('**/user/home')
  assert.deepEqual(failures, [])
  console.log('PASS: login redirect, anonymous/role guards, user create/edit/delete, blank password preservation, log query/detail/503')
} finally { await browser.close() }
