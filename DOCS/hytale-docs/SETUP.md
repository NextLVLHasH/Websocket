# Setup Instructions

## Prerequisites

### Node.js Version Requirement
This project requires **Node.js 20.9.0 or higher**.

Check your current version:
```bash
node --version
```

### Installing Node.js 20+

#### macOS (using Homebrew)
```bash
brew install node@20
```

#### macOS/Linux (using nvm)
```bash
# Install nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# Install Node.js 20
nvm install 20
nvm use 20
```

#### Windows
Download from [nodejs.org](https://nodejs.org) and install Node.js 20 LTS.

## Installation

1. **Clone or navigate to the project directory**
```bash
cd hytale-docs
```

2. **Install dependencies**
```bash
npm install
```

3. **Start development server**
```bash
npm run dev
```

The site will be available at [http://localhost:3000](http://localhost:3000)

## Available Commands

- `npm run dev` - Start development server (hot reload enabled)
- `npm run build` - Build for production
- `npm start` - Start production server
- `npm run lint` - Run linter

## Project Structure

```
hytale-docs/
├── pages/                      # All documentation pages
│   ├── index.mdx              # Homepage
│   ├── _app.jsx               # App wrapper
│   ├── _meta.json             # Navigation structure
│   ├── getting-started/       # Getting started guides
│   ├── server-setup/          # Server administration
│   ├── plugin-development/    # Plugin dev guides
│   ├── programming-languages.mdx
│   ├── common-issues.mdx
│   └── community-resources.mdx
├── theme.config.jsx           # Nextra theme configuration
├── next.config.mjs            # Next.js configuration
└── package.json               # Dependencies

```

## Editing Content

All documentation is written in MDX (Markdown + JSX):

1. Navigate to `pages/` directory
2. Edit `.mdx` files
3. Changes auto-reload in development mode

### Adding New Pages

1. Create new `.mdx` file in `pages/` directory
2. Update `_meta.json` to add to navigation

Example:
```json
{
  "my-page": "My Page Title"
}
```

## Deployment

### Vercel (Recommended)
1. Push to GitHub
2. Import project on [vercel.com](https://vercel.com)
3. Deploy automatically

### Netlify
1. Push to GitHub
2. Connect repository on [netlify.com](https://netlify.com)
3. Build command: `npm run build`
4. Publish directory: `.next`

### Self-hosted
```bash
# Build
npm run build

# Start production server
npm start
```

## Troubleshooting

### Node.js Version Error
```
Error: Node.js version ">=20.9.0" is required
```
**Solution**: Upgrade Node.js to version 20 or higher

### Port Already in Use
```
Error: Port 3000 is already in use
```
**Solution**: Use a different port
```bash
PORT=3001 npm run dev
```

### Module Not Found
```
Error: Cannot find module 'nextra'
```
**Solution**: Reinstall dependencies
```bash
rm -rf node_modules package-lock.json
npm install
```

## Contributing

See [README.md](README.md) for contribution guidelines.

## Support

- Check [Common Issues](/common-issues) page
- Community Discord
- GitHub Issues
