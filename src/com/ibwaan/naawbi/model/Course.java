package com.ibwaan.naawbi.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Course {
    private int id;
    private String name;
    private String code;
    private String description;
    private boolean active;

    public Course(int id, String name, String code, String description, boolean active) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.active = active;
    }

    public Course(String name, String code, String description, boolean active) {
        this.id = -1;
        this.name = name;
        this.code = code;
        this.description = description;
        this.active = active;
    }

    public static List<Course> fetchAll() throws SQLException {
        List<Course> courses = new ArrayList<>();
        String query = "SELECT * FROM courses ORDER BY id DESC";
        try (ResultSet rs = DB.getInstance().executeQuery(query)) {
            while (rs.next()) {
                courses.add(new Course(
                        rs.getInt("id"),
                        rs.getString("course_name"),
                        rs.getString("course_code"),
                        rs.getString("description"),
                        rs.getBoolean("is_active")));
            }
        }
        return courses;
    }

    public static List<Course> fetchByUserId(int userId) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String query =
            "SELECT DISTINCT c.id, c.course_name, c.course_code, c.description, c.is_active " +
            "FROM courses c " +
            "WHERE c.id IN (SELECT course_id FROM course_enrollments WHERE user_id = ?) " +
            "   OR c.created_by = ? " +
            "ORDER BY c.id DESC";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(new Course(
                            rs.getInt("id"),
                            rs.getString("course_name"),
                            rs.getString("course_code"),
                            rs.getString("description"),
                            rs.getBoolean("is_active")));
                }
            }
        }
        return courses;
    }

    /** Returns all enrolled users for a course: [username, email, role] */
    public static java.util.List<Object[]> fetchEnrolledUsers(int courseId) throws SQLException {
        java.util.List<Object[]> users = new java.util.ArrayList<>();
        String sql =
            "SELECT u.username, u.email, ce.role " +
            "FROM course_enrollments ce JOIN users u ON ce.user_id = u.id " +
            "WHERE ce.course_id = ? ORDER BY ce.role DESC, u.username ASC";
        try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new Object[]{
                        rs.getString("username"), rs.getString("email"), rs.getString("role")
                    });
                }
            }
        }
        return users;
    }

    public boolean save() throws SQLException {

        int createdBy = Session.getInstance().getUserId();
        String insertSQL = "INSERT INTO courses (course_name, course_code, description, is_active, created_by) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(insertSQL)) {
            pstmt.setString(1, name);
            pstmt.setString(2, code);
            pstmt.setString(3, description);
            pstmt.setBoolean(4, active);
            pstmt.setInt(5, createdBy);

            return pstmt.executeUpdate() > 0;
        }
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
