# Devin MCP vs Devin API: Comprehensive Guide

## Table of Contents

- [1. Overview](#1-overview)
- [2. Devin MCP vs Devin API -- What's the Difference?](#2-devin-mcp-vs-devin-api----whats-the-difference)
- [3. Does Devin MCP Utilize the Devin API?](#3-does-devin-mcp-utilize-the-devin-api)
- [4. All Available Devin MCP Tools](#4-all-available-devin-mcp-tools)
- [5. All Devin REST API Endpoints](#5-all-devin-rest-api-endpoints)

---

## 1. Overview

Cognition AI provides **two primary interfaces** for programmatic interaction with Devin:

| | **Devin MCP** | **Devin REST API** |
|---|---|---|
| **Protocol** | Model Context Protocol (MCP) -- a JSON-RPC-based agent-to-tool protocol | Standard REST/HTTP with JSON bodies |
| **Primary Consumer** | LLM agents (Claude, GPT, Devin itself, Devin CLI, Devin Desktop/Windsurf) | Any HTTP client (scripts, CI/CD, custom apps, dashboards) |
| **Auth** | Devin API key (`cog_` prefix) configured in the MCP client | Bearer token in `Authorization` header (`cog_` prefix) |
| **Base URL** | Varies by client (configured as an MCP server endpoint) | `https://api.devin.ai` |
| **Interaction Style** | Tool calls -- the agent discovers tools, then invokes them by name with JSON args | Standard HTTP verbs: `GET`, `POST`, `PUT`, `PATCH`, `DELETE` |
| **Best For** | Agent-to-agent workflows, IDE integrations, codebase Q&A, orchestrating child sessions from within a Devin session | External automation, CI/CD pipelines, dashboards, webhooks, batch scripting |

---

## 2. Devin MCP vs Devin API -- What's the Difference?

### Devin REST API

The **Devin REST API** (`https://api.devin.ai`) is a traditional HTTP API. You call endpoints with `curl`, Python `requests`, or any HTTP library. It exposes CRUD operations on sessions, knowledge, playbooks, secrets, schedules, PR reviews, users, and more.

**Key characteristics:**
- Versioned endpoints: `/v1/...` (legacy), `/v2/...`, `/v3/...` (current), `/v3beta1/...` (beta)
- Enterprise-scoped endpoints (`/v3/enterprise/...`) and organization-scoped endpoints (`/v3/organizations/{org_id}/...`)
- Authentication via `Authorization: Bearer <API_KEY>` header
- Returns standard JSON responses with HTTP status codes

### Devin MCP (Model Context Protocol)

The **Devin MCP server** is an MCP-compliant server that wraps Devin's capabilities into **tools** that LLM agents can discover and invoke. It follows the [Model Context Protocol](https://modelcontextprotocol.io/) specification (JSON-RPC over stdio, SSE, or HTTP transport).

**Key characteristics:**
- Agents call `list_tools` to discover available tools, then `call_tool` with JSON arguments
- Tools are higher-level abstractions (e.g., `devin_session_create`, `ask_question`, `devin_knowledge_manage`) that bundle multiple API operations into single tool calls
- Supports both **public mode** (DeepWiki -- read-only codebase exploration for any public repo) and **private mode** (full session management, knowledge, playbooks, etc. via `devin.ai` endpoints)
- Configurable in MCP clients like Claude Desktop, Cursor, Devin CLI, or Devin Desktop/Windsurf

### When to Use Which

| Scenario | Use |
|---|---|
| Building a CI/CD pipeline that creates Devin sessions | **REST API** |
| Letting an LLM agent (Claude, GPT) manage Devin sessions | **MCP** |
| Querying code documentation from within an IDE agent | **MCP** (DeepWiki tools) |
| Building a custom dashboard showing session metrics | **REST API** |
| Orchestrating child Devin sessions from a parent Devin session | **MCP** (built-in) |
| Automating bulk session creation from a script | **REST API** |
| Asking AI-powered questions about a codebase | **MCP** (`ask_question` tool) |

---

## 3. Does Devin MCP Utilize the Devin API?

**Yes.** The Devin MCP server is a **wrapper layer on top of the Devin REST API**. Evidence for this:

1. **The MCP server requires the same API key** -- it uses `cog_`-prefixed service user keys or enterprise keys, which are Devin REST API credentials.

2. **MCP tool descriptions explicitly reference the REST API** -- For example, `devin_session_create` is described as: *"Create one or more child Devin sessions **via the v3 REST API**."*

3. **Authentication table is identical** -- The MCP docs state the server supports *"the same authentication methods as the Devin API"*, including org-scoped service user keys, enterprise service user keys, and personal access tokens.

4. **MCP tools map to API endpoints** -- Each MCP tool corresponds to one or more REST API endpoints:
   - `devin_session_create` -> `POST /v3/organizations/{org_id}/sessions`
   - `devin_session_interact(action="get")` -> `GET /v3/organizations/{org_id}/sessions/{devin_id}`
   - `devin_session_interact(action="message")` -> `POST /v3/organizations/{org_id}/sessions/{devin_id}/messages`
   - `devin_knowledge_manage(action="list")` -> `GET /v3/organizations/{org_id}/knowledge`
   - `devin_playbook_manage(action="create")` -> `POST /v3/organizations/{org_id}/playbooks`
   - etc.

5. **The MCP server acts as a translation layer** -- It translates MCP JSON-RPC tool calls into the appropriate REST API HTTP requests, handling pagination, cursor-based iteration, and response formatting for agent consumption.

**Architecture:**

```
LLM Agent (Claude / GPT / Devin)
        |
        | JSON-RPC tool calls
        v
  Devin MCP Server
        |
        | HTTP REST calls (using cog_ API key)
        v
  Devin REST API (https://api.devin.ai)
        |
        v
  Devin Platform (sessions, VMs, repos, etc.)
```

---

## 4. All Available Devin MCP Tools

The Devin MCP server exposes **15 tools** across five categories. Below is every tool with its parameters and description.

### 4.1 Codebase Exploration (DeepWiki)

These tools work in both public and private modes.

---

#### `read_wiki_structure`

> Get a list of documentation topics for a GitHub repository.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `repoName` | string | Yes | GitHub repository in `owner/repo` format (e.g., `"facebook/react"`) |

---

#### `read_wiki_contents`

> View documentation about a GitHub repository.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `repoName` | string | Yes | GitHub repository in `owner/repo` format |

---

#### `ask_question`

> Ask any question about a GitHub repository and get an AI-powered, context-grounded response.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `repoName` | string or string[] | Yes | Repository or list of repositories (max 10) in `owner/repo` format |
| `question` | string | Yes | The question to ask about the repository |

---

#### `list_available_repos`

> List all repositories available to query with your Devin account.

| Parameter | Type | Required | Description |
|---|---|---|---|
| *(none)* | -- | -- | No parameters required |

*Private mode only.*

---

#### `generate_wiki`

> Generate a codebase wiki for a repository. Triggers wiki generation and waits for completion.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `repoName` | string | Yes | Repository in `owner/repo` format |

*Private mode only. Only use when explicitly requested.*

---

### 4.2 Session Management

All tools in this section require private mode (authenticated with a Devin API key).

---

#### `devin_session_create`

> Create one or more child Devin sessions via the v3 REST API.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessions` | array of objects | Yes | Array of session specs. Each object has: `prompt` (string, required), `title` (string, optional), `playbook_id` (string, optional), `tags` (string[], optional), `repos` (string[], optional), `platform` (string, optional), `structured_output_schema` (object, optional) |
| `structured_output_schema` | object or null | No | JSON Schema (Draft 7) applied to all sessions without their own |
| `repos` | string[] or null | No | Restrict ALL sessions to these repos (`"owner/repo"` format) |
| `platform` | string or null | No | VM platform for all sessions (e.g., `"linux"`, `"windows"`) |
| `devin_mode` | string or null | No | Agent mode: `"normal"`, `"fast"`, `"lite"`, or `"ultra"` |

---

#### `devin_session_interact`

> Consolidated tool for all interactions with a single Devin session.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `action` | enum | Yes | `"get"`, `"message"`, `"sleep"`, `"terminate"`, `"archive"`, `"get_messages"`, `"get_attachments"`, `"set_tags"` |
| `session_id` | string | Yes | Target session ID (must include `"devin-"` prefix) |
| `message` | string or null | No | Message text (required when action is `"message"`) |
| `tags` | string[] or null | No | Tag list (required when action is `"set_tags"`); replaces all existing tags |
| `archive_on_terminate` | boolean | No | Also archive when terminating (default: `false`) |
| `first` | integer or null | No | Page size for `get_messages` pagination |
| `after` | string or null | No | Opaque cursor for `get_messages` pagination |

---

#### `devin_session_search`

> Search and filter Devin sessions. All filters are optional and combined with AND.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `session_ids` | string[] or null | No | Filter to specific session IDs (must include `"devin-"` prefix) |
| `tags` | string[] or null | No | Filter by tags |
| `playbook_id` | string or null | No | Filter by playbook |
| `origins` | string[] or null | No | Filter by origin: `"webapp"`, `"slack"`, `"teams"`, `"api"`, `"linear"`, `"jira"`, `"scheduled"`, `"cli"`, `"other"` |
| `schedule_id` | string or null | No | Filter by schedule |
| `created_after` | integer or null | No | Unix timestamp lower bound for creation time |
| `created_before` | integer or null | No | Unix timestamp upper bound for creation time |
| `updated_after` | integer or null | No | Unix timestamp lower bound for update time |
| `updated_before` | integer or null | No | Unix timestamp upper bound for update time |
| `user_ids` | string[] or null | No | Filter by user |
| `first` | integer or null | No | Results per page (default 20, max 100) |
| `after` | string or null | No | Opaque cursor for pagination |

---

#### `devin_session_gather`

> Wait for multiple Devin sessions to reach a settled state before returning.

A session is "settled" when: status is `"exit"`, `"error"`, `"suspended"`, or `"running"` with `status_detail` of `"finished"`, `"waiting_for_user"`, or `"waiting_for_approval"`.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `session_ids` | string[] | Yes | Session IDs to wait on (must include `"devin-"` prefix; max 50) |
| `timeout_seconds` | integer | No | Max seconds to wait (default 300, max 600) |
| `poll_interval_seconds` | integer | No | Seconds between status checks (default 15, min 5) |

---

#### `devin_session_events`

> Inspect events within a Devin session -- list summaries, fetch full details, or search.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `action` | enum | Yes | `"list"`, `"details"`, or `"search"` |
| `session_id` | string | Yes | Target session ID (must include `"devin-"` prefix) |
| `event_types` | string[] or null | No | Filter by event type (list/search only) |
| `categories` | string[] or null | No | Filter by category: `shell`, `file`, `search`, `browser`, `mcp`, `git`, `message`, `status`, `secret`, `todo`, `recording`, `knowledge`, `playbook`, `webhook`, `lifecycle`, `other` |
| `direction` | string or null | No | `"incoming"` or `"outgoing"` (list/search only) |
| `created_after` | integer or null | No | Unix timestamp lower bound |
| `created_before` | integer or null | No | Unix timestamp upper bound |
| `first` | integer or null | No | Page size (default 50, max 100) |
| `after` | string or null | No | Opaque cursor for pagination |
| `event_ids` | string[] or null | No | Event IDs to fetch (details only, max 20) |
| `offset` | integer or null | No | 0-based offset into timeline (details only) |
| `limit` | integer or null | No | Number of events with offset (details only, default 20, max 50) |
| `max_content_length` | integer or null | No | Max chars per content field (default 10000) |
| `query` | string or null | No | Search text (search action only, required) |

---

### 4.3 Knowledge Management

---

#### `devin_knowledge_manage`

> Manage Devin knowledge notes and suggestions via a single action-based tool.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `action` | enum | Yes | `"list"`, `"get"`, `"create"`, `"update"`, `"delete"`, `"folders"`, `"list_suggestions"`, `"view_suggestion"`, `"dismiss_suggestions"` |
| `note_id` | string or null | No | Note ID (required for get, update, delete) |
| `name` | string or null | No | Note name (required for create, update) |
| `content` | string or null | No | Note content (required for create, update) |
| `trigger` | string or null | No | Trigger description (required for create, update) |
| `pinned_repo` | string or null | No | Pin to a specific repo (`owner/repo` format) |
| `search` | string or null | No | Search query for `"list"` (case-insensitive substring match) |
| `folder_path` | string or null | No | Filter to notes in a specific folder |
| `first` | integer or null | No | Page size for pagination (default 100, max 200) |
| `after` | string or null | No | Opaque cursor for pagination |
| `event_id` | string or null | No | Event ID (required for `view_suggestion`) |
| `event_ids` | string[] or null | No | Event IDs to dismiss (required for `dismiss_suggestions`) |
| `status` | string or null | No | Filter suggestions: `"pending"`, `"accepted"`, `"rejected"` |
| `query` | string or null | No | Search query for `list_suggestions` |
| `since_days` | integer or null | No | Only suggestions from last N days |
| `limit` | integer or null | No | Page size for `list_suggestions` (default 20, max 100) |
| `offset` | integer or null | No | Pagination offset for `list_suggestions` |

---

### 4.4 Playbook & Schedule & Automation Management

---

#### `devin_playbook_manage`

> Manage Devin playbooks with a single action-based tool.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `action` | enum | Yes | `"list"`, `"get"`, `"create"`, `"update"`, `"delete"` |
| `playbook_id` | string or null | No | Playbook ID (required for get, update, delete) |
| `title` | string or null | No | Playbook title (required for create, update) |
| `content` | string or null | No | Playbook markdown content (required for create, update) |
| `macro` | string or null | No | Automation macro (must start with `!`, e.g., `"!my_macro"`) |
| `first` | integer or null | No | Page size for `"list"` pagination (default 100, max 200) |
| `after` | string or null | No | Opaque cursor for pagination |

---

#### `devin_schedule_manage`

> Manage scheduled Devin sessions: list, get, create, update, or delete.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `action` | enum | Yes | `"list"`, `"get"`, `"create"`, `"update"`, `"delete"` |
| `schedule_id` | string or null | No | Schedule ID (required for get, update, delete) |
| `name` | string or null | No | Human-readable name (required for create) |
| `prompt` | string or null | No | Prompt Devin will run (required for create) |
| `playbook_id` | string or null | No | Playbook to attach |
| `frequency` | string or null | No | Cron expression (e.g., `"0 9 * * 1-5"`) |
| `schedule_type` | enum or null | No | `"recurring"` (default) or `"one_time"` |
| `scheduled_at` | string or null | No | ISO 8601 datetime for one-time schedules |
| `enabled` | boolean or null | No | Enable or disable the schedule |
| `notify_on` | enum or null | No | `"always"`, `"failure"`, or `"never"` |
| `agent` | enum or null | No | `"devin"`, `"data_analyst"`, or `"advanced"` |
| `bypass_approval` | boolean or null | No | Skip MCP tool permission checks |
| `target_devin_id` | string or null | No | Send prompt to existing session (one-time only) |
| `platform` | string or null | No | VM platform (e.g., `"windows"`) |
| `limit` | integer or null | No | Max results for list (capped at 100) |
| `offset` | integer or null | No | Pagination offset |

---

#### `devin_automation_manage`

> Manage Devin automations: list, get, schemas, create, update, or delete. Automations run Devin in response to events (GitHub activity, Slack messages, Linear updates, schedules, incoming webhooks).

| Parameter | Type | Required | Description |
|---|---|---|---|
| `action` | enum | Yes | `"list"`, `"get"`, `"schemas"`, `"create"`, `"update"`, `"delete"` |
| `automation_id` | string or null | No | Automation ID (required for get, update, delete) |
| `name` | string or null | No | Human-readable name, 1-500 chars (required for create) |
| `triggers` | array or null | No | Trigger objects (required for create). Use `action="schemas"` for supported event types |
| `actions` | array or null | No | Action objects (required for create). Types: `start_session`, `message_session`, `monitor_session`, `notify` |
| `enabled` | boolean or null | No | Enable or disable |
| `max_acu_limit` | integer or null | No | Max ACUs per triggered session (1-1000) |
| `invocation_limit` | integer or null | No | Max invocations per window (rate limit) |
| `invocation_limit_window_seconds` | integer or null | No | Rate limit window in seconds (>= 60) |
| `recommended_mcps` | string[] or null | No | MCP server names to enable for triggered sessions |
| `search` | string or null | No | Filter list results by name substring |
| `first` | integer or null | No | Page size (default 100, max 200) |
| `after` | string or null | No | Opaque cursor for pagination |

---

### 4.5 Integration Management

---

#### `list_integrations`

> List all native integrations and MCP servers for the organization, with status and settings/install URLs.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `filter` | enum | No | `"all"` (default), `"installed"`, or `"not_installed"` |

---

## 5. All Devin REST API Endpoints

**Base URL:** `https://api.devin.ai`

**Authentication:** All requests require `Authorization: Bearer <API_KEY>` header. API keys start with `cog_`.

The API has three scoping patterns:
- **v1 endpoints** (legacy): `/v1/...` -- personal or service API keys
- **Enterprise-scoped** (v3): `/v3/enterprise/...` -- enterprise-wide operations
- **Organization-scoped** (v3): `/v3/organizations/{org_id}/...` -- org-specific operations

> Note: Many v3 endpoints exist in both enterprise and organization-scoped variants. The examples below show the organization-scoped variant where applicable.

---

### 5.1 Sessions

#### Create Session

```
POST /v1/sessions                              (v1 legacy)
POST /v3/organizations/{org_id}/sessions       (v3 org-scoped)
POST /v3/enterprise/sessions                   (v3 enterprise-scoped)
```

**Parameters (v3):**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `prompt` | string | Yes | The task description for Devin |
| `title` | string | No | Custom title for the session |
| `playbook_id` | string | No | Playbook to attach |
| `tags` | string[] | No | Tags to add to the session |
| `repos` | string[] | No | Restrict session to these repos |
| `knowledge_ids` | string[] | No | Knowledge IDs to use (null = all, [] = none) |
| `secret_ids` | string[] | No | Secret IDs to use (null = all, [] = none) |
| `session_secrets` | object[] | No | Session-specific secrets (key, value, sensitive) |
| `max_acu_limit` | integer | No | Max ACU limit |
| `structured_output_schema` | object | No | JSON Schema for structured output |
| `structured_output_required` | boolean | No | Whether structured output is mandatory |
| `devin_mode` | enum | No | `"normal"`, `"fast"`, `"lite"`, `"ultra"` |
| `platform` | string | No | VM platform override (e.g., `"windows"`) |
| `create_as_user_id` | string | No | Create session on behalf of a user |
| `attachment_urls` | string[] | No | URLs of files to attach |
| `bypass_approval` | boolean | No | Skip MCP tool permission checks |

**Example:**

```bash
curl -X POST "https://api.devin.ai/v3/organizations/$ORG_ID/sessions" \
  -H "Authorization: Bearer $DEVIN_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Fix the login bug in issue #42",
    "tags": ["bugfix", "auth"],
    "repos": ["myorg/myrepo"]
  }'
```

**Response (200):**

```json
{
  "session_id": "devin-abc123...",
  "url": "https://app.devin.ai/sessions/devin-abc123...",
  "acus_consumed": 0,
  "created_at": 1718784000,
  "org_id": "org-...",
  "status": "new",
  "tags": ["bugfix", "auth"],
  "pull_requests": [],
  "updated_at": 1718784000
}
```

---

#### List Sessions

```
GET /v1/sessions                                (v1 legacy)
GET /v3/organizations/{org_id}/sessions         (v3 org-scoped)
GET /v3/enterprise/sessions                     (v3 enterprise-scoped)
```

**Query Parameters (v1):**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `limit` | integer | 100 | Page size |
| `offset` | integer | 0 | Pagination offset |
| `tags` | string[] | null | Filter by tags |
| `user_email` | string | null | Filter by creator email |

**Example:**

```bash
curl "https://api.devin.ai/v1/sessions?limit=10&offset=0" \
  -H "Authorization: Bearer $DEVIN_API_KEY"
```

---

#### Get Session Details

```
GET /v1/sessions/{session_id}                                        (v1 legacy)
GET /v3/organizations/{org_id}/sessions/{devin_id}                   (v3 org-scoped)
GET /v3/enterprise/sessions/{devin_id}                               (v3 enterprise-scoped)
```

**Path Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `org_id` | string | Yes (v3 org) | Organization ID (prefix: `org-`) |
| `devin_id` / `session_id` | string | Yes | Session ID (v3 prefix: `devin-`) |

**Example:**

```bash
curl "https://api.devin.ai/v3/organizations/$ORG_ID/sessions/devin-abc123" \
  -H "Authorization: Bearer $DEVIN_API_KEY"
```

---

#### Send Message to Session

```
POST /v1/sessions/{session_id}/message                                    (v1 legacy)
POST /v3/organizations/{org_id}/sessions/{devin_id}/messages              (v3 org-scoped)
POST /v3/enterprise/sessions/{devin_id}/messages                          (v3 enterprise-scoped)
```

**Request Body:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `message` | string | Yes | Message text to send |
| `message_as_user_id` | string | No | Send as a specific user (v3 only) |

**Example:**

```bash
curl -X POST "https://api.devin.ai/v3/organizations/$ORG_ID/sessions/devin-abc123/messages" \
  -H "Authorization: Bearer $DEVIN_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"message": "Please also add unit tests for the new feature"}'
```

---

#### Terminate Session

```
DELETE /v1/sessions/{session_id}                                          (v1 legacy)
DELETE /v3/organizations/{org_id}/sessions/{devin_id}                     (v3 org-scoped)
DELETE /v3/enterprise/sessions/{devin_id}                                 (v3 enterprise-scoped)
```

**Example:**

```bash
curl -X DELETE "https://api.devin.ai/v1/sessions/session-abc123" \
  -H "Authorization: Bearer $DEVIN_API_KEY"
```

---

#### Archive Session

```
POST /v3/organizations/{org_id}/sessions/{devin_id}/archive              (v3 org-scoped)
POST /v3/enterprise/sessions/{devin_id}/archive                          (v3 enterprise-scoped)
```

**Example:**

```bash
curl -X POST "https://api.devin.ai/v3/organizations/$ORG_ID/sessions/devin-abc123/archive" \
  -H "Authorization: Bearer $DEVIN_API_KEY"
```

---

#### Update Session Tags

```
PUT /v1/sessions/{session_id}/tags                                        (v1 legacy)
PUT /v3/organizations/{org_id}/sessions/{devin_id}/tags                   (v3 org-scoped)
PUT /v3/enterprise/sessions/{devin_id}/tags                               (v3 enterprise-scoped)
```

**Request Body:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `tags` | string[] | Yes | New tag list (replaces existing tags) |

**Example:**

```bash
curl -X PUT "https://api.devin.ai/v1/sessions/session-abc123/tags" \
  -H "Authorization: Bearer $DEVIN_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"tags": ["bugfix", "priority-high"]}'
```

---

#### List Session Messages

```
GET /v3/organizations/{org_id}/sessions/{devin_id}/messages              (v3 org-scoped)
GET /v3/enterprise/sessions/{devin_id}/messages                          (v3 enterprise-scoped)
```

---

#### List Session Attachments

```
GET /v3/organizations/{org_id}/sessions/{devin_id}/attachments           (v3 org-scoped)
GET /v3/enterprise/sessions/{devin_id}/attachments                       (v3 enterprise-scoped)
```

---

#### Generate Session Insights

```
POST /v3/organizations/{org_id}/sessions/{devin_id}/insights/generate    (v3 org-scoped)
POST /v3/enterprise/sessions/{devin_id}/insights/generate                (v3 enterprise-scoped)
```

---

### 5.2 Attachments

#### Upload Attachment

```
POST /v1/attachments
```

> Upload files for Devin to work with during sessions. Supports various file types including code, data, and documentation files.

**Example:**

```bash
curl -X POST "https://api.devin.ai/v1/attachments" \
  -H "Authorization: Bearer $DEVIN_API_KEY" \
  -F "file=@/path/to/file.py"
```

---

#### Download Attachment

```
GET /v1/attachments/{attachment_id}
```

> Returns a 307 redirect to a presigned URL (valid for 60 seconds).

**Example:**

```bash
curl -L "https://api.devin.ai/v1/attachments/att-abc123" \
  -H "Authorization: Bearer $DEVIN_API_KEY" \
  -o downloaded_file.py
```

---

### 5.3 PR Reviews

#### Trigger Devin Review

```
POST /v3/enterprise/pr-reviews                                           (enterprise-scoped)
POST /v3/organizations/{org_id}/pr-reviews                               (org-scoped)
```

**Request Body:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `pr_url` | string | Yes | Full URL of the PR (e.g., `https://github.com/owner/repo/pull/123`) |

**Example:**

```bash
curl -X POST "https://api.devin.ai/v3/enterprise/pr-reviews" \
  -H "Authorization: Bearer $DEVIN_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"pr_url": "https://github.com/owner/repo/pull/123"}'
```

**Response (200):**

```json
{
  "commit_sha": "abc123def...",
  "created_at": "2026-06-19T07:00:00Z",
  "pr_number": 123,
  "repo_path": "github.com/owner/repo",
  "status": "pending"
}
```

---

#### Get Latest Devin Review Status

```
GET /v3/enterprise/pr-reviews                                            (enterprise-scoped)
GET /v3/organizations/{org_id}/pr-reviews                                (org-scoped)
```

**Query Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `pr_url` | string | Yes | Full URL of the PR |
| `commit_sha` | string | No | Commit SHA (defaults to current PR head) |

**Example:**

```bash
curl "https://api.devin.ai/v3/enterprise/pr-reviews?pr_url=https://github.com/owner/repo/pull/123" \
  -H "Authorization: Bearer $DEVIN_API_KEY"
```

---

### 5.4 API Keys

#### Revoke API Key

```
DELETE /v2/enterprise/api-keys/{api_key_id}
```

**Example:**

```bash
curl -X DELETE "https://api.devin.ai/v2/enterprise/api-keys/key-abc123" \
  -H "Authorization: Bearer $DEVIN_API_KEY"
```

---

### 5.5 Knowledge (v3)

```
GET    /v3/organizations/{org_id}/knowledge                              List knowledge notes
POST   /v3/organizations/{org_id}/knowledge                              Create knowledge note
GET    /v3/organizations/{org_id}/knowledge/{note_id}                    Get knowledge note
PUT    /v3/organizations/{org_id}/knowledge/{note_id}                    Update knowledge note
DELETE /v3/organizations/{org_id}/knowledge/{note_id}                    Delete knowledge note
```

Enterprise-scoped equivalents also exist under `/v3/enterprise/knowledge/...`

---

### 5.6 Playbooks (v3)

```
GET    /v3/organizations/{org_id}/playbooks                              List playbooks
POST   /v3/organizations/{org_id}/playbooks                              Create playbook
GET    /v3/organizations/{org_id}/playbooks/{playbook_id}                Get playbook
PUT    /v3/organizations/{org_id}/playbooks/{playbook_id}                Update playbook
DELETE /v3/organizations/{org_id}/playbooks/{playbook_id}                Delete playbook
```

Enterprise-scoped equivalents also exist under `/v3/enterprise/playbooks/...`

---

### 5.7 Secrets (v3)

```
GET    /v3/organizations/{org_id}/secrets                                List secrets
POST   /v3/organizations/{org_id}/secrets                                Create secret
GET    /v3/organizations/{org_id}/secrets/{secret_id}                    Get secret metadata
PUT    /v3/organizations/{org_id}/secrets/{secret_id}                    Update secret
DELETE /v3/organizations/{org_id}/secrets/{secret_id}                    Delete secret
```

---

### 5.8 Schedules (v3)

```
GET    /v3/organizations/{org_id}/schedules                              List schedules
POST   /v3/organizations/{org_id}/schedules                              Create schedule
GET    /v3/organizations/{org_id}/schedules/{schedule_id}                Get schedule
PUT    /v3/organizations/{org_id}/schedules/{schedule_id}                Update schedule
DELETE /v3/organizations/{org_id}/schedules/{schedule_id}                Delete schedule
```

---

### 5.9 Enterprise Administration (v3)

```
GET    /v3/enterprise/organizations                                      List organizations
GET    /v3/enterprise/organizations/{org_id}                             Get organization
GET    /v3/enterprise/users                                              List users
GET    /v3/enterprise/members                                            List members
GET    /v3/enterprise/roles                                              List roles
GET    /v3/enterprise/idp-groups                                         List IDP groups
```

---

### 5.10 Consumption & Billing (v3 Enterprise)

```
GET    /v3/enterprise/consumption/cycles                                 Get billing cycles
GET    /v3/enterprise/consumption/daily                                  Get daily breakdowns
GET    /v3/enterprise/acu-limits                                         Get ACU limits
PUT    /v3/enterprise/acu-limits                                         Update ACU limits
```

---

### 5.11 Metrics (v3 Enterprise)

```
GET    /v3/enterprise/metrics/dau                                        Daily active users
GET    /v3/enterprise/metrics/wau                                        Weekly active users
GET    /v3/enterprise/metrics/mau                                        Monthly active users
GET    /v3/enterprise/metrics/prs                                        Pull request metrics
GET    /v3/enterprise/metrics/sessions                                   Session metrics
GET    /v3/enterprise/metrics/searches                                   Search metrics
GET    /v3/enterprise/metrics/active-users                               Active user metrics
GET    /v3/enterprise/metrics/usage                                      Usage metrics
```

---

### 5.12 Git Connections & Permissions (v3 Enterprise)

```
GET    /v3/enterprise/git-providers/connections                          List git connections
GET    /v3/enterprise/git-providers/connections/{id}/repositories        List repos for a connection
GET    /v3/enterprise/git-providers/permissions                          List git permissions
PATCH  /v3/enterprise/organizations/{org_id}/git-providers/permissions/{id}   Update git permission
```

---

### 5.13 Audit Logs (v3)

```
GET    /v3/enterprise/audit-logs                                         Enterprise audit logs
GET    /v3/organizations/{org_id}/audit-logs                             Organization audit logs
```

---

### 5.14 IP Access List (v3 Enterprise)

```
GET    /v3/enterprise/ip-access-list                                     Get IP access list
PUT    /v3/enterprise/ip-access-list                                     Replace IP access list
POST   /v3/enterprise/ip-access-list                                     Add IPs to access list
DELETE /v3/enterprise/ip-access-list                                     Remove IPs from access list
```

---

### 5.15 Beta Endpoints (v3beta1)

```
GET    /v3beta1/enterprise/guardrail-violations                          Enterprise guardrail violations
GET    /v3beta1/enterprise/organizations/{org_id}/guardrail-violations   Org guardrail violations
POST   /v3beta1/enterprise/service-users                                 Provision service user
GET    /v3beta1/organizations/{org_id}/members/users                     List org members (beta)
GET    /v3beta1/organizations/{org_id}/members/users/{user_id}           Get org member (beta)
GET    /v3beta1/organizations/{org_id}/members/idp-users                 List org IDP users (beta)
```

---

### 5.16 Session Insights (v2)

```
GET    /v2/sessions/{org_id}                                             List org sessions
GET    /v2/sessions/{org_id}/insights                                    List org sessions with insights
GET    /v2/enterprise/sessions                                           List enterprise sessions
GET    /v2/enterprise/sessions/insights                                  List enterprise sessions with insights
```

---

## Quick Reference: MCP Tool to API Endpoint Mapping

| MCP Tool | Action | Corresponding REST API Endpoint |
|---|---|---|
| `devin_session_create` | -- | `POST /v3/organizations/{org_id}/sessions` |
| `devin_session_interact` | `get` | `GET /v3/organizations/{org_id}/sessions/{devin_id}` |
| `devin_session_interact` | `message` | `POST /v3/organizations/{org_id}/sessions/{devin_id}/messages` |
| `devin_session_interact` | `terminate` | `DELETE /v3/organizations/{org_id}/sessions/{devin_id}` |
| `devin_session_interact` | `archive` | `POST /v3/organizations/{org_id}/sessions/{devin_id}/archive` |
| `devin_session_interact` | `set_tags` | `PUT /v3/organizations/{org_id}/sessions/{devin_id}/tags` |
| `devin_session_interact` | `get_messages` | `GET /v3/organizations/{org_id}/sessions/{devin_id}/messages` |
| `devin_session_interact` | `get_attachments` | `GET /v3/organizations/{org_id}/sessions/{devin_id}/attachments` |
| `devin_session_search` | -- | `GET /v3/organizations/{org_id}/sessions` (with filters) |
| `devin_knowledge_manage` | `list/get/create/update/delete` | `GET/POST/PUT/DELETE /v3/organizations/{org_id}/knowledge/...` |
| `devin_playbook_manage` | `list/get/create/update/delete` | `GET/POST/PUT/DELETE /v3/organizations/{org_id}/playbooks/...` |
| `devin_schedule_manage` | `list/get/create/update/delete` | `GET/POST/PUT/DELETE /v3/organizations/{org_id}/schedules/...` |
| `list_integrations` | -- | Internal API (integration/MCP listing) |
| `read_wiki_structure` | -- | DeepWiki API |
| `read_wiki_contents` | -- | DeepWiki API |
| `ask_question` | -- | DeepWiki API |

---

## Further Reading

- [Devin API Docs](https://docs.devin.ai/api-reference/overview)
- [Devin API Authentication](https://docs.devin.ai/api-reference/authentication)
- [Devin MCP Docs](https://docs.devin.ai/work-with-devin/devin-mcp)
- [API v1 to v3 Migration Guide](https://docs.devin.ai/api-reference/getting-started/migration-guide)
- [API Release Notes](https://docs.devin.ai/api-reference/release-notes)
- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
