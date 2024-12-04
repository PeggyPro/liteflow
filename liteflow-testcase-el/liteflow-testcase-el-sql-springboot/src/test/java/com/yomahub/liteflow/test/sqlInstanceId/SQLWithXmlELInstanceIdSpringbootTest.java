package com.yomahub.liteflow.test.sqlInstanceId;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.entity.InstanceIdDto;
import com.yomahub.liteflow.flow.instanceId.NodeInstanceIdManageSpi;
import com.yomahub.liteflow.flow.instanceId.NodeInstanceIdManageSpiHolder;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.JsonUtil;
import org.assertj.core.util.Sets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author jay li
 * @since 2.12.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/application-instanceId-xml.properties")
@SpringBootTest(classes = SQLWithXmlELInstanceIdSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.sql.cmp"})
public class SQLWithXmlELInstanceIdSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testSQLWithXmlChain() throws SQLException, JSONException {
        // 查询数据库实例id
        String instanceId = queryInstanceId("r_chain4");
        // 解析 JSON
        List<InstanceIdDto> instanceIdDtos = JsonUtil.parseList(instanceId, InstanceIdDto.class);
        // 构造实例id字符串
        StringBuilder result = new StringBuilder();
        int i = 0;

        for (InstanceIdDto dto : instanceIdDtos) {
            result.append(dto.getNodeId()).append("[").append(dto.getInstanceId()).append("]");
            if (i + 1 < instanceIdDtos.size()) {
                result.append("==>");
            }
            i++;
        }

        LiteflowResponse response = flowExecutor.execute2Resp("r_chain4", "arg");
        Assertions.assertEquals("c==>b==>a", response.getExecuteStepStr());
        Assertions.assertEquals(result.toString(), response.getExecuteStepStrWithInstanceId());
        // 重复执行 检查实例id是否变化
        response = flowExecutor.execute2Resp("r_chain4", "arg");
        Assertions.assertEquals(result.toString(), response.getExecuteStepStrWithInstanceId());
    }

    // 测试sql实例id 构建 坐标返回
    @Test
    public void testSQLWithXmlChain2() {
        LiteflowResponse response = flowExecutor.execute2Resp("r_chain4", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("c==>b==>a", response.getExecuteStepStr());

        String executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();
        List<String> strings = extractValuesList(executeStepStrWithInstanceId);
        NodeInstanceIdManageSpi nodeInstanceIdManageSpi = NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi();

        String[] nodes = new String[]{"c", "b", "a"};
        for (int i = 0; i < strings.size(); i++) {
            Assertions.assertEquals(nodeInstanceIdManageSpi.getNodeInstanceLocationById("r_chain4", strings.get(i)), nodes[i] + "(0)");
        }
        System.out.println(strings);
        HashSet<String> hashSet = Sets.newHashSet(strings);
        Assertions.assertEquals(hashSet.size(), 3);
    }


    public static List<String> extractValuesList(String input) {
        List<String> values = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[(.*?)]");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    public String queryInstanceId(String chainId) throws SQLException {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        SQLParserVO sqlParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), SQLParserVO.class);
        Connection connection;
        try {
            connection = DriverManager.getConnection(sqlParserVO.getUrl(), sqlParserVO.getUsername(),
                    sqlParserVO.getPassword());
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("select * from NODE_INSTANCE_ID_TABLE where APPLICATION_NAME = 'demo' " +
                    "and chain_id = '" + chainId + "' ");

            String res = "";
            while (rs.next()) {
                res = rs.getString("node_instance_id_map_json");
            }
            return res;
        } catch (SQLException e) {
            throw new ELSQLException(e.getMessage());
        }
    }
}
