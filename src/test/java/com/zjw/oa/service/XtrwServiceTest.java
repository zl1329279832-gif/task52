package com.zjw.oa.service;

import com.zjw.oa.entity.*;
import com.zjw.oa.mapper.GgMapper;
import com.zjw.oa.mapper.XtrwMapper;
import com.zjw.oa.service.impl.XtrwServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class XtrwServiceTest {

    @Mock
    private XtrwMapper xtrwMapper;

    @Mock
    private GgMapper ggMapper;

    @InjectMocks
    private XtrwServiceImpl xtrwService;

    private Xtrw sampleTask;

    @Before
    public void setUp() {
        sampleTask = new Xtrw();
        sampleTask.setXtrwId(1);
        sampleTask.setXtrwTitle("跨部门协作任务");
        sampleTask.setXtrwDesc("测试描述");
        sampleTask.setZfrUserId(1);
        sampleTask.setZfrUserName("张三");
        sampleTask.setJzTime(new Date(System.currentTimeMillis() + 86400000));
        sampleTask.setStatus(0);
        sampleTask.setProgress(0);
    }

    @Test
    public void testAddXtrw() throws Exception {
        doAnswer(invocation -> {
            Xtrw xtrw = invocation.getArgument(0);
            xtrw.setXtrwId(1);
            return null;
        }).when(xtrwMapper).addXtrw(any(Xtrw.class));

        List<XtrwXtUser> xtUsers = new ArrayList<>();
        XtrwXtUser u1 = new XtrwXtUser();
        u1.setXtUserId(2);
        u1.setXtUserName("李四");
        xtUsers.add(u1);

        XtrwXtUser u2 = new XtrwXtUser();
        u2.setXtUserId(3);
        u2.setXtUserName("王五");
        xtUsers.add(u2);

        List<XtrwVisibleDept> depts = new ArrayList<>();
        XtrwVisibleDept d1 = new XtrwVisibleDept();
        d1.setDepartmentId(10);
        d1.setDepartmentName("技术部");
        depts.add(d1);

        Xtrw xtrw = new Xtrw();
        xtrw.setXtrwTitle("新任务");
        xtrw.setZfrUserId(1);
        xtrw.setZfrUserName("张三");
        xtrw.setJzTime(new Date());

        xtrwService.addXtrw(xtrw, xtUsers, depts);

        verify(xtrwMapper).addXtrw(any(Xtrw.class));
        verify(xtrwMapper, times(2)).addXtUser(any(XtrwXtUser.class));
        verify(xtrwMapper, times(1)).addVisibleDept(any(XtrwVisibleDept.class));

        assertEquals(0, xtrw.getStatus());
        assertEquals(0, xtrw.getProgress());
        assertNotNull(xtrw.getCreateTime());
        assertNotNull(xtrw.getUpdateTime());
    }

    @Test
    public void testAddXtrwNullCollaborators() throws Exception {
        xtrwService.addXtrw(sampleTask, null, null);

        verify(xtrwMapper).addXtrw(any(Xtrw.class));
        verify(xtrwMapper, never()).addXtUser(any(XtrwXtUser.class));
        verify(xtrwMapper, never()).addVisibleDept(any(XtrwVisibleDept.class));
    }

    @Test
    public void testUpdateProgressByOwner() throws Exception {
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);

        XtrwProgress progress = new XtrwProgress();
        progress.setXtrwId(1);
        progress.setUserId(1); // owner
        progress.setUserName("张三");
        progress.setProgress(30);
        progress.setNote("完成设计");

        xtrwService.updateProgress(progress);

        verify(xtrwMapper).addProgress(any(XtrwProgress.class));
        verify(xtrwMapper).updateXtrwProgress(1, 30);
        assertNotNull(progress.getUpdateTime());
    }

    @Test
    public void testUpdateProgressByCollaborator() throws Exception {
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);
        XtrwXtUser xtUser = new XtrwXtUser();
        xtUser.setXtUserId(2);
        when(xtrwMapper.getXtUser(1, 2)).thenReturn(xtUser);

        XtrwProgress progress = new XtrwProgress();
        progress.setXtrwId(1);
        progress.setUserId(2); // collaborator
        progress.setProgress(50);

        xtrwService.updateProgress(progress);

        verify(xtrwMapper).addProgress(any(XtrwProgress.class));
        verify(xtrwMapper).updateXtrwProgress(1, 50);
    }

    @Test(expected = Exception.class)
    public void testUpdateProgressNonMember() throws Exception {
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);
        when(xtrwMapper.getXtUser(1, 999)).thenReturn(null);

        XtrwProgress progress = new XtrwProgress();
        progress.setXtrwId(1);
        progress.setUserId(999);
        progress.setProgress(50);

        xtrwService.updateProgress(progress);
    }

    @Test(expected = Exception.class)
    public void testUpdateProgressTaskNotExist() throws Exception {
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(null);

        XtrwProgress progress = new XtrwProgress();
        progress.setXtrwId(999);
        progress.setUserId(1);
        progress.setProgress(50);

        xtrwService.updateProgress(progress);
    }

    @Test(expected = Exception.class)
    public void testUpdateProgressClosedTask() throws Exception {
        sampleTask.setStatus(1);
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);

        XtrwProgress progress = new XtrwProgress();
        progress.setXtrwId(1);
        progress.setUserId(1);
        progress.setProgress(80);

        xtrwService.updateProgress(progress);
    }

    @Test
    public void testCloseTaskByOwner() throws Exception {
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);

        xtrwService.closeTask(1, 1, "任务完成");

        verify(xtrwMapper).updateXtrwStatus(1, 1, "任务完成");
    }

    @Test(expected = Exception.class)
    public void testCloseTaskByNonOwner() throws Exception {
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);

        xtrwService.closeTask(1, 2, "尝试关闭");
    }

    @Test(expected = Exception.class)
    public void testCloseAlreadyClosedTask() throws Exception {
        sampleTask.setStatus(1);
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);

        xtrwService.closeTask(1, 1, "再次关闭");
    }

    @Test
    public void testGetMyOwnedTasks() {
        List<Xtrw> list = Collections.singletonList(sampleTask);
        when(xtrwMapper.getMyOwnedTasks(1)).thenReturn(list);

        List<Xtrw> result = xtrwService.getMyOwnedTasks(1);
        assertEquals(1, result.size());
        assertEquals("跨部门协作任务", result.get(0).getXtrwTitle());
    }

    @Test
    public void testGetMyCollabTasks() {
        when(xtrwMapper.getMyCollabTasks(2)).thenReturn(Collections.singletonList(sampleTask));

        List<Xtrw> result = xtrwService.getMyCollabTasks(2);
        assertEquals(1, result.size());
    }

    @Test
    public void testAddXtrwFile() throws Exception {
        XtrwFile file = new XtrwFile();
        file.setXtrwId(1);
        file.setFileName("设计文档.pdf");
        file.setFilePath("D:/test/设计文档.pdf");
        file.setUploadUserId(1);

        xtrwService.addXtrwFile(file);

        verify(xtrwMapper).addXtrwFile(any(XtrwFile.class));
        assertNotNull(file.getUploadTime());
    }

    @Test
    public void testGetTodoList() {
        XtrwTodo todo = new XtrwTodo();
        todo.setTodoId(1);
        todo.setXtrwId(1);
        todo.setUserId(1);
        when(xtrwMapper.getTodoList(1)).thenReturn(Collections.singletonList(todo));

        List<XtrwTodo> result = xtrwService.getTodoList(1);
        assertEquals(1, result.size());
    }

    @Test
    public void testMarkTodoRead() throws Exception {
        XtrwTodo todo = new XtrwTodo();
        todo.setTodoId(1);
        todo.setIsRead(1);

        xtrwService.markTodoRead(todo);

        verify(xtrwMapper).markTodoRead(todo);
    }
}
