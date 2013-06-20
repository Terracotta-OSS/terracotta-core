package com.terracotta.management.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * LogsTest
 */
public class LogsTest extends AbstractTsaAgentTestBase {
  private static final int GROUP_COUNT = 1; // cannot have Active-Active with Open Source
  private static final int MEMBER_COUNT = 2;

  public LogsTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.setNumOfGroups(GROUP_COUNT);
    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[]{LogsTestClient.class});
  }

  public static class LogsTestClient extends AbstractTsaClient {

    public LogsTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      long testStart = System.currentTimeMillis();

      createLogEvents();

      JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getTsaGroupPort(0),
          "/tc-management-api/agents/logs?sinceWhen=" + testStart);

      assertThat(contentArray.size(), is(not(0)));

      Map<String, Integer> serverNameToIndex = createServeNameToIndexMap();

      for (Object elem :contentArray) {
        JSONObject content = (JSONObject)elem;
        serverNameToIndex.remove(content.get("sourceId"));
      }

      assertThat("Expected to find a log statement for each server", serverNameToIndex.size(), is(0));

      for (int serverIndex = 0; serverIndex < MEMBER_COUNT; serverIndex++) {
        checkServerFilteredLogs(serverIndex, testStart);
      }
    }

    private void checkServerFilteredLogs(int serverIndex, long testStart) throws IOException {
      String serverName = getGroupData(0).getServerNames()[serverIndex];
      JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getTsaGroupPort(serverIndex),
          "/tc-management-api/agents/logs;names=" + serverName + "?sinceWhen=" + testStart);

      assertThat(contentArray.size(), is(not(0)));

      for (Object elem : contentArray) {
        JSONObject content = (JSONObject)elem;
        String sourceId = (String)content.get("sourceId");
        assertThat(sourceId, equalTo(serverName));
      }
    }

    private Map<String, Integer> createServeNameToIndexMap() {
      Map<String, Integer> map = new HashMap<String, Integer>(MEMBER_COUNT);
      for (int i = 0; i < MEMBER_COUNT; i++) {
        map.put(getGroupData(0).getServerNames()[i], i);
      }
      return map;
    }

    private void createLogEvents() throws IOException {
      for (int serverIndex = 0; serverIndex < MEMBER_COUNT; serverIndex++) {
        httpPost("http://" + ConfigHelper.HOST + ":" + getGroupData(0).getTsaGroupPort(serverIndex) +
                 "/tc-management-api/agents/diagnostics/dumpClusterState");
      }
    }
  }
}
