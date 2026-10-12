# Football League Standings

A command-line Java application that reads football match results from a CSV file
and calculates the English First Division standings for the 10th week of the
1974/75 season.

---

## Historical context

### Season
The 1974/75 Football League First Division season began on **17 August 1974**.
"Week 10" is interpreted as the first ten rounds of fixtures, covering matches
played through approximately the end of September 1974.

### Points system
The Football League used a **2 points for a win** system in 1974/75.
The switch to 3 points for a win did not occur until the 1981/82 season.

| Result | Points |
|--------|--------|
| Win    | 2      |
| Draw   | 1      |
| Loss   | 0      |

### Tiebreaker: goal average (not goal difference)
The Football League used **goal average** (goals scored ÷ goals conceded) as the
first tiebreaker from 1894/95 until 1975/76, when it was replaced by goal
difference. Source: Ian Laschke, *Rothmans Book of Football League Records
1888–89 to 1978–79*.

Tiebreaker order used by this application:
1. Points (descending)
2. Goal average = GF ÷ GA (descending)
3. Goals scored (descending)
4. Team name (ascending, alphabetical) — deterministic fallback; not a historical rule

**Zero goals against:** a team with a goals-against total of zero has an
undefined goal average (division by zero). This application assigns
`Double.POSITIVE_INFINITY` internally so the team sorts above all others.
In the output CSV the `goal_average` column is left **empty** for such a team
rather than writing the string `Infinity`. This is documented as an application
convention — no Football League First Division team has ever conceded zero goals
in a completed season, so no historical precedent exists.

### Data status
> ⚠️ **The match-results CSV has PARTIAL VERIFICATION.**
> Nine of the eleven Round 10 results (28 September 1974) have been confirmed
> from individual Wikipedia club season articles, with five confirmed by two
> independent articles. Two results (Newcastle United 2–0 Ipswich Town and
> Chelsea 0–2 Wolverhampton Wanderers) come from the Italian Wikipedia
> 1974–75 First Division results matrix, which cites worldfootball.net and
> the *Rothmans Book of Football League Records 1888–89 to 1978–79*.
> See `src/main/resources/results_1974_75_week10_sources.md` for the
> per-fixture breakdown.
>
> Full verification against *Rothmans Book of Football League Records* or the
> original RSSSF archive would constitute definitive confirmation.
> Do not treat the calculated standings as fully historically verified until
> that step is completed.

### Historical scoring rules

The application implements the rules that were in force in 1974/75:

**Points system — 2 points for a win:**
The Football League used a 2-point win system from its founding until
1981/82. Using the modern 3-point rule would produce incorrect standings.

| Result | Points |
|--------|--------|
| Win    | 2      |
| Draw   | 1      |
| Loss   | 0      |

**Tiebreaker — goal average, not goal difference:**
The Football League ranked teams level on points by *goal average*
(goals scored ÷ goals conceded) from 1894/95 until 1975/76, when it
switched to goal difference. Source: Ian Laschke, *Rothmans Book of
Football League Records 1888–89 to 1978–79*.

For example, a team with GF=4 and GA=2 has goal average 2.000 and goal
difference +2. A team with GF=6 and GA=3 has the same goal difference
(+3 > +2), but a lower goal average (2.000 = 2.000 — tied in this case).
The distinction matters most when both GF and GA scale proportionally.

---

## Prerequisites

| Tool  | Version      |
|-------|--------------|
| Java  | 17 (OpenJDK) |
| Maven | 3.6+         |

---

## Building

```bash
mvn clean package
```

This produces `target/football-league-standings.jar`.

---

## Running

```
java -jar target/football-league-standings.jar <input.csv> <output.csv>
```

| Argument     | Description                                                      |
|--------------|------------------------------------------------------------------|
| `input.csv`  | CSV file containing match results (see Input CSV format below)   |
| `output.csv` | Destination path for the calculated standings                    |

The output parent directory is created automatically if it does not exist.

**Example (PowerShell / bash):**

```bash
java -jar target/football-league-standings.jar `
  src/main/resources/results_1974_75_week10.csv `
  output/standings_week10.csv
```

**Example (single line):**

```bash
java -jar target/football-league-standings.jar src/main/resources/results_1974_75_week10.csv output/standings_week10.csv
```

### Exit codes

| Code | Meaning                                      |
|------|----------------------------------------------|
| 0    | Success                                      |
| 1    | Wrong number of arguments                    |
| 2    | Input file not found                         |
| 3    | Malformed CSV input (with line number)       |
| 4    | I/O error reading or writing                 |
| 5    | Unexpected error                             |

All error messages are written to **stderr**. Success output goes to **stdout**.

---

## Input CSV format

```
home_team,away_team,home_goals,away_goals
Arsenal,Chelsea,2,1
Leeds United,Liverpool,0,0
"Notts County","Stoke City",1,2
```

| Rule | Detail |
|------|--------|
| Header | Required as the first non-blank line, case-insensitive |
| Columns | Exactly `home_team`, `away_team`, `home_goals`, `away_goals` |
| Team names | May contain spaces; names with commas must be quoted (`"Notts, County"`) |
| Goals | Non-negative integers |
| Blank lines | Silently ignored anywhere in the file |
| Encoding | UTF-8 |
| Quoted fields | RFC 4180 subset: `"..."` with `""` for embedded quotes; no multi-line fields |

---

## Output CSV format

```
position,team,played,won,drawn,lost,goals_for,goals_against,goal_average,points
1,Ipswich Town,10,7,2,1,21,8,2.625,16
2,Liverpool,10,6,3,1,18,7,2.571,15
...
```

| Column          | Description                                                   |
|-----------------|---------------------------------------------------------------|
| `position`      | 1-based rank                                                  |
| `team`          | Team name (RFC 4180-escaped if it contains commas or quotes)  |
| `played`        | Total matches played                                          |
| `won`           | Matches won                                                   |
| `drawn`         | Matches drawn                                                 |
| `lost`          | Matches lost                                                  |
| `goals_for`     | Total goals scored                                            |
| `goals_against` | Total goals conceded                                          |
| `goal_average`  | GF ÷ GA to 3 decimal places; **empty** when goals_against = 0 |
| `points`        | Total points (2 per win, 1 per draw)                          |

Rows are sorted by the ranking rules described above. The decimal separator
is always a period (`.`) regardless of system locale.

---

## Running the tests

```bash
mvn test
```

All tests use JUnit 5. No external services or network access are required.

---

## Running in IntelliJ IDEA

1. Open the project (IntelliJ detects the `pom.xml` automatically).
2. Click the Maven reload icon if prompted.
3. **Run tests:** open the Maven panel → `football-league-standings` → Lifecycle → `test`.
4. **Build JAR:** Maven panel → Lifecycle → `package`.
5. **Run the application:** open a terminal (Alt+F12) and use the command above,
   or create a Run Configuration with:
   - Main class: `za.co.football.Main`
   - Program arguments: `<path-to-input.csv> <path-to-output.csv>`

---

## Project structure

```
src/main/java/za/co/football/
├── Main.java                  # CLI entry point (argument parsing, exit codes)
├── MatchResult.java           # Immutable record: home team, away team, scores
├── TeamStanding.java          # Accumulates statistics for one team
├── StandingsCalculator.java   # Pure business logic — no I/O
├── CsvMatchReader.java        # Reads match-results CSV → List<MatchResult>
├── CsvStandingsWriter.java    # Writes List<TeamStanding> → CSV
└── CsvParseException.java     # Unchecked exception carrying line number

src/test/java/za/co/football/
├── MainTest.java
├── CsvMatchReaderTest.java
├── CsvStandingsWriterTest.java
├── StandingsCalculatorTest.java
├── TeamStandingTest.java
└── MatchResultTest.java

src/main/resources/
└── results_1974_75_week10.csv  # Historical input (UNVERIFIED — see above)

ai/                             # Genuine Kiro AI session exports
.claude/sessions/               # Assessment artefact directory (see CLAUDE.md)
```

---

## AI tooling

This project was developed with **Kiro**, an AI-powered IDE built on VS Code.
See [CLAUDE.md](CLAUDE.md) for full AI tool disclosure and session records.
