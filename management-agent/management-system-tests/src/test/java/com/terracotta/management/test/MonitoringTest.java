package com.terracotta.management.test;

import net.sf.ehcache.CacheManager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

import java.util.Arrays;

/**
 * MonitoringTest
 */
public class MonitoringTest extends AbstractTsaAgentTestBase {
  private static final int GROUP_COUNT = 1; // cannot have Active-Active with Open Source
  private static final int MEMBER_COUNT = 2;

  public MonitoringTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.setNumOfGroups(GROUP_COUNT);
    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[]{MonitoringServersClientTest.class});
  }

  public static class MonitoringServersClientTest extends AbstractTsaClient {

    public MonitoringServersClientTest(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      CacheManager cacheManager = createCacheManager(ConfigHelper.HOST, Integer.toString(getGroupData(0).getTsaGroupPort(0)));

      for (int serverIndex = 0; serverIndex < MEMBER_COUNT; serverIndex++) {
        JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getManagementPort(serverIndex),
            "/tc-management-api/agents/statistics/servers");

        assertThat(contentArray.size(), is(MEMBER_COUNT));

        for (int i = 0; i < MEMBER_COUNT; i++) {
          checkServerStatisticContent((JSONObject)contentArray.get(i));
        }

        String serverName = getGroupData(0).getServerNames()[serverIndex];
        JSONArray singleServerStats = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getManagementPort(serverIndex),
            "/tc-management-api/agents/statistics/servers;names=" + serverName);
        assertThat(singleServerStats.size(), is(1));

        JSONObject statistics = (JSONObject)singleServerStats.get(0);
        assertThat((String)statistics.get("sourceId"), equalTo(serverName));
        checkServerStatisticContent(statistics);

        contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getManagementPort(serverIndex),
            "/tc-management-api/agents/statistics/clients");

        assertThat(contentArray.size(), is(1));
        checkClientStatisticContent((JSONObject)contentArray.get(0));
      }

      cacheManager.shutdown();
    }

    private void checkServerStatisticContent(JSONObject content) {
      String sourceId = (String)content.get("sourceId");
      assertThat(Arrays.asList(getGroupData(0).getServerNames()).contains(sourceId), is(true));

      JSONObject statistics = (JSONObject)content.get("statistics");
      assertThat(statistics.isEmpty(), is(false));
    }

    private void checkClientStatisticContent(JSONObject content) {
      assertThat(content.get("sourceId"), is(notNullValue()));

      JSONObject statistics = (JSONObject)content.get("statistics");
      assertThat(statistics.isEmpty(), is(false));
    }
  }
}
