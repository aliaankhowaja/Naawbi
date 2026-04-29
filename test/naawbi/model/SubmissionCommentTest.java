package naawbi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionCommentTest {

    @Test
    @DisplayName("Three-arg constructor stamps createdAt with the current time")
    void newCommentStampsCreatedAt() {
        LocalDateTime before = LocalDateTime.now();
        SubmissionComment c = new SubmissionComment(11, 42, "Looks good");
        LocalDateTime after  = LocalDateTime.now();

        assertEquals(11, c.getSubmissionId());
        assertEquals(42, c.getUserId());
        assertEquals("Looks good", c.getCommentText());
        assertNotNull(c.getCreatedAt());

        // createdAt must fall within the test execution window
        Duration delta = Duration.between(before, c.getCreatedAt());
        assertTrue(!delta.isNegative(), "createdAt cannot be before the test started");
        assertTrue(!c.getCreatedAt().isAfter(after.plusSeconds(1)),
                "createdAt cannot be in the future");

        // Author name is populated only by the JOIN query, not the construction-time ctor
        assertNull(c.getAuthorName());
        assertEquals(0, c.getId());
    }

    @Test
    @DisplayName("Hydrated 6-arg constructor preserves id, author, and supplied timestamp")
    void hydratedConstructorRoundTrip() {
        LocalDateTime t = LocalDateTime.of(2026, 4, 10, 15, 0);
        SubmissionComment c = new SubmissionComment(
                500, 11, 42, "ali", "Please rerun the tests", t);

        assertEquals(500, c.getId());
        assertEquals(11,  c.getSubmissionId());
        assertEquals(42,  c.getUserId());
        assertEquals("ali", c.getAuthorName());
        assertEquals("Please rerun the tests", c.getCommentText());
        assertEquals(t, c.getCreatedAt());
    }
}
