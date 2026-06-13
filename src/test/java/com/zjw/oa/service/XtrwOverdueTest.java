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
public class XtrwOverdueTest {

    @Mock
    private XtrwMapper xtrwMapper;

    @Mock
    private GgMapper ggMapper;

    @InjectMocks
    private XtrwServiceImpl xtrwService;

    private Xtrw overdueTask1;
    private Xtrw overdueTask2;

    @Before
    public void setUp() {
        overdueTask1 = new Xtrw();
        overdueTask1.setXtrwId(1);
        overdueTask1.setXtrwTitle("逾期任务A");
        overdueTask1.setZfrUserId(1);
        overdueTask1.setZfrUserName("张三");
        overdueTask1.setStatus(0);
        overdueTask1.setIsOverdue(0);
        overdueTask1.setJzTime(new Date(System.currentTimeMillis() - 86400000));

        overdueTask2 = new Xtrw();
        overdueTask2.setXtrwId(2);
        overdueTask2.setXtrwTitle("逾期任务B");
        overdueTask2.setZfrUserId(3);
        overdueTask2.setZfrUserName("王五");
        overdueTask2.setStatus(0);
        overdueTask2.setIsOverdue(0);
        overdueTask2.setJzTime(new Date(System.currentTimeMillis() - 172800000));
    }

    @Test
    public void testCheckOverdueNoTasks() throws Exception {
        when(xtrwMapper.findNewlyOverdueTasks()).thenReturn(Collections.emptyList());

        int count = xtrwService.checkOverdue(false);

        assertEquals(0, count);
        verify(xtrwMapper, never()).markOverdue(anyInt());
        verify(xtrwMapper, never()).addTodo(any(XtrwTodo.class));
    }

    @Test
    public void testCheckOverdueMarksTasks() throws Exception {
        when(xtrwMapper.findNewlyOverdueTasks())
                .thenReturn(Arrays.asList(overdueTask1, overdueTask2));
        when(xtrwMapper.getXtUsersByXtrwId(anyInt()))
                .thenReturn(Collections.emptyList());

        int count = xtrwService.checkOverdue(false);

        assertEquals(2, count);
        verify(xtrwMapper).markOverdue(1);
        verify(xtrwMapper).markOverdue(2);
    }

    @Test
    public void testCheckOverdueCreatesTodosForOwner() throws Exception {
        when(xtrwMapper.findNewlyOverdueTasks())
                .thenReturn(Collections.singletonList(overdueTask1));
        when(xtrwMapper.getXtUsersByXtrwId(1))
                .thenReturn(Collections.emptyList());

        xtrwService.checkOverdue(false);

        ArgumentCaptor<XtrwTodo> captor = ArgumentCaptor.forClass(XtrwTodo.class);
        verify(xtrwMapper).addTodo(captor.capture());

        XtrwTodo todo = captor.getValue();
        assertEquals(1, todo.getXtrwId());
        assertEquals("逾期任务A", todo.getXtrwTitle());
        assertEquals(1, todo.getUserId()); // owner
        assertEquals(0, todo.getIsRead());
        assertNotNull(todo.getCreateTime());
    }

    @Test
    public void testCheckOverdueCreatesTodosForCollaborators() throws Exception {
        when(xtrwMapper.findNewlyOverdueTasks())
                .thenReturn(Collections.singletonList(overdueTask1));

        List<XtrwXtUser> collaborators = new ArrayList<>();
        XtrwXtUser u1 = new XtrwXtUser();
        u1.setXtUserId(2);
        u1.setXtUserName("李四");
        collaborators.add(u1);

        XtrwXtUser u2 = new XtrwXtUser();
        u2.setXtUserId(4);
        u2.setXtUserName("赵六");
        collaborators.add(u2);

        when(xtrwMapper.getXtUsersByXtrwId(1)).thenReturn(collaborators);

        xtrwService.checkOverdue(false);

        // 1 for owner + 2 for collaborators = 3 todos
        verify(xtrwMapper, times(3)).addTodo(any(XtrwTodo.class));

        ArgumentCaptor<XtrwTodo> captor = ArgumentCaptor.forClass(XtrwTodo.class);
        verify(xtrwMapper, times(3)).addTodo(captor.capture());

        List<XtrwTodo> todos = captor.getAllValues();
        Set<Integer> userIds = new HashSet<>();
        for (XtrwTodo t : todos) {
            userIds.add(t.getUserId());
        }
        assertTrue(userIds.contains(1));  // owner
        assertTrue(userIds.contains(2));  // collaborator 1
        assertTrue(userIds.contains(4));  // collaborator 2
    }

    @Test
    public void testCheckOverdueWithAnnouncementGeneration() throws Exception {
        when(xtrwMapper.findNewlyOverdueTasks())
                .thenReturn(Collections.singletonList(overdueTask1));
        when(xtrwMapper.getXtUsersByXtrwId(1))
                .thenReturn(Collections.emptyList());

        doAnswer(invocation -> {
            Gsgg gsgg = invocation.getArgument(0);
            gsgg.setGgId(100);
            return null;
        }).when(ggMapper).addGg(any(Gsgg.class));

        XtrwTodo existingTodo = new XtrwTodo();
        existingTodo.setTodoId(50);
        existingTodo.setXtrwId(1);
        when(xtrwMapper.getTodoByXtrwId(1)).thenReturn(existingTodo);

        int count = xtrwService.checkOverdue(true);

        assertEquals(1, count);

        // Verify announcement was created
        ArgumentCaptor<Gsgg> ggCaptor = ArgumentCaptor.forClass(Gsgg.class);
        verify(ggMapper).addGg(ggCaptor.capture());
        Gsgg gsgg = ggCaptor.getValue();
        assertTrue(gsgg.getGgTitle().contains("逾期任务A"));
        assertTrue(gsgg.getGgNr().contains("张三"));
        assertEquals(1, gsgg.getIsZs());
        assertNotNull(gsgg.getGgTime());

        // Verify todo was linked to announcement
        verify(xtrwMapper).updateTodoGgId(50, 100);
    }

    @Test
    public void testCheckOverdueWithoutAnnouncementGeneration() throws Exception {
        when(xtrwMapper.findNewlyOverdueTasks())
                .thenReturn(Collections.singletonList(overdueTask1));
        when(xtrwMapper.getXtUsersByXtrwId(1))
                .thenReturn(Collections.emptyList());

        xtrwService.checkOverdue(false);

        // No announcement should be created
        verify(ggMapper, never()).addGg(any(Gsgg.class));
        verify(xtrwMapper, never()).updateTodoGgId(anyInt(), anyInt());
    }

    @Test
    public void testCheckOverdueMultipleTasks() throws Exception {
        when(xtrwMapper.findNewlyOverdueTasks())
                .thenReturn(Arrays.asList(overdueTask1, overdueTask2));

        // Task 1 has 1 collaborator
        XtrwXtUser collab = new XtrwXtUser();
        collab.setXtUserId(2);
        when(xtrwMapper.getXtUsersByXtrwId(1))
                .thenReturn(Collections.singletonList(collab));

        // Task 2 has no collaborators
        when(xtrwMapper.getXtUsersByXtrwId(2))
                .thenReturn(Collections.emptyList());

        int count = xtrwService.checkOverdue(false);

        assertEquals(2, count);
        verify(xtrwMapper).markOverdue(1);
        verify(xtrwMapper).markOverdue(2);
        // Task1: 1 owner + 1 collab = 2 todos; Task2: 1 owner = 1 todo; total = 3
        verify(xtrwMapper, times(3)).addTodo(any(XtrwTodo.class));
    }

    @Test
    public void testAnnouncementContent() throws Exception {
        when(xtrwMapper.findNewlyOverdueTasks())
                .thenReturn(Collections.singletonList(overdueTask1));
        when(xtrwMapper.getXtUsersByXtrwId(1))
                .thenReturn(Collections.emptyList());
        when(xtrwMapper.getTodoByXtrwId(1)).thenReturn(null);

        xtrwService.checkOverdue(true);

        ArgumentCaptor<Gsgg> captor = ArgumentCaptor.forClass(Gsgg.class);
        verify(ggMapper).addGg(captor.capture());

        Gsgg gg = captor.getValue();
        assertEquals("任务逾期提醒：逾期任务A", gg.getGgTitle());
        assertTrue(gg.getGgNr().contains("逾期任务A"));
        assertTrue(gg.getGgNr().contains("张三"));
        assertTrue(gg.getGgNr().contains("尽快处理"));
    }

    @Test
    public void testGetOverdueTasks() {
        when(xtrwMapper.getOverdueTasks())
                .thenReturn(Arrays.asList(overdueTask1, overdueTask2));

        List<Xtrw> result = xtrwService.getOverdueTasks();
        assertEquals(2, result.size());
    }

    @Test
    public void testGetCompletedTasks() {
        Xtrw completed = new Xtrw();
        completed.setXtrwId(10);
        completed.setStatus(1);
        when(xtrwMapper.getCompletedTasks())
                .thenReturn(Collections.singletonList(completed));

        List<Xtrw> result = xtrwService.getCompletedTasks();
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getStatus());
    }
}
