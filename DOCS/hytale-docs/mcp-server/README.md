# Hytale Docs MCP Server

An MCP (Model Context Protocol) server that provides AI assistants with access to the Hytale documentation.

## Features

- **search_docs** - Search the documentation for relevant content
- **read_doc** - Read the full content of a specific documentation page
- **list_docs** - List all available documentation pages

## Installation

```bash
cd mcp-server
npm install
```

## Usage with Claude Code

Add to your Claude Code MCP settings (`.claude/settings.json` or VS Code settings):

```json
{
  "mcpServers": {
    "hytale-docs": {
      "command": "node",
      "args": ["/path/to/hytale-docs/mcp-server/index.js"]
    }
  }
}
```

## Usage with Claude Desktop

Add to your Claude Desktop config (`~/Library/Application Support/Claude/claude_desktop_config.json` on macOS):

```json
{
  "mcpServers": {
    "hytale-docs": {
      "command": "node",
      "args": ["/path/to/hytale-docs/mcp-server/index.js"]
    }
  }
}
```

## Available Tools

### search_docs

Search the Hytale documentation for relevant content.

**Parameters:**
- `query` (required): Search query to find in the documentation
- `limit` (optional): Maximum number of results to return (default: 5)

### read_doc

Read the full content of a specific documentation page.

**Parameters:**
- `path` (required): The documentation path (e.g., 'plugin-development/commands')

### list_docs

List all available documentation pages.

**Parameters:**
- `category` (optional): Filter by category (e.g., 'plugin-development', 'server-setup')

## Running Standalone

```bash
npm run mcp
# or
node mcp-server/index.js
```

The server communicates via stdio (stdin/stdout) following the MCP protocol.
