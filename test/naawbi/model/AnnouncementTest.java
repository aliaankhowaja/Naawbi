package naawbi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnnouncementTest {

    @Test
    @DisplayName("New-announcement constructor leaves id/timestamps/author null")
    void newConstructorLeavesPersistenceFieldsBlank() {
        Announcement a = new Announcement(
                10, 2, "Welcome", "<p>Hello class</p>", "html");

        assertEquals(10, a.getCourseId());
        assertEquals(2,  a.getCreatedBy());
        assertEquals("Welcome", a.getTitle());
        assertEquals("<p>Hello class</p>", a.getContent());
        assertEquals("html", a.getContentType());

        assertEquals(0, a.getId());
        assertNull(a.getAuthorName());
        assertNull(a.getCreatedAt());
        assertNull(a.getUpdatedAt());
    }

    @Test
    @DisplayName("Hydrated constructor populates id, author, and timestamps")
    void hydratedConstructorPopulatesAllFields() {
        LocalDateTime created = LocalDateTime.of(2026, 4, 1,  9, 0);
        LocalDateTime updated = LocalDateTime.of(2026, 4, 2, 10, 0);

        Announcement a = new Announcement(
                77, 10, 2, "ali", "Title", "Body", "html", created, updated);

        assertEquals(77, a.getId());
        assertEquals("ali", a.getAuthorName());
        assertEquals(created, a.getCreatedAt());
        assertEquals(updated, a.getUpdatedAt());
    }

    @Test
    @DisplayName("contentType is stored verbatim — caller decides 'html' / 'plain'")
    void contentTypePassthrough() {
        Announcement html  = new Announcement(1, 1, "t", "c", "html");
        Announcement plain = new Announcement(1, 1, "t", "c", "plain");
        assertEquals("html",  html.getContentType());
        assertEquals("plain", plain.getContentType());
    }
}
