package com.zjw.oa.service;

import com.zjw.oa.entity.Xtrw;
import com.zjw.oa.entity.XtrwFile;
import com.zjw.oa.entity.XtrwProgress;
import com.zjw.oa.entity.XtrwTodo;

import java.util.List;

public interface XtrwService {

    void addXtrw(Xtrw xtrw, String xtUserIds, String visibleDeptIds) throws Exception;

    Xtrw getXtrwDetail(Xtrw xtrw);

    void updateProgress(XtrwProgress progress) throws Exception;

    void closeXtrw(Xtrw xtrw) throws Exception;

    void addXtrwFile(XtrwFile file) throws Exception;

    List<Xtrw> getMyOwnedTasks(long userId);

    List<Xtrw> getMyCollabTasks(long userId);

    List<Xtrw> getOverdueTasks();

    List<Xtrw> getCompletedTasks();

    List<Xtrw> getVisibleTasksByDeptId(int departmentId);

    void checkAndGenerateOverdueTodos();

    int generateAnnouncement(int xtrwId) throws Exception;

    List<XtrwTodo> getTodoList(long userId);

    void markTodoRead(XtrwTodo todo) throws Exception;
}
