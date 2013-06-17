package com.terracotta.management.test;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

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
      for (int group = 0; group < GROUP_COUNT; group++) {
        for (int member = 0; member < MEMBER_COUNT; member++) {
          int tsaGroupPort = getGroupData(group).getTsaGroupPort(member);

          JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, tsaGroupPort, "/tc-management-api/agents/configurations/servers;names=" + getGroupData(group).getServerNames()[member]);
          assertThat(contentArray.size(), is(1));
          JSONObject content = (JSONObject)contentArray.get(0);
          checkServerConfigurationWithName(group, member, content);

          contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, tsaGroupPort, "/tc-management-api/agents/configurations/servers");
          assertThat(contentArray.size(), is(MEMBER_COUNT * GROUP_COUNT));
          content = (JSONObject)contentArray.get(0);
          checkServerConfigurationWithName(0, 0, content);
          content = (JSONObject)contentArray.get(1);
          checkServerConfigurationWithName(0, 1, content);
        }
      }
    }

    private void checkServerConfigurationWithName(int groupIndex, int serverIndex, JSONObject content) {
      String sourceId = (String)content.get("sourceId");
      assertThat(sourceId.contains(getGroupData(groupIndex).getServerNames()[serverIndex]), is(true));

      JSONObject attributes = (JSONObject)content.get("attributes");
      assertThat(attributes, is(notNullValue()));

      String environment = (String)attributes.get("environment");
      assertThat(environment, containsString("user.home"));

      JSONArray arguments = (JSONArray)attributes.get("processArguments");
      assertThat(arguments.size(), is(not(0)));

      String config = (String)attributes.get("config");
      System.out.println(config);
      assertThat(config, containsString("group-name=\"testGroup0\">"));

      String tcProperties = (String)attributes.get("tcProperties");
      assertThat(tcProperties, containsString("tc.config.total.timeout"));

      assertThat((String)content.get("version"), is(guessVersion()));
    }

  }

  public static class ConfigurationClientTestClient extends AbstractTsaClient {

    public ConfigurationClientTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      for (int group = 0; group < GROUP_COUNT; group++) {
        for (int member = 0; member < MEMBER_COUNT; member++) {
          int tsaGroupPort = getGroupData(group).getTsaGroupPort(member);
          JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, tsaGroupPort, "/tc-management-api/agents/configurations/clients");
          assertThat(contentArray.size(), is(0));
        }
      }
    }

  }


}
