package com.terracotta.management.test;

import net.sf.ehcache.CacheManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * ConfigurationTest
 */
public class ConfigurationTest extends AbstractTsaAgentTestBase {
  private static final int GROUP_COUNT = 1; // cannot have Active-Active with Open Source
  private static final int MEMBER_COUNT = 2;

  public ConfigurationTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.setNumOfGroups(GROUP_COUNT);
    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[]{ConfigurationServerTestClient.class, ConfigurationClientTestClient.class});
  }


  public static class ConfigurationServerTestClient extends AbstractTsaClient {

    public ConfigurationServerTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      testResources(0, 0);
      testResources(0, 1);

      // crash the active -> make sure it's broken and that the passive is not impacted
      getTestControlMbean().crashActiveServer(0);
      try {
        testResources(0, 0);
        fail("expected IOException");
      } catch (IOException e) {
        // expected
      }
      testResources(0, 1, 1);

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
      testResources(group, member, 0);
    }

    private void testResources(int group, int member, int expectedDownCount) throws IOException {
      int managementPort = getGroupData(group).getManagementPort(member);

      JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, managementPort, "/tc-management-api/agents/configurations/servers;names=" + getGroupData(group).getServerNames()[member]);
      assertThat(contentArray.size(), is(1));
      JSONObject content = (JSONObject)contentArray.get(0);
      checkServerConfigurationWithName(group, content);

      contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, managementPort, "/tc-management-api/agents/configurations/servers");
      assertThat(contentArray.size(), is(MEMBER_COUNT * GROUP_COUNT));
      content = (JSONObject)contentArray.get(0);
      int downCount = 0;
      if (checkServerConfigurationWithName(0, content)) { downCount++; }
      content = (JSONObject)contentArray.get(1);
      if (checkServerConfigurationWithName(0, content)) { downCount++; }

      assertThat(downCount, is(expectedDownCount));
    }

    /**
     * @return true if that server was reported as down by the agent
     */
    private boolean checkServerConfigurationWithName(int groupIndex, JSONObject content) {
      String sourceId = (String)content.get("sourceId");
      List<String> names = Arrays.asList(getGroupData(groupIndex).getServerNames());
      assertThat(names.contains(sourceId), is(true));

      JSONObject attributes = (JSONObject)content.get("attributes");
      assertThat(attributes, is(notNullValue()));

      assertThat((String)content.get("version"), is(guessVersion()));

      String error = (String)attributes.get("Error");
      if (error != null) {
        return true;
      } else {
        String environment = (String)attributes.get("environment");
        assertThat(environment, containsString("user.home"));

        JSONArray arguments = (JSONArray)attributes.get("processArguments");
        assertThat(arguments.size(), is(not(0)));

        String config = (String)attributes.get("config");
        System.out.println(config);
        assertThat(config, containsString("group-name=\"testGroup0\">"));

        String tcProperties = (String)attributes.get("tcProperties");
        assertThat(tcProperties, containsString("tc.config.total.timeout"));
        return false;
      }
    }

  }

  public static class ConfigurationClientTestClient extends AbstractTsaClient {

    public ConfigurationClientTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      CacheManager cacheManager = createCacheManager(ConfigHelper.HOST, Integer.toString(getGroupData(0).getTsaGroupPort(0)));

      for (int group = 0; group < GROUP_COUNT; group++) {
        for (int member = 0; member < MEMBER_COUNT; member++) {
          int managementPort = getGroupData(group).getManagementPort(member);
          JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, managementPort, "/tc-management-api/agents/configurations/clients");
          assertThat(contentArray.size(), is(1));

          JSONObject obj = (JSONObject)contentArray.get(0);
          assertThat((String)obj.get("version"), is(guessVersion()));
          assertThat(obj.get("sourceId"), is(notNullValue()));
          assertThat(((JSONObject)obj.get("attributes")).size(), is(4));

          contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, managementPort, "/tc-management-api/agents/configurations");
          assertThat(contentArray.size(), is(3));
        }
      }

      cacheManager.shutdown();
    }

  }


}
