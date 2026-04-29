package naawbi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordUtilTest {

    @Test
    @DisplayName("hash() is deterministic — same input produces same output")
    void hashIsDeterministic() {
        String a = PasswordUtil.hash("password123");
        String b = PasswordUtil.hash("password123");
        assertEquals(a, b);
    }

    @Test
    @DisplayName("hash() differentiates different inputs")
    void differentInputsProduceDifferentHashes() {
        String a = PasswordUtil.hash("password123");
        String b = PasswordUtil.hash("password124");
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("hash() returns 64-char lowercase hex (SHA-256)")
    void hashFormatIsLowercaseHex64() {
        String h = PasswordUtil.hash("anything");
        assertNotNull(h);
        assertEquals(64, h.length(), "SHA-256 hex digest must be 64 chars");
        assertEquals(h.toLowerCase(), h, "digest should be lowercase hex");
        assertEquals(true, h.matches("[0-9a-f]{64}"));
    }

    @Test
    @DisplayName("hash() of empty string matches the canonical SHA-256 of empty")
    void hashOfEmptyStringMatchesKnownValue() {
        String expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        assertEquals(expected, PasswordUtil.hash(""));
    }

    @Test
    @DisplayName("hash() of 'password123' matches the canonical SHA-256")
    void hashOfPassword123MatchesKnownValue() {
        // Independently verifiable: echo -n "password123" | sha256sum
        String expected = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f";
        assertEquals(expected, PasswordUtil.hash("password123"));
    }

    @Test
    @DisplayName("hash() handles unicode input correctly (UTF-8 encoding)")
    void hashHandlesUnicode() {
        String h = PasswordUtil.hash("naïve café 🔐");
        assertNotNull(h);
        assertEquals(64, h.length());
    }

    @Test
    @DisplayName("hash() is case-sensitive")
    void hashIsCaseSensitive() {
        assertNotEquals(PasswordUtil.hash("Password"), PasswordUtil.hash("password"));
    }

    @Test
    @DisplayName("hash(null) throws NullPointerException")
    void hashOfNullThrows() {
        assertThrows(NullPointerException.class, () -> PasswordUtil.hash(null));
    }
}
