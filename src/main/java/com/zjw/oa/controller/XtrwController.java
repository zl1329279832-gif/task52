package com.zjw.oa.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zjw.oa.entity.*;
import com.zjw.oa.service.XtrwService;
import com.zjw.oa.util.JsonUtil;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

@RestController
@EnableAutoConfiguration
@RequestMapping(value = "/xtrw")
public class XtrwController {

    private static final String FILE_PATH = "D:/test/";

    @Resource
    private XtrwService xtrwService;

    /**
     * 创建协作任务
     */
    @RequestMapping(value = "/add")
    @ResponseBody
    @CrossOrigin
    public JSONObject addXtrw(@RequestBody JSONObject param) {
        try {
            Xtrw xtrw = new Xtrw();
            xtrw.setXtrwTitle(param.getString("xtrwTitle"));
            xtrw.setXtrwDesc(param.getString("xtrwDesc"));
            xtrw.setZfrUserId(param.getIntValue("zfrUserId"));
            xtrw.setZfrUserName(param.getString("zfrUserName"));
            xtrw.setJzTime(param.getDate("jzTime"));

            List<XtrwXtUser> xtUsers = null;
            JSONArray xtUserArr = param.getJSONArray("xtUsers");
            if (xtUserArr != null) {
                xtUsers = xtUserArr.toJavaList(XtrwXtUser.class);
            }

            List<XtrwVisibleDept> visibleDepts = null;
            JSONArray deptArr = param.getJSONArray("visibleDepts");
            if (deptArr != null) {
                visibleDepts = deptArr.toJavaList(XtrwVisibleDept.class);
            }

            xtrwService.addXtrw(xtrw, xtUsers, visibleDepts);
            JSONObject result = JSON.parseObject("{\"success\":true,\"msg\":\"创建协作任务成功！\"}");
            result.put("xtrwId", xtrw.getXtrwId());
            return result;
        } catch (Exception e) {
            return JSON.parseObject("{\"success\":false,\"msg\":\"创建协作任务失败！\"}");
        }
    }

    /**
     * 任务详情（含进度记录、附件、协办人、可见部门）
     */
    @RequestMapping(value = "/detail")
    @ResponseBody
    @CrossOrigin
    public JSONObject detail(Xtrw xtrw) {
        Xtrw task = xtrwService.getXtrwDetail(xtrw);
        if (task == null) {
            return JSON.parseObject("{\"success\":false,\"msg\":\"任务不存在\"}");
        }
        String jsonStr = JsonUtil.serializeDate(task);
        JSONObject result = JSON.parseObject(jsonStr);
        int xtrwId = task.getXtrwId();
        result.put("xtUsers", JSON.parseArray(JsonUtil.serializeDate(xtrwService.getXtUsers(xtrwId))));
        result.put("progressList", JSON.parseArray(JsonUtil.serializeDate(xtrwService.getProgressList(xtrwId))));
        result.put("files", JSON.parseArray(JsonUtil.serializeDate(xtrwService.getFileList(xtrwId))));
        result.put("visibleDepts", JSON.parseArray(JsonUtil.serializeDate(xtrwService.getVisibleDepts(xtrwId))));
        return result;
    }

    /**
     * 我负责的任务
     */
    @RequestMapping(value = "/myOwned")
    @ResponseBody
    @CrossOrigin
    public JSONArray myOwned(int zfrUserId) {
        List<Xtrw> list = xtrwService.getMyOwnedTasks(zfrUserId);
        return JSON.parseArray(JsonUtil.serializeDate(list));
    }

    /**
     * 我协办的任务
     */
    @RequestMapping(value = "/myCollab")
    @ResponseBody
    @CrossOrigin
    public JSONArray myCollab(int xtUserId) {
        List<Xtrw> list = xtrwService.getMyCollabTasks(xtUserId);
        return JSON.parseArray(JsonUtil.serializeDate(list));
    }

    /**
     * 已逾期任务
     */
    @RequestMapping(value = "/overdue")
    @ResponseBody
    @CrossOrigin
    public JSONArray overdue() {
        List<Xtrw> list = xtrwService.getOverdueTasks();
        return JSON.parseArray(JsonUtil.serializeDate(list));
    }

    /**
     * 已完成任务
     */
    @RequestMapping(value = "/completed")
    @ResponseBody
    @CrossOrigin
    public JSONArray completed() {
        List<Xtrw> list = xtrwService.getCompletedTasks();
        return JSON.parseArray(JsonUtil.serializeDate(list));
    }

    /**
     * 更新进度（协办人/负责人）
     */
    @RequestMapping(value = "/updateProgress")
    @ResponseBody
    @CrossOrigin
    public JSONObject updateProgress(@RequestBody XtrwProgress progress) {
        try {
            xtrwService.updateProgress(progress);
            return JSON.parseObject("{\"success\":true,\"msg\":\"更新进度成功！\"}");
        } catch (Exception e) {
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("msg", e.getMessage());
            return result;
        }
    }

    /**
     * 负责人关闭/完成任务
     */
    @RequestMapping(value = "/close")
    @ResponseBody
    @CrossOrigin
    public JSONObject close(int xtrwId, int userId, String summary) {
        try {
            xtrwService.closeTask(xtrwId, userId, summary);
            return JSON.parseObject("{\"success\":true,\"msg\":\"任务已关闭！\"}");
        } catch (Exception e) {
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("msg", e.getMessage());
            return result;
        }
    }

    /**
     * 待办提醒列表
     */
    @RequestMapping(value = "/todoList")
    @ResponseBody
    @CrossOrigin
    public JSONArray todoList(int userId) {
        List<XtrwTodo> list = xtrwService.getTodoList(userId);
        return JSON.parseArray(JsonUtil.serializeDate(list));
    }

    /**
     * 标记待办已读
     */
    @RequestMapping(value = "/markTodoRead")
    @ResponseBody
    @CrossOrigin
    public JSONObject markTodoRead(XtrwTodo todo) {
        try {
            todo.setIsRead(1);
            xtrwService.markTodoRead(todo);
            return JSON.parseObject("{\"success\":true,\"msg\":\"标记已读成功！\"}");
        } catch (Exception e) {
            return JSON.parseObject("{\"success\":false,\"msg\":\"标记已读失败！\"}");
        }
    }

    /**
     * 检查逾期任务并生成待办提醒（可选同步生成公告）
     */
    @RequestMapping(value = "/checkOverdue")
    @ResponseBody
    @CrossOrigin
    public JSONObject checkOverdue(@RequestParam(defaultValue = "false") boolean generateGg) {
        try {
            int count = xtrwService.checkOverdue(generateGg);
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("msg", "检查完成，发现" + count + "个新逾期任务");
            result.put("count", count);
            return result;
        } catch (Exception e) {
            return JSON.parseObject("{\"success\":false,\"msg\":\"检查逾期任务失败！\"}");
        }
    }

    /**
     * 上传任务附件
     */
    @RequestMapping(value = "/uploadFile")
    @ResponseBody
    @CrossOrigin
    public JSONObject uploadFile(@RequestParam("file") MultipartFile file,
                                 int xtrwId, int uploadUserId) {
        try {
            String fileName = file.getOriginalFilename();
            File dest = new File(FILE_PATH + fileName);
            file.transferTo(dest);

            XtrwFile xtrwFile = new XtrwFile();
            xtrwFile.setXtrwId(xtrwId);
            xtrwFile.setFileName(fileName);
            xtrwFile.setFilePath(FILE_PATH + fileName);
            xtrwFile.setUploadUserId(uploadUserId);
            xtrwService.addXtrwFile(xtrwFile);

            return JSON.parseObject("{\"success\":true,\"msg\":\"上传附件成功！\"}");
        } catch (Exception e) {
            return JSON.parseObject("{\"success\":false,\"msg\":\"上传附件失败！\"}");
        }
    }
}
