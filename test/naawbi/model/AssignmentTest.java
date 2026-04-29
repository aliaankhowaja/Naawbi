package naawbi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentTest {

    private final LocalDateTime deadline  = LocalDateTime.of(2026, 5, 15, 23, 59);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 4, 20,  9,  0);
    private final LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 22, 14, 30);

    @Test
    @DisplayName("New-assignment constructor stores creation fields, leaves DB fields default")
    void newConstructorRoundTrip() {
        Assignment a = new Assignment(
                10, 2, "HW1", "Solve problem set", deadline, 100, true);

        assertEquals(10, a.getCourseId());
        assertEquals(2,  a.getCreatedBy());
        assertEquals("HW1", a.getTitle());
        assertEquals("Solve problem set", a.getDescription());
        assertEquals(deadline, a.getDeadline());
        assertEquals(100, a.getTotalPoints());
        assertTrue(a.isLateSubmissionsAllowed());

        // Not yet persisted: id is the int default (0), DB-set fields are null
        assertEquals(0, a.getId());
        assertNull(a.getAuthorName());
        assertNull(a.getCreatedAt());
        assertNull(a.getUpdatedAt());
    }

    @Test
    @DisplayName("Hydrated-from-DB constructor populates all fields including author name")
    void hydratedConstructorRoundTrip() {
        Assignment a = new Assignment(
                55, 10, 2, "ali",
                "HW1", "Solve problem set",
                deadline, createdAt, updatedAt,
                100, false);

        assertEquals(55, a.getId());
        assertEquals("ali", a.getAuthorName());
        assertEquals(deadline,  a.getDeadline());
        assertEquals(createdAt, a.getCreatedAt());
        assertEquals(updatedAt, a.getUpdatedAt());
        assertFalse(a.isLateSubmissionsAllowed());
    }

    @Test
    @DisplayName("Setters update title, description, deadline")
    void settersUpdateFields() {
        Assignment a = new Assignment(
                10, 2, "old title", "old desc", deadline, 50, false);

        LocalDateTime newDeadline = deadline.plusDays(7);
        a.setTitle("new title");
        a.setDescription("new desc");
        a.setDeadline(newDeadline);

        assertEquals("new title", a.getTitle());
        assertEquals("new desc",  a.getDescription());
        assertEquals(newDeadline, a.getDeadline());
    }

    @Test
    @DisplayName("userStatus is null until populated, then round-trips")
    void userStatusRoundTrip() {
        Assignment a = new Assignment(
                10, 2, "HW", "d", deadline, 100, false);
        assertNull(a.getUserStatus(),
                "userStatus is only populated by fetchWithStatusForUser()");

        a.setUserStatus("Submitted");
        assertEquals("Submitted", a.getUserStatus());

        a.setUserStatus("Late");
        assertEquals("Late", a.getUserStatus());
    }
}
