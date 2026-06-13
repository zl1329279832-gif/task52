package com.zjw.oa.service;

import com.zjw.oa.entity.Dk;
import com.zjw.oa.entity.Dto.UserDto;
import com.zjw.oa.entity.Hys;
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

    Map<String, Object> bookMeeting(String hysbh, String hynr, Date ksTime, Date jsTime,
                                     List<Long> userIdList, long operatorId);

    boolean checkRoomAvailable(String hysbh, Date ksTime, Date jsTime);

}
