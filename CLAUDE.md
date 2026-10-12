# AI Tool Usage — CLAUDE.md

> **Transparency notice — please read**
>
> The assessment brief specifies `CLAUDE.md` and a `.claude/` directory, which
> are naming conventions associated with Anthropic's *Claude Code* CLI tool.
>
> **This project was developed using Kiro** (an AI-powered IDE built on
> VS Code, provided by AWS), not Claude Code. This file fulfils the
> equivalent role: it documents which AI tool was used, the constraints placed
> on it, what it helped produce, and what the developer verified or wrote
> independently.
>
> The `.claude/sessions/` directory exists solely to satisfy the assessment's
> directory-structure check. It is intentionally empty — no Claude Code
> sessions were generated, and fabricating them would be dishonest.
> Genuine AI session records belong in the `ai/` directory as Kiro exports
> (see `ai/README.md`).

---

## AI tool used

| Field          | Value                                        |
|----------------|----------------------------------------------|
| Tool           | Kiro IDE                                     |
| Provider       | AWS (built on VS Code)                       |
| Session type   | Vibe (conversational coding assistant)       |
| Underlying model | Auto — dynamically selected by the server  |
| Session dates  | October 2026 — see `ai/` for exported logs  |

---

## Constraints given to the AI throughout the session

These constraints were stated explicitly at the start and remained in force:

- Java 17 and Maven only. JUnit 5 for tests. No Spring Boot, no database.
- Do not invent historical results or claim verification without evidence.
- Do not claim tests pass unless they have actually been executed and reported.
- Do not fabricate AI conversation exports or misrepresent Kiro as Claude Code.
- Keep the design simple enough for the developer to explain in an interview.
- Do not overwrite existing files without explaining the proposed change first.
- Proceed in small, reviewable stages; wait for approval before implementing.
- Use historical **goal average** (GF ÷ GA), not goal difference.
- Use the **2-points-for-a-win** system (correct for the 1974/75 season).
- Package name: `za.co.football`.

---

## What the AI helped with

- Proposing the full project structure, class responsibilities, and test strategy
  before any code was written
- Researching the historical scoring rules — identifying the 2-point win system
  and goal average tiebreaker as the two likely "traps" for the 1974/75 era
- Drafting `pom.xml` with pinned dependency versions
- Implementing all six Java source classes and five test classes, one stage at
  a time, waiting for approval between stages
- Researching and sourcing 9 of the 11 Round 10 match results from Wikipedia
  club season articles; flagging the two lower-confidence results clearly
- Drafting all documentation: `README.md`, `CLAUDE.md`, `AI_REFLECTION.md`,
  `ai/README.md`, and the historical data source notes
- Identifying the `CLAUDE.md` / Kiro naming discrepancy proactively and
  recommending an honest approach rather than fabricating artefacts

---

## What the developer verified or decided independently

- Reviewed every generated class and test before approving each stage
- Chose the `za.co.football` package name
- Ran `mvn clean test` and `mvn package` independently to confirm the reported
  results (the AI cannot execute commands on this machine)
- Confirmed historical scoring rules against public sources before accepting
  the AI's implementation
- Corrected two historical match scores (Newcastle 1–0 Ipswich; Chelsea 0–1
  Wolverhampton) that the AI had read incorrectly from a secondary source
- Applied all corrections to the CSV after reviewing the AI's explanation
- Reviewed and accepted the goal-average edge-case policy (GA = 0 →
  `POSITIVE_INFINITY` internally, empty field in CSV output)

---

## Data verification status

Nine of the eleven Round 10 results have been confirmed from individual
Wikipedia club season articles (five confirmed by two independent articles).
Two results were corrected externally after the AI's initial sourcing.
See `src/main/resources/results_1974_75_week10_sources.md` for the
per-fixture breakdown.

Full verification against the *Rothmans Book of Football League Records
1888–89 to 1978–79* would constitute definitive confirmation.

---

## Assessment compliance note

The assessment permits AI coding assistants and names `CLAUDE.md`, `.claude/`,
`ai/`, and `AI_REFLECTION.md` as required artefacts. All four exist. The tool
used was Kiro, not Claude Code; this is disclosed here rather than concealed.
If the assessor requires genuine Claude Code artefacts specifically, that
should be raised directly — fabricating them is not done here.

---

## Session export location

Genuine Kiro session exports (JSON/JSONL format) are stored in `ai/`.
See `ai/README.md` for export instructions and provenance notes.
