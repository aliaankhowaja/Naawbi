package naawbi.model;

import naawbi.IntegrationTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDAOTest extends IntegrationTestBase {

    @Test
    @DisplayName("register() inserts a new user and returns it")
    void registerInsertsNewUser() throws SQLException {
        Optional<User> created = User.register(
                "ibbi", "ibbi@naawbi.edu", PasswordUtil.hash("password123"), "student");

        assertTrue(created.isPresent());
        assertTrue(created.get().getId() > 0);
        assertEquals("ibbi", created.get().getUsername());
        assertEquals("student", created.get().getRole());
    }

    @Test
    @DisplayName("register() returns empty Optional when email already exists")
    void registerReturnsEmptyOnDuplicateEmail() throws SQLException {
        String email = "dupe@naawbi.edu";
        String hash  = PasswordUtil.hash("password123");

        assertTrue(User.register("first",  email, hash, "student").isPresent());
        Optional<User> dup = User.register("second", email, hash, "student");
        assertFalse(dup.isPresent(),
                "Second register() with same email must return empty (UNIQUE constraint)");
    }

    @Test
    @DisplayName("findByEmailAndPassword() returns the user when credentials match")
    void findByEmailAndPasswordSuccess() throws SQLException {
        String hash = PasswordUtil.hash("correct horse");
        insertUser("ali", "ali@naawbi.edu", hash, "instructor");

        Optional<User> found = User.findByEmailAndPassword("ali@naawbi.edu", hash);
        assertTrue(found.isPresent());
        assertEquals("ali", found.get().getUsername());
        assertEquals("instructor", found.get().getRole());
    }

    @Test
    @DisplayName("findByEmailAndPassword() returns empty for wrong password")
    void findByEmailAndPasswordWrongPassword() throws SQLException {
        insertUser("ali", "ali@naawbi.edu",
                PasswordUtil.hash("the-real-one"), "instructor");

        Optional<User> found = User.findByEmailAndPassword(
                "ali@naawbi.edu", PasswordUtil.hash("guess"));
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("findByEmailAndPassword() returns empty for unknown email")
    void findByEmailAndPasswordUnknownEmail() throws SQLException {
        Optional<User> found = User.findByEmailAndPassword(
                "ghost@naawbi.edu", PasswordUtil.hash("anything"));
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("fetchByCourseId() returns enrolled members ordered by role then username")
    void fetchByCourseIdReturnsEnrolledMembers() throws SQLException {
        int instructorId = insertUser("ali",  "ali@naawbi.edu",
                PasswordUtil.hash("x"), "instructor");
        int studentB     = insertUser("bibi", "b@naawbi.edu",
                PasswordUtil.hash("x"), "student");
        int studentA     = insertUser("aada", "a@naawbi.edu",
                PasswordUtil.hash("x"), "student");
        int courseId     = insertCourse("CS101", "CS101", "intro", true, instructorId);

        // Enroll directly via the same DAO path used by the app
        Course.enrollStudent(studentA, courseId);
        Course.enrollStudent(studentB, courseId);

        var members = User.fetchByCourseId(courseId);
        assertEquals(2, members.size(),
                "Only enrolled students should be returned (instructor not auto-enrolled)");
        // ORDER BY role ASC, username ASC → both 'student', so by username
        assertEquals("aada", members.get(0).getUsername());
        assertEquals("bibi", members.get(1).getUsername());
    }
}
