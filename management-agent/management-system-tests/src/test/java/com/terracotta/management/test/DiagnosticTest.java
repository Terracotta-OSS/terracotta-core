package com.terracotta.management.test;

import net.sf.ehcache.CacheManager;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.terracotta.management.resource.AgentEntity;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * DiagnosticTest
 */
public class DiagnosticTest extends AbstractTsaAgentTestBase {
  private static final int GROUP_COUNT = 1; // cannot have Active-Active with Open Source
  private static final int MEMBER_COUNT = 2;

  public DiagnosticTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.setNumOfGroups(GROUP_COUNT);
    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[] { DiagnosticThreadDumpTestClient.class, DiagnosticDGCTestClient.class, DiagnosticClusterStateDumpTestClient.class }); //DiagnosticThreadDumpTestClient.class, DiagnosticDGCTestClient.class, DiagnosticClusterStateDumpTestClient.class
  }

  public static class DiagnosticThreadDumpTestClient extends AbstractTsaClient {

    public DiagnosticThreadDumpTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      CacheManager cacheManager = createCacheManager(ConfigHelper.HOST, Integer.toString(getGroupData(0).getTsaGroupPort(0)));

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
      testResources(0, 1, new boolean[] {true, false});

      // restart crashed server -> make sure everything is back in working order
      getTestControlMbean().restartLastCrashedServer(0);
      waitUntilAllServerAgentsUp();
      testResources(0, 0);
      testResources(0, 1);

      cacheManager.shutdown();
    }

    private void testResources(int group, int member) throws IOException {
      testResources(group, member, new boolean[] {false, false});
    }

    private void testResources(int group, int member, boolean[] failures) throws IOException {
      testGroupThreadDump(group, member, failures);
      testZippedGroupThreadDump(group, member);
      testZippedServersThreadDump(group, member);
      if (!failures[0]) {
        testZippedClientsThreadDump(group, member);
      }
      testSingleServerThreadDump(group, member);
    }

    private void waitUntilAllServerAgentsUp() {
      waitUntilServerAgentUp(getGroupData(0).getManagementPort(0));
      waitUntilServerAgentUp(getGroupData(0).getManagementPort(1));
    }

    private void testGroupThreadDump(int group, int member, boolean[] failures) throws IOException {
      JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(group).getManagementPort(member),
          "/tc-management-api/agents/diagnostics/threadDump");
      assertThat(contentArray.size(), is(MEMBER_COUNT + (failures[0] ? 0 : 1)));

      JSONObject content1 = getWithSourceId(contentArray, "testserver0");
      assertThat(content1, is(notNullValue()));
      if (failures[0]) {
        assertThat((String)content1.get("dump"), is("Unavailable"));
      } else {
        assertThat((String)content1.get("dump"), is(not("Unavailable")));
      }
      assertThat((String)content1.get("version"), is(guessVersion()));
      assertThat((String)content1.get("nodeType"), is("SERVER"));
      assertThat((String)content1.get("agentId"), is(AgentEntity.EMBEDDED_AGENT_ID));


      JSONObject content2 = getWithSourceId(contentArray, "testserver1");
      assertThat(content2, is(notNullValue()));
      if (failures[1]) {
        assertThat((String)content2.get("dump"), is("Unavailable"));
      } else {
        assertThat((String)content2.get("dump"), is(not("Unavailable")));
      }
      assertThat((String)content2.get("version"), is(guessVersion()));
      assertThat((String)content2.get("nodeType"), is("SERVER"));
      assertThat((String)content2.get("agentId"), is(AgentEntity.EMBEDDED_AGENT_ID));
    }

    private JSONObject getWithSourceId(JSONArray contentArray, String expectedSourceId) {
      for (Object o : contentArray) {
        JSONObject content = (JSONObject)o;
        String sourceId = (String)content.get("sourceId");

        if (sourceId.equals(expectedSourceId)) {
          return content;
        }
      }
      return null;
    }

    private void testZippedServersThreadDump(int group, int member) throws IOException {
      Map<String, String> headers = new HashMap<String, String>() {{
        put("Accept", "application/zip");
      }};
      byte[] bytes = getTsaRawContent(ConfigHelper.HOST, getGroupData(group).getManagementPort(member),
          "/tc-management-api/agents/diagnostics/threadDumpArchive/servers", headers);

      ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes));

      while (true) {
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        if (zipEntry == null) break;

        // if we can unzip the bytes, we assume that the thread dump is good
        IOUtils.toString(zipInputStream, "UTF-8");

        zipInputStream.closeEntry();
      }

      zipInputStream.close();
    }

    private void testZippedClientsThreadDump(int group, int member) throws IOException {
      Map<String, String> headers = new HashMap<String, String>() {{
        put("Accept", "application/zip");
      }};
      byte[] bytes = getTsaRawContent(ConfigHelper.HOST, getGroupData(group).getManagementPort(member),
          "/tc-management-api/agents/diagnostics/threadDumpArchive/clients", headers);

      ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes));

      while (true) {
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        if (zipEntry == null) break;

        // if we can unzip the bytes, we assume that the thread dump is good
        IOUtils.toString(zipInputStream, "UTF-8");

        zipInputStream.closeEntry();
      }

      zipInputStream.close();
    }

    private void testZippedGroupThreadDump(int group, int member) throws IOException {
      Map<String, String> headers = new HashMap<String, String>() {{
        put("Accept", "application/zip");
      }};
      byte[] bytes = getTsaRawContent(ConfigHelper.HOST, getGroupData(group).getManagementPort(member),
          "/tc-management-api/agents/diagnostics/threadDumpArchive", headers);

      ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes));

      while (true) {
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        if (zipEntry == null) break;

        // if we can unzip the bytes, we assume that the thread dump is good
        IOUtils.toString(zipInputStream, "UTF-8");

        zipInputStream.closeEntry();
      }

      zipInputStream.close();
    }

    private void testSingleServerThreadDump(int group, int member) throws IOException {
      String serverName = getGroupData(group).getServerNames()[member];
      JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(group).getManagementPort(member),
          "/tc-management-api/agents/diagnostics/threadDump/servers;names=" + serverName);

      assertThreadDumpEntity(serverName, contentArray);
    }

    private void assertThreadDumpEntity(String sourceId, JSONArray contentArray) {
      assertThat(contentArray.size(), is(1));

      JSONObject content = (JSONObject)contentArray.get(0);

      String source = (String)content.get("sourceId");
      assertThat(source, equalTo(sourceId));

      String dump = (String)content.get("dump");
      assertThat(dump.trim().length(), is(not(0)));
    }
  }

  public static class DiagnosticDGCTestClient extends AbstractTsaClient {

    public DiagnosticDGCTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      assertThat(serverContainsAnyOfThoseLogs(0, 0, "DGC finished", "Running DGC"), is(false));

      httpPost("http://" + ConfigHelper.HOST + ":" + getGroupData(0).getManagementPort(0) + "/tc-management-api/agents/diagnostics/dgc");

      assertTrueWithin(5000L, new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return serverContainsAllOfThoseLogs(0, 0, "Running DGC", "DGC finished");
        }
      });
    }

  }

  public static class DiagnosticClusterStateDumpTestClient extends AbstractTsaClient {

    public DiagnosticClusterStateDumpTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      assertThat(serverContainsAnyOfThoseLogs(0, 0, "Server dump"), is(false));

      httpPost("http://" + ConfigHelper.HOST + ":" + getGroupData(0).getManagementPort(0) + "/tc-management-api/agents/diagnostics/dumpClusterState");

      assertTrueWithin(5000L, new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return serverContainsAllOfThoseLogs(0, 0, "Server dump");
        }
      });
    }

  }


}
