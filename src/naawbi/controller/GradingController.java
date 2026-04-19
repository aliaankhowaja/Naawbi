package naawbi.controller;

import naawbi.model.Assignment;
import naawbi.model.Submission;
import naawbi.view.ViewConstants;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * GradingController — US 19 & 20
 *
 * Provides the instructor grading interface:
 * - Left panel: scrollable list of all student submissions for an assignment
 * - Right panel: selected student's submission details, grade input (0–totalPoints),
 *   feedback textarea, and Save button.
 *
 * Opened as a modal dialog from CourseCatalogController.
 */
public class GradingController implements Initializable {

    // FXML bindings
    @FXML private StackPane rootPane;
    @FXML private Label assignmentTitleLabel;
    @FXML private Label submissionCountLabel;
    @FXML private VBox  studentListContainer;
    @FXML private VBox  placeholderPane;
    @FXML private VBox  detailPane;
    @FXML private Label studentNameLabel;
    @FXML private Label submissionStatusLabel;
    @FXML private Label filePathLabel;
    @FXML private Label submittedAtLabel;
    @FXML private Label totalPointsLabel;
    @FXML private TextField gradeField;
    @FXML private Label gradeErrorLabel;
    @FXML private TextArea feedbackArea;
    @FXML private Button saveGradeBtn;
    @FXML private Label saveSuccessLabel;

    // State
    private Assignment assignment;
    private Stage modal;
    private Submission selectedSubmission;

    private static final DateTimeFormatter DTF =
        DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        injectColorConstants();
    }

    /**
     * Called by the parent controller after load.
     * Sets the assignment context and loads the student submission list.
     */
    public void setContext(Assignment assignment, Stage modal) {
        this.assignment = assignment;
        this.modal = modal;
        assignmentTitleLabel.setText("Grading: " + assignment.getTitle());
        totalPointsLabel.setText("/ " + assignment.getTotalPoints());
        loadSubmissions();
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    /** Fetches all submissions for this assignment and renders student cards */
    private void loadSubmissions() {
        studentListContainer.getChildren().clear();
        try {
            List<Submission> submissions = Submission.fetchByAssignmentId(assignment.getId());
            submissionCountLabel.setText(submissions.size() + " student" +
                (submissions.size() == 1 ? "" : "s"));

            for (Submission sub : submissions) {
                VBox card = createStudentCard(sub);
                studentListContainer.getChildren().add(card);
            }

            if (submissions.isEmpty()) {
                Label empty = new Label("No submissions yet.");
                empty.setStyle("-fx-padding:16;-fx-text-fill:#aaa;-fx-font-size:13px;");
                studentListContainer.getChildren().add(empty);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Builds a clickable student row card for the left panel */
    private VBox createStudentCard(Submission sub) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 16, 10, 16));
        card.setStyle("-fx-border-color: transparent transparent #f0f0f0 transparent;" +
            "-fx-cursor:hand;-fx-background-color:white;");

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        String initials = sub.getStudentName() != null && !sub.getStudentName().isEmpty()
            ? sub.getStudentName().substring(0, 1).toUpperCase() : "?";
        Label avatar = new Label(initials);
        avatar.setStyle("-fx-min-width:32;-fx-min-height:32;-fx-max-width:32;-fx-max-height:32;" +
            "-fx-background-radius:16;-fx-alignment:center;-fx-font-weight:bold;" +
            "-fx-text-fill:white;-fx-background-color:#5c6bc0;");

        VBox info = new VBox(2);
        Label name = new Label(sub.getStudentName() != null ? sub.getStudentName() : "Unknown");
        name.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#202124;");

        String statusText = sub.isSubmitted() ? sub.getStatus() : "Not Submitted";
        Label status = new Label(statusText);
        status.setStyle("-fx-font-size:11px;-fx-text-fill:" +
            (sub.isSubmitted() ? "#388e3c" : "#999") + ";");
        info.getChildren().addAll(name, status);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Grade chip (if already graded)
        if (sub.getGrade() != null) {
            Label gradeChip = new Label(sub.getGrade() + "/" + assignment.getTotalPoints());
            gradeChip.setStyle("-fx-font-size:11px;-fx-padding:2 8;-fx-background-color:#e8eafd;" +
                "-fx-text-fill:#3949ab;-fx-background-radius:10;-fx-font-weight:bold;");
            row.getChildren().addAll(avatar, info, gradeChip);
        } else {
            row.getChildren().addAll(avatar, info);
        }

        card.getChildren().add(row);

        // Click → populate detail panel
        card.setOnMouseClicked(e -> selectStudent(sub, card));

        return card;
    }

    // ── Detail panel ─────────────────────────────────────────────────────────

    /** Populates the right panel with the selected student's submission */
    private void selectStudent(Submission sub, VBox card) {
        // Highlight selected card
        for (javafx.scene.Node n : studentListContainer.getChildren()) {
            n.setStyle(n.getStyle().replace("-fx-background-color:#e8eafd;",
                "-fx-background-color:white;"));
        }
        card.setStyle("-fx-border-color: transparent transparent #f0f0f0 transparent;" +
            "-fx-cursor:hand;-fx-background-color:#e8eafd;");

        selectedSubmission = sub;

        // Reveal detail panel
        placeholderPane.setVisible(false);
        placeholderPane.setManaged(false);
        detailPane.setVisible(true);
        detailPane.setManaged(true);

        // Hide success message from previous selection
        saveSuccessLabel.setVisible(false);
        saveSuccessLabel.setManaged(false);
        gradeErrorLabel.setVisible(false);
        gradeErrorLabel.setManaged(false);

        // Student name
        studentNameLabel.setText(sub.getStudentName() != null ? sub.getStudentName() : "Unknown");

        // Status badge
        String statusText = sub.isSubmitted() ? sub.getStatus() : "Not Submitted";
        String bgColor = switch (statusText != null ? statusText : "") {
            case "Submitted" -> "#5cb85c";
            case "Late"      -> "#f0ad4e";
            case "Missing"   -> "#d9534f";
            default          -> "#9e9e9e";
        };
        submissionStatusLabel.setText(statusText != null ? statusText : "Not Submitted");
        submissionStatusLabel.setStyle("-fx-font-size:12px;-fx-padding:3 10;" +
            "-fx-background-color:" + bgColor + ";-fx-text-fill:white;" +
            "-fx-background-radius:12;-fx-font-weight:bold;");

        // File path
        if (sub.getFilePath() != null && !sub.getFilePath().isBlank()) {
            filePathLabel.setText(new File(sub.getFilePath()).getName());
        } else {
            filePathLabel.setText("No file submitted");
        }

        // Submitted at timestamp
        if (sub.getSubmittedAt() != null) {
            submittedAtLabel.setText("Submitted: " + sub.getSubmittedAt().format(DTF));
        } else {
            submittedAtLabel.setText("");
        }

        // Pre-fill existing grade and feedback
        gradeField.setText(sub.getGrade() != null ? String.valueOf(sub.getGrade()) : "");
        feedbackArea.setText(sub.getFeedback() != null ? sub.getFeedback() : "");
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    @FXML
    private void handleSaveGrade(ActionEvent event) {
        if (selectedSubmission == null) return;

        String gradeText = gradeField.getText().trim();
        if (gradeText.isEmpty()) {
            showGradeError("Grade is required.");
            return;
        }

        int gradeValue;
        try {
            gradeValue = Integer.parseInt(gradeText);
        } catch (NumberFormatException e) {
            showGradeError("Grade must be a whole number.");
            return;
        }

        if (gradeValue < 0 || gradeValue > assignment.getTotalPoints()) {
            showGradeError("Grade must be between 0 and " + assignment.getTotalPoints() + ".");
            return;
        }

        // Ensure there is a submission row to grade (findOrCreate for unsubmitted students)
        try {
            if (selectedSubmission.getId() == 0) {
                // No row yet — findOrCreate first
                selectedSubmission = Submission.findOrCreate(
                    assignment.getId(), selectedSubmission.getUserId());
            }
            String feedback = feedbackArea.getText().trim();
            Submission.saveGrade(selectedSubmission.getId(), gradeValue, feedback);

            // Update in-memory object
            selectedSubmission.setGrade(gradeValue);
            selectedSubmission.setFeedback(feedback.isBlank() ? null : feedback);

            // Show success and hide error
            gradeErrorLabel.setVisible(false);
            gradeErrorLabel.setManaged(false);
            saveSuccessLabel.setVisible(true);
            saveSuccessLabel.setManaged(true);

            // Reload student list to update grade chips
            loadSubmissions();

        } catch (SQLException e) {
            e.printStackTrace();
            showGradeError("Database error. Please try again.");
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        if (modal != null) modal.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showGradeError(String message) {
        gradeErrorLabel.setText(message);
        gradeErrorLabel.setVisible(true);
        gradeErrorLabel.setManaged(true);
        saveSuccessLabel.setVisible(false);
        saveSuccessLabel.setManaged(false);
    }

    private void injectColorConstants() {
        rootPane.setStyle(String.format(
            "-vc-primary:%s;-vc-secondary:%s;-vc-accent:%s;-vc-background:%s;-vc-text:%s;",
            ViewConstants.PRIMARY_COLOR, ViewConstants.SECONDARY_COLOR,
            ViewConstants.ACCENT_COLOR, ViewConstants.BACKGROUND_COLOR,
            ViewConstants.TEXT_COLOR));
    }
}
