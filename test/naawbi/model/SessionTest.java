package naawbi.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTest {

    private final User student    = new User(1, "ibbi", "ibbi@naawbi.edu", "student");
    private final User instructor = new User(2, "ali",  "ali@naawbi.edu",  "instructor");

    @BeforeEach
    @AfterEach
    void resetSession() {
        // Session is a singleton; clear state so tests don't bleed into each other.
        Session.getInstance().logout();
    }

    @Test
    @DisplayName("getInstance() always returns the same singleton")
    void singletonIdentity() {
        assertSame(Session.getInstance(), Session.getInstance());
    }

    @Test
    @DisplayName("isLoggedIn() is false before any login")
    void notLoggedInByDefault() {
        assertFalse(Session.getInstance().isLoggedIn());
    }

    @Test
    @DisplayName("login() sets the current user and exposes their fields")
    void loginExposesUserFields() {
        Session s = Session.getInstance();
        s.login(student);

        assertTrue(s.isLoggedIn());
        assertEquals(1, s.getUserId());
        assertEquals("ibbi", s.getUsername());
        assertEquals("student", s.getRole());
    }

    @Test
    @DisplayName("isInstructor() reflects the current user's role")
    void isInstructorReflectsRole() {
        Session s = Session.getInstance();

        s.login(student);
        assertFalse(s.isInstructor());

        s.login(instructor);
        assertTrue(s.isInstructor());
    }

    @Test
    @DisplayName("logout() clears the current user")
    void logoutClearsUser() {
        Session s = Session.getInstance();
        s.login(instructor);
        assertTrue(s.isLoggedIn());

        s.logout();
        assertFalse(s.isLoggedIn());
    }

    @Test
    @DisplayName("getUserId() throws when no one is logged in")
    void getUserIdThrowsWhenLoggedOut() {
        assertThrows(IllegalStateException.class,
                () -> Session.getInstance().getUserId());
    }

    @Test
    @DisplayName("getUsername() throws when no one is logged in")
    void getUsernameThrowsWhenLoggedOut() {
        assertThrows(IllegalStateException.class,
                () -> Session.getInstance().getUsername());
    }

    @Test
    @DisplayName("getRole() throws when no one is logged in")
    void getRoleThrowsWhenLoggedOut() {
        assertThrows(IllegalStateException.class,
                () -> Session.getInstance().getRole());
    }

    @Test
    @DisplayName("isInstructor() throws when no one is logged in")
    void isInstructorThrowsWhenLoggedOut() {
        assertThrows(IllegalStateException.class,
                () -> Session.getInstance().isInstructor());
    }

    @Test
    @DisplayName("login() replaces a previous user (no stacking)")
    void loginReplacesPreviousUser() {
        Session s = Session.getInstance();
        s.login(student);
        s.login(instructor);

        assertEquals(2, s.getUserId());
        assertEquals("ali", s.getUsername());
        assertTrue(s.isInstructor());
    }
}
