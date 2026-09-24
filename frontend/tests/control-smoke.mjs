import { pathToFileURL } from 'node:url'
import assert from 'node:assert/strict'

const { chromium } = await import(pathToFileURL(process.argv[2]).href)
const browser = await chromium.launch({ channel: 'msedge', headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
let commandCount = 0
let failureMode = 'error'
let latestReads = 0
const errors = []
page.on('pageerror', error => errors.push(error.message))
await page.route('http://127.0.0.1:8080/**', async route => {
  const path = new URL(route.request().url()).pathname
  if (path.startsWith('/assets/') || path.startsWith('/vendor/')) return route.continue()
  let data = {}
  let status = 200
  if (path === '/user/info') data = { username: 'Acceptance Test' }
  else if (path === '/iot/control-rooms') data = [
    { roomId: 14, roomName: 'Test Room 14', deviceId: 'raspi-01' },
    { roomId: 15, roomName: 'Test Room 15', deviceId: 'raspi-02' },
  ]
  else if (path.endsWith('/control-access')) data = { allowed: !path.includes('raspi-02') }
  else if (path.endsWith('/commands')) {
    commandCount++
    assert.equal(route.request().postDataJSON().command, 'set_device_state')
    data = { status: 'error' }
    if (failureMode === 'timeout') status = 504
  } else if (path.endsWith('/latest')) {
    latestReads++
    data = { devices: { light_on: true, projector: true }, environment: { temperature: 25 } }
  } else if (path.endsWith('/last-seven')) data = []
  await route.fulfill({ status, contentType: 'application/json',
    headers: { 'Access-Control-Allow-Origin': 'http://127.0.0.1:5173', 'Access-Control-Allow-Credentials': 'true' },
    body: JSON.stringify({ code: 1, message: 'test device response', data }) })
})
try {
  await page.goto('http://127.0.0.1:5173/user/control')
  const light = page.locator('.device-status-row').filter({ hasText: '灯光' }).locator('.el-switch')
  await light.waitFor()
  for (failureMode of ['error', 'timeout']) {
    for (let attempt = 0; attempt < 5; attempt++) {
      const before = commandCount
      await page.waitForFunction(() => !document.querySelectorAll('.el-switch')[1].classList.contains('is-disabled'))
      await light.click()
      await page.waitForFunction(() => document.querySelectorAll('.el-switch')[1].classList.contains('is-checked'))
      await page.waitForTimeout(250)
      assert.equal(commandCount, before + 1, 'one action must send exactly one command')
      assert.equal(await page.getByText('light: 设备已确认执行', { exact: true }).count(), 0)
    }
  }
  assert.ok(latestReads >= 10)
  await page.waitForTimeout(3500)
  await page.screenshot({ path: '../run-logs/control-desktop.png', fullPage: true })
  await page.getByRole('button', { name: /Test Room 15/ }).click()
  await page.waitForTimeout(500)
  assert.ok((await light.getAttribute('class')).includes('is-disabled'))
  await page.setViewportSize({ width: 390, height: 844 })
  await page.waitForTimeout(500)
  await page.screenshot({ path: '../run-logs/control-mobile.png', fullPage: true })
  assert.deepEqual(errors, [])
  console.log(JSON.stringify({ errorAckCases: 5, timeoutCases: 5, commands: commandCount, latestReads, roomSwitchDenied: true }))
} finally {
  await browser.close()
}
