package com.terracotta.management.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.terracotta.management.resource.Representable;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

import java.io.IOException;

/**
 * AgentsTest
 */
public class AgentsTest extends AbstractTsaAgentTestBase {
  private static final int GROUP_COUNT = 1; // cannot have Active-Active with Open Source
  private static final int MEMBER_COUNT = 2;

  public AgentsTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.setNumOfGroups(GROUP_COUNT);
    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[]{AgentsTestClient.class});
  }

  public static class AgentsTestClient extends AbstractTsaClient {
    private final int groupCount;
    private final int memberCount;

    public AgentsTestClient(String[] args) {
      super(args);
      groupCount = this.getTestControlMbean().getGroupsData().length;
      memberCount = this.getTestControlMbean().getGroupsData()[0].getServerCount();
    }

    @Override
    protected void doTsaTest() throws Throwable {
      // test REST on both active and passive
      testResources(0, 0);
      testResources(0, 1);

      // crash the active -> make sure it's broken and that the passive is not impacted
      getTestControlMbean().crashActiveServer(0);
      try {
        testResources(0, 0);
        fail("expected IOException");
      } catch (IOException e) {
        // expected, the server is down
      }
      try {
        testResources(0, 1);
        fail("expected IOException");
      } catch (IOException e) {
        // expected, there is no more active coordinator to list L1 agents
        assertEquals(400, getLastHttpResponseCode());
      }

      // restart crashed server -> make sure everything is back in working order
      getTestControlMbean().restartLastCrashedServer(0);
      waitUntilAllServerAgentsUp();
      testResources(0, 0);
      testResources(0, 1);
    }

    private void waitUntilAllServerAgentsUp() {
      waitUntilServerAgentUp(getGroupData(0).getManagementPort(0));
      waitUntilServerAgentUp(getGroupData(0).getManagementPort(1));
    }

    private void testResources(int group, int member) throws IOException {
      // /agents
      // [{"version":"4.1.0-SNAPSHOT","agentId":"embedded","agencyOf":"TSA","rootRepresentables":{"urls":"http://localhost:41857,http://localhost:23787"}}]
      JSONArray agentsArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(group).getManagementPort(member), "/tc-management-api/agents");
      assertThat(agentsArray.size(), is(1));

      JSONObject content = (JSONObject)agentsArray.get(0);
      assertThat(content.size(), is(4));
      JSONObject rootRepresentables = (JSONObject)content.get("rootRepresentables");
      assertThat(rootRepresentables.size(), is(1));
      String[] urls = ((String)rootRepresentables.get("urls")).split(",");
      assertThat(urls.length, is(memberCount * groupCount));
      assertThat(urls[0], is("http://localhost:" + getGroupData(0).getManagementPort(0)));
      assertThat(urls[1], is("http://localhost:" + getGroupData(0).getManagementPort(1)));
      assertThat((String) content.get("agentId"), is(Representable.EMBEDDED_AGENT_ID));
      assertThat((String)content.get("version"), is(guessVersion()));
      assertThat((String)content.get("agencyOf"), is("TSA"));

      JSONArray agentsInfoArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getManagementPort(member), "/tc-management-api/agents/info");
      assertThat(agentsInfoArray.size(), is(1));

      // /agents/info
      // [{"agentId":"embedded","agencyOf":"TSA","available":true,"secured":false,"sslEnabled":false,"needClientAuth":false,
      //   "licensed":false,"sampleHistorySize":0,"sampleIntervalSeconds":0,"enabled":true,"restAPIVersion":"4.1.0-SNAPSHOT"}]
      JSONObject info = (JSONObject)agentsInfoArray.get(0);
      assertThat(info.size(), is(12));
      assertThat((String) info.get("version"), is(guessVersion()));
      assertThat((Boolean)info.get("available"), is(true));
      assertThat((Boolean)info.get("secured"), is(false));
      assertThat((Boolean)info.get("sslEnabled"), is(false));
      assertThat((Boolean)info.get("needClientAuth"), is(false));
      assertThat((Boolean)info.get("licensed"), is(false));
      assertThat((Long)info.get("sampleHistorySize"), is(0L));
      assertThat((Long)info.get("sampleIntervalSeconds"), is(0L));
      assertThat((Boolean)info.get("enabled"), is(true));
      assertThat((String) info.get("restAPIVersion"), is("v1"));
    }
  }


}
