import { promises as fs } from 'node:fs'
import path from 'node:path'

const CONTENT_DIR = path.join(process.cwd(), 'src', 'content')

async function collectMdxFiles(dir) {
  const entries = await fs.readdir(dir, { withFileTypes: true })
  const files = await Promise.all(
    entries.map(async (entry) => {
      const fullPath = path.join(dir, entry.name)
      if (entry.isDirectory()) {
        return collectMdxFiles(fullPath)
      }
      if (entry.isFile() && entry.name.endsWith('.mdx')) {
        return [fullPath]
      }
      return []
    })
  )
  return files.flat()
}

function filePathToRoute(filePath) {
  const relativePath = path.relative(CONTENT_DIR, filePath)
  const withoutExt = relativePath.replace(/\.mdx$/, '')
  const parts = withoutExt.split(path.sep)
  if (parts[parts.length - 1] === 'index') {
    parts.pop()
  }
  const routePath = `/${parts.join('/')}`.replace(/\/+/g, '/')
  return routePath === '' ? '/' : routePath
}

function getSiteUrl() {
  const raw =
    process.env.NEXT_PUBLIC_SITE_URL ||
    process.env.SITE_URL ||
    'http://localhost:3000'
  return raw.replace(/\/+$/, '')
}

export async function GET() {
  const siteUrl = getSiteUrl()
  const files = await collectMdxFiles(CONTENT_DIR)
  const urls = await Promise.all(
    files.map(async (filePath) => {
      const routePath = filePathToRoute(filePath)
      const stat = await fs.stat(filePath)
      const lastmod = stat.mtime.toISOString()
      return { loc: `${siteUrl}${routePath}`, lastmod }
    })
  )

  const xml = `<?xml version="1.0" encoding="UTF-8"?>\n` +
    `<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n` +
    urls
      .map(
        (url) =>
          `  <url>\n` +
          `    <loc>${url.loc}</loc>\n` +
          `    <lastmod>${url.lastmod}</lastmod>\n` +
          `  </url>`
      )
      .join('\n') +
    `\n</urlset>\n`

  return new Response(xml, {
    headers: {
      'Content-Type': 'application/xml'
    }
  })
}
