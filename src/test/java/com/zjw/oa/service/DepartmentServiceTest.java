package com.zjw.oa.service;

import com.zjw.oa.entity.Department;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
public class DepartmentServiceTest {

    @Autowired
    private DepartmentService departmentService;

    @Test
    public void testAddDepartment() throws Exception {
        Department dept = new Department();
        dept.setDepartmentName("测试部门");
        departmentService.addDept(dept);

        List<Department> list = departmentService.getDeptList();
        boolean found = false;
        for (Department d : list) {
            if ("测试部门".equals(d.getDepartmentName())) {
                found = true;
                break;
            }
        }
        assertTrue("新增部门应出现在列表中", found);
    }

    @Test
    public void testUpdateDepartment() throws Exception {
        Department dept = new Department();
        dept.setDepartmentName("原名部门");
        departmentService.addDept(dept);

        List<Department> list = departmentService.getDeptList();
        Department added = null;
        for (Department d : list) {
            if ("原名部门".equals(d.getDepartmentName())) {
                added = d;
                break;
            }
        }
        assertNotNull("新增部门应存在", added);

        added.setDepartmentName("改名部门");
        departmentService.updateDept(added);

        Department query = new Department();
        query.setDepartmentId(added.getDepartmentId());
        Department updated = departmentService.getDeptById(query);
        assertEquals("改名部门", updated.getDepartmentName());
    }

    @Test
    public void testDeleteDepartment() throws Exception {
        Department dept = new Department();
        dept.setDepartmentName("待删部门");
        departmentService.addDept(dept);

        List<Department> list = departmentService.getDeptList();
        Department added = null;
        for (Department d : list) {
            if ("待删部门".equals(d.getDepartmentName())) {
                added = d;
                break;
            }
        }
        assertNotNull(added);

        departmentService.delDept(added);

        Department query = new Department();
        query.setDepartmentId(added.getDepartmentId());
        Department deleted = departmentService.getDeptById(query);
        assertNull("删除后部门应不存在", deleted);
    }
}
