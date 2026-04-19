package naawbi.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SubmissionComment {
    private int id;
    private int submissionId;
    private int userId;
    private String authorName;
    private String commentText;
    private LocalDateTime createdAt;

    public SubmissionComment(int submissionId, int userId, String commentText) {
        this.submissionId = submissionId;
        this.userId = userId;
        this.commentText = commentText;
        this.createdAt = LocalDateTime.now();
    }

    public SubmissionComment(int id, int submissionId, int userId, String authorName, String commentText, LocalDateTime createdAt) {
        this.id = id;
        this.submissionId = submissionId;
        this.userId = userId;
        this.authorName = authorName;
        this.commentText = commentText;
        this.createdAt = createdAt;
    }

    public boolean save() throws SQLException {
        String insertSQL = "INSERT INTO submission_comments (submission_id, user_id, comment_text, created_at) VALUES(?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(insertSQL)) {
            pstmt.setInt(1, submissionId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, commentText);
            return pstmt.executeUpdate() > 0;
        }
    }

    public static List<SubmissionComment> fetchBySubmissionId(int submissionId) throws SQLException {
        List<SubmissionComment> comments = new ArrayList<>();
        String query = "SELECT c.id, c.submission_id, c.user_id, u.username, c.comment_text, c.created_at " +
                       "FROM submission_comments c " +
                       "JOIN users u ON c.user_id = u.id " +
                       "WHERE c.submission_id = ? " +
                       "ORDER BY c.created_at ASC";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(query)) {
            pstmt.setInt(1, submissionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    comments.add(new SubmissionComment(
                            rs.getInt("id"),
                            rs.getInt("submission_id"),
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("comment_text"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        }
        return comments;
    }

    public int getId() { return id; }
    public int getSubmissionId() { return submissionId; }
    public int getUserId() { return userId; }
    public String getAuthorName() { return authorName; }
    public String getCommentText() { return commentText; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
