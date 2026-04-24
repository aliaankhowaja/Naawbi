package naawbi.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    /**
     * Returns courses for the given user with instructor name and user's role.
     * Each Object[] row: [Course, String instructorName, String myRole]
     */
    public static List<Object[]> fetchByUserIdWithInstructor(int userId) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql =
            "SELECT c.id, c.course_name, c.course_code, c.description, c.is_active, " +
            "       u.username AS instructor_name, " +
            "       COALESCE(ce_me.role, 'instructor') AS my_role " +
            "FROM courses c " +
            "LEFT JOIN course_enrollments ce_me ON c.id = ce_me.course_id AND ce_me.user_id = ? " +
            "LEFT JOIN course_enrollments ce_inst ON c.id = ce_inst.course_id AND ce_inst.role = 'instructor' " +
            "LEFT JOIN users u ON ce_inst.user_id = u.id " +
            "WHERE (c.id IN (SELECT course_id FROM course_enrollments WHERE user_id = ?) " +
            "   OR c.created_by = ?) " +
            "ORDER BY c.id DESC";
        try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Course course = new Course(
                        rs.getInt("id"),
                        rs.getString("course_name"),
                        rs.getString("course_code"),
                        rs.getString("description"),
                        rs.getBoolean("is_active"));
                    rows.add(new Object[]{
                        course,
                        rs.getString("instructor_name"),
                        rs.getString("my_role")
                    });
                }
            }
        }
        return rows;
    }

    /**
     * Looks up a course by its code (case-insensitive). Returns empty if not found.
     */
    public static Optional<Course> fetchByCode(String code) throws SQLException {
        String sql = "SELECT id, course_name, course_code, description, is_active " +
                     "FROM courses WHERE UPPER(course_code) = UPPER(?)";
        try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Course(
                        rs.getInt("id"),
                        rs.getString("course_name"),
                        rs.getString("course_code"),
                        rs.getString("description"),
                        rs.getBoolean("is_active")));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Enrolls a user as a student in the given course.
     * Returns true if newly enrolled, false if already enrolled.
     */
    public static boolean enrollStudent(int userId, int courseId) throws SQLException {
        String sql = "INSERT INTO course_enrollments (user_id, course_id, role) " +
                     "VALUES (?, ?, 'student') ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, courseId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Fetches all users enrolled in (or teaching) this course.
     * Returns Object[] per row: [username, email, role]
     */
    public static List<Object[]> fetchEnrolledUsers(int courseId) throws SQLException {
        List<Object[]> users = new ArrayList<>();
        String sql =
            "SELECT u.username, u.email, ce.role " +
            "FROM course_enrollments ce " +
            "JOIN users u ON ce.user_id = u.id " +
            "WHERE ce.course_id = ? " +
            "ORDER BY ce.role DESC, u.username ASC";
        try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new Object[]{
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("role")
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
