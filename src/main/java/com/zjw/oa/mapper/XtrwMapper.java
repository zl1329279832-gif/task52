package com.zjw.oa.mapper;

import com.zjw.oa.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface XtrwMapper {

    // Task CRUD
    void addXtrw(Xtrw xtrw) throws Exception;

    Xtrw getXtrwDetail(Xtrw xtrw);

    void updateXtrwStatus(@Param("xtrwId") int id, @Param("status") int s, @Param("summary") String sum) throws Exception;

    void updateXtrwProgress(@Param("xtrwId") int id, @Param("progress") int p) throws Exception;

    // Collaborators
    void addXtUser(XtrwXtUser u) throws Exception;

    List<XtrwXtUser> getXtUsersByXtrwId(@Param("xtrwId") int id);

    XtrwXtUser getXtUser(@Param("xtrwId") int taskId, @Param("xtUserId") long uid);

    // Progress log
    void addProgress(XtrwProgress p) throws Exception;

    List<XtrwProgress> getProgressByXtrwId(@Param("xtrwId") int id);

    // Attachments
    void addXtrwFile(XtrwFile f) throws Exception;

    List<XtrwFile> getFilesByXtrwId(@Param("xtrwId") int id);

    // Visible departments
    void addVisibleDept(XtrwVisibleDept d) throws Exception;

    List<XtrwVisibleDept> getVisibleDeptsByXtrwId(@Param("xtrwId") int id);

    // Queries
    List<Xtrw> getMyOwnedTasks(@Param("zfrUserId") long uid);

    List<Xtrw> getMyCollabTasks(@Param("xtUserId") long uid);

    List<Xtrw> getOverdueTasks();

    List<Xtrw> getCompletedTasks();

    List<Xtrw> getVisibleTasksByDeptId(@Param("departmentId") int deptId);

    // Overdue
    List<Xtrw> findNewlyOverdueTasks();

    void markOverdue(@Param("xtrwId") int id) throws Exception;

    // Todos
    void addTodo(XtrwTodo t) throws Exception;

    List<XtrwTodo> getTodoList(@Param("userId") long uid);

    void markTodoRead(XtrwTodo t) throws Exception;

    void updateTodoGgId(@Param("todoId") int tid, @Param("ggId") int gid) throws Exception;

    XtrwTodo getTodoByXtrwId(@Param("xtrwId") int id);
}
