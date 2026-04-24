# Home Screen Grid Redesign — Design Spec

## Overview

Replace the current two-panel CourseCatalogView home screen with a Google Classroom-style course card grid. Each card represents one course the logged-in user is enrolled in or teaches. Clicking a card navigates to the existing course detail view (CourseCatalogView). A back button in the course detail returns to the grid.

---

## Navigation Architecture

**Before:** `Main.java` loads `CourseCatalogView.fxml` directly after login (in DEV_MODE it bypasses login entirely).

**After:**
1. `Main.java` loads `HomeView.fxml` after login.
2. User sees the card grid.
3. Clicking a card loads `CourseCatalogView.fxml`, calls `controller.initWithCourse(course)`, and swaps the scene root via `stage.getScene().setRoot(root)`.
4. A back button in the `CourseCatalogView` header loads `HomeView.fxml` and swaps back.

No routing abstraction is introduced — the same `stage.getScene().setRoot(root)` pattern already used in `handleLogout()` is reused for all transitions.

---

## Files

### Created

| File | Purpose |
|------|---------|
| `src/naawbi/view/Home/HomeView.fxml` | Root layout: top bar + scroll area |
| `src/naawbi/view/Home/HomeView.css` | Card and grid styles |
| `src/naawbi/controller/HomeController.java` | Loads courses, builds card grid, handles navigation |

### Modified

| File | Change |
|------|--------|
| `src/naawbi/Main.java` | Load `HomeView.fxml` instead of `CourseCatalogView.fxml` after login |
| `src/naawbi/model/Course.java` | Add `fetchByUserIdWithInstructor(int userId)` and `fetchByCode(String code)` |
| `src/naawbi/view/CourseCatalog/CourseCatalogView.fxml` | Add `fx:id="backButton"` to header |
| `src/naawbi/controller/CourseCatalogController.java` | Add `initWithCourse(Course)` setter and `handleBack()` method |

---

## Data Layer

### `Course.fetchByUserIdWithInstructor(int userId)`

New static method. Returns `List<Object[]>` where each row is:
`[Course course, String instructorName, String userRole]`

SQL joins `courses`, `course_enrollments`, and `users` to find:
- The instructor: the user in `course_enrollments` with `role = 'instructor'` for that course
- The logged-in user's role in that course (defaults to `'instructor'` via COALESCE for courses where the user is `created_by` but has no enrollment row)

```sql
SELECT c.id, c.course_name, c.course_code, c.description, c.is_active,
       u.username AS instructor_name,
       COALESCE(ce_me.role, 'instructor') AS my_role
FROM courses c
LEFT JOIN course_enrollments ce_me ON c.id = ce_me.course_id AND ce_me.user_id = ?
LEFT JOIN course_enrollments ce_inst ON c.id = ce_inst.course_id AND ce_inst.role = 'instructor'
LEFT JOIN users u ON ce_inst.user_id = u.id
WHERE (c.id IN (SELECT course_id FROM course_enrollments WHERE user_id = ?)
   OR c.created_by = ?)
ORDER BY c.id DESC
```

### `Course.fetchByCode(String code)`

Returns `Optional<Course>` by looking up `course_code` (case-insensitive). Used by the Join Course dialog.

```sql
SELECT id, course_name, course_code, description, is_active
FROM courses WHERE UPPER(course_code) = UPPER(?)
```

### Join enrollment

When a student joins by code, `HomeController` executes:
```sql
INSERT OR IGNORE INTO course_enrollments (user_id, course_id, role) VALUES (?, ?, 'student')
```

---

## HomeView Layout

### FXML structure (`HomeView.fxml`)

```
BorderPane (root, stylesheets: HomeView.css)
├── top: HBox.top-bar
│   ├── Label "Naawbi"  (styleClass: app-title)
│   └── HBox (alignment: CENTER_RIGHT)
│       └── Button (fx:id="actionButton")   ← "Create Course" or "Join Course"
└── center: ScrollPane (fitToWidth=true)
    └── FlowPane (fx:id="courseGrid", hgap=16, vgap=16)
```

The `FlowPane` is used with a fixed `prefWrapLength` set programmatically to produce 4 columns. Cards are built entirely in `HomeController.java` — no static children in FXML.

### Card dimensions

Each card is a `VBox` with `prefWidth = (availableWidth - 3*gap) / 4`. The available width is computed once after the scene is laid out using `courseGrid.widthProperty().addListener(...)`.

Alternatively, set a fixed card width of 260px (works well at 1280px+). Given the app targets 1920×1080, use 280px fixed width.

---

## Card Structure (built in code)

```
VBox.course-card  (prefWidth=280, styleClass="course-card")
├── AnchorPane.card-header  (prefHeight=90, background=PALETTE[id%6])
│   ├── Label course.getName()       (styleClass="card-course-name", bottom-left anchor)
│   ├── Label course.getCode()       (styleClass="card-course-code", below name)
│   └── Button "⋮"                   (styleClass="card-menu-btn", top-right anchor)
├── AnchorPane.card-body  (prefHeight=52)
│   ├── Label instructorName          (styleClass="card-instructor", left-center)
│   └── Label initial                 (styleClass="card-avatar", top-right, overlaps header)
└── HBox.card-footer
    └── (placeholder, empty for now — reserved for future quick-action icons)
```

The circular avatar `Label` uses CSS `border-radius: 50%` and the same PALETTE color as the header. It is positioned with a negative top margin (`-19px`) to overlap the header/body boundary.

### Color palette

```java
static final String[] PALETTE = {
    "#4b5d4f", "#db7706", "#7b5ec4",
    "#3b7fc4", "#2a7a6b", "#c4453b"
};
```

Color is `PALETTE[course.getId() % PALETTE.length]` — deterministic, same course always same color.

---

## HomeController Responsibilities

1. On `initialize()`:
   - Call `Course.fetchByUserIdWithInstructor(Session.getUserId())`
   - Build one card `VBox` per course and add to `courseGrid`
   - Set `actionButton` text to "Create Course" (instructor) or "Join Course" (student) based on `Session.isInstructor()`

2. `handleActionButton()`:
   - Instructor → opens existing `CreateCourseView.fxml` as a modal (same as current CourseCatalogController does)
   - Student → opens `Join Course` dialog (see below), refreshes grid on success

3. `handleCardClick(Course course, MouseEvent event)`:
   - Loads `CourseCatalogView.fxml`
   - Gets `CourseCatalogController` from loader
   - Calls `controller.initWithCourse(course)`
   - `stage.getScene().setRoot(root)`

### Join Course dialog

Uses `TextInputDialog`:
- Header: "Join a Course"
- Content label: "Enter the course code:"
- On confirm: call `Course.fetchByCode(code)`
  - Not found → show `Alert.ERROR` "No course found with that code."
  - Already enrolled → show `Alert.INFORMATION` "You are already enrolled."
  - Success → INSERT enrollment, refresh grid

---

## CourseCatalogController Changes

### `initWithCourse(Course course)`

New public method. Called by `HomeController` immediately after loading the FXML. Sets:
```java
this.currentSelectedCourse = course;
this.currentCourseId = course.getId();
if (homeCourseTitle != null) homeCourseTitle.setText(course.getName());
this.toDoController = null;
highlightTab(streamTab);
showStream();   // loads announcements for the course
```
Skips the card-deselection UI from `handleCourseSelect()` since there is no sidebar card list when entering from HomeView.

### `handleBack(ActionEvent event)`

```java
Parent homeRoot = FXMLLoader.load(getClass().getResource("/naawbi/view/Home/HomeView.fxml"));
Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
stage.getScene().setRoot(homeRoot);
```

### FXML addition

A back button is added to the left side of the existing top-bar `HBox` in `CourseCatalogView.fxml`:
```xml
<Button fx:id="backButton" text="← Back" onAction="#handleBack" styleClass="back-button" />
```

---

## CSS (`HomeView.css`)

All properties use JavaFX CSS syntax (`-fx-` prefix). Key rules:

```css
.top-bar {
    -fx-padding: 12 24 12 24;
    -fx-background-color: #4b5d4f;
    -fx-alignment: center-left;
    -fx-spacing: 12;
}
.app-title { -fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white; }
.action-button {
    -fx-background-color: white;
    -fx-text-fill: #4b5d4f;
    -fx-font-weight: bold;
    -fx-background-radius: 6px;
    -fx-cursor: hand;
}

.course-card {
    -fx-background-color: white;
    -fx-background-radius: 8px;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 6, 0, 0, 2);
    -fx-cursor: hand;
}
.course-card:hover {
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 10, 0, 0, 4);
}

.card-course-name { -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white; }
.card-course-code { -fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.8); }
.card-menu-btn {
    -fx-background-color: transparent;
    -fx-text-fill: rgba(255,255,255,0.85);
    -fx-font-size: 18px;
    -fx-padding: 0;
    -fx-cursor: hand;
}

/* Avatar: circle achieved via -fx-background-radius equal to half of min-width */
.card-avatar {
    -fx-background-radius: 19px;
    -fx-min-width: 38px;
    -fx-min-height: 38px;
    -fx-max-width: 38px;
    -fx-max-height: 38px;
    -fx-font-size: 14px;
    -fx-font-weight: bold;
    -fx-text-fill: white;
    -fx-alignment: center;
}
.card-instructor { -fx-font-size: 11px; -fx-text-fill: #666666; }
.card-footer { -fx-border-color: #f0ece4; -fx-border-width: 1 0 0 0; -fx-padding: 6 12 6 12; }
```

---

## Out of Scope

- The three-dot card menu actions (archive, leave) — button renders but does nothing
- The card footer quick-action icons — footer region reserved but empty
- Course color stored in DB — colors are always derived from `courseId % 6`
- Search / filter on the home screen
- Pagination (all courses load at once)
