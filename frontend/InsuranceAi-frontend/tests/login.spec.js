import { test, expect } from '@playwright/test'

test('login artwork, browser metadata and motion controls load', async ({
  page,
}) => {
  await page.goto('/login')
  await expect(page).toHaveTitle(
    'Insurance AI | Your Personal Insurance Assistant',
  )
  await expect(
    page.getByRole('heading', { name: 'Welcome back.' }),
  ).toBeVisible()
  const image = page.locator('img[src="/images/insurance-shield-3d.webp"]')
  await expect
    .poll(() => image.evaluate((img) => img.complete && img.naturalWidth > 0))
    .toBe(true)
  await page.getByRole('button', { name: 'Pause background motion' }).click()
  await expect(image).toHaveCSS('animation-play-state', 'paused')
  await page.getByLabel('Username', { exact: true }).fill('arjun.sharma')
  const password = page.getByLabel('Password', { exact: true })
  await password.fill('fixture-password')
  await page.getByRole('button', { name: 'Show password', exact: true }).click()
  await expect(password).toHaveAttribute('type', 'text')
  await page.getByRole('button', { name: 'Hide password', exact: true }).click()
  await expect(password).toHaveAttribute('type', 'password')
  await expect(page.locator('meta[name="description"]')).toHaveAttribute(
    'content',
    /Indian rupees/,
  )
  for (const path of [
    '/favicon.svg?v=2',
    '/favicon.ico',
    '/favicon-32.png',
    '/apple-touch-icon.png',
    '/icon-192.png',
    '/icon-512.png',
    '/icon-maskable-512.png',
    '/social-preview.png',
  ]) {
    const response = await page.request.get(path)
    expect(response.ok(), path).toBe(true)
    expect(response.headers()['content-type'], path).not.toContain('text/html')
  }
  const manifest = await (await page.request.get('/site.webmanifest')).json()
  expect(manifest.name).toContain('Insurance AI')
  expect(manifest.icons).toHaveLength(3)
  await page.screenshot({
    path: 'test-results/login-desktop.png',
    fullPage: true,
    animations: 'disabled',
  })
})

test('login handles rejected credentials and successful authentication through existing API', async ({
  page,
}) => {
  let fail = true
  await page.route(
    (url) => url.pathname.startsWith('/api/'),
    async (route) => {
      if (new URL(route.request().url()).pathname === '/api/auth/login') {
        expect(route.request().postDataJSON()).toEqual({
          username: 'arjun.sharma',
          password: 'fixture-password',
        })
        if (fail)
          return route.fulfill({
            status: 401,
            json: { message: 'Invalid username or password' },
          })
        const token =
          'test.' +
          Buffer.from(JSON.stringify({ customerId: 'c1' })).toString(
            'base64url',
          ) +
          '.test'
        return route.fulfill({
          json: {
            token,
            username: 'arjun.sharma',
            role: 'CUSTOMER',
            expiresAt: '2099-01-01T00:00:00Z',
          },
        })
      }
      return route.fulfill({
        json: { content: [], totalPages: 1, totalElements: 0 },
      })
    },
  )
  await page.goto('/login')
  await page.getByLabel('Username', { exact: true }).fill('arjun.sharma')
  await page.getByLabel('Password', { exact: true }).fill('fixture-password')
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText(
    'Invalid username or password',
  )
  await expect(
    page.getByRole('button', { name: 'Sign in', exact: true }),
  ).toBeEnabled()
  fail = false
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
})

test('mobile and dark login remain readable and respect reduced motion', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.emulateMedia({ reducedMotion: 'reduce', colorScheme: 'dark' })
  await page.goto('/login')
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await expect(
    page.locator('img[src="/images/insurance-shield-3d.webp"]'),
  ).toHaveCSS('animation-name', 'none')
  await expect(
    page.getByRole('button', { name: 'Pause background motion' }),
  ).not.toBeVisible()
  await expect(page.getByLabel('Username', { exact: true })).toBeVisible()
  await expect(
    page.getByRole('button', { name: 'Sign in', exact: true }),
  ).toBeVisible()
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true)
  await page.screenshot({
    path: 'test-results/login-mobile-dark.png',
    fullPage: true,
    animations: 'disabled',
  })
  await page.getByRole('link', { name: 'Create one' }).click()
  await expect(page).toHaveURL(/\/register$/)
})
