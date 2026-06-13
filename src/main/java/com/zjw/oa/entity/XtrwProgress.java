package com.zjw.oa.entity;

import java.util.Date;

public class XtrwProgress {

    private int progressId;
    private int xtrwId;
    private long userId;
    private String userName;
    private int progress;
    private String note;
    private Date updateTime;

    public int getProgressId() {
        return progressId;
    }

    public void setProgressId(int progressId) {
        this.progressId = progressId;
    }

    public int getXtrwId() {
        return xtrwId;
    }

    public void setXtrwId(int xtrwId) {
        this.xtrwId = xtrwId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
