package com.ibwaan.naawbi.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class User {
    private final int id;
    private final String username;
    private final String email;
    private final String role;

    public User(int id, String username, String email, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public static Optional<User> findByEmailAndPassword(String email, String hashedPassword)
            throws SQLException {
        String sql = "SELECT id, username, email, role FROM users WHERE email = ? AND password = ?";
        try (PreparedStatement stmt = DB.getInstance().prepareStatement(sql);) {
            stmt.setString(1, email);
            stmt.setString(2, hashedPassword);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("role")));
                }
                return Optional.empty();
            }
        }
    }

    public static Optional<User> register(String username, String email,
            String hashedPassword, String role) throws SQLException {
        String sql = "INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = DB.getInstance().prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);
            stmt.setString(4, role);
            try {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new User(rs.getInt(1), username, email, role));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                    return Optional.empty();
                }
                throw e;
            }
        }
    }

    public static List<User> fetchByCourseId(int courseId) throws SQLException {
        List<User> members = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.email, ce.role " +
                     "FROM users u " +
                     "JOIN course_enrollments ce ON u.id = ce.user_id " +
                     "WHERE ce.course_id = ? " +
                     "ORDER BY ce.role ASC, u.username ASC";
        try (PreparedStatement stmt = DB.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, courseId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    members.add(new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("role")));
                }
            }
        }
        return members;
    }

    public int getId()          { return id; }
    public String getUsername() { return username; }
    public String getEmail()    { return email; }
    public String getRole()     { return role; }
}
