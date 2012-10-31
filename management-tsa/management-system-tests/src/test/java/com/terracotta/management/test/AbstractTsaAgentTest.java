package com.terracotta.management.test;

import org.json.simple.JSONValue;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.tests.base.AbstractTestBase;
import org.terracotta.toolkit.ToolkitFactory;

import com.tc.management.beans.L2MBeanNames;
import com.tc.test.config.model.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractTsaAgentTest extends AbstractTestBase {

  public AbstractTsaAgentTest(TestConfig testConfig) {
    super(testConfig);

    String war = guessWarLocation();
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc.management.war=" + war);
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc.management.bindMBeansToPlatformMBeanServer=" + true);
  }

  private String guessWarLocation() {
    String m2Root = System.getProperty("user.home") + "/.m2/repository".replace('/', File.separatorChar);
    String version = guessVersion();

    String agentDir = m2Root + "/org/terracotta/management-tsa-war/".replace('/', File.separatorChar) + version;

    List<String> files = Arrays.asList(new File(agentDir).list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".war") && !name.endsWith("-sources.jar") && !name.endsWith("-tests.jar");
      }
    }));
    if (files.isEmpty()) {
      throw new AssertionError("No agent WAR file found in [" + agentDir + "]");
    }
    Collections.sort(files);

    // always take the last one of the sorted list, it should be the latest version
    return agentDir + File.separator + files.get(files.size() - 1);
  }

  private String guessVersion() {
    // e.g. /home/userXYZ/.m2/repository/org/terracotta/terracotta-toolkit-runtime/3.8.0-SNAPSHOT/terracotta-toolkit-runtime-3.8.0-SNAPSHOT.jar
    String toolkitJar = TestBaseUtil.jarFor(ToolkitFactory.class);
    if (toolkitJar == null) {
      throw new AssertionError("Cannot find toolkit JAR");
    }

    String[] pathes = toolkitJar.split("\\/");
    if (pathes.length > 2) {
      return pathes[pathes.length - 2];
    }

    throw new AssertionError("Invalid toolkit JAR: " + toolkitJar);
  }

  @Override
  protected String createClassPath(Class client) throws IOException {
    String expressRuntime = TestBaseUtil.jarFor(ToolkitFactory.class);
    String clientBase = TestBaseUtil.jarFor(AbstractTsaAgentTest.class);
    String l2Mbean = TestBaseUtil.jarFor(L2MBeanNames.class);
    String jsonParser = TestBaseUtil.jarFor(JSONValue.class);
    return makeClasspath(expressRuntime, clientBase, l2Mbean, jsonParser);
  }

  public abstract static class AbstractTsaClient extends AbstractClientBase {


    @Override
    protected final void doTest() throws Throwable {
      // wait for the TSA agent to finish up initialization
      for (int i = 0; i < 5; i++) {
        try {
          httpGet("http://" + this.getTerracottaUrl() + "/tc-management-api/agents");
          break;
        } catch (IOException ioe) {
          Thread.sleep(1000);
        }
      }

      doTsaTest();
    }

    protected abstract void doTsaTest() throws Throwable;

    protected String httpGet(String urlString) throws IOException {
      URL url = new URL(urlString);
      InputStream inputStream = url.openStream();

      StringBuilder sb = new StringBuilder();
      BufferedReader bufferedReader = null;
      try {
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        while (true) {
          String line = bufferedReader.readLine();
          if (line == null) {
            break;
          }
          sb.append(line).append("\n");
        }

      } finally {
        if (bufferedReader != null) {
          bufferedReader.close();
        }
      }

      return sb.toString();
    }

    public AbstractTsaClient(String[] args) {
      super(args);
    }
  }


}
