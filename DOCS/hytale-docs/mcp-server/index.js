import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  ListResourcesRequestSchema,
  ReadResourceRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { glob } from "glob";
import matter from "gray-matter";
import fs from "fs/promises";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Path to documentation content
const CONTENT_DIR = path.join(__dirname, "..", "src", "content");

// Cache for documentation content
let docsCache = null;
let lastCacheTime = 0;
const CACHE_TTL = 60000; // 1 minute cache

async function loadDocs() {
  const now = Date.now();
  if (docsCache && now - lastCacheTime < CACHE_TTL) {
    return docsCache;
  }

  const files = await glob("**/*.mdx", { cwd: CONTENT_DIR });
  const docs = [];

  for (const file of files) {
    const filePath = path.join(CONTENT_DIR, file);
    const content = await fs.readFile(filePath, "utf-8");
    const { data: frontmatter, content: body } = matter(content);

    // Extract title from first heading or filename
    const titleMatch = body.match(/^#\s+(.+)$/m);
    const title =
      frontmatter.title ||
      (titleMatch ? titleMatch[1] : path.basename(file, ".mdx"));

    // Generate URL path from file path
    const urlPath = file
      .replace(/\.mdx$/, "")
      .replace(/index$/, "")
      .replace(/\/$/, "");

    docs.push({
      file,
      path: urlPath || "/",
      title,
      content: body,
      frontmatter,
    });
  }

  docsCache = docs;
  lastCacheTime = now;
  return docs;
}

// Create MCP server
const server = new Server(
  {
    name: "hytale-docs",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
      resources: {},
    },
  }
);

// List available tools
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "search_docs",
        description:
          "Search the Hytale documentation for relevant content. Returns matching sections with context.",
        inputSchema: {
          type: "object",
          properties: {
            query: {
              type: "string",
              description: "Search query to find in the documentation",
            },
            limit: {
              type: "number",
              description: "Maximum number of results to return (default: 5)",
            },
          },
          required: ["query"],
        },
      },
      {
        name: "read_doc",
        description:
          "Read the full content of a specific documentation page by its path.",
        inputSchema: {
          type: "object",
          properties: {
            path: {
              type: "string",
              description:
                "The documentation path (e.g., 'plugin-development/commands', 'getting-started')",
            },
          },
          required: ["path"],
        },
      },
      {
        name: "list_docs",
        description:
          "List all available documentation pages with their titles and paths.",
        inputSchema: {
          type: "object",
          properties: {
            category: {
              type: "string",
              description:
                "Optional category to filter by (e.g., 'plugin-development', 'server-setup')",
            },
          },
        },
      },
    ],
  };
});

// Handle tool calls
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  switch (name) {
    case "search_docs": {
      const docs = await loadDocs();
      const query = args.query.toLowerCase();
      const limit = args.limit || 5;

      // Search through docs
      const results = [];
      for (const doc of docs) {
        const content = doc.content.toLowerCase();
        const title = doc.title.toLowerCase();

        if (content.includes(query) || title.includes(query)) {
          // Find matching context
          const lines = doc.content.split("\n");
          const matchingLines = [];

          for (let i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().includes(query)) {
              // Get context: 2 lines before and after
              const start = Math.max(0, i - 2);
              const end = Math.min(lines.length, i + 3);
              const context = lines.slice(start, end).join("\n");
              matchingLines.push({
                line: i + 1,
                context,
              });
            }
          }

          results.push({
            path: doc.path,
            title: doc.title,
            matches: matchingLines.slice(0, 3), // Max 3 matches per doc
          });
        }
      }

      // Sort by number of matches and limit
      results.sort((a, b) => b.matches.length - a.matches.length);
      const limited = results.slice(0, limit);

      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(limited, null, 2),
          },
        ],
      };
    }

    case "read_doc": {
      const docs = await loadDocs();
      const requestedPath = args.path.replace(/^\//, "").replace(/\/$/, "");

      const doc = docs.find((d) => {
        const docPath = d.path.replace(/^\//, "").replace(/\/$/, "");
        return (
          docPath === requestedPath ||
          d.file.replace(".mdx", "").replace("/index", "") === requestedPath
        );
      });

      if (!doc) {
        return {
          content: [
            {
              type: "text",
              text: `Documentation page not found: ${args.path}\n\nAvailable paths:\n${docs.map((d) => `- ${d.path}`).join("\n")}`,
            },
          ],
        };
      }

      return {
        content: [
          {
            type: "text",
            text: `# ${doc.title}\n\nPath: ${doc.path}\n\n${doc.content}`,
          },
        ],
      };
    }

    case "list_docs": {
      const docs = await loadDocs();
      let filtered = docs;

      if (args.category) {
        filtered = docs.filter(
          (d) =>
            d.path.startsWith(args.category) || d.file.startsWith(args.category)
        );
      }

      const list = filtered.map((d) => ({
        path: d.path,
        title: d.title,
      }));

      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(list, null, 2),
          },
        ],
      };
    }

    default:
      throw new Error(`Unknown tool: ${name}`);
  }
});

// List available resources (documentation pages)
server.setRequestHandler(ListResourcesRequestSchema, async () => {
  const docs = await loadDocs();

  return {
    resources: docs.map((doc) => ({
      uri: `hytale-docs://${doc.path || "index"}`,
      name: doc.title,
      description: `Hytale documentation: ${doc.title}`,
      mimeType: "text/markdown",
    })),
  };
});

// Read a specific resource
server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
  const { uri } = request.params;
  const docs = await loadDocs();

  const docPath = uri.replace("hytale-docs://", "");
  const doc = docs.find(
    (d) => d.path === docPath || d.path === `/${docPath}` || d.file === docPath
  );

  if (!doc) {
    throw new Error(`Resource not found: ${uri}`);
  }

  return {
    contents: [
      {
        uri,
        mimeType: "text/markdown",
        text: doc.content,
      },
    ],
  };
});

// Start the server
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("Hytale Docs MCP server running on stdio");
}

main().catch(console.error);
