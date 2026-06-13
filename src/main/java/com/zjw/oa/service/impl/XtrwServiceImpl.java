package com.zjw.oa.service.impl;

import com.zjw.oa.entity.*;
import com.zjw.oa.mapper.GgMapper;
import com.zjw.oa.mapper.XtrwMapper;
import com.zjw.oa.service.XtrwService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class XtrwServiceImpl implements XtrwService {

    @Autowired
    private XtrwMapper xtrwMapper;

    @Autowired
    private GgMapper ggMapper;

    @Override
    public void addXtrw(Xtrw xtrw, List<XtrwXtUser> xtUsers, List<XtrwVisibleDept> visibleDepts) throws Exception {
        Date now = new Date();
        xtrw.setCreateTime(now);
        xtrw.setUpdateTime(now);
        xtrw.setProgress(0);
        xtrw.setStatus(0);
        xtrw.setIsOverdue(0);
        xtrwMapper.addXtrw(xtrw);

        if (xtUsers != null) {
            for (XtrwXtUser xtUser : xtUsers) {
                xtUser.setXtrwId(xtrw.getXtrwId());
                xtrwMapper.addXtUser(xtUser);
            }
        }

        if (visibleDepts != null) {
            for (XtrwVisibleDept dept : visibleDepts) {
                dept.setXtrwId(xtrw.getXtrwId());
                xtrwMapper.addVisibleDept(dept);
            }
        }
    }

    @Override
    public Xtrw getXtrwDetail(Xtrw xtrw) {
        return xtrwMapper.getXtrwDetail(xtrw);
    }

    @Override
    public List<XtrwXtUser> getXtUsers(int xtrwId) {
        return xtrwMapper.getXtUsersByXtrwId(xtrwId);
    }

    @Override
    public List<XtrwProgress> getProgressList(int xtrwId) {
        return xtrwMapper.getProgressByXtrwId(xtrwId);
    }

    @Override
    public List<XtrwFile> getFileList(int xtrwId) {
        return xtrwMapper.getFilesByXtrwId(xtrwId);
    }

    @Override
    public List<XtrwVisibleDept> getVisibleDepts(int xtrwId) {
        return xtrwMapper.getVisibleDeptsByXtrwId(xtrwId);
    }

    @Override
    public List<Xtrw> getMyOwnedTasks(int zfrUserId) {
        return xtrwMapper.getMyOwnedTasks(zfrUserId);
    }

    @Override
    public List<Xtrw> getMyCollabTasks(int xtUserId) {
        return xtrwMapper.getMyCollabTasks(xtUserId);
    }

    @Override
    public List<Xtrw> getOverdueTasks() {
        return xtrwMapper.getOverdueTasks();
    }

    @Override
    public List<Xtrw> getCompletedTasks() {
        return xtrwMapper.getCompletedTasks();
    }

    @Override
    public void updateProgress(XtrwProgress progress) throws Exception {
        int xtrwId = progress.getXtrwId();
        int userId = progress.getUserId();

        Xtrw task = new Xtrw();
        task.setXtrwId(xtrwId);
        Xtrw existing = xtrwMapper.getXtrwDetail(task);
        if (existing == null) {
            throw new Exception("任务不存在");
        }
        if (existing.getStatus() != 0) {
            throw new Exception("任务已结束，无法更新进度");
        }

        boolean isOwner = existing.getZfrUserId() == userId;
        XtrwXtUser xtUser = xtrwMapper.getXtUser(xtrwId, userId);
        if (!isOwner && xtUser == null) {
            throw new Exception("无权限更新此任务进度");
        }

        progress.setUpdateTime(new Date());
        xtrwMapper.addProgress(progress);

        xtrwMapper.updateXtrwProgress(xtrwId, progress.getProgress());
    }

    @Override
    public void closeTask(int xtrwId, int userId, String summary) throws Exception {
        Xtrw task = new Xtrw();
        task.setXtrwId(xtrwId);
        Xtrw existing = xtrwMapper.getXtrwDetail(task);
        if (existing == null) {
            throw new Exception("任务不存在");
        }
        if (existing.getZfrUserId() != userId) {
            throw new Exception("只有主负责人才能关闭任务");
        }
        if (existing.getStatus() != 0) {
            throw new Exception("任务已结束");
        }

        xtrwMapper.updateXtrwStatus(xtrwId, 1, summary);
    }

    @Override
    public void addXtrwFile(XtrwFile xtrwFile) throws Exception {
        xtrwFile.setUploadTime(new Date());
        xtrwMapper.addXtrwFile(xtrwFile);
    }

    @Override
    public List<XtrwTodo> getTodoList(int userId) {
        return xtrwMapper.getTodoList(userId);
    }

    @Override
    public void markTodoRead(XtrwTodo todo) throws Exception {
        xtrwMapper.markTodoRead(todo);
    }

    @Override
    public int checkOverdue(boolean generateGg) throws Exception {
        List<Xtrw> overdueList = xtrwMapper.findNewlyOverdueTasks();
        int count = 0;

        for (Xtrw task : overdueList) {
            xtrwMapper.markOverdue(task.getXtrwId());

            // Create todo for owner
            createTodo(task, task.getZfrUserId());

            // Create todo for all collaborators
            List<XtrwXtUser> xtUsers = xtrwMapper.getXtUsersByXtrwId(task.getXtrwId());
            for (XtrwXtUser xtUser : xtUsers) {
                createTodo(task, xtUser.getXtUserId());
            }

            // Generate company announcement if requested
            if (generateGg) {
                Gsgg gsgg = new Gsgg();
                gsgg.setGgTitle("任务逾期提醒：" + task.getXtrwTitle());
                gsgg.setGgNr("协作任务【" + task.getXtrwTitle() + "】已逾期，"
                        + "负责人：" + task.getZfrUserName()
                        + "，请相关人员尽快处理。");
                gsgg.setGgTime(new Date());
                gsgg.setIsZs(1);
                ggMapper.addGg(gsgg);

                // Link announcement to todo
                XtrwTodo todo = xtrwMapper.getTodoByXtrwId(task.getXtrwId());
                if (todo != null) {
                    xtrwMapper.updateTodoGgId(todo.getTodoId(), gsgg.getGgId());
                }
            }

            count++;
        }
        return count;
    }

    private void createTodo(Xtrw task, int userId) throws Exception {
        XtrwTodo todo = new XtrwTodo();
        todo.setXtrwId(task.getXtrwId());
        todo.setXtrwTitle(task.getXtrwTitle());
        todo.setUserId(userId);
        todo.setCreateTime(new Date());
        todo.setIsRead(0);
        xtrwMapper.addTodo(todo);
    }
}
