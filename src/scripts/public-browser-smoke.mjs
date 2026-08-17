import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const { chromium } = require('playwright')

const publicUrl = (process.env.GROUPSYNC_PUBLIC_URL || 'https://group-sync-khaki.vercel.app').replace(/\/$/, '')
const ownerEmail = process.env.GROUPSYNC_SMOKE_EMAIL
const ownerPassword = process.env.GROUPSYNC_SMOKE_PASSWORD
const studyGroupId = process.env.GROUPSYNC_STUDY_GROUP_ID
const badmintonGroupId = process.env.GROUPSYNC_BADMINTON_GROUP_ID
const badmintonSessionId = process.env.GROUPSYNC_BADMINTON_SESSION_ID
const runId = new Date().toISOString().replace(/\D/g, '').slice(0, 14)

if (!ownerEmail || !ownerPassword || !studyGroupId || !badmintonGroupId || !badmintonSessionId) {
  throw new Error('Set GROUPSYNC_SMOKE_EMAIL, GROUPSYNC_SMOKE_PASSWORD, GROUPSYNC_STUDY_GROUP_ID, GROUPSYNC_BADMINTON_GROUP_ID and GROUPSYNC_BADMINTON_SESSION_ID.')
}

const outputRoot = path.resolve('design-work', 'qa')
const screenshotDir = path.join(outputRoot, 'screenshots')
fs.mkdirSync(screenshotDir, { recursive: true })

const browser = await chromium.launch({ headless: true })
const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, locale: 'vi-VN' })
const page = await context.newPage()
const consoleErrors = []
const pageErrors = []
const failedResponses = []
let phase = 'guest'

page.on('console', (message) => {
  if (message.type() === 'error') consoleErrors.push({ phase, text: message.text() })
})
page.on('pageerror', (error) => pageErrors.push({ phase, text: error.message }))
page.on('response', (response) => {
  if (response.status() >= 400 && response.url().startsWith(`${publicUrl}/api/`)) {
    failedResponses.push({ phase, status: response.status(), url: response.url() })
  }
})

async function waitForApp() {
  await page.waitForLoadState('domcontentloaded')
  await page.locator('main').waitFor({ state: 'visible', timeout: 30000 })
  await page.waitForLoadState('networkidle', { timeout: 30000 })
  await page.waitForTimeout(750)
}

async function inspectRoute(route, label, screenshot = false) {
  phase = label
  await page.goto(`${publicUrl}${route}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await waitForApp()
  const alertText = await page.locator('[role="alert"], .status-card--error').allTextContents()
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth + 1)
  const mainTextLength = (await page.locator('main').innerText()).trim().length
  if (mainTextLength < 10) throw new Error(`${label}: main content is unexpectedly empty.`)
  if (alertText.some((text) => text.trim())) throw new Error(`${label}: visible error: ${alertText.join(' | ')}`)
  if (overflow) throw new Error(`${label}: horizontal overflow at 1440px.`)
  if (screenshot) await page.screenshot({ path: path.join(screenshotDir, `${label}-desktop.png`), fullPage: true })
  return { label, route, url: page.url(), mainTextLength, overflow, status: 'PASS' }
}

const routes = []
try {
  phase = 'login-page'
  await page.goto(`${publicUrl}/login`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await waitForApp()
  await page.screenshot({ path: path.join(screenshotDir, 'login-desktop.png'), fullPage: true })
  await page.keyboard.press('Tab')
  const focusedTag = await page.evaluate(() => document.activeElement?.tagName || '')
  if (!['A', 'BUTTON', 'INPUT', 'SELECT', 'TEXTAREA'].includes(focusedTag)) throw new Error('Login page keyboard focus is not visible on an interactive control.')

  phase = 'register-ui'
  const uiEmail = `browser.${runId}@example.com`
  await page.goto(`${publicUrl}/register`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('#register-name').fill('Browser Smoke User')
  await page.locator('#register-email').fill(uiEmail)
  await page.locator('#register-password').fill(ownerPassword)
  await page.locator('#register-confirm-password').fill(ownerPassword)
  await page.locator('form button[type="submit"], form button.button--primary').click()
  await page.waitForURL(/\/profile\/setup$/, { timeout: 30000 })
  const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', 'base64')
  await page.locator('input[type="file"]').setInputFiles({ name: 'avatar.png', mimeType: 'image/png', buffer: png })
  await page.locator('form button.button--primary').click()
  await page.waitForURL(/\/dashboard$/, { timeout: 30000 })
  routes.push({ label: 'register-profile-ui', route: '/register -> /profile/setup -> /dashboard', url: page.url(), status: 'PASS' })

  phase = 'logout-ui'
  await page.locator('.sign-out-button').click()
  await page.waitForURL(/\/login$/, { timeout: 30000 })

  phase = 'login-ui'
  await page.locator('#login-email').fill(ownerEmail)
  await page.locator('#login-password').fill(ownerPassword)
  await page.locator('form button.button--primary').click()
  await page.waitForURL(/\/dashboard$/, { timeout: 30000 })
  routes.push({ label: 'logout-login-ui', route: '/dashboard -> /login -> /dashboard', url: page.url(), status: 'PASS' })

  const checks = [
    ['/dashboard', 'dashboard', true],
    ['/calendar', 'calendar', true],
    ['/groups', 'groups', true],
    [`/groups/${studyGroupId}`, 'study-group-detail', false],
    [`/groups/${badmintonGroupId}`, 'badminton-group-detail', false],
    [`/groups/${badmintonGroupId}/availability`, 'availability', false],
    [`/study?groupId=${studyGroupId}`, 'study', false],
    [`/badminton?groupId=${badmintonGroupId}`, 'badminton', true],
    [`/badminton/sessions/${badmintonSessionId}`, 'badminton-session', false],
    [`/badminton/profile?groupId=${badmintonGroupId}`, 'badminton-profile', false],
    [`/tournaments?groupId=${badmintonGroupId}`, 'tournaments', true],
    ['/notifications', 'notifications', false],
    ['/profile', 'profile', false],
    ['/health', 'health', false],
  ]
  for (const [route, label, screenshot] of checks) routes.push(await inspectRoute(route, label, screenshot))

  phase = 'mobile'
  await page.setViewportSize({ width: 390, height: 844 })
  for (const [route, label] of [
    ['/dashboard', 'dashboard-mobile'],
    ['/calendar', 'calendar-mobile'],
    ['/groups', 'groups-mobile'],
    [`/badminton?groupId=${badmintonGroupId}`, 'badminton-mobile'],
    [`/tournaments?groupId=${badmintonGroupId}`, 'tournaments-mobile'],
  ]) {
    await page.goto(`${publicUrl}${route}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await waitForApp()
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth + 1)
    if (overflow) throw new Error(`${label}: horizontal overflow at 390px.`)
    await page.screenshot({ path: path.join(screenshotDir, `${label}.png`), fullPage: true })
    routes.push({ label, route, url: page.url(), overflow, status: 'PASS' })
  }

  const expectedGuestAuthCheck = (entry) =>
    entry.status === 401 &&
    entry.url.endsWith('/api/auth/me') &&
    ['login-page', 'register-ui', 'logout-ui'].includes(entry.phase)
  const unexpectedResponses = failedResponses.filter((entry) => !expectedGuestAuthCheck(entry))
  const actionableConsoleErrors = consoleErrors.filter((entry) => !entry.text.startsWith('Failed to load resource:'))
  if (pageErrors.length) throw new Error(`Page errors: ${JSON.stringify(pageErrors)}`)
  if (actionableConsoleErrors.length) throw new Error(`Console errors: ${JSON.stringify(actionableConsoleErrors)}`)
  if (unexpectedResponses.length) throw new Error(`Failed API responses: ${JSON.stringify(unexpectedResponses)}`)

  const report = {
    status: 'PUBLIC_BROWSER_SMOKE_PASS',
    publicUrl,
    desktopViewport: { width: 1440, height: 1000 },
    mobileViewport: { width: 390, height: 844 },
    keyboardFocus: 'PASS',
    registrationAndProfileSetup: 'PASS',
    logoutAndLogin: 'PASS',
    routes,
    consoleErrors: actionableConsoleErrors,
    pageErrors,
    failedResponses: unexpectedResponses,
    screenshots: fs.readdirSync(screenshotDir).filter((name) => name.endsWith('.png')).sort(),
  }
  fs.writeFileSync(path.join(outputRoot, 'public-browser-smoke.json'), JSON.stringify(report, null, 2))
  process.stdout.write(JSON.stringify(report, null, 2))
} finally {
  await browser.close()
}
