package com.zjw.oa.entity;

import java.util.Date;
import java.util.List;

public class Xtrw {

    private int xtrwId;
    private String xtrwTitle;
    private String xtrwDesc;
    private long zfrUserId;
    private String zfrUserName;
    private Date jzTime;
    private Date createTime;
    private Date updateTime;
    private int progress;
    private int status;
    private String summary;
    private int isOverdue;

    // DTO fields (populated by service, not in DB)
    private List<XtrwXtUser> xtUserList;
    private List<XtrwFile> fileList;
    private List<XtrwProgress> progressList;
    private List<XtrwVisibleDept> visibleDeptList;

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

    public long getZfrUserId() {
        return zfrUserId;
    }

    public void setZfrUserId(long zfrUserId) {
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getIsOverdue() {
        return isOverdue;
    }

    public void setIsOverdue(int isOverdue) {
        this.isOverdue = isOverdue;
    }

    public List<XtrwXtUser> getXtUserList() {
        return xtUserList;
    }

    public void setXtUserList(List<XtrwXtUser> xtUserList) {
        this.xtUserList = xtUserList;
    }

    public List<XtrwFile> getFileList() {
        return fileList;
    }

    public void setFileList(List<XtrwFile> fileList) {
        this.fileList = fileList;
    }

    public List<XtrwProgress> getProgressList() {
        return progressList;
    }

    public void setProgressList(List<XtrwProgress> progressList) {
        this.progressList = progressList;
    }

    public List<XtrwVisibleDept> getVisibleDeptList() {
        return visibleDeptList;
    }

    public void setVisibleDeptList(List<XtrwVisibleDept> visibleDeptList) {
        this.visibleDeptList = visibleDeptList;
    }
}
