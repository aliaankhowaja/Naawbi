package naawbi.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Submission {
    private int id, assignmentId, userId;
    private boolean submitted;
    private String filePath;
    private String status;
    private Integer grade;       // null = not graded yet
    private String feedback;     // instructor feedback text
    /** Populated by fetchByAssignmentId() */
    private String studentName;
    /** Populated by fetchGradedForStudent() */
    private String assignmentTitle;
    private String courseName;
    private int totalPoints;

    private LocalDateTime submittedAt, updatedAt;

    // ── Constructors ────────────────────────────────────────────────────────

    public Submission(int assignmentId, int userId) {
        this.assignmentId = assignmentId;
        this.userId = userId;
        this.submitted = false;
        this.status = "Missing";
    }

    /** Legacy constructor — no grade/feedback; kept for backward compat */
    public Submission(int id, int assignmentId, int userId, boolean submitted,
                      String filePath, String status,
                      LocalDateTime submittedAt, LocalDateTime updatedAt) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.userId = userId;
        this.submitted = submitted;
        this.filePath = filePath;
        this.status = status;
        this.submittedAt = submittedAt;
        this.updatedAt = updatedAt;
    }

    /** Full constructor including grade and feedback (Sprint 3) */
    public Submission(int id, int assignmentId, int userId, boolean submitted,
                      String filePath, String status, Integer grade, String feedback,
                      LocalDateTime submittedAt, LocalDateTime updatedAt) {
        this(id, assignmentId, userId, submitted, filePath, status, submittedAt, updatedAt);
        this.grade = grade;
        this.feedback = feedback;
    }

    // ── Write operations ────────────────────────────────────────────────────

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
        String insertSQL = "INSERT INTO submissions (assignment_id,user_id,submitted,status,updated_at) " +
                           "VALUES(?,?,false,'Missing',CURRENT_TIMESTAMP) ON CONFLICT (assignment_id,user_id) DO NOTHING";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(insertSQL)) {
            pstmt.setInt(1, assignmentId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
        return getByAssignmentAndUser(assignmentId, userId);
    }

    public static boolean markAsSubmitted(int assignmentId, int userId, String filePath, String status) throws SQLException {
        String updateSQL = "UPDATE submissions SET submitted = true, file_path = ?, status = ?, " +
                           "submitted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP " +
                           "WHERE assignment_id = ? AND user_id = ?";
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
        String updateSQL = "UPDATE submissions SET submitted = false, file_path = NULL, status = 'Missing', " +
                           "submitted_at = NULL, updated_at = CURRENT_TIMESTAMP " +
                           "WHERE assignment_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(updateSQL)) {
            pstmt.setInt(1, assignmentId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Persists an instructor grade + optional feedback for a specific submission row.
     */
    public static boolean saveGrade(int submissionId, int grade, String feedback) throws SQLException {
        String sql = "UPDATE submissions SET grade = ?, feedback = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
            ps.setInt(1, grade);
            if (feedback != null && !feedback.isBlank()) {
                ps.setString(2, feedback);
            } else {
                ps.setNull(2, java.sql.Types.VARCHAR);
            }
            ps.setInt(3, submissionId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Read operations ─────────────────────────────────────────────────────

    public static Submission getByAssignmentAndUser(int assignmentId, int userId) throws SQLException {
        String query = "SELECT id, assignment_id, user_id, submitted, file_path, status, " +
                       "grade, feedback, submitted_at, updated_at " +
                       "FROM submissions WHERE assignment_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(query)) {
            pstmt.setInt(1, assignmentId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return fromResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Fetches all submission rows for a given assignment, joined with student usernames.
     * Returns one row per enrolled student (including those who haven't submitted yet).
     * Used by GradingController (US 19 & 20).
     */
    public static List<Submission> fetchByAssignmentId(int assignmentId) throws SQLException {
        List<Submission> list = new ArrayList<>();
        // LEFT JOIN ensures we catch students with no submission row yet
        String sql =
            "SELECT s.id, s.assignment_id, s.user_id, s.submitted, s.file_path, s.status, " +
            "       s.grade, s.feedback, s.submitted_at, s.updated_at, " +
            "       u.username AS student_name " +
            "FROM submissions s " +
            "JOIN users u ON s.user_id = u.id " +
            "WHERE s.assignment_id = ? " +
            "ORDER BY u.username ASC";
        try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
            ps.setInt(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Submission sub = fromResultSet(rs);
                    sub.studentName = rs.getString("student_name");
                    list.add(sub);
                }
            }
        }
        return list;
    }

    /**
     * Fetches all graded submissions for a student across all enrolled courses.
     * Used by GradebookController (US 22 — student personal gradebook).
     */
    public static List<Submission> fetchGradedForStudent(int userId) throws SQLException {
        List<Submission> list = new ArrayList<>();
        String sql =
            "SELECT s.id, s.assignment_id, s.user_id, s.submitted, s.file_path, s.status, " +
            "       s.grade, s.feedback, s.submitted_at, s.updated_at, " +
            "       a.title AS assignment_title, a.total_points, c.course_name " +
            "FROM submissions s " +
            "JOIN assignments a ON s.assignment_id = a.id " +
            "JOIN courses c ON a.course_id = c.id " +
            "WHERE s.user_id = ? AND s.grade IS NOT NULL " +
            "ORDER BY c.course_name ASC, a.deadline ASC";
        try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Submission sub = fromResultSet(rs);
                    sub.assignmentTitle = rs.getString("assignment_title");
                    sub.totalPoints     = rs.getInt("total_points");
                    sub.courseName      = rs.getString("course_name");
                    list.add(sub);
                }
            }
        }
        return list;
    }

    /**
     * Fetches all submissions for every assignment in a course, joined with student names.
     * Used by GradebookController (US 21 — instructor class gradebook).
     */
    public static List<Submission> fetchGradebookForCourse(int courseId) throws SQLException {
        List<Submission> list = new ArrayList<>();
        String sql =
            "SELECT s.id, s.assignment_id, s.user_id, s.submitted, s.file_path, s.status, " +
            "       s.grade, s.feedback, s.submitted_at, s.updated_at, " +
            "       u.username AS student_name, a.title AS assignment_title, a.total_points " +
            "FROM submissions s " +
            "JOIN users u ON s.user_id = u.id " +
            "JOIN assignments a ON s.assignment_id = a.id " +
            "WHERE a.course_id = ? " +
            "ORDER BY u.username ASC, a.deadline ASC";
        try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Submission sub = fromResultSet(rs);
                    sub.studentName     = rs.getString("student_name");
                    sub.assignmentTitle = rs.getString("assignment_title");
                    sub.totalPoints     = rs.getInt("total_points");
                    list.add(sub);
                }
            }
        }
        return list;
    }

    /** Helper: hydrate a Submission from a ResultSet positioned at a valid row */
    private static Submission fromResultSet(ResultSet rs) throws SQLException {
        int grade = rs.getInt("grade");
        Integer gradeVal = rs.wasNull() ? null : grade;
        return new Submission(
            rs.getInt("id"),
            rs.getInt("assignment_id"),
            rs.getInt("user_id"),
            rs.getBoolean("submitted"),
            rs.getString("file_path"),
            rs.getString("status"),
            gradeVal,
            rs.getString("feedback"),
            rs.getTimestamp("submitted_at") != null ? rs.getTimestamp("submitted_at").toLocalDateTime() : null,
            rs.getTimestamp("updated_at") != null    ? rs.getTimestamp("updated_at").toLocalDateTime()  : null
        );
    }

    // ── Getters / Setters ───────────────────────────────────────────────────

    public int getId()              { return id; }
    public int getAssignmentId()    { return assignmentId; }
    public int getUserId()          { return userId; }
    public boolean isSubmitted()    { return submitted; }
    public String getFilePath()     { return filePath; }
    public String getStatus()       { return status; }
    public Integer getGrade()       { return grade; }
    public String getFeedback()     { return feedback; }
    public String getStudentName()  { return studentName; }
    public String getAssignmentTitle() { return assignmentTitle; }
    public String getCourseName()   { return courseName; }
    public int getTotalPoints()     { return totalPoints; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getUpdatedAt()   { return updatedAt; }

    public void setSubmitted(boolean submitted)          { this.submitted = submitted; }
    public void setFilePath(String filePath)             { this.filePath = filePath; }
    public void setStatus(String status)                 { this.status = status; }
    public void setGrade(Integer grade)                  { this.grade = grade; }
    public void setFeedback(String feedback)             { this.feedback = feedback; }
    public void setSubmittedAt(LocalDateTime submittedAt){ this.submittedAt = submittedAt; }
}
