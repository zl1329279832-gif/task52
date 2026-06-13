package com.zjw.oa.service;

import com.zjw.oa.entity.Hys;
import com.zjw.oa.entity.Hytz;
import com.zjw.oa.mapper.HytzMapper;
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
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * 会议室预约冲突检测测试
 * <p>
 * 覆盖场景：
 * 1. 同房间时间段重叠 → 拒绝
 * 2. 同房间相邻时间段（不重叠） → 允许
 * 3. 跨天预约 → 正确判断
 * 4. 不同房间同时段 → 互不影响
 * 5. 参会人时间冲突 → 拒绝
 * 6. 并发提交 → 仅一次成功
 * 7. 参数校验（非法时间、空用户、不存在的用户）
 */
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

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private Date parse(String s) throws Exception {
        return sdf.parse(s);
    }

    // ======================== 1. 同房间重叠 ========================

    @Test
    public void testSameRoom_ExactOverlap_ShouldConflict() throws Exception {
        // 第一次预约成功
        Map<String, Object> r1 = userService.bookMeeting(
                "A101", "项目评审",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(3L), 1L);
        assertTrue("第一次预约应成功", (Boolean) r1.get("success"));

        // 同一房间完全相同时间段 → 冲突
        Map<String, Object> r2 = userService.bookMeeting(
                "A101", "另一个会",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(5L), 1L);
        assertFalse("完全重叠的预约应失败", (Boolean) r2.get("success"));
        assertTrue("错误消息应包含冲突提示",
                r2.get("msg").toString().contains("冲突"));
    }

    @Test
    public void testSameRoom_PartialOverlap_ShouldConflict() throws Exception {
        // 预约 09:00-10:00
        Map<String, Object> r1 = userService.bookMeeting(
                "A101", "晨会",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(3L), 1L);
        assertTrue("第一次预约应成功", (Boolean) r1.get("success"));

        // 09:30-10:30 与 09:00-10:00 部分重叠
        Map<String, Object> r2 = userService.bookMeeting(
                "A101", "延后会",
                parse("2026-06-15 09:30"), parse("2026-06-15 10:30"),
                Arrays.asList(5L), 1L);
        assertFalse("部分重叠的预约应失败", (Boolean) r2.get("success"));
    }

    @Test
    public void testSameRoom_ContainedOverlap_ShouldConflict() throws Exception {
        // 预约 09:00-12:00（长时间段）
        Map<String, Object> r1 = userService.bookMeeting(
                "A101", "全天会",
                parse("2026-06-15 09:00"), parse("2026-06-15 12:00"),
                Arrays.asList(3L), 1L);
        assertTrue((Boolean) r1.get("success"));

        // 10:00-11:00 被包含在 09:00-12:00 中
        Map<String, Object> r2 = userService.bookMeeting(
                "A101", "插入会议",
                parse("2026-06-15 10:00"), parse("2026-06-15 11:00"),
                Arrays.asList(5L), 1L);
        assertFalse("被包含的预约应失败", (Boolean) r2.get("success"));
    }

    // ======================== 2. 相邻不重叠 ========================

    @Test
    public void testSameRoom_AdjacentMeetings_ShouldNotConflict() throws Exception {
        // 预约 09:00-10:00
        Map<String, Object> r1 = userService.bookMeeting(
                "A101", "上午第一场",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(3L), 1L);
        assertTrue("第一场应成功", (Boolean) r1.get("success"));

        // 10:00-11:00 紧邻第一场（边界相邻不算冲突）
        Map<String, Object> r2 = userService.bookMeeting(
                "A101", "上午第二场",
                parse("2026-06-15 10:00"), parse("2026-06-15 11:00"),
                Arrays.asList(5L), 1L);
        assertTrue("相邻时间段不应冲突", (Boolean) r2.get("success"));
    }

    @Test
    public void testSameRoom_AdjacentReverseOrder_ShouldNotConflict() throws Exception {
        // 先约后面的 10:00-11:00
        Map<String, Object> r1 = userService.bookMeeting(
                "B202", "后半场",
                parse("2026-06-15 10:00"), parse("2026-06-15 11:00"),
                Arrays.asList(3L), 1L);
        assertTrue((Boolean) r1.get("success"));

        // 再约前面的 09:00-10:00（与已存在的 10:00-11:00 相邻）
        Map<String, Object> r2 = userService.bookMeeting(
                "B202", "前半场",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(5L), 1L);
        assertTrue("反向相邻时间段不应冲突", (Boolean) r2.get("success"));
    }

    // ======================== 3. 跨天 ========================

    @Test
    public void testCrossDay_Overlapping_ShouldConflict() throws Exception {
        // 跨天预约：6月15日 22:00 → 6月16日 01:00
        Map<String, Object> r1 = userService.bookMeeting(
                "A101", "通宵部署",
                parse("2026-06-15 22:00"), parse("2026-06-16 01:00"),
                Arrays.asList(3L), 1L);
        assertTrue("跨天预约应成功", (Boolean) r1.get("success"));

        // 6月15日 23:00-23:30 → 与跨天预约重叠
        Map<String, Object> r2 = userService.bookMeeting(
                "A101", "临时会议",
                parse("2026-06-15 23:00"), parse("2026-06-15 23:30"),
                Arrays.asList(5L), 1L);
        assertFalse("与跨天预约重叠应失败", (Boolean) r2.get("success"));

        // 6月16日 00:30-02:00 → 与跨天预约重叠
        Map<String, Object> r3 = userService.bookMeeting(
                "A101", "凌晨会议",
                parse("2026-06-16 00:30"), parse("2026-06-16 02:00"),
                Arrays.asList(5L), 1L);
        assertFalse("跨天后半段重叠也应失败", (Boolean) r3.get("success"));
    }

    @Test
    public void testCrossDay_NotOverlapping_ShouldSucceed() throws Exception {
        // 跨天预约：6月15日 22:00 → 6月16日 01:00
        Map<String, Object> r1 = userService.bookMeeting(
                "A101", "通宵部署",
                parse("2026-06-15 22:00"), parse("2026-06-16 01:00"),
                Arrays.asList(3L), 1L);
        assertTrue((Boolean) r1.get("success"));

        // 6月16日 01:00-03:00 → 紧邻跨天预约结束时间，不重叠
        Map<String, Object> r2 = userService.bookMeeting(
                "A101", "凌晨收尾",
                parse("2026-06-16 01:00"), parse("2026-06-16 03:00"),
                Arrays.asList(5L), 1L);
        assertTrue("跨天预约结束后紧邻的会议应成功", (Boolean) r2.get("success"));
    }

    @Test
    public void testCrossMidnight_WholeDay_ShouldConflict() throws Exception {
        // 整天预约：6月15日 00:00 → 6月16日 00:00
        Map<String, Object> r1 = userService.bookMeeting(
                "C303", "全天占用",
                parse("2026-06-15 00:00"), parse("2026-06-16 00:00"),
                Arrays.asList(3L), 1L);
        assertTrue((Boolean) r1.get("success"));

        // 6月15日 14:00-15:00 → 被包含在整天预约中
        Map<String, Object> r2 = userService.bookMeeting(
                "C303", "下午会议",
                parse("2026-06-15 14:00"), parse("2026-06-15 15:00"),
                Arrays.asList(5L), 1L);
        assertFalse("整天预约内任何时段都应冲突", (Boolean) r2.get("success"));
    }

    // ======================== 4. 不同房间互不影响 ========================

    @Test
    public void testDifferentRooms_SameTime_ShouldBothSucceed() throws Exception {
        Map<String, Object> r1 = userService.bookMeeting(
                "A101", "技术评审",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(3L), 1L);
        assertTrue("A101 预约应成功", (Boolean) r1.get("success"));

        Map<String, Object> r2 = userService.bookMeeting(
                "B202", "市场会议",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(5L), 1L);
        assertTrue("B202 同时段预约应成功（不同房间）", (Boolean) r2.get("success"));
    }

    // ======================== 5. 参会人时间冲突 ========================

    @Test
    public void testSameUser_DifferentRooms_SameTime_ShouldConflict() throws Exception {
        // 用户 3 在 A101 有会
        Map<String, Object> r1 = userService.bookMeeting(
                "A101", "技术会",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(3L), 1L);
        assertTrue((Boolean) r1.get("success"));

        // 同一用户 3 在 B202 同时段 → 参会人冲突
        Map<String, Object> r2 = userService.bookMeeting(
                "B202", "市场会",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(3L), 1L);
        assertFalse("同一用户同时段不同房间应冲突", (Boolean) r2.get("success"));
        assertTrue("应提示参会人冲突",
                r2.get("msg").toString().contains("参会人"));
    }

    // ======================== 6. 并发提交 ========================

    @Test
    @Rollback(false) // 需要提交以便并发线程可见
    public void testConcurrentBooking_OnlyOneSucceeds() throws Exception {
        // 准备数据（需要提交才对新线程可见）
        String room = "A101";
        Date ksTime = parse("2026-06-20 14:00");
        Date jsTime = parse("2026-06-20 15:00");

        // 通过 bookMeeting 先预约一次来准备基础数据不行，因为会占用时间段
        // 直接用 mapper 插入一个不同的预约来验证并发场景
        // 测试场景：两个线程同时预约同一会议室同一时间段

        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Map<String, Object>> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 所有线程同时开始
                    Map<String, Object> result = userService.bookMeeting(
                            room, "并发会议" + idx,
                            ksTime, jsTime,
                            // 使用不同的用户避免参会人冲突（idx=0用userId3，idx=1用userId5）
                            idx == 0 ? Arrays.asList(3L) : Arrays.asList(5L),
                            1L);
                    results.add(result);
                } catch (Exception e) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("success", false);
                    err.put("msg", e.getMessage());
                    results.add(err);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 放行
        assertTrue("所有线程应在10秒内完成", doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        long successCount = 0;
        long failCount = 0;
        for (Map<String, Object> r : results) {
            if (Boolean.TRUE.equals(r.get("success"))) {
                successCount++;
            } else {
                failCount++;
            }
        }

        assertEquals("并发预约应恰好成功一次", 1, successCount);
        assertEquals("并发预约应恰好失败一次", 1, failCount);
    }

    // ======================== 7. 参数校验 ========================

    @Test
    public void testInvalidTime_EndBeforeStart_ShouldFail() throws Exception {
        Map<String, Object> result = userService.bookMeeting(
                "A101", "时间错误",
                parse("2026-06-15 10:00"), parse("2026-06-15 09:00"),
                Arrays.asList(3L), 1L);
        assertFalse("结束早于开始应失败", (Boolean) result.get("success"));
        assertTrue(result.get("msg").toString().contains("不合法"));
    }

    @Test
    public void testInvalidTime_SameStartEnd_ShouldFail() throws Exception {
        Map<String, Object> result = userService.bookMeeting(
                "A101", "零时长",
                parse("2026-06-15 10:00"), parse("2026-06-15 10:00"),
                Arrays.asList(3L), 1L);
        assertFalse("开始等于结束应失败", (Boolean) result.get("success"));
    }

    @Test
    public void testNullTime_ShouldFail() throws Exception {
        Map<String, Object> result = userService.bookMeeting(
                "A101", "空时间", null, null,
                Arrays.asList(3L), 1L);
        assertFalse("时间为null应失败", (Boolean) result.get("success"));
    }

    @Test
    public void testEmptyUserList_ShouldFail() throws Exception {
        Map<String, Object> result = userService.bookMeeting(
                "A101", "空用户",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Collections.emptyList(), 1L);
        assertFalse("空用户列表应失败", (Boolean) result.get("success"));
        assertTrue(result.get("msg").toString().contains("参会人"));
    }

    @Test
    public void testNonExistentUser_ShouldFail() throws Exception {
        Map<String, Object> result = userService.bookMeeting(
                "A101", "幽灵用户",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(9999L), 1L);
        assertFalse("不存在的用户应失败", (Boolean) result.get("success"));
        assertTrue(result.get("msg").toString().contains("不存在"));
    }

    @Test
    public void testEmptyRoomNumber_ShouldFail() throws Exception {
        Map<String, Object> result = userService.bookMeeting(
                "", "空房间",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(3L), 1L);
        assertFalse("空会议室编号应失败", (Boolean) result.get("success"));
    }

    // ======================== 8. checkRoomAvailable ========================

    @Test
    public void testCheckRoomAvailable_NoReservations_ShouldBeTrue() throws Exception {
        assertTrue("无预约时会议室应可用",
                userService.checkRoomAvailable("A101",
                        parse("2026-06-15 09:00"), parse("2026-06-15 10:00")));
    }

    @Test
    public void testCheckRoomAvailable_WithOverlap_ShouldBeFalse() throws Exception {
        userService.bookMeeting(
                "A101", "占用",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(3L), 1L);

        assertFalse("有重叠预约时会议室应不可用",
                userService.checkRoomAvailable("A101",
                        parse("2026-06-15 09:30"), parse("2026-06-15 10:30")));
    }

    @Test
    public void testCheckRoomAvailable_Adjacent_ShouldBeTrue() throws Exception {
        userService.bookMeeting(
                "A101", "占用",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(3L), 1L);

        assertTrue("相邻时间段会议室应可用",
                userService.checkRoomAvailable("A101",
                        parse("2026-06-15 10:00"), parse("2026-06-15 11:00")));
    }

    // ======================== 9. 整点边界 ========================

    @Test
    public void testHourBoundary_ExactHourMeetings_ShouldNotConflict() throws Exception {
        // 多个整点会议背靠背
        Map<String, Object> r1 = userService.bookMeeting(
                "A101", "整点1",
                parse("2026-06-15 09:00"), parse("2026-06-15 10:00"),
                Arrays.asList(3L), 1L);
        assertTrue((Boolean) r1.get("success"));

        Map<String, Object> r2 = userService.bookMeeting(
                "A101", "整点2",
                parse("2026-06-15 10:00"), parse("2026-06-15 11:00"),
                Arrays.asList(5L), 1L);
        assertTrue("整点相邻不应冲突", (Boolean) r2.get("success"));

        Map<String, Object> r3 = userService.bookMeeting(
                "A101", "整点3",
                parse("2026-06-15 11:00"), parse("2026-06-15 12:00"),
                Arrays.asList(3L), 1L);
        assertTrue("三个整点背靠背都应成功", (Boolean) r3.get("success"));
    }
}
