package com.ibwaan.naawbi.controller;

import com.ibwaan.naawbi.model.Announcement;
import com.ibwaan.naawbi.model.Course;
import com.ibwaan.naawbi.model.Session;
import com.ibwaan.naawbi.view.ViewConstants;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.io.IOException;
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

    /* ── Tabs ────────────────────────────────────────── */
    @FXML
    private Label streamTab;
    @FXML
    private Label assignmentsTab;
    @FXML
    private Label peopleTab;
    @FXML
    private Label todoTab;

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
    }

    /**
     * Loads all courses from the database and populates the catalog list.
     */
    private void loadCoursesFromDB() {
        courseListContainer.getChildren().clear();
        try {
            List<Course> courses = Course.fetchAll();
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
     * Shows the Assignments tab content (stub)
     */
    private void showAssignments() {
        contentHBox.setVisible(true);
        contentHBox.setManaged(true);
        todoContainer.setVisible(false);
        todoContainer.setManaged(false);
        createAssignmentBtn.setVisible(false);
        createAssignmentBtn.setManaged(false);
        // TODO: Load and display assignments for this course
        // For now, just show the assignments as announcements would be shown
    }

    /**
     * Shows the People tab content (stub)
     */
    private void showPeople() {
        contentHBox.setVisible(true);
        contentHBox.setManaged(true);
        todoContainer.setVisible(false);
        todoContainer.setManaged(false);
        createAssignmentBtn.setVisible(false);
        createAssignmentBtn.setManaged(false);
        // TODO: Load and display course members/people
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
                            getClass().getResource("/com/ibwaan/naawbi/view/ToDoView.fxml"));
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

        // Add active class to the clicked tab
        if (!activeTab.getStyleClass().contains("tab-active")) {
            activeTab.getStyleClass().add("tab-active");
        }
    }

    /* ── Handlers ────────────────────────────────────── */

    /**
     * Navigates to the Create Course view.
     */
    @FXML
    private void handleNavigateToCreate(ActionEvent event) {
        try {
            Parent createRoot = FXMLLoader
                    .load(getClass().getResource("/com/ibwaan/naawbi/view/CreateCourse/CreateCourseView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Naawbi - Create Course");
            stage.getScene().setRoot(createRoot);
        } catch (IOException e) {
            e.printStackTrace();
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
        return card;
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
                    getClass().getResource("/com/ibwaan/naawbi/view/CreateAnnouncement/CreateAnnouncementView.fxml"));
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
                    getClass().getResource("/com/ibwaan/naawbi/view/CreateAssignment/CreateAssignmentView.fxml"));
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
