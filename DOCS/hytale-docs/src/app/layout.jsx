import { Banner, Head } from 'nextra/components'
import { Footer, Layout, Navbar } from 'nextra-theme-docs'
import { getPageMap } from 'nextra/page-map'
import Script from 'next/script'
import 'nextra-theme-docs/style.css'

const banner = (
  <Banner storageKey="hytale-docs-banner">
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
      <img src="/hytale-logo.png" alt="Hytale logo" width="20" height="20" />
      <span style={{ fontWeight: 600 }}>Community-built docs</span>
      <span style={{ opacity: 0.85 }}>Open source and updated by the community.</span>
    </span>
  </Banner>
)
const navbar = (
  <Navbar
    logo={
      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
        <img src="/hytale-logo.png" alt="Hytale logo" width="28" height="28" />
        <b>Hytale Documentation for devs</b>
      </span>
    }
    projectLink="https://github.com/corentingosselin/hytale-dev-doc"
  >
    <a
      href="https://www.paypal.com/donate/?hosted_button_id=RG7M76J5ZDFYW"
      target="_blank"
      rel="noopener noreferrer"
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '6px',
        padding: '6px 12px',
        backgroundColor: '#0070ba',
        color: 'white',
        borderRadius: '6px',
        fontSize: '14px',
        fontWeight: 500,
        textDecoration: 'none',
        marginRight: '8px'
      }}
    >
      Donate
    </a>
  </Navbar>
)
const footer = <Footer>MIT {new Date().getFullYear()} © Hytale Community Documentation</Footer>

export const metadata = {
  title: {
    template: '%s – Hytale Dev Docs',
    default: 'Hytale Dev Docs'
  },
  description: 'Community-driven Hytale development documentation',
  icons: {
    icon: [
      { url: '/favicon.ico' },
      { url: '/favicon.svg', type: 'image/svg+xml' },
      { url: '/favicon-96x96.png', type: 'image/png', sizes: '96x96' }
    ],
    apple: [{ url: '/apple-touch-icon.png', sizes: '180x180', type: 'image/png' }]
  },
  manifest: '/site.webmanifest'
}

export default async function RootLayout({ children }) {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'Organization',
    name: 'HytaleDocs',
    url: 'https://hytale-docs.com',
    logo: 'https://hytale-docs.com/hytale-logo.png'
  }

  return (
    <html lang="en" dir="ltr" suppressHydrationWarning>
      <Head>
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      </Head>
      <body>
        <Script
          src="https://www.googletagmanager.com/gtag/js?id=G-QBZFXC32PV"
          strategy="afterInteractive"
        />
        <Script id="google-analytics" strategy="afterInteractive">
          {`
            window.dataLayer = window.dataLayer || [];
            function gtag(){dataLayer.push(arguments);}
            gtag('js', new Date());
            gtag('config', 'G-QBZFXC32PV');
          `}
        </Script>
        <Layout
          banner={banner}
          navbar={navbar}
          pageMap={await getPageMap()}
          docsRepositoryBase="https://github.com/yourusername/hytale-docs/tree/main"
          footer={footer}
        >
          {children}
        </Layout>
      </body>
    </html>
  )
}
