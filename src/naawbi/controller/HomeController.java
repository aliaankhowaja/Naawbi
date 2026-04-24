package naawbi.controller;

import naawbi.model.Course;
import naawbi.model.DB;
import naawbi.model.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    private static final String[] PALETTE = {
        "#4b5d4f", "#db7706", "#7b5ec4", "#3b7fc4", "#2a7a6b", "#c4453b"
    };

    @FXML private Button actionButton;
    @FXML private FlowPane courseGrid;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        actionButton.setText(Session.getInstance().isInstructor() ? "+ Create Course" : "+ Join Course");
        loadCourses();
    }

    private void loadCourses() {
        courseGrid.getChildren().clear();
        try {
            List<Object[]> rows = Course.fetchByUserIdWithInstructor(Session.getInstance().getUserId());
            for (Object[] row : rows) {
                Course course = (Course) row[0];
                String instructorName = (String) row[1];
                courseGrid.getChildren().add(buildCard(course, instructorName));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox buildCard(Course course, String instructorName) {
        String color = PALETTE[Math.abs(course.getId()) % PALETTE.length];

        // Header
        AnchorPane header = new AnchorPane();
        header.setPrefHeight(90);
        header.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8 8 0 0;");

        Label courseName = new Label(course.getName());
        courseName.getStyleClass().add("card-course-name");
        courseName.setMaxWidth(220);
        AnchorPane.setBottomAnchor(courseName, 20.0);
        AnchorPane.setLeftAnchor(courseName, 12.0);
        AnchorPane.setRightAnchor(courseName, 36.0);

        Label courseCode = new Label(course.getCode());
        courseCode.getStyleClass().add("card-course-code");
        AnchorPane.setBottomAnchor(courseCode, 6.0);
        AnchorPane.setLeftAnchor(courseCode, 12.0);

        Button menuBtn = new Button("⋮");
        menuBtn.getStyleClass().add("card-menu-btn");
        AnchorPane.setTopAnchor(menuBtn, 6.0);
        AnchorPane.setRightAnchor(menuBtn, 6.0);

        header.getChildren().addAll(courseName, courseCode, menuBtn);

        // Body
        AnchorPane body = new AnchorPane();
        body.setPrefHeight(52);
        body.setStyle("-fx-background-color: white;");

        String initial = (instructorName != null && !instructorName.isEmpty())
            ? String.valueOf(instructorName.charAt(0)).toUpperCase() : "?";

        Label avatar = new Label(initial);
        avatar.getStyleClass().add("card-avatar");
        avatar.setStyle("-fx-background-color: " + color + ";");
        AnchorPane.setTopAnchor(avatar, -19.0);
        AnchorPane.setRightAnchor(avatar, 12.0);

        Label instructor = new Label(instructorName != null ? instructorName : "");
        instructor.getStyleClass().add("card-instructor");
        AnchorPane.setTopAnchor(instructor, 16.0);
        AnchorPane.setLeftAnchor(instructor, 12.0);

        body.getChildren().addAll(avatar, instructor);

        // Footer
        HBox footer = new HBox();
        footer.getStyleClass().add("card-footer");
        footer.setPrefHeight(20);

        // Card
        VBox card = new VBox(header, body, footer);
        card.getStyleClass().add("course-card");
        card.setPrefWidth(280);
        card.setOnMouseClicked(event -> handleCardClick(course, event));

        return card;
    }

    @FXML
    private void handleActionButton(ActionEvent event) {
        if (Session.getInstance().isInstructor()) {
            openCreateCourse(event);
        } else {
            openJoinCourse();
        }
    }

    private void openCreateCourse(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/naawbi/view/CreateCourse/CreateCourseView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Naawbi - Create Course");
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openJoinCourse() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Join a Course");
        dialog.setHeaderText("Join a Course");
        dialog.setContentText("Enter the course code:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(code -> {
            if (code.isBlank()) return;
            try {
                Optional<Course> found = Course.fetchByCode(code.trim());
                if (found.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Not Found", "No course found with that code.");
                    return;
                }
                Course course = found.get();
                int userId = Session.getInstance().getUserId();
                String sql = "INSERT INTO course_enrollments (user_id, course_id, role) " +
                             "VALUES (?, ?, 'student') ON CONFLICT DO NOTHING";
                try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, course.getId());
                    int rows = ps.executeUpdate();
                    if (rows == 0) {
                        showAlert(Alert.AlertType.INFORMATION, "Already Enrolled",
                            "You are already enrolled in \"" + course.getName() + "\".");
                        return;
                    }
                }
                loadCourses();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Database error: " + e.getMessage());
            }
        });
    }

    private void handleCardClick(Course course, MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/naawbi/view/CourseCatalog/CourseCatalogView.fxml"));
            Parent root = loader.load();
            CourseCatalogController controller = loader.getController();
            controller.initWithCourse(course);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Naawbi");
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
