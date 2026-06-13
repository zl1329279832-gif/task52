package com.zjw.oa.entity;

import java.util.Date;

public class Xtrw {

    private int xtrwId;
    private String xtrwTitle;
    private String xtrwDesc;
    private int zfrUserId;
    private String zfrUserName;
    private Date jzTime;
    private Date createTime;
    private Date updateTime;
    private int progress;
    private int status;
    private int isOverdue;
    private String summary;

    public int getXtrwId() {
        return xtrwId;
    }

    public void setXtrwId(int xtrwId) {
        this.xtrwId = xtrwId;
    }

    public String getXtrwTitle() {
        return xtrwTitle;
    }

    public void setXtrwTitle(String xtrwTitle) {
        this.xtrwTitle = xtrwTitle;
    }

    public String getXtrwDesc() {
        return xtrwDesc;
    }

    public void setXtrwDesc(String xtrwDesc) {
        this.xtrwDesc = xtrwDesc;
    }

    public int getZfrUserId() {
        return zfrUserId;
    }

    public void setZfrUserId(int zfrUserId) {
        this.zfrUserId = zfrUserId;
    }

    public String getZfrUserName() {
        return zfrUserName;
    }

    public void setZfrUserName(String zfrUserName) {
        this.zfrUserName = zfrUserName;
    }

    public Date getJzTime() {
        return jzTime;
    }

    public void setJzTime(Date jzTime) {
        this.jzTime = jzTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getIsOverdue() {
        return isOverdue;
    }

    public void setIsOverdue(int isOverdue) {
        this.isOverdue = isOverdue;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
