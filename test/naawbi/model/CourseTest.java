package naawbi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseTest {

    @Test
    @DisplayName("Full constructor stores id and all fields")
    void fullConstructorRoundTrip() {
        Course c = new Course(7, "Software Engineering", "SE301", "Sprint-based dev", true);
        assertEquals(7, c.getId());
        assertEquals("Software Engineering", c.getName());
        assertEquals("SE301", c.getCode());
        assertEquals("Sprint-based dev", c.getDescription());
        assertTrue(c.isActive());
    }

    @Test
    @DisplayName("Unsaved-course constructor sets id to -1")
    void unsavedConstructorAssignsSentinelId() {
        Course c = new Course("Databases", "CS204", "Relational + SQL", false);
        assertEquals(-1, c.getId(),
                "Unsaved courses must use id=-1 as a sentinel before persistence");
        assertEquals("Databases", c.getName());
        assertEquals("CS204", c.getCode());
        assertFalse(c.isActive());
    }

    @Test
    @DisplayName("Setters update the corresponding fields")
    void settersUpdateFields() {
        Course c = new Course("X", "X100", "old", false);
        c.setId(11);
        c.setName("New Name");
        c.setCode("NEW100");
        c.setDescription("new");
        c.setActive(true);

        assertEquals(11, c.getId());
        assertEquals("New Name", c.getName());
        assertEquals("NEW100", c.getCode());
        assertEquals("new", c.getDescription());
        assertTrue(c.isActive());
    }

    @Test
    @DisplayName("Inactive courses report isActive() == false")
    void inactiveFlagPreserved() {
        Course c = new Course(1, "Draft Course", "DRAFT", null, false);
        assertFalse(c.isActive());
    }
}
