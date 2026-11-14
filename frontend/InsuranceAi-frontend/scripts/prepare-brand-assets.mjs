import { chromium } from '@playwright/test'
import { readFile, writeFile, mkdir } from 'node:fs/promises'
import { resolve } from 'node:path'
const original = process.argv[2]
if (!original)
  throw new Error(
    'Pass the original generated shield PNG path as the argument.',
  )
const output = resolve('public')
await mkdir(resolve(output, 'images'), { recursive: true })
const browser = await chromium.launch()
try {
  const page = await browser.newPage({
    viewport: { width: 1200, height: 630 },
    deviceScaleFactor: 1,
  })
  const data =
    'data:image/png;base64,' + (await readFile(original)).toString('base64')
  const webp = await page.evaluate(async (data) => {
    const image = new Image()
    image.src = data
    await image.decode()
    const canvas = document.createElement('canvas')
    canvas.width = image.width
    canvas.height = image.height
    canvas.getContext('2d').drawImage(image, 0, 0)
    return canvas.toDataURL('image/webp', 0.88).split(',')[1]
  }, data)
  await writeFile(
    resolve(output, 'images/insurance-shield-3d.webp'),
    Buffer.from(webp, 'base64'),
  )
  const svg = await readFile(resolve(output, 'favicon.svg'), 'utf8')
  for (const [name, size, maskable] of [
    ['favicon-32.png', 32, false],
    ['apple-touch-icon.png', 180, false],
    ['icon-192.png', 192, false],
    ['icon-512.png', 512, false],
    ['icon-maskable-512.png', 512, true],
  ]) {
    await page.setViewportSize({ width: size, height: size })
    await page.setContent(
      `<html><body style="margin:0;width:100vw;height:100vh;background:${maskable ? '#173B5E' : 'transparent'};display:grid;place-items:center"><div style="width:${maskable ? '64%' : '100%'};height:${maskable ? '64%' : '100%'}">${svg.replace('<svg ', '<svg width="100%" height="100%" ')}</div></body></html>`,
    )
    await page.screenshot({
      path: resolve(output, name),
      omitBackground: !maskable,
    })
  }
  // ICO supports embedded PNG images for current Windows and browser versions.
  const png = await readFile(resolve(output, 'favicon-32.png'))
  const header = Buffer.alloc(22)
  header.writeUInt16LE(1, 2)
  header.writeUInt16LE(1, 4)
  header[6] = 32
  header[7] = 32
  header.writeUInt16LE(1, 10)
  header.writeUInt16LE(32, 12)
  header.writeUInt32LE(png.length, 14)
  header.writeUInt32LE(22, 18)
  await writeFile(resolve(output, 'favicon.ico'), Buffer.concat([header, png]))
  await page.setViewportSize({ width: 1200, height: 630 })
  await page.setContent(
    `<html><body style="margin:0;background:#0c2036;width:1200px;height:630px;overflow:hidden;font-family:Arial,sans-serif;color:white"><img src="${data}" style="position:absolute;right:-70px;top:0;width:945px;height:630px;object-fit:cover"/><div style="position:absolute;inset:0;background:linear-gradient(90deg,#0c2036 10%,#0c2036ea 34%,transparent 75%)"></div><div style="position:absolute;left:64px;top:66px;display:flex;align-items:center;gap:14px;font-size:26px;font-weight:bold"><div style="width:48px;height:48px">${svg}</div>Insurance AI</div><div style="position:absolute;left:64px;top:235px;max-width:550px"><h1 style="font-size:56px;line-height:1.1;letter-spacing:-2px;margin:0 0 24px">Your personal<br/>insurance guide.</h1><p style="font-size:21px;color:#b9cce0;line-height:1.6">Clear answers for your policies,<br/>premiums, and claims.</p></div></body></html>`,
  )
  await page.locator('img').evaluate((image) => image.decode())
  await page.screenshot({ path: resolve(output, 'social-preview.png') })
  console.log(
    'Generated optimized WebP, favicons, touch and app icons, and social preview.',
  )
} finally {
  await browser.close()
}
