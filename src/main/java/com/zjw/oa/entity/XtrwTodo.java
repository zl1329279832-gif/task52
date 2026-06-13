package com.zjw.oa.entity;

import java.util.Date;

public class XtrwTodo {

    private int todoId;
    private int xtrwId;
    private String xtrwTitle;
    private long userId;
    private Date createTime;
    private int isRead;
    private Integer ggId;

    public int getTodoId() {
        return todoId;
    }

    public void setTodoId(int todoId) {
        this.todoId = todoId;
    }

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

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getIsRead() {
        return isRead;
    }

    public void setIsRead(int isRead) {
        this.isRead = isRead;
    }

    public Integer getGgId() {
        return ggId;
    }

    public void setGgId(Integer ggId) {
        this.ggId = ggId;
    }
}
