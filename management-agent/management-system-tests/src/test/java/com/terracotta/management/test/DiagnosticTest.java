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

/**
 * DiagnosticTest
 */
public class DiagnosticTest extends AbstractTsaAgentTestBase {

  private static final int MEMBER_COUNT = 1;

  public DiagnosticTest(TestConfig testConfig) {
    super(testConfig);

    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[]{DiagnosticServerTestClient.class});
  }

  public static class DiagnosticServerTestClient extends AbstractTsaClient {

    public DiagnosticServerTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      testServerThreadDump();
    }

    private void testServerThreadDump() throws IOException {
      String serverName = getGroupData(0).getServerNames()[0];
      JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getTsaGroupPort(0),
          "/tc-management-api/agents/diagnostics/threadDump/servers;names=" + serverName);

      assertThat(contentArray.size(), is(1));

      JSONObject content = (JSONObject)contentArray.get(0);

      String source = (String)content.get("sourceId");
      assertThat(source, equalTo(serverName));

      String dump = (String)content.get("dump");
      assertThat(dump.trim().length(), is(not(0)));

    }
  }

}
