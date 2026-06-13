package com.zjw.oa.service;

import com.zjw.oa.entity.Hytz;
import com.zjw.oa.mapper.HytzMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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
public class MeetingRoomBookingTest {

    @Autowired
    private UserService userService;

    @Autowired
    private HytzMapper hytzMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager txManager;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Date parse(String s) {
        try {
            return sdf.parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Long> userIds(Long... ids) {
        return Arrays.asList(ids);
    }

    // ========== 1. 同房间时间重叠 — 必须失败 ==========

    @Test
    public void testSameRoomExactOverlap() {
        Date ks = parse("2026-07-01 09:00:00");
        Date js = parse("2026-07-01 10:00:00");

        Map<String, Object> r1 = userService.bookMeeting("A101", "会议1", ks, js, userIds(1L), 1);
        assertTrue((Boolean) r1.get("success"));

        Map<String, Object> r2 = userService.bookMeeting("A101", "会议2", ks, js, userIds(3L), 1);
        assertFalse("完全相同时间段应冲突", (Boolean) r2.get("success"));
        assertTrue(r2.get("msg").toString().contains("已被预约"));
    }

    @Test
    public void testSameRoomPartialOverlap() {
        Date ks1 = parse("2026-07-01 09:00:00");
        Date js1 = parse("2026-07-01 10:30:00");
        Date ks2 = parse("2026-07-01 10:00:00");
        Date js2 = parse("2026-07-01 11:00:00");

        Map<String, Object> r1 = userService.bookMeeting("A101", "会议1", ks1, js1, userIds(1L), 1);
        assertTrue((Boolean) r1.get("success"));

        Map<String, Object> r2 = userService.bookMeeting("A101", "会议2", ks2, js2, userIds(3L), 1);
        assertFalse("部分重叠应冲突", (Boolean) r2.get("success"));
    }

    @Test
    public void testSameRoomContainedOverlap() {
        Date ks1 = parse("2026-07-01 09:00:00");
        Date js1 = parse("2026-07-01 12:00:00");
        Date ks2 = parse("2026-07-01 10:00:00");
        Date js2 = parse("2026-07-01 11:00:00");

        Map<String, Object> r1 = userService.bookMeeting("A101", "会议1", ks1, js1, userIds(1L), 1);
        assertTrue((Boolean) r1.get("success"));

        Map<String, Object> r2 = userService.bookMeeting("A101", "会议2", ks2, js2, userIds(3L), 1);
        assertFalse("被包含的时间段应冲突", (Boolean) r2.get("success"));
    }

    // ========== 2. 相邻不重叠 — 必须成功 ==========

    @Test
    public void testAdjacentMeetingsNoConflict() {
        Date ks1 = parse("2026-07-01 09:00:00");
        Date js1 = parse("2026-07-01 10:00:00");
        Date ks2 = parse("2026-07-01 10:00:00");
        Date js2 = parse("2026-07-01 11:00:00");

        Map<String, Object> r1 = userService.bookMeeting("A101", "会议1", ks1, js1, userIds(1L), 1);
        assertTrue((Boolean) r1.get("success"));

        Map<String, Object> r2 = userService.bookMeeting("A101", "会议2", ks2, js2, userIds(3L), 1);
        assertTrue("相邻时间段（前一个结束=后一个开始）不应冲突", (Boolean) r2.get("success"));
    }

    @Test
    public void testHourBoundaryBackToBack() {
        Date ks1 = parse("2026-07-01 14:00:00");
        Date js1 = parse("2026-07-01 15:00:00");
        Date ks2 = parse("2026-07-01 15:00:00");
        Date js2 = parse("2026-07-01 16:00:00");
        Date ks3 = parse("2026-07-01 16:00:00");
        Date js3 = parse("2026-07-01 17:00:00");

        Map<String, Object> r1 = userService.bookMeeting("B202", "会议1", ks1, js1, userIds(1L), 1);
        assertTrue((Boolean) r1.get("success"));

        Map<String, Object> r2 = userService.bookMeeting("B202", "会议2", ks2, js2, userIds(3L), 1);
        assertTrue("整点边界背靠背不应冲突", (Boolean) r2.get("success"));

        Map<String, Object> r3 = userService.bookMeeting("B202", "会议3", ks3, js3, userIds(5L), 1);
        assertTrue("连续三个整点背靠背不应冲突", (Boolean) r3.get("success"));
    }

    // ========== 3. 跨天预约 ==========

    @Test
    public void testCrossDayBooking() {
        Date ks1 = parse("2026-07-01 22:00:00");
        Date js1 = parse("2026-07-02 02:00:00");

        Map<String, Object> r1 = userService.bookMeeting("A101", "夜间会议", ks1, js1, userIds(1L), 1);
        assertTrue((Boolean) r1.get("success"));

        // 与跨天预约重叠（午夜时段）
        Date ks2 = parse("2026-07-01 23:00:00");
        Date js2 = parse("2026-07-02 01:00:00");
        Map<String, Object> r2 = userService.bookMeeting("A101", "冲突会议", ks2, js2, userIds(3L), 1);
        assertFalse("跨天预约的重叠时段应冲突", (Boolean) r2.get("success"));

        // 跨天预约结束后紧邻
        Date ks3 = parse("2026-07-02 02:00:00");
        Date js3 = parse("2026-07-02 03:00:00");
        Map<String, Object> r3 = userService.bookMeeting("A101", "不冲突会议", ks3, js3, userIds(3L), 1);
        assertTrue("跨天预约结束后的相邻时段不应冲突", (Boolean) r3.get("success"));
    }

    // ========== 4. 不同房间同时段 — 必须成功 ==========

    @Test
    public void testDifferentRoomsSameTime() {
        Date ks = parse("2026-07-01 09:00:00");
        Date js = parse("2026-07-01 10:00:00");

        Map<String, Object> r1 = userService.bookMeeting("A101", "会议1", ks, js, userIds(1L), 1);
        assertTrue((Boolean) r1.get("success"));

        Map<String, Object> r2 = userService.bookMeeting("B202", "会议2", ks, js, userIds(3L), 1);
        assertTrue("不同房间同时段应允许", (Boolean) r2.get("success"));
    }

    // ========== 5. 同一用户日程冲突 ==========

    @Test
    public void testUserScheduleConflict() {
        Date ks = parse("2026-07-01 09:00:00");
        Date js = parse("2026-07-01 10:00:00");

        Map<String, Object> r1 = userService.bookMeeting("A101", "会议1", ks, js, userIds(1L), 1);
        assertTrue((Boolean) r1.get("success"));

        Map<String, Object> r2 = userService.bookMeeting("B202", "会议2", ks, js, userIds(1L), 1);
        assertFalse("同一用户在不同房间同一时段应冲突", (Boolean) r2.get("success"));
        assertTrue(r2.get("msg").toString().contains("已有其他会议"));
    }

    // ========== 6. 并发提交 — 只能成功一次 ==========

    @Test
    public void testConcurrentBooking() throws Exception {
        final Date ks = parse("2026-07-01 09:00:00");
        final Date js = parse("2026-07-01 10:00:00");

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(2);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failCount = new AtomicInteger(0);

        Runnable task1 = () -> {
            try {
                startLatch.await();
                Map<String, Object> r = userService.bookMeeting(
                        "C303", "并发会议1", ks, js, userIds(1L), 1);
                if ((Boolean) r.get("success")) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                startLatch.await();
                Map<String, Object> r = userService.bookMeeting(
                        "C303", "并发会议2", ks, js, userIds(3L), 1);
                if ((Boolean) r.get("success")) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        try {
            Thread t1 = new Thread(task1);
            Thread t2 = new Thread(task2);
            t1.start();
            t2.start();

            startLatch.countDown();
            doneLatch.await();

            assertEquals("并发预约同一房间同一时段，应恰好成功一次", 1, successCount.get());
            assertEquals("并发预约同一房间同一时段，应恰好失败一次", 1, failCount.get());
        } finally {
            // 线程在独立事务中提交的数据不会被测试的@Rollback回滚，必须在独立事务中清理
            TransactionTemplate cleanupTx = new TransactionTemplate(txManager);
            cleanupTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            cleanupTx.execute(status -> {
                jdbcTemplate.execute("DELETE FROM hytz");
                return null;
            });
        }
    }

    // ========== 7. 参数校验 ==========

    @Test
    public void testEndBeforeStart() {
        Date ks = parse("2026-07-01 10:00:00");
        Date js = parse("2026-07-01 09:00:00");

        Map<String, Object> r = userService.bookMeeting("A101", "会议", ks, js, userIds(1L), 1);
        assertFalse((Boolean) r.get("success"));
        assertTrue(r.get("msg").toString().contains("早于"));
    }

    @Test
    public void testEmptyUserList() {
        Date ks = parse("2026-07-01 09:00:00");
        Date js = parse("2026-07-01 10:00:00");

        Map<String, Object> r = userService.bookMeeting("A101", "会议", ks, js, new ArrayList<>(), 1);
        assertFalse((Boolean) r.get("success"));
        assertTrue(r.get("msg").toString().contains("不能为空"));
    }

    @Test
    public void testNonExistentUser() {
        Date ks = parse("2026-07-01 09:00:00");
        Date js = parse("2026-07-01 10:00:00");

        Map<String, Object> r = userService.bookMeeting("A101", "会议", ks, js, userIds(99999L), 1);
        assertFalse((Boolean) r.get("success"));
        assertTrue(r.get("msg").toString().contains("不存在"));
    }

    // ========== 8. checkRoomAvailable ==========

    @Test
    public void testCheckRoomAvailable() {
        Date ks = parse("2026-07-01 09:00:00");
        Date js = parse("2026-07-01 10:00:00");

        assertTrue("无预约时应可用", userService.checkRoomAvailable("A101", ks, js));

        userService.bookMeeting("A101", "会议", ks, js, userIds(1L), 1);

        assertFalse("已预约时应不可用", userService.checkRoomAvailable("A101", ks, js));
        assertTrue("不同时段应可用",
                userService.checkRoomAvailable("A101",
                        parse("2026-07-01 10:00:00"), parse("2026-07-01 11:00:00")));
    }
}
