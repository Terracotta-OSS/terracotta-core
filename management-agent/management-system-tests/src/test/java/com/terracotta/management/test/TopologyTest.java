package com.terracotta.management.test;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

public class TopologyTest extends AbstractTsaAgentTestBase {
  public TopologyTest(TestConfig testConfig) {
    super(testConfig);

    testConfig.getGroupConfig().setMemberCount(2);

    testConfig.getClientConfig().setClientClasses(new Class[] { TopologyTestClient.class });
  }

  public static class TopologyTestClient extends AbstractTsaClient {
    @Override
    protected void doTsaTest() throws Throwable {

      for (int i = 0; i < 2; i++) {
        int port = getGroupData(0).getTsaGroupPort(i);
        String host = ConfigHelper.HOST;

        String result = httpGet("http://" + host + ":" + port + "/tc-management-api/agents/topologies");
        System.out.println("Server " + i);
        System.out.println(result);

        JSONArray parsed = (JSONArray)JSONValue.parse(result);
        assertEquals(1, parsed.size());
        JSONObject o0 = (JSONObject)parsed.get(0);
        JSONArray serverGroupEntities = (JSONArray)o0.get("serverGroupEntities");
        assertEquals(1, serverGroupEntities.size());
        JSONObject o1 = (JSONObject)serverGroupEntities.get(0);

        JSONArray servers = (JSONArray)o1.get("servers");
        assertEquals("testGroup0", o1.get("name"));
        assertEquals(2, servers.size());

        JSONObject attributes0 = (JSONObject)((JSONObject)servers.get(0)).get("attributes");
        if (attributes0.get("Name").equals("testserver0")) {
          assertEquals((long) getGroupData(0).getTsaGroupPort(0), attributes0.get("TSAGroupPort"));
          assertEquals("ACTIVE-COORDINATOR", attributes0.get("State"));
        } else {
          assertEquals("testserver1", attributes0.get("Name"));
          assertEquals((long) getGroupData(0).getTsaGroupPort(1), attributes0.get("TSAGroupPort"));
          assertEquals("PASSIVE-STANDBY", attributes0.get("State"));
        }

        JSONObject attributes1 = (JSONObject)((JSONObject)servers.get(1)).get("attributes");
        if (attributes1.get("Name").equals("testserver1")) {
          assertEquals((long) getGroupData(0).getTsaGroupPort(1), attributes1.get("TSAGroupPort"));
          assertEquals("PASSIVE-STANDBY", attributes1.get("State"));
        } else {
          assertEquals("testserver0", attributes1.get("Name"));
          assertEquals((long) getGroupData(0).getTsaGroupPort(0), attributes1.get("TSAGroupPort"));
          assertEquals("ACTIVE-COORDINATOR", attributes1.get("State"));
        }
      }


    }

    public TopologyTestClient(String[] args) {
      super(args);
    }
  }


}
