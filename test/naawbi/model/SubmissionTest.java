package naawbi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionTest {

    private final LocalDateTime submittedAt = LocalDateTime.of(2026, 4, 20, 12,  0);
    private final LocalDateTime updatedAt   = LocalDateTime.of(2026, 4, 21, 10, 30);

    @Test
    @DisplayName("Two-arg constructor: brand-new submission defaults to Missing + not submitted")
    void brandNewDefaults() {
        Submission s = new Submission(7, 42);

        assertEquals(7,  s.getAssignmentId());
        assertEquals(42, s.getUserId());
        assertFalse(s.isSubmitted());
        assertEquals("Missing", s.getStatus(),
                "A fresh submission row must start in 'Missing' status");
        assertNull(s.getFilePath());
        assertNull(s.getGrade());
        assertNull(s.getFeedback());
    }

    @Test
    @DisplayName("Legacy 8-arg constructor stores no grade/feedback")
    void legacyConstructorHasNoGradeOrFeedback() {
        Submission s = new Submission(
                100, 7, 42, true, "/uploads/hw1.pdf",
                "Submitted", submittedAt, updatedAt);

        assertEquals(100, s.getId());
        assertTrue(s.isSubmitted());
        assertEquals("/uploads/hw1.pdf", s.getFilePath());
        assertEquals("Submitted", s.getStatus());
        assertEquals(submittedAt, s.getSubmittedAt());
        assertEquals(updatedAt,   s.getUpdatedAt());
        assertNull(s.getGrade());
        assertNull(s.getFeedback());
    }

    @Test
    @DisplayName("Full constructor stores grade and feedback")
    void fullConstructorStoresGradeAndFeedback() {
        Submission s = new Submission(
                100, 7, 42, true, "/uploads/hw1.pdf",
                "Graded", 87, "Solid work — watch the edge case on input 0.",
                submittedAt, updatedAt);

        assertEquals(Integer.valueOf(87), s.getGrade());
        assertEquals("Solid work — watch the edge case on input 0.", s.getFeedback());
    }

    @Test
    @DisplayName("Grade can be cleared back to null via setter")
    void gradeCanBeClearedToNull() {
        Submission s = new Submission(
                1, 7, 42, true, null, "Submitted",
                submittedAt, updatedAt);

        s.setGrade(50);
        assertEquals(Integer.valueOf(50), s.getGrade());

        s.setGrade(null);
        assertNull(s.getGrade(),
                "Grade must be nullable so 'ungraded' is distinguishable from 'zero'");
    }

    @Test
    @DisplayName("Setters round-trip submitted/filePath/status/feedback/submittedAt")
    void settersRoundTrip() {
        Submission s = new Submission(7, 42);

        s.setSubmitted(true);
        s.setFilePath("/u/x.pdf");
        s.setStatus("Late");
        s.setFeedback("Try again");
        LocalDateTime t = LocalDateTime.of(2026, 4, 25, 9, 0);
        s.setSubmittedAt(t);

        assertTrue(s.isSubmitted());
        assertEquals("/u/x.pdf", s.getFilePath());
        assertEquals("Late", s.getStatus());
        assertEquals("Try again", s.getFeedback());
        assertEquals(t, s.getSubmittedAt());
    }

    @Test
    @DisplayName("Brand-new submission has no submitted-at timestamp")
    void brandNewHasNoSubmittedAt() {
        Submission s = new Submission(7, 42);
        assertNull(s.getSubmittedAt());
        assertNull(s.getUpdatedAt());
    }

    @Test
    @DisplayName("Legacy constructor leaves join-only fields (studentName, courseName) null")
    void joinOnlyFieldsAreNullWhenNotPopulated() {
        Submission s = new Submission(
                100, 7, 42, true, null, "Submitted",
                submittedAt, updatedAt);

        // These fields are populated only by specific fetch* queries.
        assertNull(s.getStudentName());
        assertNull(s.getAssignmentTitle());
        assertNull(s.getCourseName());
        assertEquals(0, s.getTotalPoints(),
                "totalPoints is only set by fetchGradedForStudent / fetchGradebookForCourse");
        // sanity: status/filePath still readable
        assertNotNull(s.getStatus());
    }
}
