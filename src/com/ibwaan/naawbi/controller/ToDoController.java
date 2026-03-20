package com.ibwaan.naawbi.controller;

import com.ibwaan.naawbi.model.Assignment;
import com.ibwaan.naawbi.model.Course;
import com.ibwaan.naawbi.model.Submission;
import com.ibwaan.naawbi.view.ViewConstants;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for ToDoView.fxml
 *
 * Responsibilities:
 * 1. Display pending assignments partitioned into "Active" and "Missing" sections
 * 2. Calculate assignment urgency: Overdue (red), Due Soon < 48hrs (orange), or normal deadline
 * 3. Allow students to mark assignments as submitted with instant UI refresh
 * 4. Provide a refresh button to reload assignments from the database
 */
public class ToDoController implements Initializable {

    /* ── Root (color injection) ─────────────────────── */
    @FXML
    private StackPane todoContainer;

    /* ── Section containers ─────────────────────────── */
    @FXML
    private VBox activeSection;
    @FXML
    private VBox missingSection;

    /* ── Assignment lists ────────────────────────────── */
    @FXML
    private VBox activeAssignmentsList;
    @FXML
    private VBox missingAssignmentsList;

    /* ── Empty state labels ──────────────────────────── */
    @FXML
    private Label activeEmptyState;
    @FXML
    private Label missingEmptyState;

    /* ── Buttons ─────────────────────────────────────── */
    @FXML
    private Button refreshBtn;

    /* ── State ───────────────────────────────────────── */
    private int courseId;
    private int userId;
    private Course course;

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    /* ── Lifecycle ───────────────────────────────────── */

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        injectColorConstants();
    }

    /**
     * Sets the context (course and user) for this to-do controller.
     * Must be called before any UI operations.
     */
    public void setContext(int courseId, int userId, Course course) {
        this.courseId = courseId;
        this.userId = userId;
        this.course = course;
        refreshAssignments();
    }

    /**
     * Loads all pending assignments for the student, partitions them into
     * active (not overdue) and missing (overdue and not submitted), and
     * rebuilds the UI with assignment cards.
     */
    public void refreshAssignments() {
        activeAssignmentsList.getChildren().clear();
        missingAssignmentsList.getChildren().clear();

        try {
            // Fetch assignments already sorted by deadline
            List<Assignment> assignments = Assignment.fetchStudentPending(courseId, userId);
            List<Assignment> activeList = new ArrayList<>();
            List<Assignment> missingList = new ArrayList<>();

            LocalDateTime now = LocalDateTime.now();

            for (Assignment assignment : assignments) {
                LocalDateTime deadline = assignment.getDeadline();

                // Check if assignment is submitted
                Submission submission = Submission.getByAssignmentAndUser(assignment.getId(), userId);
                boolean isSubmitted = submission != null && submission.isSubmitted();

                // Determine section: missing if overdue and not submitted
                if (deadline.isBefore(now) && !isSubmitted) {
                    missingList.add(assignment);
                } else {
                    activeList.add(assignment);
                }
            }

            // Populate active assignments
            if (activeList.isEmpty()) {
                activeEmptyState.setVisible(true);
                activeEmptyState.setManaged(true);
            } else {
                activeEmptyState.setVisible(false);
                activeEmptyState.setManaged(false);
                for (Assignment assignment : activeList) {
                    VBox card = createAssignmentCard(assignment, false);
                    activeAssignmentsList.getChildren().add(card);
                }
            }

            // Populate missing assignments
            if (missingList.isEmpty()) {
                missingEmptyState.setVisible(true);
                missingEmptyState.setManaged(true);
            } else {
                missingEmptyState.setVisible(false);
                missingEmptyState.setManaged(false);
                for (Assignment assignment : missingList) {
                    VBox card = createAssignmentCard(assignment, true);
                    missingAssignmentsList.getChildren().add(card);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            activeEmptyState.setText("Error loading assignments");
            activeEmptyState.setVisible(true);
            activeEmptyState.setManaged(true);
        }
    }

    /**
     * Programmatically creates an assignment card VBox containing:
     * - Title and course code
     * - Deadline with urgency status badge
     * - Submit button
     */
    private VBox createAssignmentCard(Assignment assignment, boolean isMissing) {
        VBox card = new VBox();
        card.getStyleClass().add("assignment-card");
        card.setSpacing(8);
        card.setPadding(new Insets(12));

        // Header: Title + Course Code
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);

        Label titleLabel = new Label(assignment.getTitle());
        titleLabel.getStyleClass().add("assignment-card-title");
        titleLabel.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label codeLabel = new Label(course != null ? course.getCode() : "N/A");
        codeLabel.getStyleClass().add("assignment-course-code");

        header.getChildren().addAll(titleLabel, spacer, codeLabel);

        // Deadline + Status Badge
        HBox deadlineRow = new HBox();
        deadlineRow.setAlignment(Pos.CENTER_LEFT);
        deadlineRow.setSpacing(12);

        LocalDateTime deadline = assignment.getDeadline();
        String deadlineText = deadline.format(DEADLINE_FORMAT);
        Label deadlineLabel = new Label(deadlineText);
        deadlineLabel.getStyleClass().add("assignment-deadline");

        String statusText = calculateStatusText(deadline);
        Label statusBadge = new Label(statusText);
        statusBadge.getStyleClass().add("assignment-status-badge");

        // Determine badge urgency styling
        if (isMissing) {
            statusBadge.getStyleClass().add("status-overdue");
        } else if (isOverdue(deadline)) {
            statusBadge.getStyleClass().add("status-overdue");
        } else if (isDueSoon(deadline)) {
            statusBadge.getStyleClass().add("status-due-soon");
        } else {
            statusBadge.getStyleClass().add("status-normal");
        }

        deadlineRow.getChildren().addAll(deadlineLabel, statusBadge);

        // Submit Button
        HBox buttonRow = new HBox();
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        Button submitBtn = new Button("Mark as Submitted");
        submitBtn.getStyleClass().add("assignment-submit-btn");
        submitBtn.setOnAction(event -> handleSubmit(assignment));

        buttonRow.getChildren().add(submitBtn);

        // Assemble card
        card.getChildren().addAll(header, deadlineRow, buttonRow);
        return card;
    }

    /**
     * Determines the status text based on deadline and current time.
     * - "Overdue" if deadline is in the past
     * - "Due Soon" if deadline is within 48 hours
     * - Otherwise, the formatted deadline date
     */
    private String calculateStatusText(LocalDateTime deadline) {
        LocalDateTime now = LocalDateTime.now();

        if (deadline.isBefore(now)) {
            return "Overdue";
        }

        Duration remaining = Duration.between(now, deadline);
        long hours = remaining.toHours();

        if (hours <= 48 && hours > 0) {
            return "Due Soon";
        }

        return deadline.format(DEADLINE_FORMAT);
    }

    /**
     * Helper: checks if deadline is in the past
     */
    private boolean isOverdue(LocalDateTime deadline) {
        return deadline.isBefore(LocalDateTime.now());
    }

    /**
     * Helper: checks if deadline is within 48 hours
     */
    private boolean isDueSoon(LocalDateTime deadline) {
        LocalDateTime now = LocalDateTime.now();
        if (deadline.isBefore(now)) {
            return false;
        }
        Duration remaining = Duration.between(now, deadline);
        return remaining.toHours() <= 48;
    }

    /**
     * Refresh button handler: reloads assignments from database
     */
    @FXML
    private void handleRefresh(ActionEvent event) {
        refreshAssignments();
    }

    /**
     * Submit button handler: marks assignment as submitted and refreshes UI
     */
    private void handleSubmit(Assignment assignment) {
        try {
            Submission.markAsSubmitted(assignment.getId(), userId);
            System.out.println("Assignment marked as submitted: " + assignment.getTitle());
            refreshAssignments();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error marking assignment as submitted: " + e.getMessage());
        }
    }

    /**
     * Injects ViewConstants hex values as JavaFX CSS variables
     * on the root StackPane so all descendants can reference
     * -vc-primary, -vc-secondary, -vc-accent, -vc-background, -vc-text
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
        todoContainer.setStyle(style);
    }
}
