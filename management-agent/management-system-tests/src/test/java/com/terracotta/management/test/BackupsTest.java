package com.terracotta.management.test;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.terracotta.management.resource.AgentEntity;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * BackupsTest
 */
public class BackupsTest extends AbstractTsaAgentTestBase {
  private static final int GROUP_COUNT = 1; // cannot have Active-Active with Open Source
  private static final int MEMBER_COUNT = 2;

  public BackupsTest(TestConfig testConfig) {
    super(testConfig);
    //testConfig.setRestartable(true); // cannot have Restartable with Open Source
    testConfig.setNumOfGroups(GROUP_COUNT);
    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[]{BackupsTestClient.class});
  }

  public static class BackupsTestClient extends AbstractTsaClient {

    public BackupsTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      // test REST on both active and passive
      testResources(0, 0);
      testResources(0, 1);
    }

    private void testResources(int group, int member) throws IOException {
      // /agents/backups POST
      String response = httpPost("http://" + ConfigHelper.HOST + ":" + getGroupData(group).getManagementPort(member) + "/tc-management-api/agents/backups");
      System.out.println(">>> >>> >>> " + response);
      JSONArray responseArray = (JSONArray)JSONValue.parse(response);

      JSONObject backup = (JSONObject)responseArray.get(0);
      assertThat((String)backup.get("agentId"), is(AgentEntity.EMBEDDED_AGENT_ID));
      assertThat((String)backup.get("sourceId"), is("testserver0")); // the 1st L2 always answers first that it cannot backup
      assertThat((String)backup.get("status"), is("FAILED"));
      assertThat(backup.get("error"), notNullValue());

      // /agents/backups GET
      JSONArray backupsArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(group).getManagementPort(member), "/tc-management-api/agents/backups");
      assertThat(backupsArray.size(), is(0));
    }
  }


}
