package naawbi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UserTest {

    @Test
    @DisplayName("Constructor stores all fields and getters return them")
    void constructorRoundTrip() {
        User u = new User(42, "ibbi", "ibbi@naawbi.edu", "student");
        assertEquals(42, u.getId());
        assertEquals("ibbi", u.getUsername());
        assertEquals("ibbi@naawbi.edu", u.getEmail());
        assertEquals("student", u.getRole());
    }

    @Test
    @DisplayName("Two distinct users do not share field values")
    void distinctUsersAreIndependent() {
        User a = new User(1, "ali",  "ali@naawbi.edu",  "instructor");
        User b = new User(2, "sara", "sara@naawbi.edu", "student");
        assertNotEquals(a.getId(),       b.getId());
        assertNotEquals(a.getUsername(), b.getUsername());
        assertNotEquals(a.getEmail(),    b.getEmail());
        assertNotEquals(a.getRole(),     b.getRole());
    }

    @Test
    @DisplayName("Role can be 'instructor' or 'student' — caller-controlled, not validated")
    void rolePassthrough() {
        // The model layer does not validate role; it stores whatever it's given.
        // This test pins down that contract so a future change is intentional.
        User weird = new User(99, "x", "x@x.com", "ANYTHING");
        assertEquals("ANYTHING", weird.getRole());
    }
}
