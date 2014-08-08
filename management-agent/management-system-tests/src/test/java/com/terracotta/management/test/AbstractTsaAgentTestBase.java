package com.terracotta.management.test;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import org.apache.commons.codec.Encoder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.LoggerFactory;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.tests.base.AbstractTestBase;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.util.ToolkitVersion;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.management.beans.L2MBeanNames;
import com.tc.test.config.builder.MavenArtifactFinder;
import com.tc.test.config.model.TestConfig;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public abstract class AbstractTsaAgentTestBase extends AbstractTestBase {

  public AbstractTsaAgentTestBase(TestConfig testConfig) {
    super(testConfig);

    String war = guessWarLocation();
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc.management.war=" + war);
    testConfig.getL2Config().addExtraServerJvmArg("-Djava.awt.headless=true");
    testConfig.getClientConfig().setParallelClients(false);
    testConfig.getClientConfig().addExtraClientJvmArg("-Djava.awt.headless=true");
  }

  private String guessWarLocation() {
    String version = AbstractTsaClient.guessMavenArtifactVersion();
    return MavenArtifactFinder.findArtifactLocation("org.terracotta", "management-tsa-war", version, null, "war");
  }

  @Override
  protected String createClassPath(Class client) {
    String tk = TestBaseUtil.jarFor(ToolkitVersion.class);
    String common = TestBaseUtil.jarFor(Os.class);
    String expressRuntime = TestBaseUtil.jarFor(ToolkitFactory.class);
    String fs = TestBaseUtil.jarFor(com.terracotta.toolkit.api.TerracottaToolkitFactoryService.class);
    String l1 = TestBaseUtil.jarFor(org.terracotta.toolkit.internal.TerracottaL1Instance.class);
    String clientBase = TestBaseUtil.jarFor(AbstractTsaAgentTestBase.class);
    String l2Mbean = TestBaseUtil.jarFor(L2MBeanNames.class);
    String jsonParser = TestBaseUtil.jarFor(JSONValue.class);
    String ehCache = TestBaseUtil.jarFor(CacheManager.class);
    String slf4J = TestBaseUtil.jarFor(LoggerFactory.class);
    String commonsIo = TestBaseUtil.jarFor(IOUtils.class);
    String ehcache = TestBaseUtil.jarFor(Ehcache.class);
    String httpClient = TestBaseUtil.jarFor(HttpClient.class);
    String httpCore = TestBaseUtil.jarFor(HttpVersion.class);
    String commonsCodec = TestBaseUtil.jarFor(Encoder.class);
    String commonsLogging = TestBaseUtil.jarFor(Log.class);
    String coreMatchers = TestBaseUtil.jarFor(CoreMatchers.class);
    String commonCli = TestBaseUtil.jarFor(org.apache.commons.cli.ParseException.class);
    String commonLang = TestBaseUtil.jarFor(org.apache.commons.lang.StringUtils.class);
    return makeClasspath(tk, common, expressRuntime, fs, l1, clientBase, l2Mbean, jsonParser, ehCache, slf4J,
                         commonsIo, ehcache, httpClient, httpCore, commonsCodec, commonsLogging, coreMatchers,
                         commonCli, commonLang);
  }

  public abstract static class AbstractTsaClient extends AbstractClientBase {

    protected static final String      TSA_TEST_CACHE = "tsaTest";

    private static CloseableHttpClient httpClient;

    protected String guessVersion() {
      return guessMavenArtifactVersion();
    }

    @Override
    protected final void doTest() throws Throwable {
      HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
      httpClientBuilder.disableContentCompression();
      httpClient = httpClientBuilder.build();

      // wait for the TSA agent to finish up initialization
      boolean initSuccessful = false;
      System.out.println("Starting test for " + getTerracottaUrl());
      for (int i = 0; i < 20; i++) {
        try {
          for (int j = 0; j < getGroupData(0).getServerCount(); j++) {
            httpGet("http://" + ConfigHelper.HOST + ":" + getGroupData(0).getManagementPort(j)
                    + "/tc-management-api/agents");
          }
          initSuccessful = true;
          break;
        } catch (IOException ioe) {
          Thread.sleep(1000);
        }
      }
      assertThat("Server initialization issue", initSuccessful, is(true));

      try {
        doTsaTest();
      } finally {
        httpClient.close();
      }
    }

    protected abstract void doTsaTest() throws Throwable;

    protected byte[] getTsaRawContent(String host, int port, String path, Map<String, String> headers)
        throws IOException {
      return httpRawGet("http://" + host + ":" + port + path, headers);
    }

    protected JSONArray getTsaJSONArrayContent(String host, int port, String path) throws IOException {
      String result = httpGet("http://" + host + ":" + port + path);
      System.out.println("Server ");
      System.out.println(result);
      return (JSONArray) JSONValue.parse(result);
    }

    protected byte[] httpRawGet(String urlString, Map<String, String> headers) throws IOException {
      HttpGet httpGet = new HttpGet(urlString);
      if (headers != null) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          httpGet.addHeader(entry.getKey(), entry.getValue());
        }
      }

      CloseableHttpResponse response = httpClient.execute(httpGet);

      try {
        if (response.getStatusLine().getStatusCode() != 200) {
          HttpEntity entity2 = response.getEntity();
          EntityUtils.consume(entity2);
          throw new IOException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
        }

        HttpEntity entity = response.getEntity();
        return EntityUtils.toByteArray(entity);
      } finally {
        response.close();
      }
    }

    private int lastHttpResponseCode = 0;

    protected int getLastHttpResponseCode() {
      return lastHttpResponseCode;
    }

    protected String httpGet(String urlString) throws IOException {
      HttpGet httpGet = new HttpGet(urlString);
      httpGet.addHeader("Accept", "application/json");
      CloseableHttpResponse response = httpClient.execute(httpGet);

      try {
        if (response.getStatusLine().getStatusCode() != 200) {
          HttpEntity entity2 = response.getEntity();
          String s = EntityUtils.toString(entity2);
          System.out.println("Error");
          System.out.println(s);
          lastHttpResponseCode = response.getStatusLine().getStatusCode();
          throw new IOException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
        }

        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
      } finally {
        response.close();
      }
    }

    protected String httpPost(String urlString) throws IOException {
      HttpPost httpPost = new HttpPost(urlString);
      String charset = "UTF-8";
      httpPost.addHeader("Accept-Charset", charset);
      httpPost.addHeader("Content-Type", "application/json;charset=" + charset);
      CloseableHttpResponse response = httpClient.execute(httpPost);

      try {
        if (response.getStatusLine().getStatusCode() != 200) {
          HttpEntity entity2 = response.getEntity();
          EntityUtils.consume(entity2);
          throw new IOException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
        }

        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
      } finally {
        response.close();
      }
    }

    protected void waitUntilServerAgentUp(int port) {
      while (true) {
        try {
          httpGet("http://localhost:" + port + "/tc-management-api/agents");
          break;
        } catch (IOException e) {
          ThreadUtil.reallySleep(1000);
        }
      }
    }

    public AbstractTsaClient(String[] args) {
      super(args);
    }

    protected CacheManager createCacheManager(String host, String port) {
      Configuration configuration = new Configuration();
      TerracottaClientConfiguration terracottaClientConfiguration = new TerracottaClientConfiguration();
      terracottaClientConfiguration.url(host, port);

      configuration.addTerracottaConfig(terracottaClientConfiguration);

      configuration.addDefaultCache(new CacheConfiguration("default", 100).eternal(false));

      CacheConfiguration cacheConfiguration = new CacheConfiguration(TSA_TEST_CACHE, 100).eternal(false)
          .terracotta(new TerracottaConfiguration());

      configuration.addCache(cacheConfiguration);

      return new CacheManager(configuration);
    }

    static String guessMavenArtifactVersion() {
      try {
        return MavenArtifactFinder.figureCurrentArtifactMavenVersion();
      } catch (IOException ioe) {
        throw new AssertionError("cannot guess version");
      }
    }

    protected boolean serverContainsAllOfThoseLogs(int group, int member, String... logs) throws IOException {
      JSONArray logsArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(group).getManagementPort(member),
                                                   "/tc-management-api/agents/logs");
      boolean[] contains = new boolean[logs.length];

      for (Object aLogsArray : logsArray) {
        JSONObject o = (JSONObject) aLogsArray;
        String message = (String) o.get("message");

        for (int j = 0; j < logs.length; j++) {
          contains[j] |= message.contains(logs[j]);
        }

        boolean allTrue = true;
        for (boolean contain : contains) {
          allTrue &= contain;
        }
        if (allTrue) { return true; }
      }

      return false;
    }

    protected boolean serverContainsAnyOfThoseLogs(int group, int member, String... logs) throws IOException {
      JSONArray logsArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(group).getManagementPort(member),
                                                   "/tc-management-api/agents/logs");
      for (Object aLogsArray : logsArray) {
        JSONObject o = (JSONObject) aLogsArray;
        String message = (String) o.get("message");

        for (String log : logs) {
          if (message.contains(log)) { return true; }
        }
      }
      return false;
    }

    protected void assertTrueWithin(long timeInMs, Callable<Boolean> callable) throws Exception {
      Boolean success = false;

      while (timeInMs > 0) {
        success = callable.call();

        if (success != null && success) {
          break;
        }

        ThreadUtil.reallySleep(1000L);
        timeInMs -= 1000L;
      }

      assertThat(success, is(true));
    }
  }

}
