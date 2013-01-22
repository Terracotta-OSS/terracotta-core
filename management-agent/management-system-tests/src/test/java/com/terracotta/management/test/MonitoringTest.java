package com.terracotta.management.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

import java.util.Arrays;

/**
 * MonitoringTest
 */
public class MonitoringTest extends AbstractTsaAgentTestBase {
  private static final int MEMBER_COUNT = 2;

  public MonitoringTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[]{MonitoringServersClientTest.class});
  }

  public static class MonitoringServersClientTest extends AbstractTsaClient {

    public MonitoringServersClientTest(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      for (int serverIndex = 0; serverIndex < MEMBER_COUNT; serverIndex++) {
        JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getTsaGroupPort(serverIndex),
            "/tc-management-api/agents/statistics/servers");

        assertThat(contentArray.size(), is(MEMBER_COUNT));

        for (int i = 0; i < MEMBER_COUNT; i++) {
          checkStatisticContent((JSONObject)contentArray.get(i));
        }

        String serverName = getGroupData(0).getServerNames()[serverIndex];
        JSONArray singleServerStats = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getTsaGroupPort(serverIndex),
            "/tc-management-api/agents/statistics/servers;names=" + serverName);
        assertThat(singleServerStats.size(), is(1));

        JSONObject statistics = (JSONObject)singleServerStats.get(0);
        assertThat((String)statistics.get("sourceId"), equalTo(serverName));
        checkStatisticContent(statistics);
      }

    }

    private void checkStatisticContent(JSONObject content) {
      String sourceId = (String)content.get("sourceId");
      assertThat(Arrays.asList(getGroupData(0).getServerNames()).contains(sourceId), is(true));

      JSONObject statistics = (JSONObject)content.get("statistics");
      assertThat(statistics.isEmpty(), is(false));
    }
  }
}
