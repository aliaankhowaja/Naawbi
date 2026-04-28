package com.ibwaan.naawbi.controller;

import com.ibwaan.naawbi.model.Assignment;
import com.ibwaan.naawbi.model.Submission;
import com.ibwaan.naawbi.model.SubmissionComment;
import com.ibwaan.naawbi.view.ViewConstants;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AssignmentDetailsController implements Initializable {

    @FXML
    private StackPane rootPane;
    @FXML
    private Label titleLabel;
    @FXML
    private Label courseContextLabel;
    @FXML
    private Label pointsLabel;
    @FXML
    private Label deadlineLabel;
    @FXML
    private Label latePolicyLabel;
    @FXML
    private Label descriptionLabel;

    @FXML
    private Label statusLabel;
    @FXML
    private Label fileLabel;
    @FXML
    private Button uploadBtn;
    @FXML
    private Button submitBtn;
    @FXML
    private Button unsubmitBtn;

    @FXML
    private VBox commentsBox;
    @FXML
    private TextField commentInput;

    private int userId;
    private Assignment assignment;
    private Submission submission;
    private Stage modal;

    private String selectedFilePath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        injectColorConstants();
        try {
            if (this.assignment == null) {
                this.assignment = Assignment.getById(1);
                if (this.assignment != null) {
                    this.userId = 1;
                    loadAssignmentDetails();
                    loadSubmission();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setContext(Assignment assignment, int userId, Stage modal) {
        this.assignment = assignment;
        this.userId = userId;
        this.modal = modal;
        loadAssignmentDetails();
        loadSubmission();
    }

    private void loadAssignmentDetails() {
        titleLabel.setText(assignment.getTitle());
        courseContextLabel.setText("By " + assignment.getAuthorName());
        pointsLabel.setText(assignment.getTotalPoints() + " Points");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");
        deadlineLabel.setText("Due: " + assignment.getDeadline().format(dtf));

        if (assignment.isLateSubmissionsAllowed()) {
            latePolicyLabel.setText("Late submissions allowed");
        } else {
            latePolicyLabel.setText("Hard deadline");
        }

        descriptionLabel.setText(
                assignment.getDescription() != null ? assignment.getDescription() : "No description provided.");
    }

    private void loadSubmission() {
        try {
            submission = Submission.getByAssignmentAndUser(assignment.getId(), userId);
            if (submission == null) {
                // Ensure there is a submission record for commenting logic
                submission = Submission.findOrCreate(assignment.getId(), userId);
            }
            updateSubmissionUI();
            loadComments();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateSubmissionUI() {
        boolean pastDeadline = LocalDateTime.now().isAfter(assignment.getDeadline());

        if (submission.isSubmitted()) {
            statusLabel.setText(submission.getStatus());
            if ("Late".equals(submission.getStatus())) {
                statusLabel.setStyle(
                        "-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-color: #f0ad4e; -fx-background-radius: 12;");
            } else {
                statusLabel.setStyle(
                        "-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-color: #5cb85c; -fx-background-radius: 12;");
            }

            fileLabel.setText(submission.getFilePath() != null ? new File(submission.getFilePath()).getName()
                    : "No file attached");

            uploadBtn.setVisible(false);
            uploadBtn.setManaged(false);
            submitBtn.setVisible(false);
            submitBtn.setManaged(false);

            // Allow unsubmit if not past deadline, or if past deadline but late submissions
            // are allowed
            boolean canUnsubmit = !pastDeadline || assignment.isLateSubmissionsAllowed();
            unsubmitBtn.setVisible(canUnsubmit);
            unsubmitBtn.setManaged(canUnsubmit);

        } else {
            if (pastDeadline && !assignment.isLateSubmissionsAllowed()) {
                statusLabel.setText("Missing");
                statusLabel.setStyle(
                        "-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-color: #d9534f; -fx-background-radius: 12;");
                uploadBtn.setDisable(true);
                submitBtn.setDisable(true);
            } else {
                statusLabel.setText("Not Submitted");
                statusLabel.setStyle(
                        "-fx-font-size: 14px; -fx-text-fill: gray; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-color: #f0f0f0; -fx-background-radius: 12;");
                uploadBtn.setDisable(false);
                submitBtn.setDisable(selectedFilePath == null);
            }

            fileLabel.setText(selectedFilePath != null ? new File(selectedFilePath).getName() : "No file uploaded");

            uploadBtn.setVisible(true);
            uploadBtn.setManaged(true);
            submitBtn.setVisible(true);
            submitBtn.setManaged(true);
            unsubmitBtn.setVisible(false);
            unsubmitBtn.setManaged(false);
        }
    }

    private void loadComments() {
        commentsBox.getChildren().clear();
        try {
            List<SubmissionComment> comments = SubmissionComment.fetchBySubmissionId(submission.getId());
            for (SubmissionComment comment : comments) {
                VBox cb = new VBox(2);
                cb.setStyle(
                        "-fx-background-color: white; -fx-padding: 8; -fx-background-radius: 5; -fx-border-color: #eee; -fx-border-radius: 5;");

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, h:mm a");
                Label authorLbl = new Label(comment.getAuthorName() + " • " + comment.getCreatedAt().format(dtf));
                authorLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

                Label textLbl = new Label(comment.getCommentText());
                textLbl.setWrapText(true);
                textLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: black;");

                cb.getChildren().addAll(authorLbl, textLbl);
                commentsBox.getChildren().add(cb);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpload(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Submission File");
        File file = fileChooser.showOpenDialog(modal);
        if (file != null) {
            selectedFilePath = file.getAbsolutePath();
            updateSubmissionUI(); // Re-evaluate submit button state
        }
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        if (selectedFilePath == null)
            return;

        try {
            boolean pastDeadline = LocalDateTime.now().isAfter(assignment.getDeadline());
            String status = pastDeadline ? "Late" : "Submitted";

            Submission.markAsSubmitted(assignment.getId(), userId, selectedFilePath, status);
            loadSubmission(); // Reload to update state and UI
            System.out.println("Submitted file: " + selectedFilePath);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUnsubmit(ActionEvent event) {
        try {
            Submission.unsubmit(assignment.getId(), userId);
            selectedFilePath = null;
            loadSubmission(); // Reload to revert UI state
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddComment(ActionEvent event) {
        String text = commentInput.getText().trim();
        if (text.isEmpty())
            return;

        try {
            SubmissionComment sc = new SubmissionComment(submission.getId(), userId, text);
            sc.save();
            commentInput.clear();
            loadComments(); // Refresh comment list
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        if (modal != null)
            modal.close();
    }

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
}
