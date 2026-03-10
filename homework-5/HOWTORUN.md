# HOWTORUN — Homework 5: MCP Servers Setup Guide

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21+ | Required for the custom MCP server |
| Maven | 3.9+ | Build tool for the custom MCP server |
| Node.js / npm | 18+ | Required for GitHub and Filesystem MCP servers (via `npx`) |
| VS Code | Latest | With GitHub Copilot extension |

---

## MCP Configuration File

All MCP servers are registered in `.vscode/mcp.json` at the workspace root. GitHub Copilot in VS Code reads this file automatically on startup.

```json
{
  "servers": {
    "github": {
      "type": "http",
      "url": "https://api.githubcopilot.com/mcp/"
    },
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/directory"]
    },
    "custom-lorem-ipsum": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/custom-mcp-server/target/custom-mcp-server-1.0.0.jar"
      ]
    }
  }
}
```

Update paths to match your machine before running.

---

## Task 1: GitHub MCP

### How it works
The GitHub MCP server is provided by GitHub as a hosted HTTP endpoint. Authentication is handled automatically via your logged-in GitHub Copilot session in VS Code — no token setup required.

### Configuration
The entry in `.vscode/mcp.json` is:
```json
"github": {
  "type": "http",
  "url": "https://api.githubcopilot.com/mcp/"
}
```

### Connect & Use
1. Ensure you are signed into GitHub Copilot in VS Code.
2. Open the Copilot Chat panel.
3. The GitHub MCP server starts automatically — no manual steps needed.
4. Example prompts:
   - *"List my recent pull requests"*
   - *"Summarize the last 5 commits on my repo"*
   - *"Create an issue titled 'Fix login bug' in my repo"*

---

## Task 2: Filesystem MCP

### How it works
The Filesystem MCP server exposes a local directory to Copilot. It runs via `npx` (no global install required) and communicates over STDIO.

### Configuration
```json
"filesystem": {
  "command": "npx",
  "args": ["-y", "@modelcontextprotocol/server-filesystem", "/Users/admin/AI-Coding-Partner-Homework"]
}
```
Change the path argument to any directory you want to expose.

### Connect & Use
1. VS Code launches the server automatically when Copilot starts.
2. Node.js / `npx` must be on your `PATH`.
3. Example prompts in Copilot Chat:
   - *"List all files in the project directory"*
   - *"Read the contents of README.md"*
   - *"Summarize the directory structure"*

---

## Task 3: Jira MCP

### How it works
The Jira MCP server connects to your Atlassian Jira instance. It requires your Jira URL and a personal API token.

### Configuration
In global VS Code configurations file

### Connect & Use
1. Add the config above, then reload VS Code (`Cmd+Shift+P` → *Reload Window*).
2. Example prompts:
   - *"Give me the last 5 bug tickets in project KEY"*
   - *"Show open issues assigned to me"*

---

## Task 4: Custom MCP Server

### Overview
The custom server is built with **Spring Boot 3** and **Spring AI MCP Server** (`spring-ai-starter-mcp-server`). It runs as a STDIO process and exposes:

- **Tool `read`** — callable action: returns N words from `lorem-ipsum.md` (default: 30).
- **Resource `lorem-ipsum://content`** — readable URI: same content, accessible via `resources/read`.

> **Resources vs Tools**
> - **Resources** are read-only URIs the client can list and read, similar to files or API endpoints. The client calls `resources/list` then `resources/read`.
> - **Tools** are callable actions with typed parameters, similar to functions. The client calls `tools/call` and receives a structured result. Tools can perform side effects or return data.

### 1. Build

```bash
cd custom-mcp-server
mvn clean package -q
```

This produces `target/custom-mcp-server-1.0.0.jar`.

### 2. Run Standalone (for testing)

```bash
java -jar target/custom-mcp-server-1.0.0.jar
```

The server reads JSON-RPC messages from stdin and writes responses to stdout. Press `Ctrl+C` to stop.

To use a custom lorem ipsum file (optional):

```bash
LOREM_IPSUM_PATH=/path/to/your/lorem-ipsum.md java -jar target/custom-mcp-server-1.0.0.jar
```

### 3. Connect to Copilot (VS Code)

Add the following entry to `.vscode/mcp.json`, using the **absolute path** to the JAR:

```json
"custom-lorem-ipsum": {
  "type": "stdio",
  "command": "java",
  "args": [
    "-jar",
    "/Users/admin/AI-Coding-Partner-Homework/homework-5/custom-mcp-server/target/custom-mcp-server-1.0.0.jar"
  ]
}
```

Reload VS Code (`Cmd+Shift+P` → *Reload Window*). Copilot will launch the server process automatically via STDIO.

### 4. Use the `read` Tool

In Copilot Chat, invoke the tool directly:

| Prompt | Result |
|--------|--------|
| *"Use the read tool to get 30 words of lorem ipsum"* | Returns first 30 words |
| *"Call read with wordCount 50"* | Returns first 50 words |
| *"Read 10 words from the lorem ipsum server"* | Returns first 10 words |

The tool signature:
```
Tool name:  read
Parameter:  wordCount (integer, optional, default: 30)
Returns:    String — the first N words from lorem-ipsum.md
```

### 5. Verify the Server Started

Check that Copilot recognised the server by opening the MCP output channel:
`Cmd+Shift+P` → *Output: Focus on Output* → select **MCP** from the dropdown.

You should see the server listed as connected with tools `read` available.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `java` not found | Ensure Java 21 is installed and on `PATH` (`java -version`) |
| `npx` not found | Install Node.js from https://nodejs.org |
| JAR path wrong | Use the absolute path; run `pwd` inside `custom-mcp-server/target/` |
| Server not appearing in Copilot | Reload VS Code window after editing `.vscode/mcp.json` |
| Jira auth fails | Regenerate the API token; check `JIRA_EMAIL` matches your Atlassian account |
