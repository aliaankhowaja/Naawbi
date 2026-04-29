package naawbi.model;

import naawbi.IntegrationTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseDAOTest extends IntegrationTestBase {

    @Test
    @DisplayName("enrollStudent() returns true on first enroll")
    void enrollStudentReturnsTrueOnFirstEnroll() throws SQLException {
        int instructor = insertUser("ali",  "ali@naawbi.edu",
                PasswordUtil.hash("x"), "instructor");
        int student    = insertUser("ibbi", "ibbi@naawbi.edu",
                PasswordUtil.hash("x"), "student");
        int courseId   = insertCourse("CS101", "CS101", "intro", true, instructor);

        assertTrue(Course.enrollStudent(student, courseId));
    }

    @Test
    @DisplayName("enrollStudent() is idempotent — returns false on duplicate")
    void enrollStudentIsIdempotent() throws SQLException {
        int instructor = insertUser("ali",  "ali@naawbi.edu",
                PasswordUtil.hash("x"), "instructor");
        int student    = insertUser("ibbi", "ibbi@naawbi.edu",
                PasswordUtil.hash("x"), "student");
        int courseId   = insertCourse("CS101", "CS101", "intro", true, instructor);

        Course.enrollStudent(student, courseId);
        assertFalse(Course.enrollStudent(student, courseId),
                "Re-enrolling the same student must be a no-op (ON CONFLICT DO NOTHING)");
    }

    @Test
    @DisplayName("fetchByCode() is case-insensitive")
    void fetchByCodeIsCaseInsensitive() throws SQLException {
        int instructor = insertUser("ali", "ali@naawbi.edu",
                PasswordUtil.hash("x"), "instructor");
        insertCourse("Software Engineering", "SE301", "MVC sprint", true, instructor);

        assertTrue(Course.fetchByCode("se301").isPresent());
        assertTrue(Course.fetchByCode("Se301").isPresent());
        assertTrue(Course.fetchByCode("SE301").isPresent());
    }

    @Test
    @DisplayName("fetchByCode() returns empty for unknown code")
    void fetchByCodeUnknownReturnsEmpty() throws SQLException {
        Optional<Course> result = Course.fetchByCode("DOES-NOT-EXIST");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("fetchByUserId() returns courses where user is enrolled OR creator")
    void fetchByUserIdIncludesCreatedAndEnrolled() throws SQLException {
        int instructor = insertUser("ali",  "ali@naawbi.edu",
                PasswordUtil.hash("x"), "instructor");
        int student    = insertUser("ibbi", "ibbi@naawbi.edu",
                PasswordUtil.hash("x"), "student");

        int taught   = insertCourse("Taught",   "TC1", null, true, instructor);
        int enrolled = insertCourse("Enrolled", "EC1", null, true, instructor);
        int unrelated = insertCourse("Other",   "OC1", null, true, instructor);

        Course.enrollStudent(student, enrolled);

        var instructorCourses = Course.fetchByUserId(instructor);
        assertEquals(3, instructorCourses.size(),
                "Instructor sees all three courses they created");

        var studentCourses = Course.fetchByUserId(student);
        assertEquals(1, studentCourses.size(),
                "Student sees only the course they're enrolled in");
        assertEquals("EC1", studentCourses.get(0).getCode());

        // Sanity: prove `unrelated` exists and is unrelated to the student
        assertTrue(unrelated > 0);
    }
}
