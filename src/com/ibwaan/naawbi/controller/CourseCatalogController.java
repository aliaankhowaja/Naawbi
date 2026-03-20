package com.ibwaan.naawbi.controller;

import com.ibwaan.naawbi.model.Course;
import com.ibwaan.naawbi.view.ViewConstants;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
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

    /* ── Lifecycle ───────────────────────────────────── */

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        injectColorConstants();
        wireSearch();
        loadCoursesFromDB();
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
        card.setOnMouseClicked(this::handleCourseSelect);

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
     * Handles clicking a course card in the catalog.
     * Toggles the -selected style class and would trigger a Course Home
     * content swap in a real implementation.
     */
    @FXML
    private void handleCourseSelect(MouseEvent event) {
        // Find the root card VBox from the click target
        Node target = (Node) event.getSource();

        // Deselect all cards
        for (Node node : courseListContainer.getChildren()) {
            if (node instanceof VBox card) {
                card.getStyleClass().remove("course-card-selected");
            }
        }

        // Select the clicked card
        if (target instanceof VBox card) {
            if (!card.getStyleClass().contains("course-card-selected")) {
                card.getStyleClass().add("course-card-selected");
            }
        }

        // TODO: load the corresponding course's stream data into streamContainer
        // courseService.getCourse(courseId).thenAccept(course -> {
        // Platform.runLater(() -> streamController.populate(course));
        // });
        System.out.println("Course selected: " + event.getSource());
    }

    /**
     * Handles the "+ Post Announcement" button on the Course Home header.
     * In production this would open a CreateAnnouncementView dialog.
     */
    @FXML
    private void handlePostAnnouncement(ActionEvent event) {
        // TODO: open CreateAnnouncementView as a modal stage
        System.out.println("Post announcement clicked.");
    }
}
