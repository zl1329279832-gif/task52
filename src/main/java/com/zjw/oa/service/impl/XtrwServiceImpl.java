package com.zjw.oa.service.impl;

import com.zjw.oa.entity.*;
import com.zjw.oa.mapper.DepartmentMapper;
import com.zjw.oa.mapper.GgMapper;
import com.zjw.oa.mapper.UserMapper;
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
    private UserMapper userMapper;

    @Autowired
    private GgMapper ggMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    public void addXtrw(Xtrw xtrw, String xtUserIds, String visibleDeptIds) throws Exception {
        // Set defaults
        Date now = new Date();
        xtrw.setCreateTime(now);
        xtrw.setUpdateTime(now);
        xtrw.setStatus(0);
        xtrw.setProgress(0);
        xtrw.setIsOverdue(0);

        // Lookup owner name
        User ownerQuery = new User();
        ownerQuery.setUserId(xtrw.getZfrUserId());
        User owner = userMapper.getUser(ownerQuery);
        if (owner != null) {
            xtrw.setZfrUserName(owner.getUserName());
        }

        // Insert main task (useGeneratedKeys populates xtrwId)
        xtrwMapper.addXtrw(xtrw);

        // Parse and insert collaborators
        if (xtUserIds != null && !xtUserIds.trim().isEmpty()) {
            String[] ids = xtUserIds.split(",");
            for (String idStr : ids) {
                long uid = Long.parseLong(idStr.trim());
                User uQuery = new User();
                uQuery.setUserId(uid);
                User u = userMapper.getUser(uQuery);
                XtrwXtUser xtUser = new XtrwXtUser();
                xtUser.setXtrwId(xtrw.getXtrwId());
                xtUser.setXtUserId(uid);
                xtUser.setXtUserName(u != null ? u.getUserName() : "");
                xtrwMapper.addXtUser(xtUser);
            }
        }

        // Parse and insert visible departments
        if (visibleDeptIds != null && !visibleDeptIds.trim().isEmpty()) {
            String[] ids = visibleDeptIds.split(",");
            for (String idStr : ids) {
                int deptId = Integer.parseInt(idStr.trim());
                Department dQuery = new Department();
                dQuery.setDepartmentId(deptId);
                Department d = departmentMapper.getDeptById(dQuery);
                XtrwVisibleDept vd = new XtrwVisibleDept();
                vd.setXtrwId(xtrw.getXtrwId());
                vd.setDepartmentId(deptId);
                vd.setDepartmentName(d != null ? d.getDepartmentName() : "");
                xtrwMapper.addVisibleDept(vd);
            }
        }
    }

    @Override
    public Xtrw getXtrwDetail(Xtrw xtrw) {
        Xtrw task = xtrwMapper.getXtrwDetail(xtrw);
        if (task != null) {
            int id = task.getXtrwId();
            task.setXtUserList(xtrwMapper.getXtUsersByXtrwId(id));
            task.setProgressList(xtrwMapper.getProgressByXtrwId(id));
            task.setFileList(xtrwMapper.getFilesByXtrwId(id));
            task.setVisibleDeptList(xtrwMapper.getVisibleDeptsByXtrwId(id));
        }
        return task;
    }

    @Override
    public void updateProgress(XtrwProgress progress) throws Exception {
        // Validate user is a participant
        Xtrw query = new Xtrw();
        query.setXtrwId(progress.getXtrwId());
        Xtrw task = xtrwMapper.getXtrwDetail(query);
        if (task == null) {
            throw new Exception("任务不存在！");
        }

        boolean isParticipant = false;
        if (progress.getUserId() == task.getZfrUserId()) {
            isParticipant = true;
        } else {
            XtrwXtUser xtUser = xtrwMapper.getXtUser(progress.getXtrwId(), progress.getUserId());
            if (xtUser != null) {
                isParticipant = true;
            }
        }
        if (!isParticipant) {
            throw new Exception("您不是该任务的负责人或协办人，无法更新进度！");
        }

        // Lookup updater name
        User uQuery = new User();
        uQuery.setUserId(progress.getUserId());
        User u = userMapper.getUser(uQuery);
        progress.setUserName(u != null ? u.getUserName() : "");
        progress.setUpdateTime(new Date());

        // Insert progress log
        xtrwMapper.addProgress(progress);

        // Update task progress
        xtrwMapper.updateXtrwProgress(progress.getXtrwId(), progress.getProgress());

        // Auto-complete if progress >= 100
        if (progress.getProgress() >= 100) {
            xtrwMapper.updateXtrwStatus(progress.getXtrwId(), 1, null);
        }
    }

    @Override
    public void closeXtrw(Xtrw xtrw) throws Exception {
        // Lookup task and verify owner
        Xtrw query = new Xtrw();
        query.setXtrwId(xtrw.getXtrwId());
        Xtrw task = xtrwMapper.getXtrwDetail(query);
        if (task == null) {
            throw new Exception("任务不存在！");
        }
        if (xtrw.getZfrUserId() != task.getZfrUserId()) {
            throw new Exception("只有主要负责人才能关闭任务！");
        }

        xtrwMapper.updateXtrwStatus(xtrw.getXtrwId(), 2, xtrw.getSummary());
    }

    @Override
    public void addXtrwFile(XtrwFile file) throws Exception {
        file.setUploadTime(new Date());
        xtrwMapper.addXtrwFile(file);
    }

    @Override
    public List<Xtrw> getMyOwnedTasks(long userId) {
        checkAndGenerateOverdueTodos();
        return xtrwMapper.getMyOwnedTasks(userId);
    }

    @Override
    public List<Xtrw> getMyCollabTasks(long userId) {
        checkAndGenerateOverdueTodos();
        return xtrwMapper.getMyCollabTasks(userId);
    }

    @Override
    public List<Xtrw> getOverdueTasks() {
        checkAndGenerateOverdueTodos();
        return xtrwMapper.getOverdueTasks();
    }

    @Override
    public List<Xtrw> getCompletedTasks() {
        checkAndGenerateOverdueTodos();
        return xtrwMapper.getCompletedTasks();
    }

    @Override
    public List<Xtrw> getVisibleTasksByDeptId(int departmentId) {
        checkAndGenerateOverdueTodos();
        return xtrwMapper.getVisibleTasksByDeptId(departmentId);
    }

    @Override
    public void checkAndGenerateOverdueTodos() {
        List<Xtrw> newlyOverdue = xtrwMapper.findNewlyOverdueTasks();
        for (Xtrw task : newlyOverdue) {
            try {
                // Mark as overdue
                xtrwMapper.markOverdue(task.getXtrwId());

                // Create todo reminder for task owner
                XtrwTodo todo = new XtrwTodo();
                todo.setXtrwId(task.getXtrwId());
                todo.setXtrwTitle(task.getXtrwTitle());
                todo.setUserId(task.getZfrUserId());
                todo.setCreateTime(new Date());
                todo.setIsRead(0);
                xtrwMapper.addTodo(todo);
            } catch (Exception e) {
                // Log and continue - don't let one failure block others
                e.printStackTrace();
            }
        }
    }

    @Override
    public int generateAnnouncement(int xtrwId) throws Exception {
        // Lookup task details
        Xtrw query = new Xtrw();
        query.setXtrwId(xtrwId);
        Xtrw task = xtrwMapper.getXtrwDetail(query);
        if (task == null) {
            throw new Exception("任务不存在！");
        }

        // Create announcement
        Gsgg gg = new Gsgg();
        gg.setGgTitle("【任务逾期提醒】" + task.getXtrwTitle());
        gg.setGgNr("任务【" + task.getXtrwTitle() + "】已逾期，截止时间：" + task.getJzTime()
                + "，当前进度：" + task.getProgress() + "%，负责人：" + task.getZfrUserName() + "。请尽快处理！");
        gg.setGgTime(new Date());
        gg.setIsZs(1);
        ggMapper.addGg(gg);

        // Update todo with announcement reference
        XtrwTodo todo = xtrwMapper.getTodoByXtrwId(xtrwId);
        if (todo != null) {
            xtrwMapper.updateTodoGgId(todo.getTodoId(), gg.getGgId());
        }

        return gg.getGgId();
    }

    @Override
    public List<XtrwTodo> getTodoList(long userId) {
        return xtrwMapper.getTodoList(userId);
    }

    @Override
    public void markTodoRead(XtrwTodo todo) throws Exception {
        todo.setIsRead(1);
        xtrwMapper.markTodoRead(todo);
    }
}
