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

    void updateXtrwStatus(@Param("xtrwId") int xtrwId,
                          @Param("status") int status,
                          @Param("summary") String summary) throws Exception;

    void updateXtrwProgress(@Param("xtrwId") int xtrwId,
                            @Param("progress") int progress) throws Exception;

    // Collaborators
    void addXtUser(XtrwXtUser xtUser) throws Exception;

    List<XtrwXtUser> getXtUsersByXtrwId(@Param("xtrwId") int xtrwId);

    XtrwXtUser getXtUser(@Param("xtrwId") int xtrwId,
                         @Param("xtUserId") int xtUserId);

    // Progress Log
    void addProgress(XtrwProgress progress) throws Exception;

    List<XtrwProgress> getProgressByXtrwId(@Param("xtrwId") int xtrwId);

    // Attachments
    void addXtrwFile(XtrwFile xtrwFile) throws Exception;

    List<XtrwFile> getFilesByXtrwId(@Param("xtrwId") int xtrwId);

    // Visible Departments
    void addVisibleDept(XtrwVisibleDept dept) throws Exception;

    List<XtrwVisibleDept> getVisibleDeptsByXtrwId(@Param("xtrwId") int xtrwId);

    // Query Methods
    List<Xtrw> getMyOwnedTasks(@Param("zfrUserId") int zfrUserId);

    List<Xtrw> getMyCollabTasks(@Param("xtUserId") int xtUserId);

    List<Xtrw> getOverdueTasks();

    List<Xtrw> getCompletedTasks();

    List<Xtrw> getVisibleTasksByDeptId(@Param("departmentId") int departmentId);

    // Overdue
    List<Xtrw> findNewlyOverdueTasks();

    void markOverdue(@Param("xtrwId") int xtrwId) throws Exception;

    // Todos
    void addTodo(XtrwTodo todo) throws Exception;

    List<XtrwTodo> getTodoList(@Param("userId") int userId);

    void markTodoRead(XtrwTodo todo) throws Exception;

    void updateTodoGgId(@Param("todoId") int todoId,
                        @Param("ggId") int ggId) throws Exception;

    XtrwTodo getTodoByXtrwId(@Param("xtrwId") int xtrwId);
}
