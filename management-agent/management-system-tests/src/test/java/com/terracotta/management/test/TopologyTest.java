package com.terracotta.management.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import net.sf.ehcache.CacheManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

public class TopologyTest extends AbstractTsaAgentTestBase {

  private static final int MEMBER_COUNT = 2;

  public TopologyTest(TestConfig testConfig) {
    super(testConfig);

    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[] { TopologyTestClient.class, TopologyServerTestClient.class, TopologyClientTestClient.class});
  }

  public static abstract class AbstractTopologyTestClient extends AbstractTsaClient {

    public AbstractTopologyTestClient(String[] args) {
      super(args);
    }

    protected void parseAndAssertServerGroupEntities(JSONObject o0) {
      JSONArray serverGroupEntities = (JSONArray)o0.get("serverGroupEntities");
      assertThat(serverGroupEntities.size(), is(1));
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

    protected void parseAndAssertClientEntities(JSONObject o0) {
      JSONArray clientEntitiesArray = (JSONArray)o0.get("clientEntities");

      for (int i = 0; i < clientEntitiesArray.size(); i++) {
        JSONObject clientEntity = (JSONObject)clientEntitiesArray.get(i);

        JSONObject attributes = (JSONObject)clientEntity.get("attributes");
        assertThat((String) attributes.get("RemoteAddress"), containsString("localhost"));
        assertThat(attributes.get("ClientID"), notNullValue());
      }

    }
  }

  public static class TopologyTestClient extends AbstractTopologyTestClient {
    @Override
    protected void doTsaTest() throws Throwable {

      String host = ConfigHelper.HOST;

      CacheManager cacheManager = createCacheManager(host, Integer.toString(getGroupData(0).getTsaPort(0)));

      for (int i = 0; i < MEMBER_COUNT; i++) {
        int port = getGroupData(0).getTsaGroupPort(i);

        JSONArray content = getTsaJSONArrayContent(host, port, "/tc-management-api/agents/topologies");

        assertThat(content.size(), is(1));
        JSONObject o0 = (JSONObject)content.get(0);
        parseAndAssertServerGroupEntities(o0);

        parseAndAssertClientEntities(o0);

        JSONObject unreadOperatorEventCount = (JSONObject)o0.get("unreadOperatorEventCount");
        assertThat(unreadOperatorEventCount.size(), is(5));
      }

      cacheManager.shutdown();
    }

    public TopologyTestClient(String[] args) {
      super(args);
    }
  }

  public static class TopologyServerTestClient extends AbstractTopologyTestClient {

    @Override
    protected void doTsaTest() throws Throwable {
      for (int i = 0; i < MEMBER_COUNT; i++) {
        int port = getGroupData(0).getTsaGroupPort(i);
        String host = ConfigHelper.HOST;

        JSONArray content = getTsaJSONArrayContent(host, port, "/tc-management-api/agents/topologies/servers");

        assertThat(content.size(), is(1));
        JSONObject o0 = (JSONObject)content.get(0);
        parseAndAssertServerGroupEntities(o0);

        JSONArray clientEntities = (JSONArray)o0.get("clientEntities");
        assertThat(clientEntities.size(), is(0));

        assertThat(o0.get("unreadOperatorEventCount"), nullValue());
      }
    }

    public TopologyServerTestClient(String[] args) {
      super(args);
    }
  }


  public static class TopologyClientTestClient extends AbstractTopologyTestClient {

    @Override
    protected void doTsaTest() throws Throwable {

      CacheManager cacheManager = createCacheManager(ConfigHelper.HOST, Integer.toString(getGroupData(0).getTsaPort(0)));

      for (int i = 0; i < MEMBER_COUNT; i++) {
        int port = getGroupData(0).getTsaGroupPort(i);
        String host = ConfigHelper.HOST;

        JSONArray content = getTsaJSONArrayContent(host, port, "/tc-management-api/agents/topologies/clients");

        assertThat(content.size(), is(1));
        JSONObject o0 = (JSONObject)content.get(0);

        parseAndAssertClientEntities(o0);

        JSONArray serverGroupEntities = (JSONArray)o0.get("serverGroupEntities");
        assertThat(serverGroupEntities.size(), is(0));

        assertThat(o0.get("unreadOperatorEventCount"), nullValue());

      }

      cacheManager.shutdown();
    }

    public TopologyClientTestClient(String[] args) {
      super(args);
    }
  }

}
