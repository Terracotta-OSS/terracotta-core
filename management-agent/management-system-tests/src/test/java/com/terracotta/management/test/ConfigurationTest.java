package com.terracotta.management.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

/**
 * ConfigurationTest
 */
public class ConfigurationTest extends AbstractTsaAgentTestBase {

  private static final int MEMBER_COUNT = 2;

  public ConfigurationTest(TestConfig testConfig) {
    super(testConfig);

    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[]{ConfigurationTestClient.class, ConfigurationServerTestClient.class});
  }

  public static class ConfigurationTestClient extends AbstractTsaClient {

    public ConfigurationTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      int groupIndex = 0;

      for (int serverIndex = 0; serverIndex < MEMBER_COUNT; serverIndex++) {

        int tsaGroupPort = getGroupData(groupIndex).getTsaGroupPort(serverIndex);
        JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, tsaGroupPort, "/tc-management-api/agents/configurations");

        assertThat(contentArray.size(), is(MEMBER_COUNT));

        JSONObject content = (JSONObject)contentArray.get(serverIndex);

        checkServerConfiguration(groupIndex, serverIndex, content);
      }
    }

    protected void checkServerConfiguration(int groupIndex, int serverIndex, JSONObject content) {
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
      assertThat(config, containsString("<mirror-group group-name=\"testGroup" + groupIndex + "\">"));

      String tcProperties = (String)attributes.get("tcProperties");
      assertThat(tcProperties, containsString("tc.config.total.timeout"));
    }
  }

  public static class ConfigurationServerTestClient extends ConfigurationTestClient {

    public ConfigurationServerTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      int groupIndex = 0;
      int serverIndex = 0;

      int tsaGroupPort = getGroupData(groupIndex).getTsaGroupPort(serverIndex);
      JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, tsaGroupPort,
          "/tc-management-api/agents/configurations/servers;names=" + getGroupData(groupIndex).getServerNames()[serverIndex]);

      assertThat(contentArray.size(), is(1));
      JSONObject content = (JSONObject)contentArray.get(0);

      checkServerConfiguration(groupIndex, serverIndex, content);
    }
  }


}
