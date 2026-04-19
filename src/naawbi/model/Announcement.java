package naawbi.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Announcement {
    private int id, courseId, createdBy;
    private String authorName, title, content, contentType;
    private LocalDateTime createdAt, updatedAt;

    public Announcement(int courseId, int createdBy, String title, String content, String contentType) {
        this.courseId = courseId;
        this.createdBy = createdBy;
        this.title = title;
        this.content = content;
        this.contentType = contentType;
    }

    public Announcement(int id, int courseId, int createdBy, String authorName, String title, String content,
            String contentType, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.courseId = courseId;
        this.createdBy = createdBy;
        this.authorName = authorName;
        this.title = title;
        this.content = content;
        this.contentType = contentType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int save() throws SQLException {
        String insertSQL = "INSERT INTO announcements (course_id,created_by,title,content,content_type,created_at) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP) RETURNING id";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(insertSQL)) {
            pstmt.setInt(1, courseId);
            pstmt.setInt(2, createdBy);
            pstmt.setString(3, title);
            pstmt.setString(4, content);
            pstmt.setString(5, contentType);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public static List<Announcement> fetchByCourseId(int courseId) throws SQLException {
        List<Announcement> announcements = new ArrayList<>();
        String query = "SELECT a.id, a.course_id, a.created_by, u.username, a.title, a.content, a.content_type, a.created_at, a.updated_at FROM announcements a JOIN users u ON a.created_by = u.id WHERE a.course_id = ? ORDER BY a.created_at DESC";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(query)) {
            pstmt.setInt(1, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    announcements.add(new Announcement(
                            rs.getInt("id"),
                            rs.getInt("course_id"),
                            rs.getInt("created_by"),
                            rs.getString("username"),
                            rs.getString("title"),
                            rs.getString("content"),
                            rs.getString("content_type"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("updated_at").toLocalDateTime()));
                }
            }
        }
        return announcements;
    }

    public static boolean delete(int id) throws SQLException {
        String deleteSQL = "DELETE FROM announcements WHERE id = ?";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(deleteSQL)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public int getId() {
        return id;
    }

    public int getCourseId() {
        return courseId;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
