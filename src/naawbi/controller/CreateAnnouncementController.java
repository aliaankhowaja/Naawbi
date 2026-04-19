package naawbi.controller;

import naawbi.model.Announcement;
import naawbi.model.AnnouncementAttachment;
import naawbi.model.Course;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CreateAnnouncementController implements Initializable {
    @FXML
    private Label courseNameLabel;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea contentArea;
    @FXML
    private Button boldBtn, italicBtn, underlineBtn, bulletBtn, uploadBtn, addLinkBtn;
    @FXML
    private TextField linkUrlField;
    @FXML
    private VBox fileListContainer;

    private int courseId, currentUserId;
    private List<File> attachedFiles;
    private List<String[]> attachedLinks; // [displayName, url]
    private Runnable onSuccessCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        attachedFiles = new ArrayList<>();
        attachedLinks = new ArrayList<>();
    }

    public void setCourseContext(int courseId, int userId, Course course, Runnable onSuccess) {
        this.courseId = courseId;
        this.currentUserId = userId;
        this.onSuccessCallback = onSuccess;
        if (course != null)
            courseNameLabel.setText(course.getName());
    }

    @FXML
    private void handleBold(ActionEvent event) {
        toggleFormatting("b");
    }

    @FXML
    private void handleItalic(ActionEvent event) {
        toggleFormatting("i");
    }

    @FXML
    private void handleUnderline(ActionEvent event) {
        toggleFormatting("u");
    }

    @FXML
    private void handleBulletList(ActionEvent event) {
        int cursorPos = contentArea.getCaretPosition();
        String before = contentArea.getText().substring(0, cursorPos);
        String after = contentArea.getText().substring(cursorPos);
        String bulletText = "\n<ul>\n<li>Item 1</li>\n<li>Item 2</li>\n</ul>\n";
        contentArea.setText(before + bulletText + after);
        contentArea.positionCaret(cursorPos + bulletText.length());
    }

    @FXML
    private void handleFileUpload(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select file to attach");
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
        Stage stage = (Stage) uploadBtn.getScene().getWindow();
        List<File> files = fc.showOpenMultipleDialog(stage);
        if (files != null)
            for (File file : files) {
                attachedFiles.add(file);
                addFileToUI(file);
            }
    }

    private void addFileToUI(File file) {
        HBox fileItem = new HBox(8);
        fileItem.setStyle("-fx-padding: 4;");
        Label fileName = new Label("📎 " + file.getName());
        Button removeBtn = new Button("✕");
        removeBtn.setOnAction(e -> {
            attachedFiles.remove(file);
            fileListContainer.getChildren().remove(fileItem);
        });
        fileItem.getChildren().addAll(fileName, removeBtn);
        fileListContainer.getChildren().add(fileItem);
    }

    @FXML
    private void handleAddLink(ActionEvent event) {
        String url = linkUrlField.getText().trim();
        if (url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            showError("Please enter a valid URL starting with http:// or https://");
            return;
        }
        String[] entry = { url, url };
        attachedLinks.add(entry);
        addLinkToUI(entry);
        linkUrlField.clear();
    }

    private void addLinkToUI(String[] entry) {
        HBox linkItem = new HBox(8);
        linkItem.setStyle("-fx-padding: 4;");
        Label linkLabel = new Label("🔗 " + entry[0]);
        Button removeBtn = new Button("✕");
        removeBtn.setOnAction(e -> {
            attachedLinks.remove(entry);
            fileListContainer.getChildren().remove(linkItem);
        });
        linkItem.getChildren().addAll(linkLabel, removeBtn);
        fileListContainer.getChildren().add(linkItem);
    }

    private void toggleFormatting(String tag) {
        String selectedText = contentArea.getSelectedText();
        if (selectedText == null || selectedText.isEmpty()) {
            contentArea.insertText(contentArea.getCaretPosition(), "<" + tag + ">text</" + tag + ">");
        } else {
            int startIdx = contentArea.getSelection().getStart();
            String wrapped = "<" + tag + ">" + selectedText + "</" + tag + ">";
            contentArea.replaceText(contentArea.getSelection(), wrapped);
            contentArea.positionCaret(startIdx + wrapped.length());
        }
    }

    @FXML
    private void handlePost(ActionEvent event) {
        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();

        if (title.isEmpty()) {
            showError("Title is required");
            return;
        }
        if (content.isEmpty()) {
            showError("Content is required");
            return;
        }

        try {
            Announcement announcement = new Announcement(courseId, currentUserId, title, content, "html");
            int announcementId = announcement.save();
            if (announcementId > 0) {
                for (File f : attachedFiles)
                    AnnouncementAttachment.saveFile(announcementId, f.getName(), f.getAbsolutePath(), f.length());
                for (String[] link : attachedLinks)
                    AnnouncementAttachment.saveLink(announcementId, link[0], link[1]);
                if (onSuccessCallback != null)
                    onSuccessCallback.run();
                closeDialog();
            } else
                showError("Failed to save announcement");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setContentText(message);
        a.showAndWait();
    }

    private void showInfo(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Success");
        a.setContentText(message);
        a.showAndWait();
    }
}
