package com.zjw.oa.service;

import com.zjw.oa.entity.Dk;
import com.zjw.oa.entity.Dto.UserDto;
import com.zjw.oa.entity.Hys;
import com.zjw.oa.entity.Hytz;
import com.zjw.oa.entity.User;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Description 用户service层
 *
 * @author dddz97
 * @date 2019-03-21 10:47:26
 */
public interface UserService {

    User login(User user);

    List<User> getUserList(User user);

    void addDk(Dk dk) throws Exception;

    List<UserDto> getDkList(UserDto userDto);

    List<Hys> hysList();

    void updateHys(Hys hys)throws Exception;

    User getUser(User user);

    /**
     * 预约会议室（含冲突检测与并发控制）。
     * 在同一时刻仅允许一个线程执行，保证并发预约只有一次成功。
     *
     * @param hysbh      会议室编号
     * @param hynr       会议内容
     * @param ksTime     开始时间
     * @param jsTime     结束时间
     * @param userIdList 参会人 ID 列表
     * @param operatorId 发起人 ID
     * @return 结果 Map：success=true/false, msg=描述信息
     */
    Map<String, Object> bookMeeting(String hysbh, String hynr, Date ksTime, Date jsTime,
                                    List<Long> userIdList, long operatorId);

    /**
     * 查询会议室在指定时间段是否存在重叠预约。
     *
     * @return true 表示可用（无重叠），false 表示有冲突
     */
    boolean checkRoomAvailable(String hysbh, Date ksTime, Date jsTime);
}
