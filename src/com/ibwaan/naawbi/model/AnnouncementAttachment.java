package com.ibwaan.naawbi.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementAttachment {
    private int id;
    private int announcementId;
    private String fileName;
    private String filePath;
    private String linkUrl;
    private long fileSize;
    private boolean isLink;

    public AnnouncementAttachment(int id, int announcementId, String fileName,
            String filePath, String linkUrl, long fileSize, boolean isLink) {
        this.id = id;
        this.announcementId = announcementId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.linkUrl = linkUrl;
        this.fileSize = fileSize;
        this.isLink = isLink;
    }

    public static boolean saveFile(int announcementId, String fileName, String filePath, long fileSize)
            throws SQLException {
        String sql = "INSERT INTO announcement_attachments (announcement_id, file_name, file_path, file_size, is_link) VALUES (?, ?, ?, ?, false)";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(sql)) {
            pstmt.setInt(1, announcementId);
            pstmt.setString(2, fileName);
            pstmt.setString(3, filePath);
            pstmt.setLong(4, fileSize);
            return pstmt.executeUpdate() > 0;
        }
    }

    public static boolean saveLink(int announcementId, String displayName, String url) throws SQLException {
        String sql = "INSERT INTO announcement_attachments (announcement_id, file_name, link_url, is_link) VALUES (?, ?, ?, true)";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(sql)) {
            pstmt.setInt(1, announcementId);
            pstmt.setString(2, displayName);
            pstmt.setString(3, url);
            return pstmt.executeUpdate() > 0;
        }
    }

    public static List<AnnouncementAttachment> fetchByAnnouncementId(int announcementId) throws SQLException {
        List<AnnouncementAttachment> list = new ArrayList<>();
        String sql = "SELECT * FROM announcement_attachments WHERE announcement_id = ? ORDER BY id ASC";
        try (PreparedStatement pstmt = DB.getInstance().prepareStatement(sql)) {
            pstmt.setInt(1, announcementId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new AnnouncementAttachment(
                            rs.getInt("id"),
                            rs.getInt("announcement_id"),
                            rs.getString("file_name"),
                            rs.getString("file_path"),
                            rs.getString("link_url"),
                            rs.getLong("file_size"),
                            rs.getBoolean("is_link")));
                }
            }
        }
        return list;
    }

    public int getId() { return id; }
    public int getAnnouncementId() { return announcementId; }
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public String getLinkUrl() { return linkUrl; }
    public long getFileSize() { return fileSize; }
    public boolean isLink() { return isLink; }
}
