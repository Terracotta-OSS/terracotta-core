package com.terracotta.management.test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import net.sf.ehcache.CacheManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class TopologyTest extends AbstractTsaAgentTestBase {
  private static final int GROUP_COUNT = 1; // cannot have Active-Active with Open Source
  private static final int MEMBER_COUNT = 2;

  public TopologyTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.setNumOfGroups(GROUP_COUNT);
    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[] { TopologyTestClient.class, TopologyServerTestClient.class, TopologyClientTestClient.class });
  }

  public static abstract class AbstractTopologyTestClient extends AbstractTsaClient {

    public AbstractTopologyTestClient(String[] args) {
      super(args);
    }

    protected void parseAndAssertServerGroupEntities(JSONObject o0, boolean[] serversDown) {
      JSONArray serverGroupEntities = (JSONArray)o0.get("serverGroupEntities");
      assertThat(serverGroupEntities.size(), is(1));
      JSONObject o1 = (JSONObject)serverGroupEntities.get(0);

      JSONArray servers = (JSONArray)o1.get("servers");
      assertEquals("testGroup0", o1.get("name"));
      assertEquals(2, servers.size());

      JSONObject attributes0 = (JSONObject)((JSONObject)servers.get(0)).get("attributes");
      if (attributes0.get("Name").equals("testserver0")) {
        if (serversDown[0]) {
          assertNull(attributes0.get("TSAGroupPort"));
          assertNull(attributes0.get("State"));
        } else {
          assertEquals((long)getGroupData(0).getTsaGroupPort(0), attributes0.get("TSAGroupPort"));
          assertThat((String)attributes0.get("State"), anyOf(is("ACTIVE-COORDINATOR"), is("PASSIVE-STANDBY")));
        }
      } else {
        assertEquals("testserver1", attributes0.get("Name"));
        if (serversDown[1]) {
          assertNull(attributes0.get("TSAGroupPort"));
          assertNull(attributes0.get("State"));
        } else {
          assertEquals((long) getGroupData(0).getTsaGroupPort(1), attributes0.get("TSAGroupPort"));
          assertThat((String)attributes0.get("State"), anyOf(is("ACTIVE-COORDINATOR"), is("PASSIVE-STANDBY")));
        }
      }

      JSONObject attributes1 = (JSONObject)((JSONObject)servers.get(1)).get("attributes");
      if (attributes1.get("Name").equals("testserver1")) {
        if (serversDown[1]) {
          assertNull(attributes1.get("TSAGroupPort"));
          assertNull(attributes1.get("State"));
        } else {
          assertEquals((long) getGroupData(0).getTsaGroupPort(1), attributes1.get("TSAGroupPort"));
          assertThat((String)attributes1.get("State"), anyOf(is("ACTIVE-COORDINATOR"), is("PASSIVE-STANDBY")));
        }
      } else {
        assertEquals("testserver0", attributes1.get("Name"));
        if (serversDown[0]) {
          assertNull(attributes1.get("TSAGroupPort"));
          assertNull(attributes1.get("State"));
        } else {
          assertEquals((long) getGroupData(0).getTsaGroupPort(0), attributes1.get("TSAGroupPort"));
          assertThat((String)attributes1.get("State"), anyOf(is("ACTIVE-COORDINATOR"), is("PASSIVE-STANDBY")));
        }
      }
    }

    protected void parseAndAssertClientEntities(JSONObject o0) throws UnknownHostException {
      JSONArray clientEntitiesArray = (JSONArray)o0.get("clientEntities");

      for (Object aClientEntitiesArray : clientEntitiesArray) {
        JSONObject clientEntity = (JSONObject)aClientEntitiesArray;

        JSONObject attributes = (JSONObject)clientEntity.get("attributes");
        assertThat((String) attributes.get("RemoteAddress"),
                   anyOf(containsString("localhost"), containsString("127.0.0.1"), containsString(InetAddress
                       .getLocalHost().getHostName())));
        assertThat(attributes.get("ClientID"), notNullValue());
      }
    }

    protected void waitUntilAllServerAgentsUp() {
      waitUntilServerAgentUp(getGroupData(0).getTsaGroupPort(0));
      waitUntilServerAgentUp(getGroupData(0).getTsaGroupPort(1));
    }

    protected void testResources(int group, int member, String resourcePath) throws IOException {
      testResources(group, member, resourcePath, new boolean[] {false, false});
    }

    protected void testResources(int group, int member, String resourcePath, boolean[] failures) throws IOException {
      int port = getGroupData(group).getTsaGroupPort(member);

      JSONArray content = getTsaJSONArrayContent(ConfigHelper.HOST, port, resourcePath);

      assertThat(content.size(), is(1));
      JSONObject o0 = (JSONObject)content.get(0);
      parseAndAssertServerGroupEntities(o0, failures);

      parseAndAssertClientEntities(o0);

      JSONObject unreadOperatorEventCount = (JSONObject)o0.get("unreadOperatorEventCount");
      assertThat(unreadOperatorEventCount.size(), is(5));

      assertThat((String)o0.get("version"), is(guessVersion()));
    }

  }

  public static class TopologyTestClient extends AbstractTopologyTestClient {
    @Override
    protected void doTsaTest() throws Throwable {
      String resourcePath = "/tc-management-api/agents/topologies";

      // test REST on both active and passive
      testResources(0, 0, resourcePath);
      testResources(0, 1, resourcePath);

      // crash the active -> make sure it's broken and that the passive is not impacted
      getTestControlMbean().crashActiveServer(0);
      try {
        testResources(0, 0, resourcePath);
        fail("expected IOException");
      } catch (IOException e) {
        // expected
      }
      testResources(0, 1, resourcePath, new boolean[] {true, false});

      // restart crashed server -> make sure everything is back in working order
      getTestControlMbean().restartLastCrashedServer(0);
      waitUntilAllServerAgentsUp();
      testResources(0, 0, resourcePath);
      testResources(0, 1, resourcePath);
    }

    public TopologyTestClient(String[] args) {
      super(args);
    }
  }

  public static class TopologyServerTestClient extends AbstractTopologyTestClient {
    @Override
    protected void doTsaTest() throws Throwable {
      String resourcePath = "/tc-management-api/agents/topologies/servers";

      // test REST on both active and passive
      testResources(0, 0, resourcePath);
      testResources(0, 1, resourcePath);

      // crash the active -> make sure it's broken and that the passive is not impacted
      getTestControlMbean().crashActiveServer(0);
      try {
        testResources(0, 1, resourcePath);
        fail("expected IOException");
      } catch (IOException e) {
        // expected
      }
      testResources(0, 0, resourcePath, new boolean[] {false, true});

      // restart crashed server -> make sure everything is back in working order
      getTestControlMbean().restartLastCrashedServer(0);
      waitUntilAllServerAgentsUp();
      testResources(0, 0, resourcePath);
      testResources(0, 1, resourcePath);
    }

    public TopologyServerTestClient(String[] args) {
      super(args);
    }
  }

  public static class TopologyClientTestClient extends AbstractTopologyTestClient {

    @Override
    protected void doTsaTest() throws Throwable {
      CacheManager cacheManager = createCacheManager(ConfigHelper.HOST, Integer.toString(getGroupData(0).getTsaGroupPort(0)));

      for (int i = 0; i < MEMBER_COUNT; i++) {
        int port = getGroupData(0).getTsaGroupPort(i);
        String host = ConfigHelper.HOST;

        JSONArray content = getTsaJSONArrayContent(host, port, "/tc-management-api/agents/topologies/clients");

        assertThat(content.size(), is(1));
        JSONObject o0 = (JSONObject)content.get(0);

        parseAndAssertClientEntities(o0);

        JSONArray serverGroupEntities = (JSONArray)o0.get("serverGroupEntities");
        assertThat(serverGroupEntities.size(), is(0));

        JSONObject unreadOperatorEventCount = (JSONObject) o0.get("unreadOperatorEventCount");
        assertThat(unreadOperatorEventCount.size(), is(0));
      }

      cacheManager.shutdown();
    }

    public TopologyClientTestClient(String[] args) {
      super(args);
    }
  }
}
