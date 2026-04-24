package naawbi.controller;

import naawbi.model.Announcement;
import naawbi.model.AnnouncementAttachment;
import naawbi.model.Course;
import naawbi.model.Session;
import naawbi.view.ViewConstants;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for CourseCatalogView.fxml
 *
 * Responsibilities:
 * 1. Inject ViewConstants color variables onto rootPane so all -vc-*
 * CSS looked-up colors cascade throughout the scene (same pattern
 * as CreateCourseController).
 * 2. Wire up the course card selection: highlight the selected card
 * and (in a real implementation) swap the Course Home panel content.
 * 3. Wire the search field for live catalog filtering.
 * 4. Dynamically load courses from the database.
 */
public class CourseCatalogController implements Initializable {

    /* ── Root (color injection) ─────────────────────── */
    @FXML
    private StackPane rootPane;

    /* ── Catalog ────────────────────────────────────── */
    @FXML
    private Label courseCountLabel;
    @FXML
    private Button createCourseBtn;
    @FXML
    private TextField searchField;
    @FXML
    private VBox courseListContainer;

    /* ── Stream ─────────────────────────────────────── */
    @FXML
    private VBox streamContainer;
    @FXML
    private Label homeCourseTitle;
    @FXML
    private Label homeCourseCode;
    @FXML
    private Label homeCourseMeta;

    /* ── Tabs ────────────────────────────────────────── */
    @FXML
    private Label streamTab;
    @FXML
    private Label assignmentsTab;
    @FXML
    private Label peopleTab;
    @FXML
    private Label todoTab;
    @FXML
    private Label gradesTab;

    /* ── Content containers ──────────────────────────── */
    @FXML
    private HBox contentHBox;
    @FXML
    private VBox todoContainer;

    /* ── Buttons ─────────────────────────────────────── */
    @FXML
    private Button createAssignmentBtn;
    @FXML
    private Button postAnnouncementBtn;

    /* ── State ───────────────────────────────────────── */
    private Course currentSelectedCourse;
    private int currentCourseId = -1;
    private int currentUserId;
    private ToDoController toDoController;

    /* ── Entry point (called by HomeController after load) ──────────── */

    public void initWithCourse(Course course) {
        currentSelectedCourse = course;
        currentCourseId = course.getId();
        if (homeCourseTitle != null) homeCourseTitle.setText(course.getName());
        if (homeCourseCode != null) homeCourseCode.setText(course.getCode());
        if (homeCourseMeta != null) homeCourseMeta.setText("");
        toDoController = null;
        highlightTab(streamTab);
        showStream();
    }

    /* ── Lifecycle ───────────────────────────────────── */

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        injectColorConstants();
        currentUserId = Session.getInstance().getUserId();
        applyRoleGating();
        wireSearch();
        setupTabSwitching();
        loadCoursesFromDB();
    }

    /**
     * Loads all courses from the database and populates the catalog list.
     */
    private void setupTabSwitching() {
        // Stream tab (default)
        streamTab.setOnMouseClicked(event -> {
            showStream();
            highlightTab(streamTab);
        });

        // Assignments tab
        assignmentsTab.setOnMouseClicked(event -> {
            showAssignments();
            highlightTab(assignmentsTab);
        });

        // People tab
        peopleTab.setOnMouseClicked(event -> {
            showPeople();
            highlightTab(peopleTab);
        });

        // To-Do tab
        todoTab.setOnMouseClicked(event -> {
            showToDo();
            highlightTab(todoTab);
        });

        // Grades tab
        gradesTab.setOnMouseClicked(event -> {
            highlightTab(gradesTab);
            showGrades();
        });
    }

    /**
     * Loads all courses from the database and populates the catalog list.
     */
    private void loadCoursesFromDB() {
        courseListContainer.getChildren().clear();
        try {
            List<Course> courses = Course.fetchByUserId(currentUserId);
            for (Course course : courses) {
                VBox card = createCourseCard(course);
                courseListContainer.getChildren().add(card);
            }
            courseCountLabel.setText(courses.size() + " active courses");
        } catch (SQLException e) {
            e.printStackTrace();
            courseCountLabel.setText("Error loading courses");
        }
    }

    /**
     * Programmatically creates a course card VBox mirroring the FXML structure.
     */
    private VBox createCourseCard(Course course) {
        VBox card = new VBox();
        card.getStyleClass().add("course-card");
        card.setSpacing(0);
        card.setOnMouseClicked(event -> handleCourseSelect(event, course));

        // Accent bar
        HBox accent = new HBox();
        accent.getStyleClass().addAll("course-card-accent", "course-accent-primary");

        // Body
        VBox body = new VBox();
        body.getStyleClass().add("course-card-body");
        body.setSpacing(6);

        // Header row (Code + Role)
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setSpacing(8);

        Label codeLabel = new Label(course.getCode() != null ? course.getCode() : "N/A");
        codeLabel.getStyleClass().add("course-code-tag");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label roleLabel = new Label("Instructor");
        roleLabel.getStyleClass().add("role-badge");

        header.getChildren().addAll(codeLabel, spacer, roleLabel);

        // Name
        Label nameLabel = new Label(course.getName());
        nameLabel.getStyleClass().add("course-card-name");
        nameLabel.setWrapText(true);

        // Meta (Instructor + Students)
        Label metaLabel = new Label("You · 0 students");
        metaLabel.getStyleClass().add("course-card-meta");

        // Stats row
        HBox stats = new HBox();
        stats.setSpacing(12);
        stats.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label pendingStat = new Label("📋  0 pending");
        pendingStat.getStyleClass().add("course-stat");

        Label newStat = new Label("🔔  0 new");
        newStat.getStyleClass().add("course-stat");

        stats.getChildren().addAll(pendingStat, newStat);

        body.getChildren().addAll(header, nameLabel, metaLabel, stats);
        card.getChildren().addAll(accent, body);

        return card;
    }

    private void applyRoleGating() {
        boolean isInstructor = Session.getInstance().isInstructor();
        createCourseBtn.setVisible(isInstructor);
        createCourseBtn.setManaged(isInstructor);
        postAnnouncementBtn.setVisible(isInstructor);
        postAnnouncementBtn.setManaged(isInstructor);
        createAssignmentBtn.setVisible(isInstructor);
        createAssignmentBtn.setManaged(isInstructor);
        todoTab.setVisible(!isInstructor);
        todoTab.setManaged(!isInstructor);
    }

    /**
     * Injects ViewConstants hex values as JavaFX CSS looked-up color
     * variables on the root StackPane so every descendant can reference
     * -vc-primary, -vc-secondary, -vc-accent, -vc-background, -vc-text.
     */
    private void injectColorConstants() {
        String style = String.format(
                "-vc-primary:    %s;" +
                        "-vc-secondary:  %s;" +
                        "-vc-accent:     %s;" +
                        "-vc-background: %s;" +
                        "-vc-text:       %s;",
                ViewConstants.PRIMARY_COLOR,
                ViewConstants.SECONDARY_COLOR,
                ViewConstants.ACCENT_COLOR,
                ViewConstants.BACKGROUND_COLOR,
                ViewConstants.TEXT_COLOR);
        rootPane.setStyle(style);
    }

    /**
     * Filters the visible course cards in real-time as the user types.
     * Matches against the card's course-name label text (case-insensitive).
     */
    private void wireSearch() {
        searchField.textProperty().addListener((obs, oldVal, query) -> {
            String q = query == null ? "" : query.trim().toLowerCase();
            for (Node node : courseListContainer.getChildren()) {
                if (!(node instanceof VBox card))
                    continue;
                if (q.isBlank()) {
                    card.setVisible(true);
                    card.setManaged(true);
                    continue;
                }
                // Collect all label text within the card for matching
                String cardText = collectText(card).toLowerCase();
                boolean matches = cardText.contains(q);
                card.setVisible(matches);
                card.setManaged(matches);
            }
        });
    }

    /** Recursively collects all Label text within a VBox for search matching. */
    private String collectText(VBox container) {
        StringBuilder sb = new StringBuilder();
        for (Node n : container.getChildrenUnmodifiable()) {
            if (n instanceof javafx.scene.control.Label lbl) {
                sb.append(lbl.getText()).append(' ');
            } else if (n instanceof VBox inner) {
                sb.append(collectText(inner));
            } else if (n instanceof javafx.scene.layout.HBox hb) {
                for (Node child : hb.getChildrenUnmodifiable()) {
                    if (child instanceof javafx.scene.control.Label lbl) {
                        sb.append(lbl.getText()).append(' ');
                    }
                }
            }
        }
        return sb.toString();
    }

    /* ── Handlers ────────────────────────────────────── */

    /**
     * Shows the Stream (announcements) tab content
     */
    private void showStream() {
        contentHBox.setVisible(true);
        contentHBox.setManaged(true);
        todoContainer.setVisible(false);
        todoContainer.setManaged(false);
        createAssignmentBtn.setVisible(false);
        createAssignmentBtn.setManaged(false);
        if (currentCourseId > 0) {
            loadAnnouncementsForCourse(currentCourseId);
        }
    }

    /**
     * Shows the Assignments tab — loads all assignments for the selected course
     * and renders them as cards in streamContainer with submission status badges.
     * Instructors see a "Grade Submissions" button (opens GradingView — US 19/20).
     */
    private void showAssignments() {
        contentHBox.setVisible(true);
        contentHBox.setManaged(true);
        todoContainer.setVisible(false);
        todoContainer.setManaged(false);

        boolean isInstructor = Session.getInstance().isInstructor();
        createAssignmentBtn.setVisible(isInstructor);
        createAssignmentBtn.setManaged(isInstructor);

        if (currentCourseId <= 0) {
            streamContainer.getChildren().clear();
            Label empty = new Label("Select a course first.");
            empty.getStyleClass().add("empty-state-label");
            streamContainer.getChildren().add(empty);
            return;
        }
        loadAssignmentsForCourse(currentCourseId, currentUserId, isInstructor);
    }

    /**
     * Shows the People tab — loads the course roster and renders role-badged cards.
     */
    private void showPeople() {
        contentHBox.setVisible(true);
        contentHBox.setManaged(true);
        todoContainer.setVisible(false);
        todoContainer.setManaged(false);
        createAssignmentBtn.setVisible(false);
        createAssignmentBtn.setManaged(false);

        streamContainer.getChildren().clear();

        if (currentCourseId <= 0) {
            Label empty = new Label("Select a course to see its roster.");
            empty.getStyleClass().add("empty-state-label");
            streamContainer.getChildren().add(empty);
            return;
        }

        try {
            java.util.List<Object[]> users = Course.fetchEnrolledUsers(currentCourseId);
            if (users.isEmpty()) {
                Label empty = new Label("No enrolled users yet.");
                empty.getStyleClass().add("empty-state-label");
                streamContainer.getChildren().add(empty);
                return;
            }
            Label instructorsHeader = new Label("Instructors");
            instructorsHeader.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#3c4043;-fx-padding:8 0 4 0;");
            VBox instructorsList = new VBox(6);
            Label studentsHeader = new Label("Students");
            studentsHeader.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#3c4043;-fx-padding:16 0 4 0;");
            VBox studentsList = new VBox(6);
            boolean hasInstructors = false, hasStudents = false;
            for (Object[] row : users) {
                String username = (String) row[0];
                String email    = (String) row[1];
                String role     = (String) row[2];
                HBox card = new HBox(12);
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                card.setPadding(new Insets(10, 14, 10, 14));
                card.setStyle("-fx-background-color:white;-fx-background-radius:8;" +
                    "-fx-border-color:#e0e0e0;-fx-border-radius:8;");
                Label avatar = new Label(username.substring(0, 1).toUpperCase());
                avatar.setStyle("-fx-min-width:38;-fx-min-height:38;-fx-max-width:38;-fx-max-height:38;" +
                    "-fx-background-radius:19;-fx-alignment:center;-fx-font-weight:bold;" +
                    "-fx-text-fill:white;-fx-background-color:" +
                    ("instructor".equalsIgnoreCase(role) ? "#5c6bc0" : "#26a69a") + ";");
                VBox info = new VBox(2);
                Label nameLbl = new Label(username);
                nameLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#202124;");
                Label emailLbl = new Label(email);
                emailLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#666;");
                info.getChildren().addAll(nameLbl, emailLbl);
                HBox.setHgrow(info, Priority.ALWAYS);
                Label roleBadge = new Label(role.substring(0, 1).toUpperCase() + role.substring(1));
                roleBadge.setStyle("-fx-font-size:11px;-fx-padding:3 10;-fx-background-color:" +
                    ("instructor".equalsIgnoreCase(role) ? "#e8eafd" : "#e0f2f1") +
                    ";-fx-text-fill:" + ("instructor".equalsIgnoreCase(role) ? "#3949ab" : "#00796b") +
                    ";-fx-background-radius:10;-fx-font-weight:bold;");
                card.getChildren().addAll(avatar, info, roleBadge);
                if ("instructor".equalsIgnoreCase(role)) {
                    instructorsList.getChildren().add(card); hasInstructors = true;
                } else {
                    studentsList.getChildren().add(card); hasStudents = true;
                }
            }
            if (hasInstructors) streamContainer.getChildren().addAll(instructorsHeader, instructorsList);
            if (hasStudents) streamContainer.getChildren().addAll(studentsHeader, studentsList);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows the To-Do tab content with pending assignments
     */
    private void showToDo() {
        contentHBox.setVisible(false);
        contentHBox.setManaged(false);
        todoContainer.setVisible(true);
        todoContainer.setManaged(true);
        createAssignmentBtn.setVisible(true);
        createAssignmentBtn.setManaged(true);

        if (currentCourseId > 0) {
            try {
                // Load ToDoView if not already loaded
                if (toDoController == null) {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/naawbi/view/ToDoView.fxml"));
                    Parent todoRoot = loader.load();
                    toDoController = loader.getController();
                    todoContainer.getChildren().clear();
                    todoContainer.getChildren().add(todoRoot);
                }

                // Set context and refresh assignments
                toDoController.setContext(currentCourseId, currentUserId, currentSelectedCourse);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to load To-Do view: " + e.getMessage());
            }
        }
    }

    /**
     * Highlights the active tab by updating CSS classes
     */
    private void highlightTab(Label activeTab) {
        // Remove active class from all tabs
        streamTab.getStyleClass().remove("tab-active");
        assignmentsTab.getStyleClass().remove("tab-active");
        peopleTab.getStyleClass().remove("tab-active");
        todoTab.getStyleClass().remove("tab-active");
        gradesTab.getStyleClass().remove("tab-active");

        // Add active class to the clicked tab
        if (!activeTab.getStyleClass().contains("tab-active")) {
            activeTab.getStyleClass().add("tab-active");
        }
    }

    /**
     * Opens GradebookView as a modal for the current course.
     * Instructor sees the class gradebook (US 21); student sees personal grades (US 22).
     */
    private void showGrades() {
        if (currentCourseId <= 0 || currentSelectedCourse == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Grades");
            alert.setHeaderText("No course selected");
            alert.setContentText("Please select a course first to view its gradebook.");
            alert.showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/naawbi/view/Gradebook/GradebookView.fxml"));
            Parent root = loader.load();
            GradebookController ctrl = loader.getController();
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.UNDECORATED);
            modal.setResizable(true);
            modal.setScene(new Scene(root));
            ctrl.setContext(currentCourseId, currentSelectedCourse.getName(), modal);
            modal.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to open GradebookView: " + e.getMessage());
        }
    }


    /* ── Handlers ────────────────────────────────────── */

    private void denyAccess() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Access Denied");
        alert.setHeaderText("Access Denied");
        alert.setContentText("You do not have permission to perform this action.");
        alert.showAndWait();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent homeRoot = FXMLLoader.load(
                getClass().getResource("/naawbi/view/Home/HomeView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Naawbi");
            stage.getScene().setRoot(homeRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Session.getInstance().logout();
        try {
            Parent loginRoot = FXMLLoader.load(
                getClass().getResource("/naawbi/view/Login/LoginView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Naawbi");
            stage.getScene().setRoot(loginRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNavigateToCreate(ActionEvent event) {
        if (!Session.getInstance().isInstructor()) {
            denyAccess();
            return;
        }
        try {
            Parent createRoot = FXMLLoader
                    .load(getClass().getResource("/naawbi/view/CreateCourse/CreateCourseView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Naawbi - Create Course");
            stage.getScene().setRoot(createRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAssignmentsForCourse(int courseId, int userId, boolean isInstructor) {
        streamContainer.getChildren().clear();
        try {
            java.util.List<naawbi.model.Assignment> assignments =
                naawbi.model.Assignment.fetchWithStatusForUser(courseId, userId);
            if (assignments.isEmpty()) {
                Label emptyLabel = new Label("No assignments yet.");
                emptyLabel.getStyleClass().add("empty-state-label");
                streamContainer.getChildren().add(emptyLabel);
            } else {
                for (naawbi.model.Assignment a : assignments) {
                    VBox card = createAssignmentListCard(a, isInstructor);
                    streamContainer.getChildren().add(card);
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createAssignmentListCard(naawbi.model.Assignment a, boolean isInstructor) {
        VBox card = new VBox();
        card.getStyleClass().add("announcement-card");
        card.setSpacing(8);
        card.setPadding(new Insets(16));

        java.time.format.DateTimeFormatter dtf =
            java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label titleLbl = new Label(a.getTitle());
        titleLbl.getStyleClass().add("announcement-title");
        titleLbl.setWrapText(true);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);
        Label ptsLbl = new Label(a.getTotalPoints() + " pts");
        ptsLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#666;-fx-font-weight:bold;");
        titleRow.getChildren().addAll(titleLbl, ptsLbl);

        HBox metaRow = new HBox(12);
        metaRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label deadlineLbl = new Label("Due: " + a.getDeadline().format(dtf));
        deadlineLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#555;");
        Label lateLbl = new Label(a.isLateSubmissionsAllowed() ? "Late OK" : "Hard deadline");
        lateLbl.setStyle("-fx-font-size:11px;-fx-background-color:" +
            (a.isLateSubmissionsAllowed() ? "#e8f5e9" : "#fce4ec") +
            ";-fx-text-fill:" + (a.isLateSubmissionsAllowed() ? "#388e3c" : "#c62828") +
            ";-fx-padding:2 8;-fx-background-radius:10;");
        metaRow.getChildren().addAll(deadlineLbl, lateLbl);

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        if (isInstructor) {
            Button gradeBtn = new Button("Grade Submissions");
            gradeBtn.setStyle("-fx-background-color:#5c6bc0;-fx-text-fill:white;" +
                "-fx-font-size:12px;-fx-padding:6 14;-fx-background-radius:6;-fx-cursor:hand;");
            gradeBtn.setOnAction(ev -> openGradingDialog(a));
            actionRow.getChildren().add(gradeBtn);
        } else {
            String status = a.getUserStatus();
            if (status == null) status = "Not Submitted";
            Label statusLbl = new Label(status);
            String bgColor = switch (status) {
                case "Submitted" -> "#5cb85c";
                case "Late"      -> "#f0ad4e";
                case "Missing"   -> "#d9534f";
                default          -> "#9e9e9e";
            };
            statusLbl.setStyle("-fx-font-size:12px;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-padding:3 10;-fx-background-color:" + bgColor + ";-fx-background-radius:12;");
            Button viewBtn = new Button("View / Submit");
            viewBtn.setStyle("-fx-background-color:transparent;-fx-border-color:#5c6bc0;" +
                "-fx-border-radius:6;-fx-text-fill:#5c6bc0;-fx-font-size:12px;" +
                "-fx-padding:5 12;-fx-cursor:hand;");
            viewBtn.setOnAction(ev -> openStudentAssignmentDetails(a));
            actionRow.getChildren().addAll(statusLbl, viewBtn);
        }

        card.getChildren().addAll(titleRow, metaRow, actionRow);
        return card;
    }

    private void openStudentAssignmentDetails(naawbi.model.Assignment a) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/naawbi/view/AssignmentDetails/AssignmentDetailsView.fxml"));
            Parent root = loader.load();
            AssignmentDetailsController ctrl = loader.getController();
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.UNDECORATED);
            modal.setResizable(false);
            modal.setScene(new Scene(root));
            ctrl.setContext(a, currentUserId, modal);
            modal.showAndWait();
            loadAssignmentsForCourse(currentCourseId, currentUserId, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens GradingView.fxml as a modal for the given assignment (instructor only).
     * Implements US 19 (view submissions) and US 20 (grade & feedback).
     */
    private void openGradingDialog(naawbi.model.Assignment a) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/naawbi/view/Grading/GradingView.fxml"));
            Parent root = loader.load();
            GradingController ctrl = loader.getController();
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.UNDECORATED);
            modal.setResizable(false);
            modal.setScene(new Scene(root));
            ctrl.setContext(a, modal);
            modal.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to open GradingView: " + e.getMessage());
        }
    }

    /**
     * Loads announcements for the selected course and populates streamContainer
     */
    private void loadAnnouncementsForCourse(int courseId) {

        streamContainer.getChildren().clear();
        try {
            List<Announcement> announcements = Announcement.fetchByCourseId(courseId);
            if (announcements.isEmpty()) {
                Label emptyLabel = new Label("No announcements yet. Be the first to post!");
                emptyLabel.getStyleClass().add("empty-state-label");
                streamContainer.getChildren().add(emptyLabel);
            } else {
                for (Announcement announcement : announcements) {
                    VBox card = createAnnouncementCard(announcement);
                    streamContainer.getChildren().add(card);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label errorLabel = new Label("Error loading announcements");
            errorLabel.getStyleClass().add("error-label");
            streamContainer.getChildren().add(errorLabel);
        }
    }

    /**
     * Creates a VBox announcement card from an Announcement model
     * Displays: author, timestamp, title, content with HTML rendering (basic)
     */
    private VBox createAnnouncementCard(Announcement announcement) {
        VBox card = new VBox();
        card.getStyleClass().add("announcement-card");
        card.setSpacing(8);
        card.setPadding(new Insets(12));

        HBox authorLine = new HBox();
        authorLine.setSpacing(8);
        authorLine.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label authorLabel = new Label(announcement.getAuthorName());
        authorLabel.getStyleClass().add("announcement-author");

        Label timeLabel = new Label(formatTime(announcement.getCreatedAt()));
        timeLabel.getStyleClass().add("announcement-timestamp");

        authorLine.getChildren().addAll(authorLabel, timeLabel);

        Label titleLabel = new Label(announcement.getTitle());
        titleLabel.getStyleClass().addAll("announcement-title");
        titleLabel.setWrapText(true);

        TextFlow contentFlow = new TextFlow();
        contentFlow.getStyleClass().add("announcement-content");
        contentFlow.setPrefWidth(500);

        // Parse simple HTML tags (b, i, u) and render as styled text
        Text contentText = new Text(stripHtmlTags(announcement.getContent()));
        contentText.getStyleClass().add("announcement-content-text");
        contentFlow.getChildren().add(contentText);

        card.getChildren().addAll(authorLine, titleLabel, contentFlow);

        try {
            List<AnnouncementAttachment> attachments = AnnouncementAttachment
                    .fetchByAnnouncementId(announcement.getId());
            if (!attachments.isEmpty()) {
                HBox chipRow = new HBox(6);
                chipRow.setStyle("-fx-padding: 4 0 0 0;");
                for (AnnouncementAttachment att : attachments) {
                    Label chip = new Label((att.isLink() ? "🔗 " : "📎 ") + att.getFileName());
                    chip.getStyleClass().add("attachment-chip");
                    chip.setCursor(Cursor.HAND);
                    chip.setOnMouseClicked(e -> {
                        if (att.isLink())
                            openUrl(att.getLinkUrl());
                        else
                            openFile(att.getFilePath());
                    });
                    chipRow.getChildren().add(chip);
                }
                card.getChildren().add(chipRow);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return card;
    }

    private void openFile(String path) {
        File f = new File(path);
        if (!f.exists()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("File Not Found");
            alert.setContentText("File not found at: " + path);
            alert.showAndWait();
            return;
        }
        try {
            Desktop.getDesktop().open(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper: formats a LocalDateTime to a relative time string
     * Examples: "2 hours ago", "1 day ago", "5 minutes ago"
     */
    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null)
            return "Unknown";
        try {
            Duration duration = Duration.between(dateTime, LocalDateTime.now());
            long minutes = duration.getSeconds() / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days >= 1)
                return days + " day" + (days == 1 ? "" : "s") + " ago";
            if (hours >= 1)
                return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
            if (minutes >= 1)
                return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
            return "Just now";
        } catch (Exception e) {
            return "Recently";
        }
    }

    /**
     * Helper: strips HTML tags for display (basic implementation)
     * This is a simplified version - in production use HTMLEditor or similar
     */
    private String stripHtmlTags(String html) {
        if (html == null)
            return "";
        return html.replaceAll("<[^>]*>", "");
    }

    /**
     * Handles clicking a course card: select it and load its announcements
     */
    private void handleCourseSelect(MouseEvent event, Course course) {
        // Deselect all cards
        for (Node node : courseListContainer.getChildren()) {
            if (node instanceof VBox card) {
                card.getStyleClass().remove("course-card-selected");
            }
        }

        // Select the clicked card
        Node target = (Node) event.getSource();
        if (target instanceof VBox card) {
            if (!card.getStyleClass().contains("course-card-selected")) {
                card.getStyleClass().add("course-card-selected");
            }
        }

        // Store current course
        currentSelectedCourse = course;
        currentCourseId = course.getId();
        if (homeCourseTitle != null) homeCourseTitle.setText(course.getName());
        if (homeCourseCode != null) homeCourseCode.setText(course.getCode());
        if (homeCourseMeta != null) homeCourseMeta.setText("");

        // Reset to Stream tab and load announcements
        highlightTab(streamTab);
        showStream();

        // Reset ToDoController for new course context
        toDoController = null;
    }

    /**
     * Handles the "+ Post Announcement" button
     * Opens CreateAnnouncementView as a modal dialog
     */
    @FXML
    private void handlePostAnnouncement(ActionEvent event) {
        if (!Session.getInstance().isInstructor()) {
            denyAccess();
            return;
        }
        if (currentCourseId <= 0) {
            System.out.println("No course selected");
            return;
        }
        openCreateAnnouncementDialog();
    }

    /**
     * Handles the "+ Create Assignment" button
     * Opens CreateAssignmentView as a modal dialog
     */
    @FXML
    private void handleCreateAssignment(ActionEvent event) {
        if (!Session.getInstance().isInstructor()) {
            denyAccess();
            return;
        }
        if (currentCourseId <= 0) {
            System.out.println("No course selected");
            return;
        }
        openCreateAssignmentDialog();
    }

    /**
     * Opens CreateAnnouncementView as a modal stage
     * On success, reloads announcements for the current course
     */
    private void openCreateAnnouncementDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/naawbi/view/CreateAnnouncement/CreateAnnouncementView.fxml"));
            Parent dialogRoot = loader.load();

            CreateAnnouncementController controller = loader.getController();
            controller.setCourseContext(
                    currentCourseId,
                    currentUserId,
                    currentSelectedCourse,
                    () -> loadAnnouncementsForCourse(currentCourseId));

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Post Announcement - "
                    + (currentSelectedCourse != null ? currentSelectedCourse.getName() : "Course"));
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to open announcement dialog: " + e.getMessage());
        }
    }

    /**
     * Opens CreateAssignmentView as a modal stage
     * On success, reloads assignments in the To-Do view
     */
    private void openCreateAssignmentDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/naawbi/view/CreateAssignment/CreateAssignmentView.fxml"));
            Parent dialogRoot = loader.load();

            CreateAssignmentController controller = loader.getController();
            Stage dialogStage = new Stage();

            controller.setCourseContext(currentCourseId, currentUserId, dialogStage);

            dialogStage.setTitle("Create Assignment - "
                    + (currentSelectedCourse != null ? currentSelectedCourse.getName() : "Course"));
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);

            // When dialog closes, refresh the To-Do view if visible
            dialogStage.setOnHidden(event -> {
                if (toDoController != null) {
                    toDoController.refreshAssignments();
                }
            });

            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to open assignment dialog: " + e.getMessage());
        }
    }
}
