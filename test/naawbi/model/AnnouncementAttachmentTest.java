package naawbi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnouncementAttachmentTest {

    @Test
    @DisplayName("File attachment: isLink=false, linkUrl=null, file fields populated")
    void fileAttachmentRoundTrip() {
        AnnouncementAttachment att = new AnnouncementAttachment(
                1, 100, "syllabus.pdf", "/uploads/syllabus.pdf", null, 24_576L, false);

        assertEquals(1, att.getId());
        assertEquals(100, att.getAnnouncementId());
        assertEquals("syllabus.pdf", att.getFileName());
        assertEquals("/uploads/syllabus.pdf", att.getFilePath());
        assertNull(att.getLinkUrl());
        assertEquals(24_576L, att.getFileSize());
        assertFalse(att.isLink());
    }

    @Test
    @DisplayName("Link attachment: isLink=true, linkUrl populated, filePath null, fileSize=0")
    void linkAttachmentRoundTrip() {
        AnnouncementAttachment att = new AnnouncementAttachment(
                2, 100, "Course site", null, "https://example.com", 0L, true);

        assertEquals("Course site", att.getFileName());
        assertNull(att.getFilePath());
        assertEquals("https://example.com", att.getLinkUrl());
        assertEquals(0L, att.getFileSize());
        assertTrue(att.isLink());
    }

    @Test
    @DisplayName("Large file size (>2GB) survives round-trip via long")
    void largeFileSizeFitsInLong() {
        long threeGB = 3L * 1024 * 1024 * 1024;
        AnnouncementAttachment att = new AnnouncementAttachment(
                3, 100, "big.zip", "/uploads/big.zip", null, threeGB, false);
        assertEquals(threeGB, att.getFileSize());
    }
}
