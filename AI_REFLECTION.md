# AI Reflection

A reflection on using Kiro as an AI coding assistant throughout this
software engineering assessment. Written by the developer.

---

## Tool and context

| Field      | Value                                              |
|------------|----------------------------------------------------|
| AI tool    | Kiro IDE (AWS, VS Code-based)                      |
| Project    | Football League Standings calculator               |
| Stack      | Java 17, Maven, JUnit 5                            |
| Objective  | Production-quality CLI app with historical CSV data, tests, and documented AI usage |

---

## Stage 1 — Project planning and documentation

**What worked well.**
Kiro produced a coherent project plan from a single detailed prompt: class
responsibilities, historical domain rules, edge cases, and a test strategy
arrived together rather than requiring separate follow-up prompts. It
correctly identified the two most consequential historical rules — the 2-point
win system and goal average rather than goal difference — before any code was
written.

It also flagged the CLAUDE.md / Kiro naming discrepancy proactively and
recommended an honest disclosure approach, which became the foundation of
`CLAUDE.md`.

**What required human judgment.**
The correct interpretation of "Week 10" (which fixtures, which date) was
flagged by the AI as unresolved and needed manual research. The package name
`za.co.football` was a developer decision. The decision to keep all
documentation in plain language suitable for an interview explanation also
came from the developer.

**Limitations observed.**
Kiro cannot execute Maven commands, run tests, or read files outside the
workspace. Every `mvn clean test` and `mvn package` had to be run manually,
and the reported results had to be pasted back into the conversation.

---

## Stage 2 — Domain model (`MatchResult`, `TeamStanding`)

**What worked well.**
Both classes were implemented correctly on the first attempt. The AI
correctly used a Java 17 `record` for the immutable `MatchResult` and a
mutable accumulator class for `TeamStanding`, matching the design discussed
in Stage 1. The 2-point win rule was explicitly documented in constants
(`POINTS_FOR_WIN = 2`) and tested with a named assertion that would fail if
the value were ever changed to 3.

The goal-average edge case (GA = 0 returns `Double.POSITIVE_INFINITY`) was
researched before implementation. The AI confirmed that the Football League
never formally defined this case because it never occurred in a full season,
and chose `POSITIVE_INFINITY` as the mathematically consistent limit — with
a note that the CSV writer would need to handle the output separately.

**What required human judgment.**
The AI proposed `freeze()` as a package-private method on `TeamStanding` to
prevent callers from mutating returned standings. This was reviewed and
accepted as the simplest sensible protection without redesigning the domain
model.

**Tests run and passed:** 40 (after Stage 2 completion).

---

## Stage 3 — `StandingsCalculator`

**What worked well.**
The ranking comparator was implemented correctly using `Comparator.comparing`
with each criterion scoped independently. The AI documented an important Java
pitfall: calling `.reversed()` on a `thenComparingInt(...)` chain reverses
the *entire* accumulated comparator, not just the last criterion. The
solution — wrapping each descending criterion in its own
`Comparator.comparingX(fn).reversed()` before passing to `thenComparing` —
was explained clearly in both code comments and in the conversation.

**What required human judgment.**
Verifying that the comparator actually behaved correctly required running a
standalone test, which the developer did using `mvn test`. The AI attempted
to verify the comparator logic via a temporary Java snippet in PowerShell but
encountered encoding issues; the Maven test run was the definitive check.

**Tests run and passed:** 63.

---

## Stage 4 — CSV I/O (`CsvMatchReader`, `CsvStandingsWriter`)

**What worked well.**
The AI identified a locale-safety issue before it was raised: `String.format(
"%.3f", v)` uses the JVM default locale, which would write `2,333` instead of
`2.333` on systems with a comma-decimal locale (e.g. `de_DE`). This was fixed
to `String.format(Locale.ROOT, "%.3f", v)` and a regression test was added.

The hand-rolled RFC 4180 parser correctly handles quoted fields with embedded
commas and escaped double-quotes. The limitation (no multi-line quoted fields)
was documented explicitly and tested to confirm it produces a `CsvParseException`
rather than silently misparses.

A `CsvParseException` carrying the 1-based line number was introduced to
produce actionable error messages for the user without exposing stack traces.

**What required human judgment.**
The integration test in `CsvStandingsWriterTest` initially had the wrong
expected ranking order (Arsenal first, but Liverpool's goal average was
higher). The AI fixed the assertion correctly after recognising the error, and
documented the hand-calculation in a comment. The developer verified the
corrected expectation independently before accepting the change.

**Tests run and passed:** 113.

---

## Stage 5 — CLI entry point (`Main`)

**What worked well.**
The `run(String[], PrintStream)` / `main(String[])` split meant that tests
could call `run()` directly without triggering `System.exit()`. This pattern
was proposed by the AI and accepted as the simplest testable design. Named
exit-code constants (`EXIT_OK`, `EXIT_BAD_ARGS`, etc.) made test assertions
self-documenting.

The output directory creation policy (create automatically if missing) was
proposed, documented, and tested. A test confirmed that `output/subdir/`
is created when it does not yet exist.

**What required human judgment.**
The developer confirmed the policy choice (auto-create vs. error if missing)
before implementation. The developer ran `mvn clean package` and the JAR
independently to verify the reported build result.

**Tests run and passed:** 132.

---

## Stage 6 — Historical match data

**What worked well.**
The AI sourced 9 of the 11 Round 10 results (28 September 1974) from
individual Wikipedia club season articles, with 5 confirmed by two independent
articles. The primary source (Ian Laschke, *Rothmans Book of Football League
Records 1888–89 to 1978–79*) was identified and cited. Results that could not
be independently confirmed were flagged clearly rather than asserted as fact.

The data source notes (`results_1974_75_week10_sources.md`) were created
alongside the CSV to document per-fixture confidence levels — this was an AI
suggestion accepted by the developer.

**What required human judgment.**
Two results that the AI read from a secondary source were subsequently
corrected by the developer:
- Newcastle United 2–0 Ipswich Town → corrected to **1–0**
- Chelsea 0–2 Wolverhampton Wanderers → corrected to **0–1**

The AI updated the CSV and source notes to reflect both corrections accurately,
recording that they were externally revised rather than silently substituting
the new values.

The developer ran the final JAR against the corrected CSV, confirmed exit code
0, 22 teams in the output, and one match played per team.

---

## Overall assessment

**Where Kiro added clear value.**
Kiro was most effective at holding the full context of the design — historical
rules, edge cases, class interactions, test coverage — across a long
multi-stage conversation. It made technically sound decisions (the comparator
scoping fix, the locale-safe formatting, the freeze pattern) that a developer
working alone might have noticed only during a code review. It consistently
declined to fabricate data or claim verification it had not performed.

**Where human oversight was essential.**
Code review before each stage was not optional — it was necessary. The
integration test ranking error (Stage 4) and the two incorrect historical
scores (Stage 6) are concrete examples of the AI producing plausible but
wrong output that passed inspection only at the point of manual verification.
The AI's inability to run commands meant every test and build result had to
be independently confirmed.

**What I would do differently.**
Keeping the Kiro session export as a concurrent task rather than deferring it
to the end would have produced a more complete audit trail. Export the session
after each major stage while the context is fresh.

**Honest summary.**
This project was built collaboratively: the AI handled the bulk of the
implementation detail and documentation drafting; the developer provided
domain decisions, approval gates, independent test runs, and corrections.
Neither the AI alone nor the developer alone would have produced the same
result in the same time.
