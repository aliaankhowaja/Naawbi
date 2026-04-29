package naawbi.model;

import naawbi.IntegrationTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionDAOTest extends IntegrationTestBase {

    /** Builds a complete instructor+student+course+assignment fixture for one test. */
    private record Fixture(int instructorId, int studentId, int courseId, int assignmentId) {}

    private Fixture seedFixture() throws SQLException {
        int instructor = insertUser("ali",  "ali@naawbi.edu",
                PasswordUtil.hash("x"), "instructor");
        int student    = insertUser("ibbi", "ibbi@naawbi.edu",
                PasswordUtil.hash("x"), "student");
        int courseId   = insertCourse("CS101", "CS101", null, true, instructor);
        Course.enrollStudent(student, courseId);
        int assignment = insertAssignment(
                courseId, instructor, "HW1", "First homework",
                LocalDateTime.now().plusDays(7), 100, false);
        return new Fixture(instructor, student, courseId, assignment);
    }

    @Test
    @DisplayName("findOrCreate() inserts a Missing row when none exists")
    void findOrCreateInsertsMissingRow() throws SQLException {
        Fixture f = seedFixture();

        Submission s = Submission.findOrCreate(f.assignmentId(), f.studentId());

        assertNotNull(s);
        assertTrue(s.getId() > 0);
        assertFalse(s.isSubmitted());
        assertEquals("Missing", s.getStatus());
        assertNull(s.getFilePath());
        assertNull(s.getGrade());
    }

    @Test
    @DisplayName("findOrCreate() is idempotent — returns the same row twice")
    void findOrCreateIsIdempotent() throws SQLException {
        Fixture f = seedFixture();

        Submission first  = Submission.findOrCreate(f.assignmentId(), f.studentId());
        Submission second = Submission.findOrCreate(f.assignmentId(), f.studentId());

        assertEquals(first.getId(), second.getId(),
                "Calling findOrCreate twice must reuse the existing row, not insert a duplicate");
    }

    @Test
    @DisplayName("markAsSubmitted() flips submitted=true and updates path/status")
    void markAsSubmittedFlipsState() throws SQLException {
        Fixture f = seedFixture();
        Submission.findOrCreate(f.assignmentId(), f.studentId());

        boolean updated = Submission.markAsSubmitted(
                f.assignmentId(), f.studentId(), "/uploads/hw1.pdf", "Submitted");
        assertTrue(updated);

        Submission s = Submission.getByAssignmentAndUser(f.assignmentId(), f.studentId());
        assertTrue(s.isSubmitted());
        assertEquals("Submitted", s.getStatus());
        assertEquals("/uploads/hw1.pdf", s.getFilePath());
        assertNotNull(s.getSubmittedAt(),
                "submitted_at must be stamped at submission time");
    }

    @Test
    @DisplayName("unsubmit() clears file path and resets status to Missing")
    void unsubmitResetsRow() throws SQLException {
        Fixture f = seedFixture();
        Submission.findOrCreate(f.assignmentId(), f.studentId());
        Submission.markAsSubmitted(
                f.assignmentId(), f.studentId(), "/u/x.pdf", "Submitted");

        assertTrue(Submission.unsubmit(f.assignmentId(), f.studentId()));

        Submission s = Submission.getByAssignmentAndUser(f.assignmentId(), f.studentId());
        assertFalse(s.isSubmitted());
        assertEquals("Missing", s.getStatus());
        assertNull(s.getFilePath());
        assertNull(s.getSubmittedAt());
    }

    @Test
    @DisplayName("saveGrade() persists grade and feedback")
    void saveGradePersistsGradeAndFeedback() throws SQLException {
        Fixture f = seedFixture();
        Submission.findOrCreate(f.assignmentId(), f.studentId());
        Submission.markAsSubmitted(
                f.assignmentId(), f.studentId(), "/u/x.pdf", "Submitted");

        Submission before = Submission.getByAssignmentAndUser(
                f.assignmentId(), f.studentId());

        assertTrue(Submission.saveGrade(before.getId(), 87, "Solid work"));

        Submission after = Submission.getByAssignmentAndUser(
                f.assignmentId(), f.studentId());
        assertEquals(Integer.valueOf(87), after.getGrade());
        assertEquals("Solid work", after.getFeedback());
    }

    @Test
    @DisplayName("saveGrade() with blank feedback stores NULL, not empty string")
    void saveGradeBlankFeedbackStoresNull() throws SQLException {
        Fixture f = seedFixture();
        Submission.findOrCreate(f.assignmentId(), f.studentId());
        Submission stub = Submission.getByAssignmentAndUser(
                f.assignmentId(), f.studentId());

        assertTrue(Submission.saveGrade(stub.getId(), 50, "   "));

        Submission after = Submission.getByAssignmentAndUser(
                f.assignmentId(), f.studentId());
        assertEquals(Integer.valueOf(50), after.getGrade());
        assertNull(after.getFeedback(),
                "Blank/whitespace feedback must be stored as SQL NULL");
    }
}
