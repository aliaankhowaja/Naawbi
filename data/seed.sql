-- =============================================================
-- Naawbi — test seed data
-- Wipes all existing data and inserts a full realistic dataset.
--
-- Usage:
--   make seed                      (from project root)
--   psql -U postgres -d naawbi -f seed.sql
--
-- Accounts (all passwords: password123)
--   ali@naawbi.edu   — instructor
--   ibbi@naawbi.edu  — student
--   sara@naawbi.edu  — student (extra, enrolled in both courses)
-- =============================================================

-- ── Wipe ──────────────────────────────────────────────────────
TRUNCATE
    submission_comments,
    submissions,
    announcement_attachments,
    announcements,
    assignments,
    course_enrollments,
    courses,
    users
RESTART IDENTITY CASCADE;

-- ── Users  (SHA-256 of "password123") ─────────────────────────
INSERT INTO users (username, email, password, role) VALUES
    ('ali',  'ali@naawbi.edu',  'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'instructor'),
    ('ibbi', 'ibbi@naawbi.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'student'),
    ('sara', 'sara@naawbi.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'student');

-- ── Courses ───────────────────────────────────────────────────
INSERT INTO courses (course_name, course_code, description, created_by, is_active) VALUES
    ('Introduction to Databases', 'CS301',
     'Relational models, SQL, normalization, transactions, and indexing.',
     1, TRUE),
    ('Software Engineering',      'CS401',
     'SDLC, agile methods, design patterns, testing, and project delivery.',
     1, TRUE);

-- ── Enrollments ───────────────────────────────────────────────
INSERT INTO course_enrollments (course_id, user_id, role) VALUES
    -- CS301
    (1, 1, 'instructor'),
    (1, 2, 'student'),
    (1, 3, 'student'),
    -- CS401
    (2, 1, 'instructor'),
    (2, 2, 'student'),
    (2, 3, 'student');

-- ── Announcements ─────────────────────────────────────────────
INSERT INTO announcements (course_id, created_by, title, content, content_type) VALUES
    (1, 1, 'Welcome to CS301',
     '<p>Welcome everyone! Office hours are <strong>Mon/Wed 2–4 PM</strong>. First quiz is in week 3.</p>',
     'html'),
    (1, 1, 'ER Diagram tips',
     '<p>A few things to keep in mind for Assignment 1: identify all entities first, then relationships, then attributes. Use crow''s-foot notation.</p>',
     'html'),
    (2, 1, 'Welcome to CS401',
     '<p>We follow <strong>Scrum</strong> this semester. Teams of 3. Project kickoff is next week.</p>',
     'html'),
    (2, 1, 'Sprint 1 kickoff',
     '<p>Please submit your backlog by Friday EOD. Story points using Fibonacci (1 2 3 5 8 13).</p>',
     'html');

-- Announcement attachment (link) on the ER Diagram tips post
INSERT INTO announcement_attachments (announcement_id, file_name, link_url, is_link) VALUES
    (2, 'ER Diagram Notation Guide', 'https://www.lucidchart.com/pages/er-diagrams', TRUE);

-- ── Assignments ───────────────────────────────────────────────
-- CS301
INSERT INTO assignments (course_id, created_by, title, description, total_points, late_submissions_allowed, deadline) VALUES
    (1, 1, 'ER Diagram Design',
     'Design a complete ER diagram for a hospital management system. Include entities, relationships, cardinality, and participation constraints. Export as PDF.',
     20, TRUE,  NOW() + INTERVAL '7 days'),

    (1, 1, 'SQL Query Lab',
     'Write 10 SQL queries against the provided university schema. Queries must cover SELECT, JOIN, GROUP BY, HAVING, and subqueries. Submit as a .sql file.',
     30, FALSE, NOW() + INTERVAL '14 days'),

    (1, 1, 'Normalization Exercise',
     'Given the unnormalized relation R(A,B,C,D,E,F), identify all functional dependencies and decompose to 3NF. Show every step.',
     25, FALSE, NOW() - INTERVAL '3 days');   -- past due

-- CS401
INSERT INTO assignments (course_id, created_by, title, description, total_points, late_submissions_allowed, deadline) VALUES
    (2, 1, 'Use Case Diagrams',
     'Draw use case diagrams for the Naawbi LMS covering authentication, course management, and assignment submission. Use UML notation.',
     15, TRUE,  NOW() + INTERVAL '5 days'),

    (2, 1, 'Sprint 1 Backlog',
     'Submit your team sprint backlog with user stories, acceptance criteria, and story point estimates. Use the provided Jira template.',
     20, TRUE,  NOW() + INTERVAL '10 days'),

    (2, 1, 'Design Patterns Report',
     'Identify 3 design patterns used in the Naawbi codebase. For each: name, category (creational/structural/behavioral), where it appears, and why it was chosen.',
     30, FALSE, NOW() - INTERVAL '1 day');    -- past due

-- ── Submissions ───────────────────────────────────────────────
-- What each submission tests:
--   graded+late   → gradebook shows score, Grades tab shows feedback
--   graded+ontime → normal graded state
--   submitted     → instructor sees it in GradingView, can grade it
--   missing/none  → student sees "Missing" or "Not Submitted" badge

-- ibbi — CS301 Normalization (past due, submitted late, GRADED)
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at, grade, feedback) VALUES
    (3, 2, TRUE, 'Late', NOW() - INTERVAL '6 hours', 19,
     'Solid decomposition — 3NF achieved correctly. Minor issue: you listed F→D as a dependency but did not justify it with an example tuple. -1 pt.');

-- ibbi — CS301 ER Diagram (upcoming, submitted on time, NOT YET GRADED — ali can grade this)
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at) VALUES
    (1, 2, TRUE, 'Submitted', NOW() - INTERVAL '1 day');

-- ibbi — CS401 Design Patterns (past due, submitted on time, NOT YET GRADED — ali can grade this)
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at) VALUES
    (6, 2, TRUE, 'Submitted', NOW() - INTERVAL '2 days');

-- ibbi — CS401 Use Case Diagrams (upcoming, submitted early, GRADED)
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at, grade, feedback) VALUES
    (4, 2, TRUE, 'Submitted', NOW() - INTERVAL '3 days', 13,
     'Good coverage of auth and course management. The assignment submission use case is missing the "resubmit" extend relationship. -2 pts.');

-- ibbi — CS301 SQL Query Lab: no submission (shows Missing after deadline / Not Submitted before)
-- ibbi — CS401 Sprint 1 Backlog: no submission (upcoming, shows Not Submitted)

-- sara — CS301 Normalization (past due, on time, GRADED)
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at, grade, feedback) VALUES
    (3, 3, TRUE, 'Submitted', NOW() - INTERVAL '4 days', 24,
     'Excellent work. All FDs identified correctly and decomposition is clean. Small note: the relation name in step 4 should match step 2.');

-- sara — CS401 Design Patterns (past due, submitted late, NOT YET GRADED — ali can grade this)
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at) VALUES
    (6, 3, TRUE, 'Late', NOW() - INTERVAL '12 hours');

-- sara — CS301 ER Diagram: no submission (upcoming, shows Not Submitted for sara)

-- ── Summary ───────────────────────────────────────────────────
SELECT 'users'             AS table_name, count(*) AS rows FROM users
UNION ALL
SELECT 'courses',            count(*) FROM courses
UNION ALL
SELECT 'enrollments',        count(*) FROM course_enrollments
UNION ALL
SELECT 'announcements',      count(*) FROM announcements
UNION ALL
SELECT 'assignments',        count(*) FROM assignments
UNION ALL
SELECT 'submissions',        count(*) FROM submissions
ORDER BY 1;
