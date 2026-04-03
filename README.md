# Claude

A project for exploring and building with [Claude Code](https://claude.ai/code) — Anthropic's AI-powered coding assistant.

## System Architecture

```mermaid
graph TB
    subgraph User["Developer Environment"]
        CLI["Claude Code CLI"]
        IDE["IDE Extensions<br/>(VS Code / JetBrains)"]
        Web["Web App<br/>(claude.ai/code)"]
    end

    subgraph Core["Claude Code Core"]
        Agent["AI Agent"]
        Tools["Tool Executor"]
        Context["Context Manager"]
    end

    subgraph Workspace["Project Workspace"]
        FS["File System<br/>(Read / Write / Edit)"]
        Shell["Shell<br/>(Bash Commands)"]
        Git["Git<br/>(Version Control)"]
    end

    subgraph External["External Services"]
        API["Claude API<br/>(Anthropic)"]
        GH["GitHub<br/>(PRs / Issues)"]
        MCP["MCP Servers<br/>(Extensibility)"]
    end

    CLI --> Agent
    IDE --> Agent
    Web --> Agent

    Agent --> Tools
    Agent --> Context
    Agent <--> API

    Tools --> FS
    Tools --> Shell
    Tools --> Git
    Tools --> GH
    Tools --> MCP

    Context --> FS
```

## Project Structure

```
claude/
├── CLAUDE.md        # Instructions for Claude Code instances
└── README.md        # Project documentation (this file)
```

> As the project grows, new directories and files will be added here.

## Setup

### Prerequisites

- [Git](https://git-scm.com/) installed
- A [GitHub](https://github.com/) account
- [Claude Code](https://claude.ai/code) access (CLI, IDE extension, or web)

### Getting Started

1. **Clone the repository**

   ```bash
   git clone https://github.com/prateek20s/Claude.git
   cd Claude
   ```

2. **Start Claude Code**

   Using the CLI:
   ```bash
   claude
   ```

   Or open the project in your IDE with the Claude Code extension, or use the [web app](https://claude.ai/code).

3. **Start building**

   Claude Code will read `CLAUDE.md` for project context automatically. Ask it to scaffold features, write code, run tests, or manage git workflows.

## Usage

| Action | Command |
|---|---|
| Start Claude Code CLI | `claude` |
| Run a one-shot prompt | `claude -p "your prompt"` |
| Resume last session | `claude --continue` |
| Get help | `/help` inside a session |

## Contributing

1. Create a feature branch from `main`
2. Make your changes
3. Push and open a pull request

## License

This project is private. All rights reserved.
