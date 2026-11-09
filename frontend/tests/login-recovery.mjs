import { pathToFileURL } from 'node:url'
import process from 'node:process'
import assert from 'node:assert/strict'
const { chromium } = await import(pathToFileURL(process.argv[2]).href)
const browser = await chromium.launch({ channel: 'msedge', headless: true })
const base = 'http://127.0.0.1:5173'
try {
  for (const mode of ['transient', 'unavailable', 'expired', 'timeout']) {
    const page = await browser.newPage()
    let checks = 0, logins = 0, recovered = false
    await page.route('http://127.0.0.1:8080/**', async route => {
      const req = route.request(), path = new URL(req.url()).pathname
      const headers = { 'Access-Control-Allow-Origin': base, 'Access-Control-Allow-Credentials': 'true', 'Access-Control-Allow-Headers': '*', 'Access-Control-Allow-Methods': '*' }
      if (req.method() === 'OPTIONS') return route.fulfill({ status: 204, headers })
      let status = 200, data = { code: 1, message: 'success', data: {} }
      if (path === '/user/login/password') { logins++; data.data = { sessionId: 'new-session', role: 'super_admin' } }
      if (path === '/user/session') {
        checks++
        if (mode === 'expired') { status = 401; data = { message: 'Session expired' } }
        else if (mode === 'unavailable' && !recovered || mode === 'transient' && checks === 1) { status = 503; data = { message: 'temporarily unavailable' } }
        else if (mode === 'timeout' && checks === 1) {
          await new Promise(resolve => setTimeout(resolve, 4600))
          return route.abort().catch(() => {})
        } else data.data = { role: 'super_admin', userName: 'admin', permissions: [] }
      }
      if (path === '/api/user/all') data = []
      await route.fulfill({ status, headers, contentType: 'application/json', body: JSON.stringify(data) })
    })
    await page.goto(base)
    await page.getByPlaceholder('请输入邮箱', { exact: true }).fill('admin@example.com')
    await page.getByPlaceholder('请输入密码', { exact: true }).fill('password123')
    await page.getByRole('button', { name: '登录', exact: true }).click()
    if (mode === 'unavailable') {
      await page.getByText('暂时无法校验登录状态，请重试；无需重新输入账号密码').waitFor()
      assert.equal(await page.evaluate(() => localStorage.getItem('sessionId')), 'new-session')
      assert.equal(new URL(page.url()).pathname, '/')
      recovered = true
      await page.getByRole('button', { name: '重试进入', exact: true }).click()
      await page.waitForURL('**/system-admin/users')
      assert.equal(logins, 1)
    } else if (mode === 'expired') {
      await page.getByText('登录已过期，请重新登录').waitFor()
      assert.equal(await page.evaluate(() => localStorage.getItem('sessionId')), null)
      assert.equal(new URL(page.url()).pathname, '/')
      assert.equal(checks, 1)
    } else {
      await page.waitForURL('**/system-admin/users')
      assert.equal(checks, 2)
      assert.equal(logins, 1)
    }
    console.log(`PASS ${mode}`)
    await page.close()
  }
  const page = await browser.newPage()
  let release
  const gate = new Promise(resolve => { release = resolve })
  await page.route('http://127.0.0.1:8080/delayed-session-check', async route => {
    const headers = { 'Access-Control-Allow-Origin': base, 'Access-Control-Allow-Credentials': 'true', 'Access-Control-Allow-Headers': '*' }
    if (route.request().method() === 'OPTIONS') return route.fulfill({ status: 204, headers })
    await gate
    await route.fulfill({ status: 401, headers, contentType: 'application/json', body: '{}' })
  })
  await page.goto(base)
  const requested = page.waitForRequest(req => req.url().endsWith('/delayed-session-check') && req.method() !== 'OPTIONS')
  await page.evaluate(async () => {
    const { default: http } = await import('/src/utils/http.js')
    localStorage.setItem('sessionId', 'old-session')
    window.oldCheck = http.get('/delayed-session-check').catch(() => {})
  })
  await requested
  await page.evaluate(() => localStorage.setItem('sessionId', 'new-session'))
  release()
  await page.evaluate(() => window.oldCheck)
  assert.equal(await page.evaluate(() => localStorage.getItem('sessionId')), 'new-session')
  console.log('PASS delayed old-session 401 preserves new session')
  await page.close()
} finally { await browser.close() }
