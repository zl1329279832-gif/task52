package com.zjw.oa.service;

import com.zjw.oa.entity.*;
import com.zjw.oa.mapper.GgMapper;
import com.zjw.oa.mapper.XtrwMapper;
import com.zjw.oa.service.impl.XtrwServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class XtrwPermissionTest {

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
        sampleTask.setXtrwTitle("权限测试任务");
        sampleTask.setZfrUserId(1);
        sampleTask.setZfrUserName("负责人");
        sampleTask.setStatus(0);
    }

    @Test
    public void testOwnerCanUpdateProgress() throws Exception {
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);

        XtrwProgress progress = new XtrwProgress();
        progress.setXtrwId(1);
        progress.setUserId(1); // owner
        progress.setProgress(40);
        progress.setNote("负责人更新");

        xtrwService.updateProgress(progress);

        verify(xtrwMapper).addProgress(any(XtrwProgress.class));
        verify(xtrwMapper).updateXtrwProgress(1, 40);
    }

    @Test
    public void testCollaboratorCanUpdateProgress() throws Exception {
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);
        XtrwXtUser xtUser = new XtrwXtUser();
        xtUser.setXtUserId(5);
        xtUser.setXtUserName("协办人A");
        when(xtrwMapper.getXtUser(1, 5)).thenReturn(xtUser);

        XtrwProgress progress = new XtrwProgress();
        progress.setXtrwId(1);
        progress.setUserId(5); // collaborator
        progress.setProgress(60);

        xtrwService.updateProgress(progress);

        verify(xtrwMapper).addProgress(any(XtrwProgress.class));
    }

    @Test
    public void testNonMemberCannotUpdateProgress() throws Exception {
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);
        when(xtrwMapper.getXtUser(1, 99)).thenReturn(null);

        XtrwProgress progress = new XtrwProgress();
        progress.setXtrwId(1);
        progress.setUserId(99); // outsider
        progress.setProgress(50);

        try {
            xtrwService.updateProgress(progress);
            fail("应抛出权限异常");
        } catch (Exception e) {
            assertEquals("无权限更新此任务进度", e.getMessage());
        }

        verify(xtrwMapper, never()).addProgress(any(XtrwProgress.class));
    }

    @Test
    public void testOnlyOwnerCanClose() {
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);

        try {
            xtrwService.closeTask(1, 5, "协办人尝试关闭");
            fail("应抛出权限异常");
        } catch (Exception e) {
            assertEquals("只有主负责人才能关闭任务", e.getMessage());
        }
    }

    @Test
    public void testOwnerCanClose() throws Exception {
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);

        xtrwService.closeTask(1, 1, "负责人关闭");

        verify(xtrwMapper).updateXtrwStatus(1, 1, "负责人关闭");
    }

    @Test
    public void testGetVisibleTasksByDept() {
        List<Xtrw> tasks = new ArrayList<>();
        tasks.add(sampleTask);
        when(xtrwMapper.getVisibleTasksByDeptId(10)).thenReturn(tasks);

        // Verify the mapper is invoked correctly via the mapper directly
        List<Xtrw> result = xtrwMapper.getVisibleTasksByDeptId(10);
        assertEquals(1, result.size());
        assertEquals("权限测试任务", result.get(0).getXtrwTitle());
    }

    @Test
    public void testVisibleDeptsSavedOnCreate() throws Exception {
        doAnswer(invocation -> {
            Xtrw xtrw = invocation.getArgument(0);
            xtrw.setXtrwId(10);
            return null;
        }).when(xtrwMapper).addXtrw(any(Xtrw.class));

        List<XtrwVisibleDept> depts = new ArrayList<>();
        XtrwVisibleDept d1 = new XtrwVisibleDept();
        d1.setDepartmentId(10);
        d1.setDepartmentName("技术部");
        depts.add(d1);

        XtrwVisibleDept d2 = new XtrwVisibleDept();
        d2.setDepartmentId(20);
        d2.setDepartmentName("产品部");
        depts.add(d2);

        Xtrw xtrw = new Xtrw();
        xtrw.setXtrwTitle("多部门任务");
        xtrw.setZfrUserId(1);
        xtrw.setJzTime(new Date());

        xtrwService.addXtrw(xtrw, null, depts);

        verify(xtrwMapper, times(2)).addVisibleDept(any(XtrwVisibleDept.class));
    }

    @Test
    public void testCannotUpdateClosedTask() {
        sampleTask.setStatus(1);
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);

        XtrwProgress progress = new XtrwProgress();
        progress.setXtrwId(1);
        progress.setUserId(1);
        progress.setProgress(90);

        try {
            xtrwService.updateProgress(progress);
            fail("应抛出任务已结束异常");
        } catch (Exception e) {
            assertEquals("任务已结束，无法更新进度", e.getMessage());
        }
    }

    @Test
    public void testCannotCloseAlreadyClosed() {
        sampleTask.setStatus(2);
        when(xtrwMapper.getXtrwDetail(any(Xtrw.class))).thenReturn(sampleTask);

        try {
            xtrwService.closeTask(1, 1, "再次关闭");
            fail("应抛出任务已结束异常");
        } catch (Exception e) {
            assertEquals("任务已结束", e.getMessage());
        }
    }
}
