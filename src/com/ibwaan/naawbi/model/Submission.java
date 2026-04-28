package com.ibwaan.naawbi.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class Submission {
    private int id, assignmentId, userId;
    private boolean submitted;
    private String filePath;
    private String status;
    private LocalDateTime submittedAt, updatedAt;

    public Submission(int assignmentId, int userId) {
        this.assignmentId = assignmentId;
        this.userId = userId;
        this.submitted = false;
        this.status = "Missing";
    }

    public Submission(int id, int assignmentId, int userId, boolean submitted, String filePath, String status,
            LocalDateTime submittedAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.userId = userId;
        this.submitted = submitted;
        this.filePath = filePath;
        this.status = status;
        this.submittedAt = submittedAt;
        this.updatedAt = updatedAt;
    }

    public boolean save() throws SQLException {
        String insertSQL = "INSERT INTO submissions (assignment_id,user_id,submitted,file_path,status,updated_at) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(insertSQL)) {
            pstmt.setInt(1, assignmentId);
            pstmt.setInt(2, userId);
            pstmt.setBoolean(3, submitted);
            pstmt.setString(4, filePath);
            pstmt.setString(5, status);
            return pstmt.executeUpdate() > 0;
        }
    }

    public static Submission findOrCreate(int assignmentId, int userId) throws SQLException {
        String insertSQL = "INSERT INTO submissions (assignment_id,user_id,submitted,status,updated_at) VALUES(?,?,false,'Missing',CURRENT_TIMESTAMP) ON CONFLICT (assignment_id,user_id) DO NOTHING";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(insertSQL)) {
            pstmt.setInt(1, assignmentId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
        return getByAssignmentAndUser(assignmentId, userId);
    }

    public static boolean markAsSubmitted(int assignmentId, int userId, String filePath, String status)
            throws SQLException {
        String updateSQL = "UPDATE submissions SET submitted = true, file_path = ?, status = ?, submitted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE assignment_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(updateSQL)) {
            pstmt.setString(1, filePath);
            pstmt.setString(2, status);
            pstmt.setInt(3, assignmentId);
            pstmt.setInt(4, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public static boolean markAsSubmitted(int assignmentId, int userId) throws SQLException {
        return markAsSubmitted(assignmentId, userId, null, "Submitted");
    }

    public static boolean unsubmit(int assignmentId, int userId) throws SQLException {
        String updateSQL = "UPDATE submissions SET submitted = false, file_path = NULL, status = 'Missing', submitted_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE assignment_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(updateSQL)) {
            pstmt.setInt(1, assignmentId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public static Submission getByAssignmentAndUser(int assignmentId, int userId) throws SQLException {
        String query = "SELECT id, assignment_id, user_id, submitted, file_path, status, submitted_at, updated_at FROM submissions WHERE assignment_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(query)) {
            pstmt.setInt(1, assignmentId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Submission(
                            rs.getInt("id"),
                            rs.getInt("assignment_id"),
                            rs.getInt("user_id"),
                            rs.getBoolean("submitted"),
                            rs.getString("file_path"),
                            rs.getString("status"),
                            rs.getTimestamp("submitted_at") != null ? rs.getTimestamp("submitted_at").toLocalDateTime()
                                    : null,
                            rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        return null;
    }

    public int getId() {
        return id;
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public int getUserId() {
        return userId;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
