package com.terracotta.management.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.cluster.ClusterScheme;

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

    testConfig.getClientConfig().setClientClasses(new Class[]{ConfigurationTestClient.class, ConfigurationClientTestClient.class, ConfigurationServerTestClient.class});
  }

  public abstract static class AbstractConfigurationTestClient extends AbstractTsaClient {

    public AbstractConfigurationTestClient(String[] args) {
      super(args);
    }

    protected void checkClientConfiguration(JSONObject content) {
      checkCommonConfiguration(content);
    }

    protected void checkServerConfiguration(JSONObject content) {
      checkCommonConfiguration(content);
    }

    private void checkCommonConfiguration(JSONObject content) {

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
    }
  }

  public static class ConfigurationTestClient extends AbstractConfigurationTestClient {

    public ConfigurationTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      int groupIndex = 0;

      CacheManager cacheManager = createCacheManager(ConfigHelper.HOST, Integer.toString(getGroupData(groupIndex).getTsaPort(0)));

      for (int serverIndex = 0; serverIndex < MEMBER_COUNT; serverIndex++) {

        int tsaGroupPort = getGroupData(groupIndex).getTsaGroupPort(serverIndex);
        JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, tsaGroupPort, "/tc-management-api/agents/configurations");

        for (int i = 0; i< MEMBER_COUNT; i++) {
          JSONObject content = (JSONObject)contentArray.get(i);

          checkServerConfiguration(content);
        }

        for (int i = MEMBER_COUNT; i < contentArray.size(); i++) {
          checkClientConfiguration((JSONObject)contentArray.get(i));
        }
      }

      cacheManager.shutdown();
    }

  }

  public static class ConfigurationClientTestClient extends AbstractConfigurationTestClient {

    public ConfigurationClientTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      int tsaPort = getGroupData(0).getTsaPort(0);
      CacheManager cacheManager = createCacheManager(ConfigHelper.HOST, Integer.toString(tsaPort));
      String nodeId = cacheManager.getCluster(ClusterScheme.TERRACOTTA).getCurrentNode().getId();
      String id = nodeId.substring(nodeId.length() - 2, nodeId.length() - 1);

      JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, tsaPort,
          "/tc-management-api/agents/configurations/clients;ids=" + id);

      assertThat(contentArray.size(), is(1));
      checkClientConfigurationWithName(id, (JSONObject)contentArray.get(0));

      cacheManager.shutdown();
    }

    private void checkClientConfigurationWithName(String id, JSONObject content) {
      assertThat((String)content.get("sourceId"), equalTo(id));
      checkClientConfiguration(content);
    }
  }

  public static class ConfigurationServerTestClient extends AbstractConfigurationTestClient {

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

      checkServerConfigurationWithName(groupIndex, serverIndex, content);
    }

    private void checkServerConfigurationWithName(int groupIndex, int serverIndex, JSONObject content) {
      String sourceId = (String)content.get("sourceId");
      assertThat(sourceId.contains(getGroupData(groupIndex).getServerNames()[serverIndex]), is(true));

      checkServerConfiguration(content);
    }


  }


}
