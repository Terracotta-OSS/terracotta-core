package com.terracotta.management.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

/**
 * AgentsTest
 */
public class AgentsTest extends AbstractTsaAgentTestBase {
  private static final int MEMBER_COUNT = 2;

  public AgentsTest(TestConfig testConfig) {
    super(testConfig);

    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[]{AgentsTestClient.class});
  }

  public static class AgentsTestClient extends AbstractTsaClient {

    public AgentsTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      JSONArray agentsArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getTsaGroupPort(0),
          "/tc-management-api/agents");
      assertThat(agentsArray.size(), is(1));

      JSONObject content = (JSONObject)agentsArray.get(0);
      JSONObject rootRepresentables = (JSONObject)content.get("rootRepresentables");
      String urls = (String)rootRepresentables.get("urls");
      String[] urlArray = urls.split(",");
      assertThat(urlArray.length, is(MEMBER_COUNT));

      JSONArray agentsInfoArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getTsaGroupPort(0),
          "/tc-management-api/agents/info");
      assertThat(agentsInfoArray.size(), is(1));

      JSONObject info = (JSONObject)agentsInfoArray.get(0);
      assertThat((Boolean)info.get("available"), is(true));
      assertThat(info.containsKey("secured"), is(true));
      assertThat(info.containsKey("sslEnabled"), is(true));
      assertThat(info.containsKey("needClientAuth"), is(true));
      assertThat(info.containsKey("licensed"), is(true));
      assertThat(info.containsKey("sampleHistorySize"), is(true));
      assertThat(info.containsKey("sampleIntervalSeconds"), is(true));
      assertThat(info.containsKey("enabled"), is(true));
      assertThat(info.containsKey("restAPIVersion"), is(true));
    }
  }


}
