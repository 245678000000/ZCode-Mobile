# ZCode Remote page analysis (v0.2)

This is an unofficial, best-effort reading of the **visible Remote Control UI**, based on public ZCode docs and generic agent-console patterns.

No private protocol, cookie, token, or encrypted channel is inspected.

Live DOM was not available in this workspace. Selectors below are **heuristic** and must degrade to Unknown + WebView fallback when they miss.

## 1. Main structure (from public docs)

Remote Control opens the current ZCode **desktop window** on the phone.

Documented screens:

1. **Task home** — workspaces and tasks in this window.
2. **Session view** — one task: agent replies, execution steps, composer.

Documented chrome:

- Connection state at the top
- Workspace / timeline grouping
- Task run status on the right of each row
- Session header: task title + project
- Expandable “已工作” / work log for steps
- Bottom composer
- Copy / feedback / fork on replies
- Safety confirmation overlay when a tool/command/file change is gated

## 2. Task region — likely nodes

Stable-ish (prefer these):

- `[data-testid*="task"]`
- `[data-testid*="session"]`
- `[role="list"]` / `[role="listitem"]`
- `[aria-label*="task"]` / `[aria-label*="任务"]`

Unstable:

- Hashed CSS modules (`._abc123`)
- `:nth-child`
- React emotion/random class names

Heuristic fallback: a visible row that contains both a title-like string and a status word (`运行中` / `Running` / `已完成` / `失败`).

## 3. Session title

Likely:

- `document.title`
- `h1`, `h2`
- `[data-testid*="title"]`
- `[data-testid*="session-title"]`

Docs say the session header shows **current task and its project**.

## 4. Agent status

Docs: each task shows **run status**; sidebar can show **waiting for confirmation**.

Visible text we treat as status (not as official enums):

| Visible text | Mapped status |
| --- | --- |
| 运行中, Running, In progress, Working, 正在执行 | RUNNING |
| 等待确认, Waiting, Needs confirmation, 待确认 | WAITING_APPROVAL |
| 已完成, Completed, Done | COMPLETED |
| 失败, Failed, Error | FAILED |
| 已取消, Cancelled | CANCELLED |
| 排队, Queued | QUEUED |

If none match: `UNKNOWN`. Never invent a percentage.

## 5. Running / Completed / Failed

Yes — the public UI copy includes running / unread / failed dots and completed language. Detection is **text + structure**, not a private API.

## 6. Approval / Allow / Reject / Confirm

Public Safety Confirmation docs:

- Trigger: permission-gated request; task pauses; composer blocked
- Shows exact command / file change / tool action
- Decisions: **Allow**, **Always Allow**, reject
- Chinese: **允许**, **始终允许**, **拒绝**

Detection rule (two signals minimum):

1. Dialog / alertdialog / aria-modal / permission card
2. Allow-like **and** reject-like button, **or** waiting-for-confirmation status, **or** a command/tool block

A lone “允许” in the page is **not** an approval.

## 7. Artifact / files

Not fully specified in Remote docs. Heuristics:

- Links whose path ends in `.md .html .png .jpg .pdf .json .kt .ts .py …`
- `[data-testid*="artifact"]`, `[data-testid*="file"]`, `[download]`
- Images in the session transcript

If the URL is session-bound, preview inside WebView. Do not force an external download.

## 8. Todo / Steps / sub-agent

Docs mention expandable work steps (“已工作”) and idle-time **foreground subagents**.

v0.2 maps steps to `currentStep` / event log when visible. Sub-agent trees are **not** claimed.

## 9. React redraws

Very likely a React SPA. Expect frequent MutationObserver noise.

Mitigation: 500ms debounce, snapshot diff, event dedupe.

## 10. Selectors that should be relatively stable

- `data-testid`
- `aria-label` / `role` / `aria-modal`
- Semantic button text from the published confirmation UI
- Status words from the published task list

## 11. Selectors that will break

- CSS hashes
- nth-child
- Pixel-position hacks
- Hard-coded English-only class names

## 12. aria / role / data-*

Yes — these are the primary strategy. See `assets/zcode-selectors.json`.

## 13. URL vs session/task

Unknown whether the Remote URL path changes per task. Observer records `location.href` (redacted) and treats path changes as a possible session switch. Query/hash tokens are stripped before logging.

## 14. Observable event sources (safe)

Allowed:

- MutationObserver on visible DOM
- `document.title` / location (redacted)
- `role=dialog` appearance
- Page Notification permission usage (title/body only if the page itself shows them)
- WebSocket **open/close/error counts** (never payload)
- Resource timing failure counts (never auth URLs)

Forbidden:

- Cookie / Authorization / token capture
- fetch/XHR body interception
- WebSocket message contents

## Compatibility rule

If this analysis is wrong for a future ZCode build:

- Observer returns Unknown
- WebView still works
- Native task/approval UI simply stays empty
