# AI Session Records

This directory holds genuine conversation exports from **Kiro IDE**, the AI
tool used to develop this project.

---

## Current status

**One session export is present** — see the Contents table below.

This is an unmodified export from Kiro. The session ran on
**12 October 2026** (created 13:05 UTC, last activity 16:12 UTC).

> **Note on session scope:** Kiro sessions undergo automatic context compaction
> when the conversation grows long. This export covers the later part of the
> continuous Kiro conversation — specifically the final implementation review,
> historical result corrections, test runs, and documentation updates. Earlier
> work (project planning, domain model, CSV I/O, and CLI implementation) took
> place in the same conversation but was compacted into context snapshots before
> this export window begins. Those snapshots are present in `messages.jsonl` as
> summary entries. The file contains 1109 lines in total; it does not contain
> the complete raw, turn-by-turn history of the full project.

---

## How to export a Kiro session

1. In Kiro, right-click the chat tab for the session you want to export.
2. Select **Export Chat**.
3. Choose a save location. Kiro saves a `.zip` file.
4. Extract the zip. It contains a directory named after the session ID:

   ```
   <session-id>/
     session.json        — metadata: model, timestamps, agent name
     messages.jsonl      — conversation log (one JSON record per line)
     sub-executions/     — sub-agent runs, if any (*.jsonl files)
   ```

5. Copy the extracted directory into this `ai/` folder.
6. Update the Contents table below and commit.

---

## Contents

| Path | Description |
|------|-------------|
| `sess_8de82a3f-7014-4352-bb32-84b54436361e/` | Kiro vibe session export — 12 October 2026 |
| `sess_8de82a3f-7014-4352-bb32-84b54436361e/session.json` | Session metadata: ID, title ("Football League Standings — Final Audit"), model (`auto`), workspace path, timestamps |
| `sess_8de82a3f-7014-4352-bb32-84b54436361e/messages.jsonl` | Conversation log — 1109 lines of JSONL |
| `sess_8de82a3f-7014-4352-bb32-84b54436361e/sub-executions/b5aa19fd-7d1d-4896-bc86-efe6359ce5ec.jsonl` | Sub-agent execution records |

---

## File format

`messages.jsonl` contains one JSON record per line. Records vary in type —
they may represent conversation turns, tool interactions, context snapshots,
or session events, each with their own fields. The following is a **simplified
illustration** of the kind of fields that appear; it is not a guaranteed
schema for every record in the file:

```json
{ "id": "...", "timestamp": "2026-10-12T13:...", "payload": { "type": "human_turn", "content": "..." } }
{ "id": "...", "timestamp": "2026-10-12T14:...", "payload": { "type": "assistant_turn", "content": "..." } }
```

The file is not human-readable prose — it is a structured log intended for
audit and review.

---

## Integrity and provenance

- Exports placed here are unmodified output from Kiro.
- They have not been renamed, reformatted, or edited to resemble Claude Code
  sessions or any other tool's export format.
- The tool used was Kiro. See `CLAUDE.md` for full AI tool disclosure,
  including why the file is named `CLAUDE.md` despite Kiro being the tool.
- Do not add fabricated session files. A genuine partial export is more
  honest than a complete-looking fake.
