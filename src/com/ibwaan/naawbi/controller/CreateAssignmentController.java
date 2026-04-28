package com.ibwaan.naawbi.controller;

import com.ibwaan.naawbi.model.Assignment;
import com.ibwaan.naawbi.view.ViewConstants;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

/**
 * Controller for CreateAssignmentView.fxml
 *
 * Responsibilities:
 * 1. Handle form input for creating new assignments
 * 2. Validate required fields (title, deadline)
 * 3. Combine date picker + time spinners into a single LocalDateTime
 * 4. Insert assignment into database and close modal
 */
public class CreateAssignmentController implements Initializable {

    /* ── Root (color injection) ─────────────────────── */
    @FXML
    private StackPane rootPane;

    /* ── Form Fields ────────────────────────────────── */
    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private DatePicker deadlineDate;

    @FXML
    private Spinner<Integer> deadlineHour;

    @FXML
    private Spinner<Integer> deadlineMinute;

    @FXML
    private TextField pointsField;

    @FXML
    private CheckBox lateAllowedToggle;

    @FXML
    private CheckBox publishToggle;

    /* ── Error Labels ───────────────────────────────── */
    @FXML
    private Label titleError;

    @FXML
    private Label deadlineError;

    @FXML
    private Label globalError;

    /* ── Buttons ────────────────────────────────────── */
    @FXML
    private Button cancelBtn;

    @FXML
    private Button createBtn;

    /* ── State ──────────────────────────────────────── */
    private int courseId;
    private int userId;
    private Stage modal;

    /* ── Lifecycle ───────────────────────────────────── */

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeSpinners();
        injectColorConstants();
        setupValidationListeners();
    }

    /**
     * Sets the context for creating an assignment.
     * Must be called before showing the modal.
     */
    public void setCourseContext(int courseId, int userId, Stage modal) {
        this.courseId = courseId;
        this.userId = userId;
        this.modal = modal;
    }

    /**
     * Initializes hour and minute spinners with proper ranges.
     * Hour: 0-23
     * Minute: 0-59 (step 5)
     */
    private void initializeSpinners() {
        SpinnerValueFactory.IntegerSpinnerValueFactory hourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, 23, 11);
        deadlineHour.setValueFactory(hourFactory);

        SpinnerValueFactory.IntegerSpinnerValueFactory minuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, 59, 0, 5);
        deadlineMinute.setValueFactory(minuteFactory);
    }

    /**
     * Adds change listeners to clear error labels on input
     */
    private void setupValidationListeners() {
        titleField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                titleError.setVisible(false);
                titleError.setManaged(false);
            }
        });

        deadlineDate.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                deadlineError.setVisible(false);
                deadlineError.setManaged(false);
            }
        });
    }

    /**
     * Cancel button handler: closes the modal without creating an assignment
     */
    @FXML
    private void handleCancel(ActionEvent event) {
        if (modal != null) {
            modal.close();
        }
    }

    /**
     * Create button handler: validates form, creates assignment, closes modal
     */
    @FXML
    private void handleCreate(ActionEvent event) {
        if (!validateForm()) {
            return;
        }

        try {
            String title = titleField.getText().trim();
            String description = descriptionField.getText().trim();
            LocalDateTime deadline = combineDateTime();

            int totalPoints = 100;
            try {
                if (!pointsField.getText().trim().isEmpty()) {
                    totalPoints = Integer.parseInt(pointsField.getText().trim());
                }
            } catch (NumberFormatException e) {
                // Default to 100
            }

            boolean lateAllowed = lateAllowedToggle.isSelected();

            // Create and save assignment to database
            Assignment assignment = new Assignment(courseId, userId, title, description, deadline, totalPoints,
                    lateAllowed);
            assignment.save();

            System.out.println("Assignment created: " + title);

            // Close modal on success
            if (modal != null) {
                modal.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            globalError.setText("Database error: " + e.getMessage());
            globalError.setVisible(true);
            globalError.setManaged(true);
        }
    }

    /**
     * Validates the form before submission:
     * - Title must not be empty
     * - Deadline date must be selected
     * Returns false if validation fails; error labels are set
     */
    private boolean validateForm() {
        boolean isValid = true;

        // Clear previous errors
        titleError.setVisible(false);
        titleError.setManaged(false);
        deadlineError.setVisible(false);
        deadlineError.setManaged(false);
        globalError.setVisible(false);
        globalError.setManaged(false);

        // Validate title
        if (titleField.getText().trim().isEmpty()) {
            titleError.setText("Title is required");
            titleError.setVisible(true);
            titleError.setManaged(true);
            isValid = false;
        }

        // Validate deadline date
        if (deadlineDate.getValue() == null) {
            deadlineError.setText("Deadline date is required");
            deadlineError.setVisible(true);
            deadlineError.setManaged(true);
            isValid = false;
        }

        return isValid;
    }

    /**
     * Combines the DatePicker value with the hour and minute spinners
     * into a single LocalDateTime representing the assignment deadline.
     */
    private LocalDateTime combineDateTime() {
        LocalDate date = deadlineDate.getValue();
        int hour = deadlineHour.getValue();
        int minute = deadlineMinute.getValue();

        LocalTime time = LocalTime.of(hour, minute);
        return LocalDateTime.of(date, time);
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
        rootPane.setStyle(style);
    }
}
