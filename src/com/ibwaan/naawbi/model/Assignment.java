package com.ibwaan.naawbi.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Assignment {
private int id, courseId, createdBy, totalPoints;
    private String authorName, title, description;
    /** Populated only when fetched via fetchWithStatusForUser() */
    private String userStatus;
    private boolean lateSubmissionsAllowed;
    private LocalDateTime deadline, createdAt, updatedAt;

    public Assignment(int courseId, int createdBy, String title, String description, LocalDateTime deadline, int totalPoints, boolean lateSubmissionsAllowed) {
        this.courseId = courseId;
        this.createdBy = createdBy;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.totalPoints = totalPoints;
        this.lateSubmissionsAllowed = lateSubmissionsAllowed;
    }

    public Assignment(int id, int courseId, int createdBy, String authorName, String title, String description,
                            LocalDateTime deadline, LocalDateTime createdAt, LocalDateTime updatedAt, int totalPoints, boolean lateSubmissionsAllowed) {
        this.id = id;
        this.courseId = courseId;
        this.createdBy = createdBy;
        this.authorName = authorName;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.totalPoints = totalPoints;
        this.lateSubmissionsAllowed = lateSubmissionsAllowed;
    }

    public boolean save() throws SQLException {
        String insertSQL = "INSERT INTO assignments (course_id,created_by,title,description,deadline,total_points,late_submissions_allowed,created_at) VALUES(?,?,?,?,?,?,?,CURRENT_TIMESTAMP)";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(insertSQL)) {
            pstmt.setInt(1, courseId);
            pstmt.setInt(2, createdBy);
            pstmt.setString(3, title);
            pstmt.setString(4, description);
            pstmt.setObject(5, deadline);
            pstmt.setInt(6, totalPoints);
            pstmt.setBoolean(7, lateSubmissionsAllowed);
            return pstmt.executeUpdate() > 0;
        }
    }

    public static List<Assignment> fetchByCourseId(int courseId) throws SQLException {
        List<Assignment> assignments = new ArrayList<>();
        String query = "SELECT a.id, a.course_id, a.created_by, u.username, a.title, a.description, a.deadline, a.created_at, a.updated_at, a.total_points, a.late_submissions_allowed FROM assignments a JOIN users u ON a.created_by = u.id WHERE a.course_id = ? ORDER BY a.deadline ASC";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(query)) {
            pstmt.setInt(1, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    assignments.add(new Assignment(
                            rs.getInt("id"),
                            rs.getInt("course_id"),
                            rs.getInt("created_by"),
                            rs.getString("username"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getTimestamp("deadline").toLocalDateTime(),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("updated_at").toLocalDateTime(),
                            rs.getInt("total_points"),
                            rs.getBoolean("late_submissions_allowed")));
                }
            }
        }
        return assignments;
    }

    public static List<Assignment> fetchStudentPending(int courseId, int userId) throws SQLException {
        List<Assignment> assignments = new ArrayList<>();
        String query = "SELECT a.id, a.course_id, a.created_by, u.username, a.title, a.description, a.deadline, a.created_at, a.updated_at, a.total_points, a.late_submissions_allowed FROM assignments a JOIN users u ON a.created_by = u.id WHERE a.course_id = ? AND a.id NOT IN (SELECT assignment_id FROM submissions WHERE user_id = ? AND submitted = true) ORDER BY a.deadline ASC";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(query)) {
            pstmt.setInt(1, courseId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    assignments.add(new Assignment(
                            rs.getInt("id"),
                            rs.getInt("course_id"),
                            rs.getInt("created_by"),
                            rs.getString("username"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getTimestamp("deadline").toLocalDateTime(),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("updated_at").toLocalDateTime(),
                            rs.getInt("total_points"),
                            rs.getBoolean("late_submissions_allowed")));
                }
            }
        }
        return assignments;
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

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public boolean isLateSubmissionsAllowed() {
        return lateSubmissionsAllowed;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public String getUserStatus() { return userStatus; }
    public void setUserStatus(String s) { this.userStatus = s; }

    /**
     * Fetches all assignments for a course with the given user's submission status
     * joined inline. Status will be "Submitted", "Late", "Missing", or null (not yet
     * initialised = no submission row exists).
     */
    public static List<Assignment> fetchWithStatusForUser(int courseId, int userId) throws SQLException {
        List<Assignment> list = new ArrayList<>();
        String sql =
            "SELECT a.id, a.course_id, a.created_by, u.username, a.title, a.description, " +
            "       a.deadline, a.created_at, a.updated_at, a.total_points, a.late_submissions_allowed, " +
            "       s.submitted, s.status " +
            "FROM assignments a " +
            "JOIN users u ON a.created_by = u.id " +
            "LEFT JOIN submissions s ON s.assignment_id = a.id AND s.user_id = ? " +
            "WHERE a.course_id = ? " +
            "ORDER BY a.deadline ASC";
        try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Assignment a = new Assignment(
                        rs.getInt("id"),
                        rs.getInt("course_id"),
                        rs.getInt("created_by"),
                        rs.getString("username"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getTimestamp("deadline").toLocalDateTime(),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime(),
                        rs.getInt("total_points"),
                        rs.getBoolean("late_submissions_allowed"));
                    // Determine display status
                    boolean submitted = rs.getBoolean("submitted");
                    String dbStatus   = rs.getString("status");
                    if (submitted) {
                        a.setUserStatus(dbStatus != null ? dbStatus : "Submitted");
                    } else {
                        LocalDateTime now = LocalDateTime.now();
                        a.setUserStatus(rs.getTimestamp("deadline") != null
                            && a.getDeadline().isBefore(now) ? "Missing" : "Not Submitted");
                    }
                    list.add(a);
                }
            }
        }
        return list;
    }
}

