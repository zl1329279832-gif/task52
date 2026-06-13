package com.zjw.oa.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zjw.oa.entity.Department;
import com.zjw.oa.service.DepartmentService;
import com.zjw.oa.util.JsonUtil;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@EnableAutoConfiguration
@RequestMapping(value = "/dept")
public class DepartmentController {

    @Resource
    private DepartmentService departmentService;

    @RequestMapping(value = "/deptList")
    @ResponseBody
    @CrossOrigin
    public JSONArray deptList() {
        List<Department> list = departmentService.getDeptList();
        String jsonStr = JsonUtil.serializeDate(list);
        return JSON.parseArray(jsonStr);
    }

    @RequestMapping(value = "/addDept")
    @ResponseBody
    @CrossOrigin
    public JSONObject addDept(Department dept) {
        try {
            departmentService.addDept(dept);
        } catch (Exception e) {
            return JSON.parseObject("{success:false,msg:\"添加部门失败！\"}");
        }
        return JSON.parseObject("{success:true,msg:\"添加部门成功！\"}");
    }

    @RequestMapping(value = "/updateDept")
    @ResponseBody
    @CrossOrigin
    public JSONObject updateDept(Department dept) {
        try {
            departmentService.updateDept(dept);
        } catch (Exception e) {
            return JSON.parseObject("{success:false,msg:\"修改部门失败！\"}");
        }
        return JSON.parseObject("{success:true,msg:\"修改部门成功！\"}");
    }

    @RequestMapping(value = "/delDept")
    @ResponseBody
    @CrossOrigin
    public JSONObject delDept(Department dept) {
        try {
            departmentService.delDept(dept);
        } catch (Exception e) {
            return JSON.parseObject("{success:false,msg:\"删除部门失败！\"}");
        }
        return JSON.parseObject("{success:true,msg:\"删除部门成功！\"}");
    }
}
