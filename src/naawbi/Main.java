package naawbi;

import naawbi.model.DB;
import naawbi.model.Session;
import naawbi.model.User;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    // Set to true to skip login and go straight to catalog (development only)
    private static final boolean DEV_MODE = true;
    private static final String DEV_ROLE = "instructor"; // "student" or "instructor"

    @Override
    public void start(Stage primaryStage) throws Exception {
        String view = DEV_MODE
                ? "view/Home/HomeView.fxml"
                : "view/Login/LoginView.fxml";
        Parent root = FXMLLoader.load(getClass().getResource(view));
        primaryStage.setTitle("Naawbi");
        primaryStage.setScene(new Scene(root, 1920, 1080));
        primaryStage.show();
    }

    public static void main(String[] args) {
        DB.getInstance().createTables();
        if (DEV_MODE) {
            Session.getInstance().login(new User(1, "dev", "dev@naawbi.com", DEV_ROLE));
            try {
                String sql = "INSERT INTO course_enrollments (course_id, user_id, role) " +
                             "SELECT id, 1, ? FROM courses ON CONFLICT DO NOTHING";
                try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
                    ps.setString(1, DEV_ROLE);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        launch(args);
    }
}
