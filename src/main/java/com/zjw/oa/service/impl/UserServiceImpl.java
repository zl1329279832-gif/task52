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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Autowired
    private PlatformTransactionManager transactionManager;

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
    public Map<String, Object> bookMeeting(String hysbh, String hynr, Date ksTime, Date jsTime,
                                            List<Long> userIdList, long operatorId) {
        // 参数校验（无需事务和锁）
        Map<String, Object> validationResult = validateBookingParams(hysbh, ksTime, jsTime, userIdList);
        if (validationResult != null) {
            return validationResult;
        }

        // synchronized 包裹事务，确保事务在锁内提交，防止并发 TOCTOU 竞争
        synchronized (bookingLock) {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            return txTemplate.execute(status -> {
                Map<String, Object> result = new HashMap<>();

                // 校验所有用户存在
                for (Long uid : userIdList) {
                    User query = new User();
                    query.setUserId(uid);
                    User u = userMapper.getUser(query);
                    if (u == null) {
                        result.put("success", false);
                        result.put("msg", "用户ID " + uid + " 不存在");
                        return result;
                    }
                }

                // 检查会议室时间冲突
                int overlapCount = hytzMapper.countOverlappingReservations(hysbh, ksTime, jsTime);
                if (overlapCount > 0) {
                    result.put("success", false);
                    result.put("msg", "会议室 " + hysbh + " 在该时间段已被预约");
                    return result;
                }

                // 检查每个用户的日程冲突
                for (Long uid : userIdList) {
                    int userConflict = hytzMapper.countUserConflicts(uid.intValue(), ksTime, jsTime);
                    if (userConflict > 0) {
                        User query = new User();
                        query.setUserId(uid);
                        User u = userMapper.getUser(query);
                        result.put("success", false);
                        result.put("msg", "用户 " + u.getUserName() + " 在该时间段已有其他会议");
                        return result;
                    }
                }

                // 插入预约记录
                Date now = new Date();
                for (Long uid : userIdList) {
                    Hytz hytz = new Hytz();
                    hytz.setHynr(hynr);
                    hytz.setHyTime(now);
                    hytz.setHydd(hysbh);
                    hytz.setHybtzr(uid.intValue());
                    hytz.setHyztr((int) operatorId);
                    hytz.setKsTime(ksTime);
                    hytz.setJsTime(jsTime);
                    hytzMapper.insertReservation(hytz);
                }

                // 更新会议室状态
                try {
                    Hys hys = new Hys();
                    hys.setHysbh(hysbh);
                    hys.setHyszt("使用中");
                    hys.setBz("已预约");
                    userMapper.updateHys(hys);
                } catch (Exception e) {
                    // 会议室状态更新失败不影响预约结果
                }

                result.put("success", true);
                result.put("msg", "会议室预约成功");
                return result;
            });
        }
    }

    private Map<String, Object> validateBookingParams(String hysbh, Date ksTime, Date jsTime, List<Long> userIdList) {
        Map<String, Object> result = new HashMap<>();
        if (ksTime == null || jsTime == null) {
            result.put("success", false);
            result.put("msg", "会议开始时间和结束时间不能为空");
            return result;
        }
        if (!ksTime.before(jsTime)) {
            result.put("success", false);
            result.put("msg", "会议开始时间必须早于结束时间");
            return result;
        }
        if (hysbh == null || hysbh.trim().isEmpty()) {
            result.put("success", false);
            result.put("msg", "会议室编号不能为空");
            return result;
        }
        if (userIdList == null || userIdList.isEmpty()) {
            result.put("success", false);
            result.put("msg", "参会人员列表不能为空");
            return result;
        }
        return null;
    }

    @Override
    public boolean checkRoomAvailable(String hysbh, Date ksTime, Date jsTime) {
        return hytzMapper.countOverlappingReservations(hysbh, ksTime, jsTime) == 0;
    }

}
