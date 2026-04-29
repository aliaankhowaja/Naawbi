# Naawbi LMS

A university-level Learning Management System built with Java 24 + JavaFX 21 and PostgreSQL. Developed iteratively over three sprints by Team Ibwaan for CS3009 Software Engineering (Spring 2026).

## What it does

| Role | Capabilities |
|---|---|
| **Instructor** | Create courses · Post announcements with file/link attachments · Create assignments with deadlines and late policies · Grade student submissions with numeric score + feedback · View class-wide gradebook |
| **Student** | Enroll in courses via class code · View course stream · Submit assignments · Track submission status (Submitted / Late / Missing) · View personal grades and feedback · To-Do list across all enrolled courses |

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 24 |
| UI Framework | JavaFX 21 (OpenJFX) |
| Database | PostgreSQL 16+ |
| DB Driver | postgresql-42.7.4.jar |
| Build | GNU Make |
| Architecture | MVC — `model/` · `controller/` · `view/` (FXML + CSS) |

## Prerequisites

1. **Java 24** — [adoptium.net](https://adoptium.net) or any JDK 24 distribution
2. **JavaFX 21 SDK** — download for your platform from **[gluonhq.com/products/javafx](https://gluonhq.com/products/javafx/)**
   - Select version **21.0.x (LTS)**, type **SDK**, then pick your OS:
     - **Windows** — `openjfx-21.0.x_windows-x64_bin-sdk.zip`
     - **macOS** — `openjfx-21.0.x_macos-x64_bin-sdk.zip` (Intel) or `macos-aarch64` (Apple Silicon)
     - **Linux** — `openjfx-21.0.x_linux-x64_bin-sdk.zip`
   - Extract the zip. Copy everything inside the extracted `lib/` folder into **`<project-root>/lib/`**
   - Also drop the PostgreSQL driver ([postgresql-42.7.4.jar](https://jdbc.postgresql.org/download/)) into `lib/`
3. **PostgreSQL 16+** running locally on port 5432
   - Database: `naawbi` · User: `postgres` · Password: `postgres`
   - Change in `src/com/ibwaan/naawbi/model/DB.java` if your credentials differ
4. **GNU Make** (comes with Git for Windows / MSYS2 / WSL)

> `lib/` is in `.gitignore` — every team member must populate it manually as above.

## Setup

```bash
# 1. Clone
git clone https://github.com/aliaankhowaja/Naawbi.git
cd Naawbi

# 2. Populate lib/ with JavaFX SDK + PostgreSQL driver (see Prerequisites above)

# 3. Create the database
psql -U postgres -c "CREATE DATABASE naawbi;"

# 4. Initialise schema + load test data
make seed

# 5. Compile and run
make run
```

## Make Targets

| Command | What it does |
|---|---|
| `make compile` | Compile all Java sources → `bin/` |
| `make run` | Compile then launch the app |
| `make dev` | Same as run but with software renderer (no GPU needed) |
| `make test` | Compile and run JUnit unit tests under `test/` |
| `make clean` | Delete compiled classes (main + tests) |
| `make clean-test` | Delete compiled test classes only |
| `make rebuild` | `clean` then `compile` |
| `make seed` | Wipe DB and reload test data from `seed.sql` |

## Testing

Tests live under `test/` and run with **JUnit 5**. There are two kinds:

| Type | Location | Hits the DB? | Examples |
|---|---|---|---|
| **Unit tests** | `test/naawbi/model/*Test.java` | No | `PasswordUtilTest`, `SessionTest`, every model's constructor / getter / setter |
| **DAO integration tests** | `test/naawbi/model/*DAOTest.java` | Yes — `naawbi_test` only | `UserDAOTest`, `CourseDAOTest`, `SubmissionDAOTest` |

Integration tests run against a **dedicated `naawbi_test` database**, never your dev `naawbi`. The base class `IntegrationTestBase` refuses to start unless `-Dnaawbi.db.url` points at a database whose name contains `naawbi_test`, so you cannot accidentally nuke seed data.

### One-time setup for teammates

**1. Add the JUnit JAR to `lib/`** (same place as the JavaFX SDK and Postgres driver):

- File: **`junit-platform-console-standalone-1.11.4.jar`** (~2.8 MB, single fat JAR — includes API, engine, and launcher)
- Direct link: <https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar>

> `lib/` is gitignored, so every teammate populates it manually — same pattern as the JavaFX SDK and Postgres driver.

**2. That's it.** `make test` will create the `naawbi_test` database the first time you run it (idempotent — runs `data/db-test-init.sh`, which uses the same psql lookup as `db-seed.sh`). No manual `CREATE DATABASE` needed.

### Run the tests

```bash
make test         # creates naawbi_test if missing, compiles, runs everything
make test-db      # just create naawbi_test (rarely needed standalone)
make clean-test   # delete compiled test classes
```

As of the latest commit: **15 test classes, 61 tests**, all green in ~5 seconds.

### Test layout

```
test/
└── naawbi/
    ├── IntegrationTestBase.java       # @BeforeAll schema setup, @BeforeEach truncate-all
    └── model/
        ├── PasswordUtilTest.java          # SHA-256 determinism, format, known vectors, unicode, null
        ├── SessionTest.java               # Singleton, login/logout, role checks, exception cases
        ├── UserTest.java                  # Constructor + getters
        ├── CourseTest.java                # Both constructors (id=-1 sentinel), setters
        ├── AssignmentTest.java            # Both constructors, setters, userStatus lifecycle
        ├── SubmissionTest.java            # All three constructors, defaults, grade nullability
        ├── AnnouncementTest.java          # Both constructors, contentType passthrough
        ├── AnnouncementAttachmentTest.java # File vs link variants, large file sizes
        ├── SubmissionCommentTest.java     # Timestamp stamping, hydrated constructor
        ├── UserDAOTest.java               # register, findByEmailAndPassword, fetchByCourseId  (DB)
        ├── CourseDAOTest.java             # enrollStudent idempotency, fetchByCode, fetchByUserId  (DB)
        └── SubmissionDAOTest.java         # findOrCreate, markAsSubmitted, unsubmit, saveGrade  (DB)
```

### How DAO tests stay isolated

- `make test` passes `-Dnaawbi.db.url=jdbc:postgresql://localhost:5432/naawbi_test` to the JVM
- `DB.java` reads that property at startup, so the connection singleton points at the test DB
- `IntegrationTestBase.@BeforeAll` calls `DB.createTables()` — schema is auto-created, mirrors prod
- `IntegrationTestBase.@BeforeEach` runs `TRUNCATE ... RESTART IDENTITY CASCADE` on every table — every test starts from an empty DB
- The dev `naawbi` database is never opened, never queried, never modified

### Adding new tests

| You want to test... | Extend... | Annotate with... |
|---|---|---|
| Pure logic (no DB) | nothing — plain class | `@Test` |
| DB-touching methods | `IntegrationTestBase` | `@Test` |

Drop any `*Test.java` file under `test/naawbi/...` — JUnit auto-discovers them. Use `@Test` from `org.junit.jupiter.api` and the static helpers in `org.junit.jupiter.api.Assertions`.

### Running tests from your IDE

If you run a test directly from VS Code / IntelliJ rather than via `make test`, **set this VM argument** on the run configuration:

```
-Dnaawbi.db.url=jdbc:postgresql://localhost:5432/naawbi_test
```

Without it, integration tests fail fast with a clear error message — by design — so they can never accidentally run against `naawbi`.

## Test Accounts

All accounts use password **`password123`**.

| Email | Role | What to test |
|---|---|---|
| `ali@naawbi.edu` | Instructor | Post announcements, create assignments, grade submissions, view class gradebook |
| `ibbi@naawbi.edu` | Student | Submit assignments, view submission status, check personal grades and feedback |
| `sara@naawbi.edu` | Student | Second student perspective; has grades already seeded for cross-checking the gradebook |

Run `make seed` at any time to reset the database to a clean known state.

## Project Structure

```
Naawbi/
├── src/com/ibwaan/naawbi/
│   ├── Main.java                        # JavaFX entry point
│   ├── controller/                      # One controller per view
│   │   ├── LoginController.java
│   │   ├── CourseCatalogController.java # Main app shell + tab routing
│   │   ├── CreateCourseController.java
│   │   ├── CreateAnnouncementController.java
│   │   ├── CreateAssignmentController.java
│   │   ├── AssignmentDetailsController.java
│   │   ├── GradingController.java       # Instructor grading modal
│   │   ├── GradebookController.java     # Student + instructor gradebook
│   │   └── ToDoController.java
│   ├── model/                           # DB access + business logic
│   │   ├── DB.java                      # Connection singleton + schema init
│   │   ├── User.java
│   │   ├── Course.java
│   │   ├── Assignment.java
│   │   ├── Submission.java              # Includes grade/feedback + saveGrade()
│   │   ├── Announcement.java
│   │   ├── AnnouncementAttachment.java
│   │   ├── Session.java                 # Auth session singleton
│   │   ├── PasswordUtil.java            # SHA-256 hashing
│   │   └── ...
│   └── view/                            # FXML layouts + CSS per feature
│       ├── Login/
│       ├── CourseCatalog/
│       ├── CreateCourse/
│       ├── CreateAnnouncement/
│       ├── CreateAssignment/
│       ├── AssignmentDetails/
│       ├── Grading/
│       └── Gradebook/
├── data/
│   ├── seed.sql                         # Idempotent test data (3 users, 2 courses, 6 submissions)
│   └── db-seed.sh                       # Cross-version psql runner for Windows
├── docs/
│   ├── D1/                              # Deliverable 1 — Team info, Sprint 1
│   │   ├── A1.pdf                       # Assignment brief
│   │   ├── D1.pdf · D1.tex · ...        # Deliverable document + LaTeX sources
│   │   └── submissions/                 # Formally submitted PDFs
│   ├── D2/                              # Deliverable 2 — Sprint 2
│   │   ├── A2.pdf
│   │   ├── D2.docx
│   │   └── submissions/
│   └── D3/                              # Deliverable 3 — Sprint 3
│       ├── A3.pdf
│       ├── D3.md
│       └── submissions/
├── Makefile                             # Build, run, seed targets
└── .vscode/launch.json                  # VS Code Java launch config
```

## Database Schema (summary)

```
users               id, username, email, password (SHA-256), role
courses             id, course_name, course_code, description, created_by, is_active
course_enrollments  course_id, user_id, role
announcements       id, course_id, created_by, title, content, content_type
announcement_attachments  id, announcement_id, file_name, link_url, is_link
assignments         id, course_id, created_by, title, description, total_points,
                    late_submissions_allowed, deadline
submissions         id, assignment_id, user_id, submitted, status, submitted_at,
                    grade, feedback
submission_comments id, submission_id, user_id, comment, created_at
```

Schema is auto-created by `DB.createTables()` on first run — no manual migration needed.

## Sprint History

| Sprint | Deliverable | Key Features |
|---|---|---|
| Sprint 1 | D1 (Feb 2026) | Course creation, catalogue, class-code enrollment |
| Sprint 2 | D2 (Mar 2026) | Announcements, assignments, file submissions, To-Do list |
| Sprint 3 | D3 (Apr 2026) | Auth + RBAC, announcement attachments, assignments/people tabs, grading, gradebook |

## Team

**Ibwaan** — Team Ibraheem Farooq (PM), Wajih-Ur-Raza Asif (Requirements/Testing), Ali Aan Khowaja (Development/UI)
