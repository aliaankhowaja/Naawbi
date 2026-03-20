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

    public boolean save() throws SQLException {
        String insertSQL = "INSERT INTO courses (course_name, course_code, description, is_active) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(insertSQL)) {
            pstmt.setString(1, name);
            pstmt.setString(2, code);
            pstmt.setString(3, description);
            pstmt.setBoolean(4, active);

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
