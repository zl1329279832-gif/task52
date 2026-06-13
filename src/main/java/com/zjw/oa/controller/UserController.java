package com.zjw.oa.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zjw.oa.entity.Dk;
import com.zjw.oa.entity.Dto.UserDto;
import com.zjw.oa.entity.Hys;
import com.zjw.oa.entity.User;
import com.zjw.oa.service.UserService;
import com.zjw.oa.util.JsonUtil;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Description
 *
 * @author dddz97
 * @date 2019-03-21 10:49:24
 */
@RestController
@EnableAutoConfiguration
@RequestMapping(value = "/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * Description 登录，可以加入@CrossOrigin支持跨域。
     */
    @RequestMapping(value = "/login")
    @ResponseBody
    @CrossOrigin
    public JSONObject login(@RequestBody User user) {
        User user1 = userService.login(user);
        if (user1 != null&&user1.getPermission()==1) {
            return JSON.parseObject("{statusCode:200,user:{isAdmin:false,userName:\""+user1.getUserName()+"\"," +
                    "zw:\"" + user1.getZw() + "\"," +
                    "userId:\"" + user1.getUserId() + "\"," +
                    "permission:" + user1.getPermission() + "}}");
        }else if(user1 != null){
            return JSON.parseObject("{statusCode:200,user:{isAdmin:false,userName:\""+user1.getUserName()+"\"," +
                    "userId:\"" + user1.getUserId() + "\"," +
                    "zw:\"" + user1.getZw() + "\"," +
                    "permission:" + user1.getPermission() + "}}");
        }
        return JSON.parseObject("{statusCode:404}");
    }

    /**
     * Description 获取用户列表
     */
    @RequestMapping(value = "/userList")
    @ResponseBody
    @CrossOrigin
    public JSONArray getUserList(@RequestBody(required=false) User user) {
        List<User> list = userService.getUserList(user);
        String jsonStr = JsonUtil.serializeDate(list);
        return JSON.parseArray(jsonStr);
    }

    @RequestMapping(value = "/addDk")
    @ResponseBody
    @CrossOrigin
    public JSONObject addDk(@RequestBody Dk dk) {
        try{
            userService.addDk(dk);
        }catch (Exception e){
            return JSON.parseObject("{success:false,msg:\"打卡失败！\"}");
        }
        return JSON.parseObject("{success:true,msg:\"打卡成功！\"}");
    }

    @RequestMapping(value = "/getDkList")
    @ResponseBody
    @CrossOrigin
    public JSONArray getDkList(@RequestBody UserDto userDto) {
        List<UserDto> list = userService.getDkList(userDto);
        String jsonStr = JsonUtil.serializeDate(list);
        return JSON.parseArray(jsonStr);
    }

    @RequestMapping(value = "/getHysList")
    @ResponseBody
    @CrossOrigin
    public JSONArray getHysList() {
        List<Hys> list = userService.hysList();
        String jsonStr = JsonUtil.serializeDate(list);
        return JSON.parseArray(jsonStr);
    }

    @RequestMapping(value = "/updateHys")
    @ResponseBody
    @CrossOrigin
    public JSONObject updateHys(Hys hys) {
        try{
            userService.updateHys(hys);
        }catch (Exception e){
            System.out.println(e);
            return JSON.parseObject("{success:false,msg:\"更改状态失败！\"}");
        }
        return JSON.parseObject("{success:true,msg:\"更改状态成功！\"}");
    }

    /**
     * 预约会议室：检测时间冲突、校验用户、写入 hytz 记录，成功后发送短信通知。
     * 并发请求通过 synchronized 保证只成功一次，失败请求返回明确错误信息。
     *
     * @param hysbh      会议室编号
     * @param hynr       会议内容
     * @param ksTimeStr  开始时间 yyyy-MM-dd HH:mm
     * @param jsTimeStr  结束时间 yyyy-MM-dd HH:mm
     * @param userIdList 参会人 ID 列表
     * @param operatorId 发起人 ID
     */
    @RequestMapping(value = "/book")
    @ResponseBody
    @CrossOrigin
    public JSONObject bookMeeting(@RequestParam String hysbh,
                                  @RequestParam(required = false, defaultValue = "会议") String hynr,
                                  @RequestParam String ksTimeStr,
                                  @RequestParam String jsTimeStr,
                                  @RequestParam List<String> userIdList,
                                  @RequestParam(defaultValue = "0") long operatorId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date ksTime;
        Date jsTime;
        try {
            ksTime = sdf.parse(ksTimeStr);
            jsTime = sdf.parse(jsTimeStr);
        } catch (Exception e) {
            return JSON.parseObject("{success:false,msg:\"时间格式错误，请使用 yyyy-MM-dd HH:mm\"}");
        }

        List<Long> uidList = new ArrayList<>();
        try {
            for (String s : userIdList) {
                uidList.add(Long.parseLong(s));
            }
        } catch (NumberFormatException e) {
            return JSON.parseObject("{success:false,msg:\"参会人ID格式错误\"}");
        }

        Map<String, Object> result = userService.bookMeeting(hysbh, hynr, ksTime, jsTime, uidList, operatorId);
        boolean success = (Boolean) result.get("success");
        String msg = (String) result.get("msg");

        if (success) {
            // 预约成功后发送短信通知
            String timeDisplay = sdf.format(ksTime);
            for (Long uid : uidList) {
                User query = new User();
                query.setUserId(uid);
                User u = userService.getUser(query);
                if (u != null && u.getPhone() != null) {
                    try {
                        message(u.getPhone(), hysbh, timeDisplay);
                    } catch (Exception ignored) {
                        // 短信发送失败不影响预约结果
                    }
                }
            }
        }

        String jsonStr = success
                ? "{success:true,msg:\"" + msg + "\"}"
                : "{success:false,msg:\"" + msg + "\"}";
        return JSON.parseObject(jsonStr);
    }

    /**
     * 原 /ky 接口（会议通知）：增加冲突检测，不再直接覆盖会议室状态。
     * 先调用 bookMeeting 检测可用性、写入 hytz，再发送短信。
     * 保留原参数签名以兼容前端，新增 ksTime/jsTime 参数用于时间段判断。
     */
    @RequestMapping(value = "/ky")
    @ResponseBody
    @CrossOrigin
    public JSONObject kh(@RequestParam String time,
                         @RequestParam String hysbh,
                         @RequestParam List<String> userIdList,
                         @RequestParam(required = false) String ksTimeStr,
                         @RequestParam(required = false) String jsTimeStr) throws Exception {
        // 若未提供时间段，则不做冲突检测，保持原行为（仅发短信）
        if (ksTimeStr == null || jsTimeStr == null) {
            for (int i = 0; i < userIdList.size(); i++) {
                User user = new User();
                user.setUserId(Long.parseLong(userIdList.get(i)));
                User u = userService.getUser(user);
                try {
                    message(u.getPhone(), hysbh, time);
                } catch (Exception e) {
                    return JSON.parseObject("{success:false,msg:\"短信发送失败\"}");
                }
            }
            return JSON.parseObject("{success:true,msg:\"通知发送成功\"}");
        }

        // 提供了时间段：走完整的预约 + 通知流程
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date ksTime;
        Date jsTime;
        try {
            ksTime = sdf.parse(ksTimeStr);
            jsTime = sdf.parse(jsTimeStr);
        } catch (Exception e) {
            return JSON.parseObject("{success:false,msg:\"时间格式错误，请使用 yyyy-MM-dd HH:mm\"}");
        }

        List<Long> uidList = new ArrayList<>();
        for (String s : userIdList) {
            uidList.add(Long.parseLong(s));
        }

        Map<String, Object> result = userService.bookMeeting(hysbh, "会议", ksTime, jsTime, uidList, 0);
        boolean success = (Boolean) result.get("success");
        String msg = (String) result.get("msg");

        if (success) {
            for (int i = 0; i < userIdList.size(); i++) {
                User user = new User();
                user.setUserId(Long.parseLong(userIdList.get(i)));
                User u = userService.getUser(user);
                try {
                    message(u.getPhone(), hysbh, time);
                } catch (Exception e) {
                    // 短信失败不影响预约结果
                }
            }
        }

        String jsonStr = success
                ? "{success:true,msg:\"" + msg + "\"}"
                : "{success:false,msg:\"" + msg + "\"}";
        return JSON.parseObject(jsonStr);
    }

    private void message(String phone,String hysbh,String time) throws IOException {
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod("http://gbk.api.smschinese.cn");
        post.addRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=gbk");
        NameValuePair[] data = { new NameValuePair("Uid", "dddz97"),
                new NameValuePair("Key", "d41d8cd98f00b204e980"),
                new NameValuePair("smsMob", phone),
                new NameValuePair("smsText", "请于"+time+",到"+hysbh+"开会") };
        post.setRequestBody(data);

        client.executeMethod(post);
        Header[] headers = post.getResponseHeaders();
        int statusCode = post.getStatusCode();
        System.out.println("statusCode:" + statusCode);
        for (Header h : headers) {
            System.out.println(h.toString());
        }
        String result = new String(post.getResponseBodyAsString().getBytes("gbk"));
        System.out.println(result);

        post.releaseConnection();
    }

}
