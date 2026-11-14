import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const { VITE_SITE_URL } = loadEnv(mode, process.cwd(), 'VITE_')
  let siteOrigin
  if (VITE_SITE_URL) {
    const site = new URL(VITE_SITE_URL)
    if (
      site.protocol !== 'https:' ||
      site.username ||
      site.password ||
      site.pathname !== '/' ||
      site.search ||
      site.hash
    ) {
      throw new Error(
        'VITE_SITE_URL must be an HTTPS origin without a path, credentials, query or fragment.',
      )
    }
    siteOrigin = site.origin
  }
  return {
    plugins: [
      react(),
      {
        name: 'insurance-public-metadata',
        transformIndexHtml(html) {
          if (!siteOrigin) return html
          return html
            .replace(
              /(<meta\s+(?:property="og:image"|name="twitter:image")\s+content=")[^"]+("\s*\/?>)/g,
              `$1${siteOrigin}/social-preview.png$2`,
            )
            .replace(
              '</head>',
              `  <meta property="og:url" content="${siteOrigin}/" />\n  </head>`,
            )
        },
      },
    ],
    server: {
      proxy: {
        '/api': { target: 'http://localhost:8082', changeOrigin: true },
      },
    },
  }
})
