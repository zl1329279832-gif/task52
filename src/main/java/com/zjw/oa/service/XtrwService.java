package com.zjw.oa.service;

import com.zjw.oa.entity.*;

import java.util.List;

public interface XtrwService {

    void addXtrw(Xtrw xtrw, List<XtrwXtUser> xtUsers, List<XtrwVisibleDept> visibleDepts) throws Exception;

    Xtrw getXtrwDetail(Xtrw xtrw);

    List<XtrwXtUser> getXtUsers(int xtrwId);

    List<XtrwProgress> getProgressList(int xtrwId);

    List<XtrwFile> getFileList(int xtrwId);

    List<XtrwVisibleDept> getVisibleDepts(int xtrwId);

    List<Xtrw> getMyOwnedTasks(int zfrUserId);

    List<Xtrw> getMyCollabTasks(int xtUserId);

    List<Xtrw> getOverdueTasks();

    List<Xtrw> getCompletedTasks();

    void updateProgress(XtrwProgress progress) throws Exception;

    void closeTask(int xtrwId, int userId, String summary) throws Exception;

    void addXtrwFile(XtrwFile xtrwFile) throws Exception;

    List<XtrwTodo> getTodoList(int userId);

    void markTodoRead(XtrwTodo todo) throws Exception;

    int checkOverdue(boolean generateGg) throws Exception;
}
