package com.zjw.oa;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "mybatis.typeAliasesPackage=com.zjw.oa.entity",
        "mybatis.mapperLocations=classpath:mapper/*.xml"
})
public class OaApplicationTests {

    @Test
    public void contextLoads() {
    }

}
