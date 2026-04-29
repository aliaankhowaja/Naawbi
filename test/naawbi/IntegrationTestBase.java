package naawbi;

import naawbi.model.DB;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * Base class for integration tests that hit a real PostgreSQL database.
 *
 *  - Refuses to run unless -Dnaawbi.db.url points at a database whose name
 *    contains "naawbi_test" (safety net to keep dev data untouched).
 *  - Creates the schema once via DB.createTables().
 *  - Wipes every table before each test so order is irrelevant and tests
 *    are completely independent.
 *
 * Subclasses get a clean slate at the start of every @Test method.
 */
public abstract class IntegrationTestBase {

    /** Tables in dependency-safe order — TRUNCATE ... CASCADE handles FKs anyway. */
    private static final String TRUNCATE_ALL =
        "TRUNCATE TABLE " +
        "submission_comments, submissions, assignments, " +
        "announcement_attachments, announcements, " +
        "course_enrollments, courses, users " +
        "RESTART IDENTITY CASCADE";

    @BeforeAll
    static void ensureTestDatabaseAndSchema() throws SQLException {
        String url = System.getProperty("naawbi.db.url", "");
        if (!url.contains("naawbi_test")) {
            throw new IllegalStateException(
                "Refusing to run integration tests against a non-test database.\n" +
                "Got naawbi.db.url=" + (url.isEmpty() ? "<unset>" : url) + "\n" +
                "Run via `make test`, or pass " +
                "-Dnaawbi.db.url=jdbc:postgresql://localhost:5432/naawbi_test"
            );
        }
        // Idempotent — safe to call every test class load.
        DB.getInstance().createTables();
    }

    @BeforeEach
    void truncateAllTables() throws SQLException {
        try (Statement st = DB.getInstance().getConnection().createStatement()) {
            st.execute(TRUNCATE_ALL);
        }
    }

    // ── Fixture helpers ──────────────────────────────────────────────────────
    // Direct INSERTs intentionally bypass model save() methods to keep
    // fixtures decoupled from production code paths under test.

    protected int insertUser(String username, String email, String hashedPassword, String role)
            throws SQLException {
        String sql = "INSERT INTO users (username, email, password, role) " +
                     "VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, hashedPassword);
            ps.setString(4, role);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    protected int insertCourse(String name, String code, String description,
                               boolean active, int createdBy) throws SQLException {
        String sql = "INSERT INTO courses (course_name, course_code, description, is_active, created_by) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, code);
            ps.setString(3, description);
            ps.setBoolean(4, active);
            ps.setInt(5, createdBy);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    protected int insertAssignment(int courseId, int createdBy, String title,
                                   String description, LocalDateTime deadline,
                                   int totalPoints, boolean lateAllowed) throws SQLException {
        String sql = "INSERT INTO assignments " +
                     "(course_id, created_by, title, description, deadline, " +
                     " total_points, late_submissions_allowed) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = DB.getInstance().prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, createdBy);
            ps.setString(3, title);
            ps.setString(4, description);
            ps.setObject(5, deadline);
            ps.setInt(6, totalPoints);
            ps.setBoolean(7, lateAllowed);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
