package com.zjw.oa.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zjw.oa.entity.Xtrw;
import com.zjw.oa.entity.XtrwFile;
import com.zjw.oa.entity.XtrwProgress;
import com.zjw.oa.entity.XtrwTodo;
import com.zjw.oa.service.XtrwService;
import com.zjw.oa.util.JsonUtil;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@EnableAutoConfiguration
@RequestMapping(value = "/xtrw")
public class XtrwController {

    private static String FILE_PATH = "D:/test/xtrw/";

    @Resource
    private XtrwService xtrwService;

    /**
     * 创建协作任务
     */
    @RequestMapping(value = "/addXtrw")
    @ResponseBody
    @CrossOrigin
    public JSONObject addXtrw(Xtrw xtrw, String xtUserIds, String visibleDeptIds) {
        try {
            xtrwService.addXtrw(xtrw, xtUserIds, visibleDeptIds);
        } catch (Exception e) {
            return JSON.parseObject("{success:false,msg:\"添加协作任务失败！\"}");
        }
        return JSON.parseObject("{success:true,msg:\"添加协作任务成功！\"}");
    }

    /**
     * 任务详情（含协办人/附件/进度日志/可见部门）
     */
    @RequestMapping(value = "/xtrwDetail")
    @ResponseBody
    @CrossOrigin
    public JSONObject xtrwDetail(Xtrw xtrw) {
        Xtrw detail = xtrwService.getXtrwDetail(xtrw);
        String jsonStr = JsonUtil.serializeDate(detail);
        return JSON.parseObject(jsonStr);
    }

    /**
     * 更新进度（需校验参与人身份）
     */
    @RequestMapping(value = "/updateProgress")
    @ResponseBody
    @CrossOrigin
    public JSONObject updateProgress(XtrwProgress progress) {
        try {
            xtrwService.updateProgress(progress);
        } catch (Exception e) {
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("msg", e.getMessage());
            return result;
        }
        return JSON.parseObject("{success:true,msg:\"更新进度成功！\"}");
    }

    /**
     * 关闭任务（仅主负责人）
     */
    @RequestMapping(value = "/closeXtrw")
    @ResponseBody
    @CrossOrigin
    public JSONObject closeXtrw(Xtrw xtrw) {
        try {
            xtrwService.closeXtrw(xtrw);
        } catch (Exception e) {
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("msg", e.getMessage());
            return result;
        }
        return JSON.parseObject("{success:true,msg:\"任务已关闭！\"}");
    }

    /**
     * 上传任务附件
     */
    @RequestMapping(value = "/uploadFile")
    @ResponseBody
    @CrossOrigin
    public JSONObject uploadFile(int xtrwId, long uploadUserId, @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return JSON.parseObject("{success:false,msg:\"文件为空！\"}");
            }
            String fileName = file.getOriginalFilename();
            File dest = new File(FILE_PATH + fileName);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            file.transferTo(dest);

            XtrwFile xtrwFile = new XtrwFile();
            xtrwFile.setXtrwId(xtrwId);
            xtrwFile.setFileName(fileName);
            xtrwFile.setFilePath(FILE_PATH + fileName);
            xtrwFile.setUploadUserId(uploadUserId);
            xtrwService.addXtrwFile(xtrwFile);
        } catch (IOException e) {
            return JSON.parseObject("{success:false,msg:\"文件上传失败！\"}");
        } catch (Exception e) {
            return JSON.parseObject("{success:false,msg:\"附件保存失败！\"}");
        }
        return JSON.parseObject("{success:true,msg:\"附件上传成功！\"}");
    }

    /**
     * 我负责的任务
     */
    @RequestMapping(value = "/myOwned")
    @ResponseBody
    @CrossOrigin
    public JSONArray myOwned(long zfrUserId) {
        List<Xtrw> list = xtrwService.getMyOwnedTasks(zfrUserId);
        String jsonStr = JsonUtil.serializeDate(list);
        return JSON.parseArray(jsonStr);
    }

    /**
     * 我协办的任务
     */
    @RequestMapping(value = "/myCollab")
    @ResponseBody
    @CrossOrigin
    public JSONArray myCollab(long xtUserId) {
        List<Xtrw> list = xtrwService.getMyCollabTasks(xtUserId);
        String jsonStr = JsonUtil.serializeDate(list);
        return JSON.parseArray(jsonStr);
    }

    /**
     * 已逾期任务
     */
    @RequestMapping(value = "/overdue")
    @ResponseBody
    @CrossOrigin
    public JSONArray overdue() {
        List<Xtrw> list = xtrwService.getOverdueTasks();
        String jsonStr = JsonUtil.serializeDate(list);
        return JSON.parseArray(jsonStr);
    }

    /**
     * 已完成任务
     */
    @RequestMapping(value = "/completed")
    @ResponseBody
    @CrossOrigin
    public JSONArray completed() {
        List<Xtrw> list = xtrwService.getCompletedTasks();
        String jsonStr = JsonUtil.serializeDate(list);
        return JSON.parseArray(jsonStr);
    }

    /**
     * 部门可见任务
     */
    @RequestMapping(value = "/visibleTasks")
    @ResponseBody
    @CrossOrigin
    public JSONArray visibleTasks(int departmentId) {
        List<Xtrw> list = xtrwService.getVisibleTasksByDeptId(departmentId);
        String jsonStr = JsonUtil.serializeDate(list);
        return JSON.parseArray(jsonStr);
    }

    /**
     * 生成逾期公司公告
     */
    @RequestMapping(value = "/generateAnnouncement")
    @ResponseBody
    @CrossOrigin
    public JSONObject generateAnnouncement(int xtrwId) {
        try {
            int ggId = xtrwService.generateAnnouncement(xtrwId);
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("msg", "已生成逾期公告！");
            result.put("ggId", ggId);
            return result;
        } catch (Exception e) {
            return JSON.parseObject("{success:false,msg:\"生成公告失败！\"}");
        }
    }

    /**
     * 我的待办提醒
     */
    @RequestMapping(value = "/todoList")
    @ResponseBody
    @CrossOrigin
    public JSONArray todoList(long userId) {
        List<XtrwTodo> list = xtrwService.getTodoList(userId);
        String jsonStr = JsonUtil.serializeDate(list);
        return JSON.parseArray(jsonStr);
    }

    /**
     * 标记待办已读
     */
    @RequestMapping(value = "/markTodoRead")
    @ResponseBody
    @CrossOrigin
    public JSONObject markTodoRead(XtrwTodo todo) {
        try {
            xtrwService.markTodoRead(todo);
        } catch (Exception e) {
            return JSON.parseObject("{success:false,msg:\"标记已读失败！\"}");
        }
        return JSON.parseObject("{success:true,msg:\"标记已读成功！\"}");
    }
}
