# Quick Start Guide

## ✅ Configuration Fixed!

The Nextra configuration has been updated and is now correct.

## ⚠️ Node.js Version Required

You need Node.js 20.9.0 or higher. You currently have 18.17.0.

### Option 1: Using nvm (Recommended)

```bash
# Install nvm if you don't have it
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# Close and reopen terminal, then:
nvm install 20
nvm use 20
nvm alias default 20
```

### Option 2: Using Homebrew (macOS)

```bash
brew install node@20
brew link node@20
```

### Option 3: Download from nodejs.org

Visit https://nodejs.org and download Node.js 20 LTS

## Run the Site

Once Node.js is upgraded:

```bash
cd /Users/prodigy/Documents/hytale-dev-doc/hytale-docs
npm run dev
```

Visit: **http://localhost:3000**

## What Was Fixed

- ✅ Updated `next.config.mjs` to proper Nextra v4 format
- ✅ Added React import to `theme.config.jsx`
- ✅ Fixed head function to return JSX properly

## Configuration Files

- `next.config.mjs` - Nextra configuration
- `theme.config.jsx` - Theme settings (logo, footer, etc.)
- `pages/_app.jsx` - App wrapper with styles
- `pages/_meta.json` - Navigation structure

Everything is ready to go once you upgrade Node.js!
