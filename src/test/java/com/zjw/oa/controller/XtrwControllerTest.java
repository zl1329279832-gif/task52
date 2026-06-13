package com.zjw.oa.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zjw.oa.entity.*;
import com.zjw.oa.service.XtrwService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(MockitoJUnitRunner.class)
public class XtrwControllerTest {

    private MockMvc mockMvc;

    @Mock
    private XtrwService xtrwService;

    @InjectMocks
    private XtrwController xtrwController;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(xtrwController).build();
    }

    @Test
    public void testAddXtrw() throws Exception {
        doAnswer(invocation -> {
            Xtrw xtrw = invocation.getArgument(0);
            xtrw.setXtrwId(100);
            return null;
        }).when(xtrwService).addXtrw(any(Xtrw.class), anyList(), anyList());

        JSONObject param = new JSONObject();
        param.put("xtrwTitle", "跨部门需求评审");
        param.put("xtrwDesc", "评审Q3产品需求");
        param.put("zfrUserId", 1);
        param.put("zfrUserName", "张三");
        param.put("jzTime", "2026-07-01");

        List<Map<String, Object>> xtUsers = new ArrayList<>();
        Map<String, Object> user1 = new HashMap<>();
        user1.put("xtUserId", 2);
        user1.put("xtUserName", "李四");
        xtUsers.add(user1);
        param.put("xtUsers", xtUsers);

        List<Map<String, Object>> depts = new ArrayList<>();
        Map<String, Object> dept1 = new HashMap<>();
        dept1.put("departmentId", 10);
        dept1.put("departmentName", "技术部");
        depts.add(dept1);
        param.put("visibleDepts", depts);

        MvcResult result = mockMvc.perform(post("/xtrw/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(param.toJSONString()))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JSONObject json = JSON.parseObject(content);
        assertTrue(json.getBooleanValue("success"));

        verify(xtrwService).addXtrw(any(Xtrw.class), anyList(), anyList());
    }

    @Test
    public void testDetail() throws Exception {
        Xtrw task = new Xtrw();
        task.setXtrwId(1);
        task.setXtrwTitle("测试任务");
        task.setZfrUserId(1);
        task.setZfrUserName("张三");
        task.setProgress(50);
        task.setStatus(0);
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        task.setJzTime(new Date());

        when(xtrwService.getXtrwDetail(any(Xtrw.class))).thenReturn(task);
        when(xtrwService.getXtUsers(1)).thenReturn(Collections.emptyList());
        when(xtrwService.getProgressList(1)).thenReturn(Collections.emptyList());
        when(xtrwService.getFileList(1)).thenReturn(Collections.emptyList());
        when(xtrwService.getVisibleDepts(1)).thenReturn(Collections.emptyList());

        MvcResult result = mockMvc.perform(post("/xtrw/detail")
                        .param("xtrwId", "1"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JSONObject json = JSON.parseObject(content);
        assertEquals("测试任务", json.getString("xtrwTitle"));
        assertNotNull(json.getJSONArray("xtUsers"));
        assertNotNull(json.getJSONArray("progressList"));
        assertNotNull(json.getJSONArray("files"));
        assertNotNull(json.getJSONArray("visibleDepts"));
    }

    @Test
    public void testDetailNotFound() throws Exception {
        when(xtrwService.getXtrwDetail(any(Xtrw.class))).thenReturn(null);

        MvcResult result = mockMvc.perform(post("/xtrw/detail")
                        .param("xtrwId", "999"))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject json = JSON.parseObject(result.getResponse().getContentAsString());
        assertFalse(json.getBooleanValue("success"));
    }

    @Test
    public void testMyOwned() throws Exception {
        List<Xtrw> tasks = new ArrayList<>();
        Xtrw t = new Xtrw();
        t.setXtrwId(1);
        t.setXtrwTitle("我的任务");
        t.setCreateTime(new Date());
        t.setUpdateTime(new Date());
        t.setJzTime(new Date());
        tasks.add(t);

        when(xtrwService.getMyOwnedTasks(1)).thenReturn(tasks);

        MvcResult result = mockMvc.perform(post("/xtrw/myOwned")
                        .param("zfrUserId", "1"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(1, JSON.parseArray(content).size());
    }

    @Test
    public void testMyCollab() throws Exception {
        List<Xtrw> tasks = new ArrayList<>();
        Xtrw t = new Xtrw();
        t.setXtrwId(2);
        t.setXtrwTitle("协办任务");
        t.setCreateTime(new Date());
        t.setUpdateTime(new Date());
        t.setJzTime(new Date());
        tasks.add(t);

        when(xtrwService.getMyCollabTasks(2)).thenReturn(tasks);

        MvcResult result = mockMvc.perform(post("/xtrw/myCollab")
                        .param("xtUserId", "2"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(1, JSON.parseArray(result.getResponse().getContentAsString()).size());
    }

    @Test
    public void testOverdue() throws Exception {
        when(xtrwService.getOverdueTasks()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/xtrw/overdue"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    public void testCompleted() throws Exception {
        when(xtrwService.getCompletedTasks()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/xtrw/completed"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    public void testUpdateProgress() throws Exception {
        doNothing().when(xtrwService).updateProgress(any(XtrwProgress.class));

        JSONObject param = new JSONObject();
        param.put("xtrwId", 1);
        param.put("userId", 2);
        param.put("userName", "李四");
        param.put("progress", 60);
        param.put("note", "完成了接口设计");

        MvcResult result = mockMvc.perform(post("/xtrw/updateProgress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(param.toJSONString()))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject json = JSON.parseObject(result.getResponse().getContentAsString());
        assertTrue(json.getBooleanValue("success"));
    }

    @Test
    public void testUpdateProgressNoPermission() throws Exception {
        doThrow(new Exception("无权限更新此任务进度"))
                .when(xtrwService).updateProgress(any(XtrwProgress.class));

        JSONObject param = new JSONObject();
        param.put("xtrwId", 1);
        param.put("userId", 999);
        param.put("progress", 60);

        MvcResult result = mockMvc.perform(post("/xtrw/updateProgress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(param.toJSONString()))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject json = JSON.parseObject(result.getResponse().getContentAsString());
        assertFalse(json.getBooleanValue("success"));
        assertEquals("无权限更新此任务进度", json.getString("msg"));
    }

    @Test
    public void testCloseTask() throws Exception {
        doNothing().when(xtrwService).closeTask(eq(1), eq(1), eq("任务圆满完成"));

        MvcResult result = mockMvc.perform(post("/xtrw/close")
                        .param("xtrwId", "1")
                        .param("userId", "1")
                        .param("summary", "任务圆满完成"))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject json = JSON.parseObject(result.getResponse().getContentAsString());
        assertTrue(json.getBooleanValue("success"));
    }

    @Test
    public void testCloseTaskNotOwner() throws Exception {
        doThrow(new Exception("只有主负责人才能关闭任务"))
                .when(xtrwService).closeTask(eq(1), eq(2), anyString());

        MvcResult result = mockMvc.perform(post("/xtrw/close")
                        .param("xtrwId", "1")
                        .param("userId", "2")
                        .param("summary", "试图关闭"))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject json = JSON.parseObject(result.getResponse().getContentAsString());
        assertFalse(json.getBooleanValue("success"));
    }

    @Test
    public void testTodoList() throws Exception {
        List<XtrwTodo> todos = new ArrayList<>();
        XtrwTodo todo = new XtrwTodo();
        todo.setTodoId(1);
        todo.setXtrwId(5);
        todo.setXtrwTitle("逾期任务");
        todo.setUserId(1);
        todo.setIsRead(0);
        todo.setCreateTime(new Date());
        todos.add(todo);

        when(xtrwService.getTodoList(1)).thenReturn(todos);

        MvcResult result = mockMvc.perform(post("/xtrw/todoList")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(1, JSON.parseArray(result.getResponse().getContentAsString()).size());
    }

    @Test
    public void testMarkTodoRead() throws Exception {
        doNothing().when(xtrwService).markTodoRead(any(XtrwTodo.class));

        mockMvc.perform(post("/xtrw/markTodoRead")
                        .param("todoId", "1"))
                .andExpect(status().isOk());

        ArgumentCaptor<XtrwTodo> captor = ArgumentCaptor.forClass(XtrwTodo.class);
        verify(xtrwService).markTodoRead(captor.capture());
        assertEquals(1, captor.getValue().getIsRead());
    }

    @Test
    public void testCheckOverdue() throws Exception {
        when(xtrwService.checkOverdue(true)).thenReturn(3);

        MvcResult result = mockMvc.perform(post("/xtrw/checkOverdue")
                        .param("generateGg", "true"))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject json = JSON.parseObject(result.getResponse().getContentAsString());
        assertTrue(json.getBooleanValue("success"));
        assertEquals(3, json.getIntValue("count"));
    }

    @Test
    public void testCheckOverdueWithoutGg() throws Exception {
        when(xtrwService.checkOverdue(false)).thenReturn(1);

        MvcResult result = mockMvc.perform(post("/xtrw/checkOverdue"))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject json = JSON.parseObject(result.getResponse().getContentAsString());
        assertTrue(json.getBooleanValue("success"));
        assertEquals(1, json.getIntValue("count"));
    }
}
