-- =============================================================
-- Naawbi — test seed data (expanded for Sprint 2 demo)
-- Wipes all existing data and inserts a full realistic dataset.
--
-- Usage:
--   make seed                      (from project root)
--   psql -U postgres -d naawbi -f seed.sql
--
-- All passwords: password123
-- Instructors: ali, wajih
-- Students:    ibbi, sara, fatima, omar, zara, hassan
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
    -- instructors
    ('ali',    'ali@naawbi.edu',    'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'instructor'),  -- id=1
    ('wajih',  'wajih@naawbi.edu',  'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'instructor'),  -- id=2
    -- students
    ('ibbi',   'ibbi@naawbi.edu',   'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'student'),     -- id=3
    ('sara',   'sara@naawbi.edu',   'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'student'),     -- id=4
    ('fatima', 'fatima@naawbi.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'student'),     -- id=5
    ('omar',   'omar@naawbi.edu',   'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'student'),     -- id=6
    ('zara',   'zara@naawbi.edu',   'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'student'),     -- id=7
    ('hassan', 'hassan@naawbi.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'student');     -- id=8

-- ── Courses ───────────────────────────────────────────────────
INSERT INTO courses (course_name, course_code, description, created_by, is_active) VALUES
    ('Introduction to Databases',     'CS301', 'Relational models, SQL, normalization, transactions, and indexing.',                            1, TRUE),  -- id=1 ali
    ('Software Engineering',          'CS401', 'SDLC, agile methods, design patterns, testing, and project delivery.',                          1, TRUE),  -- id=2 ali
    ('Data Structures & Algorithms',  'CS201', 'Arrays, linked lists, trees, graphs, hashing, and algorithm analysis.',                          2, TRUE),  -- id=3 wajih
    ('Introduction to Programming',   'CS101', 'Programming fundamentals in Java: variables, control flow, methods, OOP basics.',                2, TRUE),  -- id=4 wajih
    ('Operating Systems',             'CS501', 'Processes, threads, scheduling, memory management, file systems, concurrency.',                  1, TRUE),  -- id=5 ali
    ('Computer Networks',             'CS305', 'OSI model, TCP/IP, routing, network security, application layer protocols.',                     2, TRUE),  -- id=6 wajih
    ('Machine Learning',              'CS410', 'Supervised and unsupervised learning, neural networks, evaluation metrics.',                     2, TRUE),  -- id=7 wajih
    ('Web Development',               'CS220', 'HTML, CSS, JavaScript, REST APIs, responsive design, frontend frameworks.',                      1, TRUE),  -- id=8 ali
    ('Software Architecture',         'CS999', 'Advanced design: microservices, event-driven systems, DDD, scalability patterns.',               2, TRUE),  -- id=9 wajih
    ('Mobile App Development',        'CS340', 'Android and iOS basics, cross-platform tools, mobile UX, app deployment.',                       1, TRUE);  -- id=10 ali

-- ── Enrollments ───────────────────────────────────────────────
INSERT INTO course_enrollments (course_id, user_id, role) VALUES
    -- CS301 (ali) — all students
    (1, 1, 'instructor'), (1, 3, 'student'), (1, 4, 'student'), (1, 5, 'student'), (1, 6, 'student'),
    -- CS401 (ali)
    (2, 1, 'instructor'), (2, 3, 'student'), (2, 4, 'student'), (2, 7, 'student'), (2, 8, 'student'),
    -- CS201 (wajih)
    (3, 2, 'instructor'), (3, 3, 'student'), (3, 5, 'student'), (3, 6, 'student'), (3, 7, 'student'), (3, 8, 'student'),
    -- CS101 (wajih)
    (4, 2, 'instructor'), (4, 5, 'student'), (4, 6, 'student'), (4, 7, 'student'), (4, 8, 'student'),
    -- CS501 (ali)
    (5, 1, 'instructor'), (5, 3, 'student'), (5, 4, 'student'), (5, 6, 'student'),
    -- CS305 (wajih)
    (6, 2, 'instructor'), (6, 4, 'student'), (6, 5, 'student'), (6, 7, 'student'),
    -- CS410 (wajih)
    (7, 2, 'instructor'), (7, 3, 'student'), (7, 5, 'student'), (7, 8, 'student'),
    -- CS220 (ali)
    (8, 1, 'instructor'), (8, 4, 'student'), (8, 6, 'student'), (8, 7, 'student'),
    -- CS999 (wajih)
    (9, 2, 'instructor'), (9, 3, 'student'), (9, 4, 'student'),
    -- CS340 (ali)
    (10, 1, 'instructor'), (10, 5, 'student'), (10, 6, 'student'), (10, 8, 'student');

-- ── Announcements ─────────────────────────────────────────────
INSERT INTO announcements (course_id, created_by, title, content, content_type) VALUES
    -- CS301 (ali)
    (1, 1, 'Welcome to CS301',         '<p>Welcome everyone! Office hours are <strong>Mon/Wed 2–4 PM</strong>. First quiz is in week 3.</p>', 'html'),
    (1, 1, 'ER Diagram tips',          '<p>For Assignment 1: identify entities, then relationships, then attributes. Use crow''s-foot notation.</p>', 'html'),
    (1, 1, 'Mid-term schedule',        '<p>Mid-term exam is on <strong>Week 8, Tuesday 10 AM</strong>. Open-book, 90 minutes. Topics: relational algebra, SQL, normalization up to 3NF.</p>', 'html'),
    -- CS401 (ali)
    (2, 1, 'Welcome to CS401',         '<p>We follow <strong>Scrum</strong> this semester. Teams of 3. Project kickoff is next week.</p>', 'html'),
    (2, 1, 'Sprint 1 kickoff',         '<p>Submit your backlog by Friday EOD. Story points using Fibonacci (1 2 3 5 8 13).</p>', 'html'),
    (2, 1, 'Code review etiquette',    '<p>When reviewing PRs: be specific, suggest improvements, never block on style alone. <em>Be kind.</em></p>', 'html'),
    -- CS201 (wajih)
    (3, 2, 'Welcome to CS201',         '<p>This semester we cover the foundations every CS student needs. Bring a notebook — we draw a lot of diagrams.</p>', 'html'),
    (3, 2, 'Big-O cheat sheet',        '<p>I''ve attached the Big-O complexity reference. Memorize the common cases: O(1), O(log n), O(n), O(n log n), O(n²).</p>', 'html'),
    (3, 2, 'Lab assistance',           '<p>TA office hours moved to <strong>Thu 3–5 PM</strong>. Drop by Lab 4 with code questions.</p>', 'html'),
    -- CS101 (wajih)
    (4, 2, 'Welcome new students!',    '<p>If this is your first programming class, don''t panic. We''ll start from <em>Hello World</em> and build up.</p>', 'html'),
    (4, 2, 'IDE setup guide',          '<p>Install IntelliJ IDEA Community Edition + JDK 21. Setup walkthrough on Moodle.</p>', 'html'),
    -- CS501 (ali)
    (5, 1, 'Welcome to OS',            '<p>Buckle up. We''ll write a mini scheduler in C this semester. Refresh your pointers and memory model.</p>', 'html'),
    (5, 1, 'Lab 1 — fork()',           '<p>First lab uses <code>fork()</code>, <code>exec()</code>, and <code>wait()</code>. Linux/Mac only — Windows users use WSL.</p>', 'html'),
    -- CS305 (wajih)
    (6, 2, 'Welcome to Networks',      '<p>We start with the OSI model and work up to HTTP. Wireshark labs every other week.</p>', 'html'),
    (6, 2, 'Subnetting practice',      '<p>Subnetting is the foundation of routing. Practice the cheat sheet — there will be a quiz next week.</p>', 'html'),
    -- CS410 (wajih)
    (7, 2, 'Welcome to ML',            '<p>Math-heavy course. Comfortable with linear algebra and probability is required. We''ll use Python + scikit-learn.</p>', 'html'),
    (7, 2, 'Dataset access',           '<p>Course datasets are on the shared drive. Do <strong>not</strong> commit them to your repo — they''re too large.</p>', 'html'),
    -- CS220 (ali)
    (8, 1, 'Welcome to Web Dev',       '<p>Build something you''d show your friends. The final project is open-ended — pick a topic you care about.</p>', 'html'),
    (8, 1, 'Frameworks decision',      '<p>For the final project, you can pick React, Vue, or Svelte. No framework is also fine if you want to flex vanilla JS.</p>', 'html'),
    -- CS999 (wajih)
    (9, 2, 'Welcome to Software Architecture', '<p>This is a senior-level course. Expect heavy reading, debate, and few right answers. Ship trade-offs, not silver bullets.</p>', 'html'),
    (9, 2, 'Reading list',             '<p>Required: <em>Designing Data-Intensive Applications</em> by Martin Kleppmann. Recommended: <em>Building Microservices</em> by Sam Newman.</p>', 'html'),
    -- CS340 (ali)
    (10, 1, 'Welcome to Mobile Dev',   '<p>We''ll build one Android app and one iOS app from scratch. Bring your laptops to every lecture.</p>', 'html'),
    (10, 1, 'Device emulators',        '<p>Android Studio + Xcode required. iOS folks need a Mac (or partner with someone). No physical device required for grading.</p>', 'html');

-- Announcement attachment (link) on the ER Diagram tips post (announcement id=2)
INSERT INTO announcement_attachments (announcement_id, file_name, link_url, is_link) VALUES
    (2, 'ER Diagram Notation Guide', 'https://www.lucidchart.com/pages/er-diagrams', TRUE),
    (8, 'Big-O Cheat Sheet',         'https://www.bigocheatsheet.com/',              TRUE),
    (15, 'Subnetting Practice',      'https://subnettingpractice.com/',               TRUE),
    (21, 'DDIA Book',                'https://dataintensive.net/',                    TRUE);

-- ── Assignments ───────────────────────────────────────────────
-- IDs 1..3 = CS301
INSERT INTO assignments (course_id, created_by, title, description, total_points, late_submissions_allowed, deadline) VALUES
    (1, 1, 'ER Diagram Design',       'Design a complete ER diagram for a hospital management system. Include entities, relationships, cardinality, and participation constraints. Export as PDF.', 20, TRUE,  NOW() + INTERVAL '7 days'),
    (1, 1, 'SQL Query Lab',           'Write 10 SQL queries against the provided university schema. Cover SELECT, JOIN, GROUP BY, HAVING, and subqueries. Submit as a .sql file.',                30, FALSE, NOW() + INTERVAL '14 days'),
    (1, 1, 'Normalization Exercise',  'Given the unnormalized relation R(A,B,C,D,E,F), identify all functional dependencies and decompose to 3NF. Show every step.',                              25, FALSE, NOW() - INTERVAL '3 days'),

-- IDs 4..6 = CS401
    (2, 1, 'Use Case Diagrams',       'Draw use case diagrams for the Naawbi LMS covering authentication, course management, and assignment submission. Use UML notation.',                       15, TRUE,  NOW() + INTERVAL '5 days'),
    (2, 1, 'Sprint 1 Backlog',        'Submit your team sprint backlog with user stories, acceptance criteria, and story point estimates. Use the provided Jira template.',                      20, TRUE,  NOW() + INTERVAL '10 days'),
    (2, 1, 'Design Patterns Report',  'Identify 3 design patterns used in the Naawbi codebase. For each: name, category, where it appears, and why it was chosen.',                               30, FALSE, NOW() - INTERVAL '1 day'),

-- IDs 7..9 = CS201 (wajih)
    (3, 2, 'Linked List Implementation', 'Implement a doubly-linked list in Java with insert/delete/find operations. Include unit tests covering edge cases (empty list, single element, duplicates).', 25, TRUE,  NOW() + INTERVAL '4 days'),
    (3, 2, 'Binary Search Tree',         'Implement a BST with insertion, deletion, in-order/pre-order/post-order traversal, and a height() method. Submit code + a brief complexity analysis.',         30, FALSE, NOW() + INTERVAL '12 days'),
    (3, 2, 'Sorting Algorithms',         'Compare merge sort, quick sort, and heap sort on arrays of size 10³, 10⁴, and 10⁵. Plot runtimes. Discuss best/worst/average case.',                            20, TRUE,  NOW() - INTERVAL '2 days'),

-- IDs 10..12 = CS101 (wajih)
    (4, 2, 'Hello World Variations',  'Write 5 versions of Hello World demonstrating: variables, if/else, loops, methods, and a basic class.',                                                     10, TRUE,  NOW() + INTERVAL '2 days'),
    (4, 2, 'Fibonacci Sequence',      'Write three implementations of Fibonacci: recursive, iterative, and memoized. Compare runtimes for n=30.',                                                  15, TRUE,  NOW() + INTERVAL '8 days'),
    (4, 2, 'Calculator Class',        'Build a Calculator class with add/subtract/multiply/divide. Handle divide-by-zero. Write at least 6 unit tests with JUnit.',                                20, TRUE,  NOW() - INTERVAL '5 days'),

-- IDs 13..14 = CS501 (ali)
    (5, 1, 'Process Scheduling Sim',  'Simulate FCFS, SJF, and Round Robin schedulers on a given workload. Output Gantt chart, average waiting time, average turnaround time.',                    25, FALSE, NOW() + INTERVAL '6 days'),
    (5, 1, 'Memory Management',       'Implement first-fit, best-fit, and worst-fit memory allocators. Measure fragmentation. Submit code + 1-page analysis.',                                     30, TRUE,  NOW() - INTERVAL '4 days'),

-- IDs 15..16 = CS305 (wajih)
    (6, 2, 'Subnetting Worksheet',    'Subnet 192.168.1.0/24 into 4 equal subnets. List network address, broadcast, host range, and subnet mask for each.',                                        15, TRUE,  NOW() + INTERVAL '3 days'),
    (6, 2, 'TCP vs UDP Lab',          'Capture and analyze TCP and UDP traffic with Wireshark. Identify the 3-way handshake and explain when UDP is preferred.',                                   20, TRUE,  NOW() + INTERVAL '11 days'),

-- IDs 17..18 = CS410 (wajih)
    (7, 2, 'Linear Regression',       'Train a linear regression model on the Boston Housing dataset. Report MSE, R², and discuss feature importance. Submit notebook.',                          25, FALSE, NOW() + INTERVAL '9 days'),
    (7, 2, 'Neural Network From Scratch', 'Implement a 2-layer NN in NumPy (no PyTorch/TF). Train on MNIST. Submit code + accuracy plot.',                                                          35, FALSE, NOW() - INTERVAL '6 days'),

-- IDs 19..20 = CS220 (ali)
    (8, 1, 'Personal Portfolio Page', 'Build a single-page portfolio with HTML + CSS only (no JS). Must be responsive (mobile + desktop). Deploy to GitHub Pages.',                                15, TRUE,  NOW() + INTERVAL '5 days'),
    (8, 1, 'JS To-Do App',            'Build a vanilla JS to-do app: add/edit/delete/complete tasks. State must persist via localStorage.',                                                        25, TRUE,  NOW() + INTERVAL '13 days'),

-- IDs 21..22 = CS999 (wajih)
    (9, 2, 'ADR Writeup',             'Write an Architecture Decision Record for a real choice in a real project (yours or open source). Use the standard ADR template.',                          20, TRUE,  NOW() + INTERVAL '7 days'),
    (9, 2, 'Microservices Trade-offs', 'Pick a monolith and propose a decomposition into 3-5 microservices. Identify boundaries, data ownership, failure modes.',                                  30, FALSE, NOW() - INTERVAL '1 day'),

-- IDs 23..24 = CS340 (ali)
    (10, 1, 'Android Hello World App', 'Build an Android app with a button that displays a Toast when clicked. Submit APK + screenshot.',                                                          10, TRUE,  NOW() + INTERVAL '4 days'),
    (10, 1, 'Counter App',            'Build a counter app with +/- buttons. State must persist across rotation. Submit code + APK.',                                                              20, TRUE,  NOW() + INTERVAL '12 days');

-- ── Submissions ───────────────────────────────────────────────
-- Mix of: graded, submitted-not-graded, late, missing
-- (status values: 'Submitted', 'Late')

-- CS301 — assignment ids 1,2,3
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at, grade, feedback) VALUES
    -- ER Diagram (upcoming)
    (1, 3, TRUE, 'Submitted', NOW() - INTERVAL '1 day', NULL, NULL),                                                        -- ibbi: ungraded
    (1, 4, TRUE, 'Submitted', NOW() - INTERVAL '2 days', 18, 'Clean diagram. Cardinality on the appointment relationship should be 1..N, not 0..N. -2 pts.'),  -- sara: graded
    -- SQL Query Lab (upcoming)
    (2, 5, TRUE, 'Submitted', NOW() - INTERVAL '6 hours', NULL, NULL),                                                      -- fatima: ungraded
    -- Normalization (past due)
    (3, 3, TRUE, 'Late',      NOW() - INTERVAL '6 hours', 19, 'Solid decomposition — 3NF achieved correctly. Minor issue: F→D not justified. -1 pt.'),  -- ibbi: late + graded
    (3, 4, TRUE, 'Submitted', NOW() - INTERVAL '4 days', 24, 'Excellent work. All FDs identified. Small note: rename relation in step 4 to match step 2.'),  -- sara: graded
    (3, 5, TRUE, 'Submitted', NOW() - INTERVAL '5 days', 22, 'Good attempt. Step 3 missing transitive FD elimination. -3 pts.');                              -- fatima: graded
    -- ibbi did not submit assignment 2; omar did nothing in CS301 (Missing for past-due 3)

-- CS401 — assignment ids 4,5,6
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at, grade, feedback) VALUES
    -- Use Case (upcoming)
    (4, 3, TRUE, 'Submitted', NOW() - INTERVAL '3 days', 13, 'Good coverage. Missing the "resubmit" extend on the submission use case. -2 pts.'),  -- ibbi
    (4, 7, TRUE, 'Submitted', NOW() - INTERVAL '1 day',  NULL, NULL),                                                                              -- zara: ungraded
    -- Sprint Backlog (upcoming)
    (5, 4, TRUE, 'Submitted', NOW() - INTERVAL '2 days', NULL, NULL),                                                                              -- sara: ungraded
    -- Design Patterns (past due)
    (6, 3, TRUE, 'Submitted', NOW() - INTERVAL '2 days', NULL, NULL),                                                                              -- ibbi: ungraded
    (6, 4, TRUE, 'Late',      NOW() - INTERVAL '12 hours', NULL, NULL),                                                                            -- sara: late + ungraded
    (6, 8, TRUE, 'Submitted', NOW() - INTERVAL '3 days', 27, 'Strong analysis. Singleton justification could be stronger. -3 pts.');               -- hassan: graded
    -- zara missed Design Patterns

-- CS201 — assignment ids 7,8,9
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at, grade, feedback) VALUES
    -- Linked List (upcoming)
    (7, 3, TRUE, 'Submitted', NOW() - INTERVAL '6 hours', NULL, NULL),                                                                            -- ibbi
    (7, 5, TRUE, 'Submitted', NOW() - INTERVAL '1 day',  23, 'Tests cover edge cases well. delete() leaks the tail node when list size is 2. -2.'),  -- fatima: graded
    (7, 6, TRUE, 'Submitted', NOW() - INTERVAL '2 days', NULL, NULL),                                                                             -- omar
    -- BST (upcoming)
    (8, 7, TRUE, 'Submitted', NOW() - INTERVAL '4 hours', NULL, NULL),                                                                            -- zara
    -- Sorting (past due)
    (9, 5, TRUE, 'Submitted', NOW() - INTERVAL '3 days', 19, 'Plots are clear. Best-case discussion of quicksort missed the partition strategy.'),  -- fatima: graded
    (9, 6, TRUE, 'Late',      NOW() - INTERVAL '8 hours', 16, 'Late submission accepted. Heap sort runtime curve is wrong — recheck timing harness.'),  -- omar: late + graded
    (9, 8, TRUE, 'Submitted', NOW() - INTERVAL '4 days', 20, 'Excellent comparison. Full marks.');                                                 -- hassan
    -- ibbi, zara missed Sorting

-- CS101 — assignment ids 10,11,12
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at, grade, feedback) VALUES
    -- Hello World (upcoming)
    (10, 5, TRUE, 'Submitted', NOW() - INTERVAL '8 hours',  10, 'Perfect. All 5 variations work.'),                                               -- fatima
    (10, 6, TRUE, 'Submitted', NOW() - INTERVAL '1 day',    9,  'Great. The if/else version uses == on Strings instead of .equals(). -1 pt.'),    -- omar
    (10, 7, TRUE, 'Submitted', NOW() - INTERVAL '2 days',  NULL, NULL),                                                                            -- zara
    (10, 8, TRUE, 'Submitted', NOW() - INTERVAL '3 hours', NULL, NULL),                                                                            -- hassan
    -- Fibonacci (upcoming)
    (11, 5, TRUE, 'Submitted', NOW() - INTERVAL '12 hours', NULL, NULL),                                                                           -- fatima
    -- Calculator (past due)
    (12, 6, TRUE, 'Submitted', NOW() - INTERVAL '6 days',  18, 'Clean class design. 5 of 6 tests pass. Edge case for 0/0 misses NaN handling.'),   -- omar
    (12, 7, TRUE, 'Late',      NOW() - INTERVAL '4 days',  15, 'Late by 1 day. -3 pts. Otherwise solid.'),                                         -- zara: late + graded
    (12, 8, TRUE, 'Submitted', NOW() - INTERVAL '5 days',  20, 'Full marks. All edge cases handled.');                                             -- hassan

-- CS501 — assignment ids 13,14
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at, grade, feedback) VALUES
    -- Process Scheduling (upcoming)
    (13, 3, TRUE, 'Submitted', NOW() - INTERVAL '1 day',  NULL, NULL),                                                                             -- ibbi
    (13, 6, TRUE, 'Submitted', NOW() - INTERVAL '4 hours', 23, 'Round Robin quantum chosen poorly. Consider context-switch overhead. -2 pts.'),    -- omar
    -- Memory Management (past due)
    (14, 3, TRUE, 'Submitted', NOW() - INTERVAL '5 days',  28, 'Strong implementation. Best-fit fragmentation analysis is excellent.'),           -- ibbi
    (14, 4, TRUE, 'Late',      NOW() - INTERVAL '3 days',  25, 'Late but well done. -3 pts. Worst-fit results match expectations.'),               -- sara: late
    (14, 6, TRUE, 'Submitted', NOW() - INTERVAL '6 days',  NULL, NULL);                                                                            -- omar: ungraded

-- CS305 — assignment ids 15,16
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at, grade, feedback) VALUES
    -- Subnetting (upcoming)
    (15, 4, TRUE, 'Submitted', NOW() - INTERVAL '6 hours', 14, 'Network/broadcast addresses correct. Host range off-by-one in subnet 3. -1 pt.'),  -- sara
    (15, 5, TRUE, 'Submitted', NOW() - INTERVAL '1 day',  NULL, NULL),                                                                             -- fatima
    (15, 7, TRUE, 'Submitted', NOW() - INTERVAL '2 days', 15, 'Full marks.');                                                                      -- zara

-- CS410 — assignment ids 17,18
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at, grade, feedback) VALUES
    -- Linear Regression (upcoming)
    (17, 3, TRUE, 'Submitted', NOW() - INTERVAL '1 day',  NULL, NULL),                                                                             -- ibbi
    (17, 5, TRUE, 'Submitted', NOW() - INTERVAL '8 hours', 22, 'MSE/R² reported correctly. Feature importance analysis is shallow. -3 pts.'),       -- fatima
    -- Neural Net (past due)
    (18, 3, TRUE, 'Submitted', NOW() - INTERVAL '7 days',  31, 'Excellent — 92% on MNIST. Backprop derivation in the writeup is missing. -4 pts.'), -- ibbi
    (18, 5, TRUE, 'Late',      NOW() - INTERVAL '5 days',  28, 'Late by 1 day. -7 pts. Code quality is strong.'),                                  -- fatima: late
    (18, 8, TRUE, 'Submitted', NOW() - INTERVAL '6 days',  NULL, NULL);                                                                            -- hassan: ungraded

-- CS220 — assignment ids 19,20
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at, grade, feedback) VALUES
    -- Portfolio (upcoming)
    (19, 4, TRUE, 'Submitted', NOW() - INTERVAL '2 days', 14, 'Looks great on desktop. Mobile breakpoints break below 380px. -1 pt.'),             -- sara
    (19, 6, TRUE, 'Submitted', NOW() - INTERVAL '1 day',  NULL, NULL),                                                                             -- omar
    (19, 7, TRUE, 'Submitted', NOW() - INTERVAL '4 hours', NULL, NULL);                                                                             -- zara

-- CS999 — assignment ids 21,22
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at, grade, feedback) VALUES
    -- ADR (upcoming)
    (21, 3, TRUE, 'Submitted', NOW() - INTERVAL '12 hours', NULL, NULL),                                                                            -- ibbi
    (21, 4, TRUE, 'Submitted', NOW() - INTERVAL '1 day',   18, 'Format correct. Consequences section is too short — what trade-offs did you accept?'),  -- sara
    -- Microservices (past due)
    (22, 3, TRUE, 'Submitted', NOW() - INTERVAL '2 days',  27, 'Solid decomposition. Saga pattern would help with order/payment consistency. -3 pts.'),  -- ibbi
    (22, 4, TRUE, 'Late',      NOW() - INTERVAL '6 hours',  NULL, NULL);                                                                            -- sara: late + ungraded

-- CS340 — assignment ids 23,24
INSERT INTO submissions (assignment_id, user_id, submitted, status, submitted_at, grade, feedback) VALUES
    -- Android Hello (upcoming)
    (23, 5, TRUE, 'Submitted', NOW() - INTERVAL '8 hours', 10, 'Full marks.'),                                                                     -- fatima
    (23, 6, TRUE, 'Submitted', NOW() - INTERVAL '1 day',  NULL, NULL),                                                                             -- omar
    (23, 8, TRUE, 'Submitted', NOW() - INTERVAL '2 hours', NULL, NULL);                                                                            -- hassan

-- ── Summary ───────────────────────────────────────────────────
SELECT 'users'             AS table_name, count(*) AS rows FROM users
UNION ALL
SELECT 'courses',            count(*) FROM courses
UNION ALL
SELECT 'enrollments',        count(*) FROM course_enrollments
UNION ALL
SELECT 'announcements',      count(*) FROM announcements
UNION ALL
SELECT 'announcement_attachments', count(*) FROM announcement_attachments
UNION ALL
SELECT 'assignments',        count(*) FROM assignments
UNION ALL
SELECT 'submissions',        count(*) FROM submissions
ORDER BY 1;
