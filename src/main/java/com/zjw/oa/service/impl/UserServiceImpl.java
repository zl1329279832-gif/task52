package com.zjw.oa.service.impl;

import com.zjw.oa.entity.Dk;
import com.zjw.oa.entity.Dto.UserDto;
import com.zjw.oa.entity.Hys;
import com.zjw.oa.entity.Hytz;
import com.zjw.oa.mapper.HytzMapper;
import com.zjw.oa.mapper.UserMapper;
import com.zjw.oa.entity.User;
import com.zjw.oa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Description
 *
 * @author dddz97
 * @date 2019-03-21 10:47:40
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private HytzMapper hytzMapper;

    /** 会议室预约互斥锁，保证同一时刻仅一个线程执行 check + insert */
    private final Object bookingLock = new Object();

    @Override
    public User login(User user) {
        User user1 = userMapper.userLogin(user);
        return user1;
    }

    @Override
    public List<User> getUserList(User user) {
        return userMapper.getUserList(user);
    }

    @Override
    public void addDk(Dk dk) throws Exception {
        userMapper.addDk(dk);
    }

    @Override
    public List<UserDto> getDkList(UserDto userDto) {
        return userMapper.getDkList(userDto);
    }

    @Override
    public List<Hys> hysList() {
        return userMapper.hysList();
    }

    @Override
    public void updateHys(Hys hys) throws Exception {
        userMapper.updateHys(hys);
    }

    @Override
    public User getUser(User user) {
        return userMapper.getUser(user);
    }

    @Override
    public boolean checkRoomAvailable(String hysbh, Date ksTime, Date jsTime) {
        return hytzMapper.countOverlappingReservations(hysbh, ksTime, jsTime) == 0;
    }

    /**
     * 预约会议室，使用 synchronized 防止并发冲突：
     * 1. 校验开始/结束时间合法性
     * 2. 校验所有参会用户存在性
     * 3. 检测会议室时间段是否有重叠
     * 4. 检测每个参会人在该时间段是否已有会议
     * 5. 为每个参会人插入预约记录
     * 6. 更新会议室状态为"使用中"
     */
    @Override
    @Transactional
    public Map<String, Object> bookMeeting(String hysbh, String hynr, Date ksTime, Date jsTime,
                                           List<Long> userIdList, long operatorId) {
        synchronized (bookingLock) {
            Map<String, Object> result = new HashMap<>();

            // 1. 基本参数校验
            if (ksTime == null || jsTime == null || !ksTime.before(jsTime)) {
                result.put("success", false);
                result.put("msg", "会议时间不合法：结束时间必须晚于开始时间");
                return result;
            }
            if (hysbh == null || hysbh.trim().isEmpty()) {
                result.put("success", false);
                result.put("msg", "会议室编号不能为空");
                return result;
            }
            if (userIdList == null || userIdList.isEmpty()) {
                result.put("success", false);
                result.put("msg", "参会人列表不能为空");
                return result;
            }

            // 2. 校验参会用户存在性
            for (Long uid : userIdList) {
                User query = new User();
                query.setUserId(uid);
                User found = userMapper.getUser(query);
                if (found == null) {
                    result.put("success", false);
                    result.put("msg", "参会人不存在，userId=" + uid);
                    return result;
                }
            }

            // 3. 检测会议室时间段重叠
            int overlapCount = hytzMapper.countOverlappingReservations(hysbh, ksTime, jsTime);
            if (overlapCount > 0) {
                result.put("success", false);
                result.put("msg", "会议室[" + hysbh + "]在所选时间段已有预约，存在冲突");
                return result;
            }

            // 4. 检测参会人时间冲突
            for (Long uid : userIdList) {
                int userConflict = hytzMapper.countUserConflicts(uid.intValue(), ksTime, jsTime);
                if (userConflict > 0) {
                    User query = new User();
                    query.setUserId(uid);
                    User u = userMapper.getUser(query);
                    String name = u != null ? u.getUserName() : String.valueOf(uid);
                    result.put("success", false);
                    result.put("msg", "参会人[" + name + "]在所选时间段已有会议安排");
                    return result;
                }
            }

            // 5. 为每个参会人插入预约记录
            for (Long uid : userIdList) {
                Hytz hytz = new Hytz();
                hytz.setHynr(hynr);
                hytz.setHyTime(ksTime);
                hytz.setHydd(hysbh);
                hytz.setHybtzr(uid.intValue());
                hytz.setHyztr((int) operatorId);
                hytz.setKsTime(ksTime);
                hytz.setJsTime(jsTime);
                hytzMapper.insertReservation(hytz);
            }

            // 6. 更新会议室状态
            try {
                Hys hys = new Hys();
                hys.setHysbh(hysbh);
                hys.setHyszt("使用中");
                hys.setBz("已预约");
                userMapper.updateHys(hys);
            } catch (Exception e) {
                // 会议室状态更新失败不影响预约
            }

            result.put("success", true);
            result.put("msg", "会议室预约成功");
            return result;
        }
    }

}
