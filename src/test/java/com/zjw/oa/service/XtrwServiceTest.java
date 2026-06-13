package com.zjw.oa.service;

import com.zjw.oa.entity.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "mybatis.typeAliasesPackage=com.zjw.oa.entity",
        "mybatis.mapperLocations=classpath:mapper/*.xml"
})
@Transactional
@Rollback
public class XtrwServiceTest {

    @Autowired
    private XtrwService xtrwService;

    /**
     * 创建一个测试用的协作任务
     */
    private Xtrw createTestTask(long ownerUserId, String title, Date deadline) throws Exception {
        Xtrw xtrw = new Xtrw();
        xtrw.setXtrwTitle(title);
        xtrw.setXtrwDesc("测试任务描述");
        xtrw.setZfrUserId(ownerUserId);
        xtrw.setJzTime(deadline);
        return xtrw;
    }

    @Test
    public void testAddXtrw() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date deadline = sdf.parse("2026-12-31 23:59:59");

        Xtrw xtrw = createTestTask(1, "跨部门协作项目A", deadline);
        // 协办人: userId=3, userId=5
        xtrwService.addXtrw(xtrw, "3,5", "1,2");

        // 验证任务详情
        Xtrw query = new Xtrw();
        query.setXtrwId(xtrw.getXtrwId());
        Xtrw detail = xtrwService.getXtrwDetail(query);

        assertNotNull("任务应存在", detail);
        assertEquals("跨部门协作项目A", detail.getXtrwTitle());
        assertEquals(1, detail.getZfrUserId());
        assertEquals("ycy", detail.getZfrUserName());
        assertEquals(0, detail.getStatus());
        assertEquals(0, detail.getProgress());
        assertEquals(0, detail.getIsOverdue());
        assertNotNull("应有协办人列表", detail.getXtUserList());
        assertEquals("应有2个协办人", 2, detail.getXtUserList().size());
        assertNotNull("应有可见部门列表", detail.getVisibleDeptList());
        assertEquals("应有2个可见部门", 2, detail.getVisibleDeptList().size());
    }

    @Test
    public void testUpdateProgress() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Xtrw xtrw = createTestTask(1, "进度更新测试", sdf.parse("2026-12-31 23:59:59"));
        xtrwService.addXtrw(xtrw, "3", "1");

        // 协办人 userId=3 更新进度到50
        XtrwProgress progress = new XtrwProgress();
        progress.setXtrwId(xtrw.getXtrwId());
        progress.setUserId(3);
        progress.setProgress(50);
        progress.setNote("需求分析完成");
        xtrwService.updateProgress(progress);

        // 验证
        Xtrw query = new Xtrw();
        query.setXtrwId(xtrw.getXtrwId());
        Xtrw detail = xtrwService.getXtrwDetail(query);
        assertEquals(50, detail.getProgress());
        assertEquals(1, detail.getProgressList().size());
        assertEquals("需求分析完成", detail.getProgressList().get(0).getNote());
        assertEquals("zjw", detail.getProgressList().get(0).getUserName());
    }

    @Test(expected = Exception.class)
    public void testUpdateProgressNotParticipant() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Xtrw xtrw = createTestTask(1, "非参与人测试", sdf.parse("2026-12-31 23:59:59"));
        xtrwService.addXtrw(xtrw, "3", "1");

        // 非参与人 userId=5 尝试更新进度，应抛异常
        XtrwProgress progress = new XtrwProgress();
        progress.setXtrwId(xtrw.getXtrwId());
        progress.setUserId(5);
        progress.setProgress(30);
        progress.setNote("不应该成功");
        xtrwService.updateProgress(progress);
    }

    @Test
    public void testCloseXtrwByOwner() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Xtrw xtrw = createTestTask(1, "关闭测试", sdf.parse("2026-12-31 23:59:59"));
        xtrwService.addXtrw(xtrw, "3", "1");

        // 负责人关闭任务
        Xtrw closeReq = new Xtrw();
        closeReq.setXtrwId(xtrw.getXtrwId());
        closeReq.setZfrUserId(1);
        closeReq.setSummary("项目已完成交付");
        xtrwService.closeXtrw(closeReq);

        Xtrw query = new Xtrw();
        query.setXtrwId(xtrw.getXtrwId());
        Xtrw detail = xtrwService.getXtrwDetail(query);
        assertEquals("状态应为已关闭", 2, detail.getStatus());
        assertEquals("项目已完成交付", detail.getSummary());
    }

    @Test(expected = Exception.class)
    public void testCloseXtrwByNonOwner() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Xtrw xtrw = createTestTask(1, "非负责人关闭测试", sdf.parse("2026-12-31 23:59:59"));
        xtrwService.addXtrw(xtrw, "3", "1");

        // 非负责人 userId=3 尝试关闭，应抛异常
        Xtrw closeReq = new Xtrw();
        closeReq.setXtrwId(xtrw.getXtrwId());
        closeReq.setZfrUserId(3);
        closeReq.setSummary("不应成功");
        xtrwService.closeXtrw(closeReq);
    }

    @Test
    public void testAutoCompleteAt100() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Xtrw xtrw = createTestTask(1, "自动完成测试", sdf.parse("2026-12-31 23:59:59"));
        xtrwService.addXtrw(xtrw, "3", "1");

        // 负责人更新进度到100
        XtrwProgress progress = new XtrwProgress();
        progress.setXtrwId(xtrw.getXtrwId());
        progress.setUserId(1);
        progress.setProgress(100);
        progress.setNote("全部完成");
        xtrwService.updateProgress(progress);

        Xtrw query = new Xtrw();
        query.setXtrwId(xtrw.getXtrwId());
        Xtrw detail = xtrwService.getXtrwDetail(query);
        assertEquals("进度100应自动完成", 1, detail.getStatus());
    }

    @Test
    public void testMyOwnedTasks() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date deadline = sdf.parse("2026-12-31 23:59:59");

        // 创建2个userId=1负责的任务
        xtrwService.addXtrw(createTestTask(1, "我的任务1", deadline), "", "1");
        xtrwService.addXtrw(createTestTask(1, "我的任务2", deadline), "", "1");

        List<Xtrw> list = xtrwService.getMyOwnedTasks(1);
        assertTrue("我负责的任务应至少有2个", list.size() >= 2);
    }

    @Test
    public void testMyCollabTasks() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date deadline = sdf.parse("2026-12-31 23:59:59");

        // 创建任务，userId=3作为协办人
        xtrwService.addXtrw(createTestTask(1, "协办任务测试", deadline), "3", "1");

        List<Xtrw> list = xtrwService.getMyCollabTasks(3);
        assertTrue("我协办的任务应至少有1个", list.size() >= 1);
    }

    @Test
    public void testOverdueDetection() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 截止时间在过去
        Date pastDeadline = sdf.parse("2020-01-01 00:00:00");

        Xtrw xtrw = createTestTask(1, "逾期任务测试", pastDeadline);
        xtrwService.addXtrw(xtrw, "3", "1");

        // 触发逾期检查
        xtrwService.checkAndGenerateOverdueTodos();

        // 验证任务已标记逾期
        Xtrw query = new Xtrw();
        query.setXtrwId(xtrw.getXtrwId());
        Xtrw detail = xtrwService.getXtrwDetail(query);
        assertEquals("应标记为逾期", 1, detail.getIsOverdue());

        // 验证待办提醒已生成
        List<XtrwTodo> todos = xtrwService.getTodoList(1);
        boolean found = false;
        for (XtrwTodo t : todos) {
            if (t.getXtrwId() == xtrw.getXtrwId()) {
                found = true;
                assertEquals("逾期任务测试", t.getXtrwTitle());
                assertEquals(0, t.getIsRead());
                break;
            }
        }
        assertTrue("应为负责人生成逾期待办", found);
    }

    @Test
    public void testOverdueTodoList() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date pastDeadline = sdf.parse("2020-01-01 00:00:00");

        Xtrw xtrw = createTestTask(1, "待办列表测试", pastDeadline);
        xtrwService.addXtrw(xtrw, "", "1");

        // 通过查询接口间接触发逾期检查
        xtrwService.getOverdueTasks();

        List<XtrwTodo> todos = xtrwService.getTodoList(1);
        boolean found = false;
        for (XtrwTodo t : todos) {
            if (t.getXtrwId() == xtrw.getXtrwId()) {
                found = true;
                break;
            }
        }
        assertTrue("查询时应自动触发逾期检测并生成待办", found);
    }

    @Test
    public void testGenerateAnnouncement() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date pastDeadline = sdf.parse("2020-01-01 00:00:00");

        Xtrw xtrw = createTestTask(1, "公告生成测试", pastDeadline);
        xtrwService.addXtrw(xtrw, "", "1");

        // 先触发逾期检查生成待办
        xtrwService.checkAndGenerateOverdueTodos();

        // 生成公告
        int ggId = xtrwService.generateAnnouncement(xtrw.getXtrwId());
        assertTrue("应返回有效的公告ID", ggId > 0);
    }

    @Test
    public void testPermissionVisibility() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date deadline = sdf.parse("2026-12-31 23:59:59");

        // 仅对部门1可见
        Xtrw xtrw = createTestTask(1, "可见性测试任务", deadline);
        xtrwService.addXtrw(xtrw, "", "1");

        // 部门1应该能看到
        List<Xtrw> dept1Tasks = xtrwService.getVisibleTasksByDeptId(1);
        boolean foundInDept1 = false;
        for (Xtrw t : dept1Tasks) {
            if (t.getXtrwId() == xtrw.getXtrwId()) {
                foundInDept1 = true;
                break;
            }
        }
        assertTrue("部门1应能看到该任务", foundInDept1);

        // 部门2不应该看到
        List<Xtrw> dept2Tasks = xtrwService.getVisibleTasksByDeptId(2);
        boolean foundInDept2 = false;
        for (Xtrw t : dept2Tasks) {
            if (t.getXtrwId() == xtrw.getXtrwId()) {
                foundInDept2 = true;
                break;
            }
        }
        assertFalse("部门2不应看到该任务", foundInDept2);
    }
}
