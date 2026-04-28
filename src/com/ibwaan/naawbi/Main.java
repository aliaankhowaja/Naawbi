package com.ibwaan.naawbi;

import com.ibwaan.naawbi.model.DB;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("view/AssignmentDetails/AssignmentDetailsView.fxml"));
        primaryStage.setTitle("Naawbi - Assignment Detail");
        primaryStage.setScene(new Scene(root, 1920, 1080));
        primaryStage.show();
    }

    public static void main(String[] args) {
        DB.getInstance().createTables();
        launch(args);
    }
}
