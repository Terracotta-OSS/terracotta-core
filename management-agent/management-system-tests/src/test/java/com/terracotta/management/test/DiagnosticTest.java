package com.terracotta.management.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import net.sf.ehcache.CacheManager;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * DiagnosticTest
 */
public class DiagnosticTest extends AbstractTsaAgentTestBase {

  private static final int MEMBER_COUNT = 1;

  public DiagnosticTest(TestConfig testConfig) {
    super(testConfig);

    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[]{DiagnosticThreadDumpTestClient.class, DiagnosticDGCTestClient.class});
  }

  public static class DiagnosticThreadDumpTestClient extends AbstractTsaClient {

    public DiagnosticThreadDumpTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {

      CacheManager cacheManager = createCacheManager(ConfigHelper.HOST, Integer.toString(getGroupData(0).getTsaPort(0)));

      try {
        testGroupThreadDump();

        testZippedGroupThreadDump();

        testSingleServerThreadDump();

        testSingleClientThreadDump();
      } finally {
        cacheManager.shutdown();
      }

    }

    private void testGroupThreadDump() throws IOException {
      for (int serverIndex = 0; serverIndex < MEMBER_COUNT; serverIndex++) {
        JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getTsaGroupPort(serverIndex),
            "/tc-management-api/agents/diagnostics/threadDump");
        assertThat(contentArray.size(), is(MEMBER_COUNT + 1));
        JSONObject content = (JSONObject)contentArray.get(0);
        assertThat(content.containsKey("dump"), is(true));
      }
    }

    private void testZippedGroupThreadDump() throws IOException {
      for (int serverIndex = 0; serverIndex < MEMBER_COUNT; serverIndex++) {
        Map<String, String> headers = new HashMap<String, String>() {{
          put("Accept", "application/zip");
        }};
        byte[] bytes = getTsaRawContent(ConfigHelper.HOST, getGroupData(0).getTsaGroupPort(serverIndex),
            "/tc-management-api/agents/diagnostics/threadDump", headers);

        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes));

        while (true) {
          ZipEntry zipEntry = zipInputStream.getNextEntry();
          if (zipEntry == null) break;

          // if we can unzip the bytes, we assume that the thread dump is good
          String stackTrace = IOUtils.toString(zipInputStream, "UTF-8");
          System.out.println("--- unzipped stack trace:" + zipEntry.getName() + " ---");
          System.out.println(stackTrace);
          System.out.println("----------");

          zipInputStream.closeEntry();
        }

        zipInputStream.close();
      }
    }

    private void testSingleClientThreadDump() throws IOException {
      String clientId = "0";
      JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getTsaGroupPort(0),
          "/tc-management-api/agents/diagnostics/threadDump/clients;ids=" + clientId);

      assertThreadDumpEntity(clientId, contentArray);

    }

    private void testSingleServerThreadDump() throws IOException {
      String serverName = getGroupData(0).getServerNames()[0];
      JSONArray contentArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getTsaGroupPort(0),
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
      httpPost("http://" + ConfigHelper.HOST + ":" + getGroupData(0).getTsaGroupPort(0) + "/tc-management-api/agents/diagnostics/dgc");
    }
  }

}
