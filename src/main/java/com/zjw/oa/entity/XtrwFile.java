package com.zjw.oa.entity;

import java.util.Date;

public class XtrwFile {

    private int xtrwId;
    private String fileName;
    private String filePath;
    private Date uploadTime;
    private int uploadUserId;

    public int getXtrwId() {
        return xtrwId;
    }

    public void setXtrwId(int xtrwId) {
        this.xtrwId = xtrwId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }

    public int getUploadUserId() {
        return uploadUserId;
    }

    public void setUploadUserId(int uploadUserId) {
        this.uploadUserId = uploadUserId;
    }
}
