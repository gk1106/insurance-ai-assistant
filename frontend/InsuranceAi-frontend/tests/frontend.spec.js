import { test, expect } from '@playwright/test'
const policies = [
  {
    id: 'p1',
    policyNumber: 'POL-TEST-HEALTH',
    customerId: 'c1',
    customerName: 'Arjun Sharma',
    policyType: 'HEALTH',
    status: 'ACTIVE',
    coverageAmount: 250000,
    premiumAmount: 1200,
    startDate: '2026-01-01',
    endDate: '2029-12-31',
    createdAt: '2026-11-01T09:00:00Z',
    updatedAt: '2026-11-04T09:00:00Z',
  },
  {
    id: 'p2',
    policyNumber: 'POL-TEST-AUTO',
    customerId: 'c1',
    customerName: 'Arjun Sharma',
    policyType: 'AUTO',
    status: 'EXPIRED',
    coverageAmount: 50000,
    premiumAmount: 600,
    startDate: '2024-01-01',
    endDate: '2025-01-01',
    createdAt: '2024-01-01T09:00:00Z',
    updatedAt: '2024-01-01T09:00:00Z',
  },
]
const claims = [
  {
    id: 'cl1',
    claimNumber: 'CLM-TEST-01',
    policyId: 'p1',
    policyNumber: 'POL-TEST-HEALTH',
    claimAmount: 2500,
    approvedAmount: null,
    status: 'UNDER_REVIEW',
    incidentDate: '2026-10-28',
    description: 'Test claim description',
    filedAt: '2026-11-02T09:00:00Z',
    createdAt: '2026-11-02T09:00:00Z',
    updatedAt: '2026-11-03T09:00:00Z',
  },
]
const paged = (content) => ({
  content,
  totalElements: content.length,
  totalPages: 1,
  number: 0,
})
async function setup(page, role = 'ADMIN') {
  await page.addInitScript(
    ({ role }) => {
      localStorage.setItem(
        'insuranceai.auth',
        JSON.stringify({
          token: 'TEST_FIXTURE_ONLY',
          username: 'arjun.sharma',
          role,
          customerId: 'c1',
          expiresAt: '2099-01-01T00:00:00Z',
        }),
      )
      localStorage.setItem('insuranceai.theme', 'light')
    },
    { role },
  )
  await page.route(
    (url) => url.pathname.startsWith('/api/'),
    (route) => {
      const u = new URL(route.request().url())
      let body
      if (u.pathname === '/api/policies') body = paged(policies)
      else if (u.pathname === '/api/claims')
        body = paged(
          u.searchParams.has('policyId')
            ? claims.filter(
                (c) => c.policyId === u.searchParams.get('policyId'),
              )
            : claims,
        )
      else if (u.pathname === '/api/customers')
        body = paged([
          {
            id: 'c1',
            firstName: 'Arjun',
            lastName: 'Sharma',
            email: 'arjun.sharma@example.test',
          },
        ])
      else if (u.pathname === '/api/policies/expiring') body = []
      else if (u.pathname === '/api/renewals') body = paged([])
      else if (u.pathname.startsWith('/api/ai/'))
        body = {
          reply:
            '## Coverage summary\n\n- Hospital care\n- Emergency treatment\n\n| Benefit | Limit |\n| --- | --- |\n| Test benefit | ₹2,50,000.00 |\n\n> Verify benefits in your policy document.',
          sources: ['test-policy.pdf'],
        }
      else
        return route.fulfill({
          status: 404,
          json: { message: 'Unmocked test endpoint' },
        })
      return route.fulfill({ json: body })
    },
  )
}
test('dashboard uses live-shaped records and customer-scoped API calls', async ({
  page,
}) => {
  await setup(page, 'CUSTOMER')
  const requests = []
  page.on('request', (r) => {
    if (r.url().includes('/api/')) requests.push(new URL(r.url()))
  })
  await page.goto('/dashboard')
  await expect(page.getByText('₹2,50,000.00', { exact: true })).toBeVisible()
  await expect(
    page.getByRole('heading', { name: 'Welcome back, arjun.sharma.' }),
  ).toBeVisible()
  await expect(page.getByText('Policy updated', { exact: true })).toBeVisible()
  expect(
    requests
      .filter((u) => u.pathname === '/api/policies')
      .every((u) => u.searchParams.get('customerId') === 'c1'),
  ).toBe(true)
  expect(
    requests
      .filter((u) => u.pathname === '/api/claims')
      .every(
        (u) =>
          u.searchParams.get('policyId') === 'p1' ||
          u.searchParams.get('policyId') === 'p2',
      ),
  ).toBe(true)
  await page.screenshot({
    animations: 'disabled',
    path: 'test-results/dashboard-desktop.png',
    fullPage: true,
  })
})
test('chat renders Markdown, sends on Enter, regenerates using read-only API, saves history', async ({
  page,
}) => {
  await setup(page)
  await page.goto('/assistant')
  await expect(
    page.getByRole('heading', {
      name: "Hello! I'm your Insurance AI Assistant.",
    }),
  ).toBeVisible()
  await page.getByLabel('Save chat text').check()
  await page.getByRole('button', { name: 'What does my policy cover?' }).click()
  const input = page.getByRole('textbox', {
    name: 'Message your insurance assistant',
  })
  await expect(input).toHaveValue('What does my policy cover?')
  await input.press('Enter')
  await expect(
    page.getByRole('heading', { name: 'Coverage summary' }),
  ).toBeVisible()
  await expect(page.getByRole('table')).toBeVisible()
  await expect(page.getByText('test-policy.pdf', { exact: true })).toBeVisible()
  const request = page.waitForRequest('**/api/ai/knowledge-agent/chat')
  await page.getByRole('button', { name: 'Regenerate', exact: true }).click()
  await request
  await expect(
    page.getByRole('status').filter({ hasText: 'Answer regenerated' }),
  ).toBeVisible()
  await page.screenshot({
    animations: 'disabled',
    path: 'test-results/chat-desktop.png',
    fullPage: true,
  })
  await page.getByRole('link', { name: 'Chat History', exact: true }).click()
  await expect(
    page.getByText('What does my policy cover?', { exact: true }),
  ).toBeVisible()
  await page.getByRole('link', { name: 'Open conversation' }).click()
  await expect(page).toHaveURL(/\/assistant\?chat=/)
  await expect(
    page.getByText('What does my policy cover?', { exact: true }),
  ).toBeVisible()
  await page
    .getByRole('textbox', { name: 'Message your insurance assistant' })
    .fill('Unsent draft')
  await page.getByRole('button', { name: 'New chat', exact: true }).click()
  await expect(
    page.getByRole('textbox', { name: 'Message your insurance assistant' }),
  ).toHaveValue('')
  await expect(
    page.getByRole('heading', {
      name: "Hello! I'm your Insurance AI Assistant.",
    }),
  ).toBeVisible()
})
test('chat multiline, pending, failure and retry states', async ({ page }) => {
  await setup(page)
  let calls = 0
  await page.route('**/api/ai/chat', async (route) => {
    calls++
    await new Promise((r) => setTimeout(r, 150))
    await route.fulfill(
      calls === 1
        ? { status: 503, json: { message: 'Test service unavailable' } }
        : { json: { reply: 'Test answer' } },
    )
  })
  await page.goto('/assistant')
  const input = page.getByRole('textbox', {
    name: 'Message your insurance assistant',
  })
  await input.fill('First line')
  await input.press('Shift+Enter')
  await input.pressSequentially('Second line')
  await expect(input).toHaveValue('First line\nSecond line')
  await input.press('Enter')
  await expect(page.getByText('Test service unavailable')).toBeVisible()
  await page.getByRole('button', { name: 'Retry last request' }).click()
  await expect(page.getByText('Test answer', { exact: true })).toBeVisible()
  expect(calls).toBe(2)
  await expect(
    page.getByRole('button', { name: 'Regenerate', exact: true }),
  ).toHaveCount(0)
})
test('policy filters, view toggle and coverage detail modal', async ({
  page,
}) => {
  await setup(page)
  await page.goto('/policies')
  await page.getByRole('button', { name: 'View details' }).first().click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await expect(
    page.getByRole('heading', { name: 'Understand your coverage' }),
  ).toBeVisible()
  await page.getByRole('button', { name: 'Close', exact: true }).click()
  await page.getByRole('combobox').selectOption('EXPIRED')
  await expect(page.getByText('POL-TEST-HEALTH', { exact: true })).toHaveCount(
    0,
  )
  await expect(page.getByText('POL-TEST-AUTO', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Table', exact: true }).click()
  await expect(page.getByRole('table')).toBeVisible()
})
test('claims details show actual lifecycle and summary', async ({ page }) => {
  await setup(page)
  await page.goto('/claims')
  await page.getByRole('button', { name: 'CLM-TEST-01' }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await expect(
    page.getByRole('heading', { name: 'Claim lifecycle' }),
  ).toBeVisible()
  await expect(page.locator('li[aria-current="step"]')).toContainText(
    'Under review',
  )
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog')).not.toBeVisible()
})
test('calculator updates duration and rejects impossible deductible', async ({
  page,
}) => {
  await setup(page)
  await page.goto('/calculator')
  await page.getByLabel('Policy duration').selectOption('24')
  await page.getByRole('button', { name: 'Calculate illustration' }).click()
  await expect(page.getByText('₹3,582.00', { exact: true })).toBeVisible()
  await expect(
    page.getByRole('definition').filter({ hasText: '24 months' }),
  ).toBeVisible()
  await page.getByLabel('Deductible (INR)').fill('100000')
  await page.getByRole('button', { name: 'Calculate illustration' }).click()
  await expect(page.getByRole('alert')).toContainText(
    'deductible below your coverage',
  )
})
test('PDF validation, persistence, preview, download and removal', async ({
  page,
}) => {
  await setup(page)
  await page.goto('/documents')
  const file = page.getByLabel('Choose PDF documents')
  await file.setInputFiles({
    name: 'invalid.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('not a pdf'),
  })
  await expect(page.getByRole('alert')).toContainText('valid PDF header')
  await file.setInputFiles({
    name: 'fixture.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('%PDF-1.4\n% Test fixture only\n%%EOF'),
  })
  await expect(page.getByText('fixture.pdf', { exact: true })).toBeVisible()
  await page.reload()
  await expect(page.getByText('fixture.pdf', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Preview fixture.pdf' }).click()
  await expect(page.getByTitle('PDF preview: fixture.pdf')).toHaveAttribute(
    'src',
    /^blob:/,
  )
  await page.getByRole('button', { name: 'Close preview' }).click()
  const download = page.waitForEvent('download')
  await page.getByRole('button', { name: 'Download fixture.pdf' }).click()
  expect((await download).suggestedFilename()).toBe('fixture.pdf')
  await page.getByRole('button', { name: 'Delete fixture.pdf' }).click()
  await page.getByRole('button', { name: 'Confirm delete' }).click()
  await expect(
    page.getByRole('heading', { name: 'No documents saved yet' }),
  ).toBeVisible()
})
test('API error surfaces and retry recovers without mock product data', async ({
  page,
}) => {
  await setup(page)
  let fail = true
  await page.route('**/api/policies?**', (route) =>
    route.fulfill(
      fail
        ? { status: 500, json: { message: 'Test backend unavailable' } }
        : { json: paged(policies) },
    ),
  )
  await page.goto('/dashboard')
  await expect(page.getByText('Test backend unavailable')).toBeVisible()
  await expect(page.getByText('₹2,50,000.00', { exact: true })).toHaveCount(0)
  fail = false
  await page.getByRole('button', { name: /retry|try again/i }).click()
  await expect(page.getByText('₹2,50,000.00', { exact: true })).toBeVisible()
})
test('mobile navigation trap, chat input and layouts avoid page overflow', async ({
  page,
}) => {
  await setup(page)
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/dashboard')
  await page.getByRole('button', { name: 'Toggle navigation' }).click()
  await expect(
    page.getByRole('link', { name: 'AI Assistant', exact: true }),
  ).toBeInViewport()
  await page.keyboard.press('Shift+Tab')
  await expect(page.getByRole('link', { name: /arjun.sharma/ })).toBeFocused()
  await page.keyboard.press('Escape')
  await expect(
    page.getByRole('button', { name: 'Toggle navigation' }),
  ).toBeFocused()
  for (const path of [
    '/dashboard',
    '/assistant',
    '/policies',
    '/claims',
    '/calculator',
    '/documents',
    '/history',
    '/settings',
  ]) {
    await page.goto(path)
    await page.locator('main').waitFor()
    await expect
      .poll(() =>
        page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
      )
      .toBe(true)
  }
  await page.goto('/assistant')
  await expect(
    page.getByRole('button', { name: 'Send message' }),
  ).toBeInViewport()
  await page.screenshot({
    animations: 'disabled',
    path: 'test-results/chat-mobile.png',
    fullPage: true,
  })
})
test('settings toggles consistent dark appearance and page search works', async ({
  page,
}) => {
  await setup(page)
  await page.goto('/settings')
  await page.getByRole('switch', { name: 'Dark mode' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await page.getByRole('textbox', { name: 'Search pages' }).fill('Documents')
  await page
    .getByRole('search')
    .getByRole('link', { name: 'Documents' })
    .click()
  await expect(
    page.getByRole('heading', { name: 'Documents', exact: true }),
  ).toBeVisible()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
})
