package com.ibwaan.naawbi.controller;

import com.ibwaan.naawbi.model.PasswordUtil;
import com.ibwaan.naawbi.model.Session;
import com.ibwaan.naawbi.model.User;
import com.ibwaan.naawbi.view.ViewConstants;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private Label signInTab;
    @FXML private Label registerTab;
    @FXML private VBox signInForm;
    @FXML private VBox registerForm;
    @FXML private TextField loginEmailField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Label loginErrorLabel;
    @FXML private TextField regUsernameField;
    @FXML private TextField regEmailField;
    @FXML private PasswordField regPasswordField;
    @FXML private Button studentRoleBtn;
    @FXML private Button instructorRoleBtn;
    @FXML private Label registerErrorLabel;

    private String selectedRole = "student";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        injectColorConstants();
        clearErrorsOnType();
    }

    private void injectColorConstants() {
        rootPane.setStyle(String.format(
                "-vc-primary: %s; -vc-secondary: %s; -vc-accent: %s;" +
                "-vc-background: %s; -vc-text: %s;",
                ViewConstants.PRIMARY_COLOR, ViewConstants.SECONDARY_COLOR,
                ViewConstants.ACCENT_COLOR, ViewConstants.BACKGROUND_COLOR,
                ViewConstants.TEXT_COLOR));
    }

    private void clearErrorsOnType() {
        loginEmailField.textProperty().addListener((o, old, n) -> hideError(loginErrorLabel));
        loginPasswordField.textProperty().addListener((o, old, n) -> hideError(loginErrorLabel));
        regUsernameField.textProperty().addListener((o, old, n) -> hideError(registerErrorLabel));
        regEmailField.textProperty().addListener((o, old, n) -> hideError(registerErrorLabel));
        regPasswordField.textProperty().addListener((o, old, n) -> hideError(registerErrorLabel));
    }

    @FXML
    private void handleSignInTab(MouseEvent event) {
        signInForm.setVisible(true);
        signInForm.setManaged(true);
        registerForm.setVisible(false);
        registerForm.setManaged(false);
        signInTab.getStyleClass().add("login-tab-active");
        registerTab.getStyleClass().remove("login-tab-active");
    }

    @FXML
    private void handleRegisterTab(MouseEvent event) {
        registerForm.setVisible(true);
        registerForm.setManaged(true);
        signInForm.setVisible(false);
        signInForm.setManaged(false);
        registerTab.getStyleClass().add("login-tab-active");
        signInTab.getStyleClass().remove("login-tab-active");
    }

    @FXML
    private void handleRoleStudent(ActionEvent event) {
        selectedRole = "student";
        studentRoleBtn.getStyleClass().add("role-chip-active");
        instructorRoleBtn.getStyleClass().remove("role-chip-active");
    }

    @FXML
    private void handleRoleInstructor(ActionEvent event) {
        selectedRole = "instructor";
        instructorRoleBtn.getStyleClass().add("role-chip-active");
        studentRoleBtn.getStyleClass().remove("role-chip-active");
    }

    @FXML
    private void handleSignIn(ActionEvent event) {
        String email = loginEmailField.getText().trim();
        String password = loginPasswordField.getText();
        if (email.isEmpty() || password.isEmpty()) {
            showError(loginErrorLabel, "Please fill in all fields");
            return;
        }
        try {
            Optional<User> user = User.findByEmailAndPassword(email, PasswordUtil.hash(password));
            if (user.isPresent()) {
                Session.getInstance().login(user.get());
                navigateToCatalog(loginErrorLabel);
            } else {
                showError(loginErrorLabel, "Invalid email or password");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError(loginErrorLabel, "Something went wrong. Please try again.");
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = regUsernameField.getText().trim();
        String email = regEmailField.getText().trim();
        String password = regPasswordField.getText();
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError(registerErrorLabel, "Please fill in all fields");
            return;
        }
        try {
            Optional<User> user = User.register(username, email, PasswordUtil.hash(password), selectedRole);
            if (user.isPresent()) {
                Session.getInstance().login(user.get());
                navigateToCatalog(registerErrorLabel);
            } else {
                showError(registerErrorLabel, "Username or email is already taken");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError(registerErrorLabel, "Something went wrong. Please try again.");
        }
    }

    private void navigateToCatalog(Label activeErrorLabel) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/ibwaan/naawbi/view/CourseCatalog/CourseCatalogView.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root, 1920, 1080));
            stage.setTitle("Naawbi");
        } catch (IOException e) {
            e.printStackTrace();
            showError(activeErrorLabel, "Something went wrong. Please try again.");
        }
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
}
