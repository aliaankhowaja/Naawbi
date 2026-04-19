package com.ibwaan.naawbi.controller;

import com.ibwaan.naawbi.model.Session;
import com.ibwaan.naawbi.model.Submission;
import com.ibwaan.naawbi.view.ViewConstants;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;

/**
 * GradebookController — US 21 & 22
 *
 * Dual-mode controller:
 * - Student mode  (US 22): shows personal grades list — course, assignment, score/total, feedback
 * - Instructor mode (US 21): shows class gradebook GridPane (students × assignments matrix)
 *
 * Mode is determined by Session.isInstructor() + the courseId context passed in.
 */
public class GradebookController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private Label headerLabel;
    @FXML private Label courseNameLabel;
    @FXML private HBox modeTabBar;
    @FXML private Label myGradesTab;
    @FXML private Label classGradebookTab;

    // Student grades pane
    @FXML private ScrollPane studentGradesPane;
    @FXML private VBox studentGradesContainer;

    // Instructor gradebook pane
    @FXML private ScrollPane instructorGradebookPane;
    @FXML private GridPane gradebookGrid;

    private Stage modal;
    private int courseId;
    private boolean isInstructor;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        injectColorConstants();
    }

    /**
     * Called after FXML load to inject context.
     *
     * @param courseId     current course (used for instructor class view)
     * @param courseName   course display name shown in header
     * @param modal        owning Stage (for close button)
     */
    public void setContext(int courseId, String courseName, Stage modal) {
        this.courseId = courseId;
        this.modal = modal;
        this.isInstructor = Session.getInstance().isInstructor();

        courseNameLabel.setText(courseName);

        if (isInstructor) {
            headerLabel.setText("Class Gradebook");
            // Show both tabs for instructors
            myGradesTab.setVisible(false);
            myGradesTab.setManaged(false);
            classGradebookTab.setVisible(true);
            classGradebookTab.setManaged(true);
            // Default instructor view: class gradebook
            showInstructorGradebook();
        } else {
            headerLabel.setText("My Grades");
            showStudentGrades();
        }
    }

    // ── Student personal gradebook (US 22) ────────────────────────────────────

    private void showStudentGrades() {
        studentGradesPane.setVisible(true);
        studentGradesPane.setManaged(true);
        instructorGradebookPane.setVisible(false);
        instructorGradebookPane.setManaged(false);

        studentGradesContainer.getChildren().clear();

        int userId = Session.getInstance().getUserId();
        try {
            List<Submission> graded = Submission.fetchGradedForStudent(userId);
            if (graded.isEmpty()) {
                Label empty = new Label("No graded assignments yet. Check back after your instructor grades your work.");
                empty.setWrapText(true);
                empty.setStyle("-fx-font-size:14px;-fx-text-fill:#aaa;-fx-padding:24;");
                studentGradesContainer.getChildren().add(empty);
                return;
            }

            // Group by course name
            Map<String, List<Submission>> byCourse = new LinkedHashMap<>();
            for (Submission s : graded) {
                byCourse.computeIfAbsent(s.getCourseName(), k -> new ArrayList<>()).add(s);
            }

            for (Map.Entry<String, List<Submission>> entry : byCourse.entrySet()) {
                // Course section header
                Label courseHeader = new Label(entry.getKey());
                courseHeader.setStyle("-fx-font-size:16px;-fx-font-weight:bold;" +
                    "-fx-text-fill:#00695c;-fx-padding:8 0 4 0;");
                studentGradesContainer.getChildren().add(courseHeader);

                // Calculate running total for the course
                int earnedTotal = 0, possibleTotal = 0;

                for (Submission s : entry.getValue()) {
                    VBox card = createStudentGradeCard(s);
                    studentGradesContainer.getChildren().add(card);
                    if (s.getGrade() != null) {
                        earnedTotal += s.getGrade();
                        possibleTotal += s.getTotalPoints();
                    }
                }

                // Course summary row
                if (possibleTotal > 0) {
                    double pct = (earnedTotal * 100.0) / possibleTotal;
                    HBox summary = new HBox(10);
                    summary.setAlignment(Pos.CENTER_RIGHT);
                    summary.setPadding(new Insets(4, 0, 12, 0));
                    Label sumLbl = new Label(String.format("Course total:  %d / %d  (%.1f%%)",
                        earnedTotal, possibleTotal, pct));
                    sumLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#555;");
                    summary.getChildren().add(sumLbl);
                    studentGradesContainer.getChildren().add(summary);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label err = new Label("Error loading grades.");
            err.setStyle("-fx-text-fill:#d32f2f;-fx-padding:16;");
            studentGradesContainer.getChildren().add(err);
        }
    }

    /** Builds a single assignment grade card for the student view */
    private VBox createStudentGradeCard(Submission s) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle("-fx-background-color:white;-fx-background-radius:8;" +
            "-fx-border-color:#e0e0e0;-fx-border-radius:8;");

        // Title row
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(s.getAssignmentTitle() != null ? s.getAssignmentTitle() : "Assignment");
        titleLbl.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#202124;");
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        // Grade badge
        Integer grade = s.getGrade();
        int total = s.getTotalPoints();
        String scoreText = (grade != null) ? (grade + " / " + total) : "Not graded";
        Label scoreLbl = new Label(scoreText);
        String scoreColor = grade == null ? "#9e9e9e"
            : (grade >= total * 0.7 ? "#388e3c" : grade >= total * 0.5 ? "#f57c00" : "#c62828");
        scoreLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + scoreColor + ";");

        // Percentage
        String pctText = "";
        if (grade != null && total > 0) {
            pctText = String.format("%.0f%%", (grade * 100.0) / total);
        }
        Label pctLbl = new Label(pctText);
        pctLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#888;");

        titleRow.getChildren().addAll(titleLbl, pctLbl, scoreLbl);

        card.getChildren().add(titleRow);

        // Feedback (if present)
        if (s.getFeedback() != null && !s.getFeedback().isBlank()) {
            Label feedbackLabel = new Label("Instructor feedback: " + s.getFeedback());
            feedbackLabel.setWrapText(true);
            feedbackLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#555;" +
                "-fx-font-style:italic;-fx-padding:4 0 0 0;");
            card.getChildren().add(feedbackLabel);
        }

        return card;
    }

    // ── Instructor class gradebook (US 21) ────────────────────────────────────

    private void showInstructorGradebook() {
        instructorGradebookPane.setVisible(true);
        instructorGradebookPane.setManaged(true);
        studentGradesPane.setVisible(false);
        studentGradesPane.setManaged(false);

        gradebookGrid.getChildren().clear();
        gradebookGrid.getColumnConstraints().clear();
        gradebookGrid.getRowConstraints().clear();

        try {
            List<Submission> all = Submission.fetchGradebookForCourse(courseId);
            if (all.isEmpty()) {
                Label empty = new Label("No submissions yet for this course.");
                empty.setStyle("-fx-font-size:14px;-fx-text-fill:#aaa;-fx-padding:24;");
                gradebookGrid.add(empty, 0, 0);
                return;
            }

            // Build unique ordered student names and assignment titles
            LinkedHashMap<String, Map<String, Integer>> matrix = new LinkedHashMap<>();
            LinkedHashSet<String> assignmentCols = new LinkedHashSet<>();

            for (Submission s : all) {
                String student    = s.getStudentName() != null ? s.getStudentName() : "Unknown";
                String assignment = s.getAssignmentTitle() != null ? s.getAssignmentTitle() : "—";
                assignmentCols.add(assignment);
                matrix.computeIfAbsent(student, k -> new LinkedHashMap<>())
                      .put(assignment, s.getGrade());
            }

            List<String> col_names = new ArrayList<>(assignmentCols);
            List<String> row_names = new ArrayList<>(matrix.keySet());

            // Header style
            String headerStyle = "-fx-background-color:#00897b;-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-font-size:12px;-fx-padding:8 12;-fx-alignment:center;";
            String cellStyle = "-fx-background-color:white;-fx-font-size:12px;" +
                "-fx-padding:6 10;-fx-alignment:center;-fx-border-color:#f0f0f0;";
            String evenRowStyle = "-fx-background-color:#f9fbe7;-fx-font-size:12px;" +
                "-fx-padding:6 10;-fx-alignment:center;-fx-border-color:#f0f0f0;";

            // Corner cell
            Label corner = new Label("Student");
            corner.setMaxWidth(Double.MAX_VALUE);
            corner.setStyle(headerStyle);
            gradebookGrid.add(corner, 0, 0);

            // Column headers (assignment titles)
            for (int c = 0; c < col_names.size(); c++) {
                Label hdr = new Label(col_names.get(c));
                hdr.setMaxWidth(Double.MAX_VALUE);
                hdr.setWrapText(true);
                hdr.setMaxWidth(120);
                hdr.setStyle(headerStyle);
                gradebookGrid.add(hdr, c + 1, 0);
            }

            // Student rows
            for (int r = 0; r < row_names.size(); r++) {
                String student = row_names.get(r);
                String rowSty  = (r % 2 == 0) ? cellStyle : evenRowStyle;

                // Student name cell
                Label nameLbl = new Label(student);
                nameLbl.setMaxWidth(Double.MAX_VALUE);
                nameLbl.setStyle(rowSty + "-fx-font-weight:bold;");
                gradebookGrid.add(nameLbl, 0, r + 1);

                Map<String, Integer> grades = matrix.get(student);
                int earned = 0, possible = 0;

                for (int c = 0; c < col_names.size(); c++) {
                    Integer g = grades.get(col_names.get(c));
                    String cellText = (g != null) ? String.valueOf(g) : "—";
                    Label cell = new Label(cellText);
                    cell.setMaxWidth(Double.MAX_VALUE);
                    cell.setAlignment(Pos.CENTER);
                    String gradeColor = g == null ? "#aaa" : (g >= 70 ? "#388e3c" : g >= 50 ? "#f57c00" : "#c62828");
                    cell.setStyle(rowSty + "-fx-text-fill:" + gradeColor + ";" +
                        (g != null ? "-fx-font-weight:bold;" : ""));
                    gradebookGrid.add(cell, c + 1, r + 1);
                    if (g != null) { earned += g; possible++; }
                }
            }

            // Set column widths
            ColumnConstraints nameCol = new ColumnConstraints(160);
            nameCol.setHgrow(Priority.NEVER);
            gradebookGrid.getColumnConstraints().add(nameCol);
            for (int c = 0; c < col_names.size(); c++) {
                ColumnConstraints cc = new ColumnConstraints(110);
                cc.setHgrow(Priority.ALWAYS);
                gradebookGrid.getColumnConstraints().add(cc);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Label err = new Label("Error loading gradebook.");
            err.setStyle("-fx-text-fill:#d32f2f;-fx-padding:16;");
            gradebookGrid.add(err, 0, 0);
        }
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    @FXML
    private void handleClose(ActionEvent event) {
        if (modal != null) modal.close();
    }

    private void injectColorConstants() {
        rootPane.setStyle(String.format(
            "-vc-primary:%s;-vc-secondary:%s;-vc-accent:%s;-vc-background:%s;-vc-text:%s;",
            ViewConstants.PRIMARY_COLOR, ViewConstants.SECONDARY_COLOR,
            ViewConstants.ACCENT_COLOR, ViewConstants.BACKGROUND_COLOR,
            ViewConstants.TEXT_COLOR));
    }
}
