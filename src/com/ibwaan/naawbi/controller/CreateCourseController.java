package com.ibwaan.naawbi.controller;

import com.ibwaan.naawbi.view.ViewConstants;
import com.ibwaan.naawbi.model.DB;
import com.ibwaan.naawbi.model.Course;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;


public class CreateCourseController implements Initializable {

    /* ── Root node (for color injection) ────────────────── */
    @FXML private StackPane rootPane;

    /* ── Form fields ─────────────────────────────────────── */
    @FXML private TextField  courseNameField;
    @FXML private TextField  courseCodeField;
    @FXML private TextArea   descriptionField;
    @FXML private CheckBox   visibilityToggle;

    /* ── Validation labels ───────────────────────────────── */
    @FXML private Label courseNameError;
    @FXML private Label globalError;

    /* ── Buttons ─────────────────────────────────────────── */
    @FXML private Button createBtn;
    @FXML private Button cancelBtn;

    /* ── Lifecycle ───────────────────────────────────────── */

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        injectColorConstants();

        // Clear validation state as the user types
        courseNameField.textProperty().addListener(
            (obs, oldVal, newVal) -> hideError(courseNameError)
        );
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
            ViewConstants.TEXT_COLOR
        );
        rootPane.setStyle(style);
    }

    @FXML
    private void handleCreate(ActionEvent event) {
        if (!validate()) return;

        String name        = courseNameField.getText().trim();
        String code        = courseCodeField.getText().trim();
        String description = descriptionField.getText().trim();
        boolean publish    = visibilityToggle.isSelected();

        Course newCourse = new Course(name, code, description, publish);

        try {
            if (newCourse.save()) {
                System.out.println("Course created successfully in database.");
                navigateToCatalog(event);
            } else {
                showError(globalError, "Failed to create course. Please try again.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError(globalError, "Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        navigateToCatalog(event);
    }


    private boolean validate() {
        boolean valid = true;

        if (courseNameField.getText() == null || courseNameField.getText().isBlank()) {
            showError(courseNameError, "Course name is required.");
            valid = false;
        }

        return valid;
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideError(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    private void navigateToCatalog(ActionEvent event) {
        try {
            Parent catalogRoot = FXMLLoader.load(getClass().getResource("/com/ibwaan/naawbi/view/CourseCatalog/CourseCatalogView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Naawbi - Course Catalog");
            stage.getScene().setRoot(catalogRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeStage(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
