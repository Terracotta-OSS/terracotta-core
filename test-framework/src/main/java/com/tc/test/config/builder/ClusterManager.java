package com.tc.test.config.builder;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.test.util.TestBaseUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tc.config.Loader;
import com.tc.test.process.ExternalDsoServer;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;
import com.terracottatech.config.Server;
import com.terracottatech.config.TcConfigDocument;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @author Ludovic Orban
 */
public class ClusterManager {

  private static final Logger LOG = LoggerFactory.getLogger(ClusterManager.class);

  private final TcConfigBuilder tcConfigBuilder;
  private final File workingDir;
  private final String version;

  private final Map<String, ExternalDsoServer> externalDsoServers = new TreeMap<String, ExternalDsoServer>();
  private String maxDirectMemorySize = "1G";

  public ClusterManager(Class<?> testClass, TcConfig tcConfig) throws IOException, XmlException {
    this(new File(System.getProperty("project.build.directory",
        "target"), "temp" + File.separator + testClass.getSimpleName()), tcConfig, true);
  }

  public ClusterManager(Class<?> testClass, TcConfig tcConfig, boolean fillUpConfig) throws IOException, XmlException {
    this(new File(System.getProperty("project.build.directory",
        "target"), "temp" + File.separator + testClass.getSimpleName()), tcConfig, fillUpConfig);
  }

  public ClusterManager(File workingDir, TcConfig tcConfig, boolean fillUpConfig) throws IOException, XmlException {
    if (fillUpConfig) {
      tcConfig.fillUpConfig(workingDir);
    }

    XStream xstream = new XStream(new DomDriver());
    xstream.autodetectAnnotations(true);
    String xml = xstream.toXML(tcConfig);
    LOG.info("Starting cluster for config: " + xml);
    TcConfigDocument parsedDoc = new Loader().parse(xml);

    this.tcConfigBuilder = new TcConfigBuilder(parsedDoc);
    this.workingDir = workingDir;
    this.version = guessMavenArtifactVersion(getClass());
    if (version == null) {
      throw new IllegalStateException("cannot figure out version");
    }
  }

  public String getVersion() {
    return version;
  }

  public File getWorkingDir() {
    return workingDir;
  }

  public String getMaxDirectMemorySize() {
    return maxDirectMemorySize;
  }

  public void setMaxDirectMemorySize(String maxDirectMemorySize) {
    this.maxDirectMemorySize = maxDirectMemorySize;
  }

  public void start() throws Exception {
    String war = guessAgentWarLocation();
    workingDir.mkdirs();

    Server[] servers = tcConfigBuilder.getServers();
    for (Server server : servers) {
      String serverName = server.getName();

      File serverWorkingDir = server.getName() != null ? new File(workingDir, server.getName()) : workingDir;
      serverWorkingDir.mkdir();
      ExternalDsoServer externalDsoServer = new ExternalDsoServer(serverWorkingDir, tcConfigBuilder.newInputStream(), serverName);
      externalDsoServer.addJvmArg("-Dcom.tc.management.war=" + war);
      externalDsoServer.addJvmArg("-XX:MaxDirectMemorySize=" + maxDirectMemorySize);
      externalDsoServer.startWithoutWait();

      externalDsoServers.put(serverName, externalDsoServer);
    }

    LOG.debug("Waiting for TSA agents to initialize...");
    for (Server server : servers) {
      waitUntilTsaAgentInitialized(server.getTsaGroupPort().getIntValue());
      LOG.debug("{} initialized", server.getName());
    }
    System.out.println("All TSA agents started successfully");
  }

  public void stop() throws Exception {
    for (ExternalDsoServer externalDsoServer : externalDsoServers.values()) {
      try {
        externalDsoServer.stop();
      } catch (Exception e) {
        LOG.error("error stopping server", e);
      }
    }
    externalDsoServers.clear();
  }

  private String guessAgentWarLocation() {
    String m2Root = System.getProperty("user.home") + "/.m2/repository".replace('/', File.separatorChar);
    if (System.getProperty("maven.repo.local") != null) {
      m2Root = System.getProperty("maven.repo.local");
      LOG.info("Found maven.repo.local defined as a system property! Using m2root=" + m2Root);
    }

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

  private static String guessMavenArtifactVersion(Class<?> clazz) {
    // e.g. /home/userXYZ/.m2/repository/org/terracotta/terracotta-toolkit-runtime/3.8.0-SNAPSHOT/terracotta-toolkit-runtime-3.8.0-SNAPSHOT.jar
    String jar = TestBaseUtil.jarFor(clazz);
    if (jar == null) {
      throw new AssertionError("Cannot find JAR for class: " + clazz);
    }

    if (jar.endsWith(".jar")) {
      String[] pathes = jar.split("\\/");
      if (pathes.length > 2) {
        return pathes[pathes.length - 2];
      }
      throw new AssertionError("Invalid JAR: " + jar);
    } else {
      // running from IDE? try to get the version from the pom file
      try {
        File fXmlFile = new File("pom.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);

        NodeList childNodes = doc.getDocumentElement().getChildNodes();
        for (int i=0;i<childNodes.getLength();i++) {
          Node node = childNodes.item(i);
          if ("version".equals(node.getNodeName())) {
            return node.getTextContent();
          }
        }
      } catch (Exception e) {
        // ignore
      }
      throw new AssertionError("cannot guess version");
    }
  }

  private static void waitUntilTsaAgentInitialized(int port) {
    for (int i = 0; i < 30; i++) {
      try {
        URL url = new URL("http://localhost:" + port + "/tc-management-api/agents");
        URLConnection urlConnection = url.openConnection();
        InputStream inputStream = urlConnection.getInputStream();
        IOUtils.copy(inputStream, new OutputStream() {
          @Override
          public void write(int b) throws IOException {
            // send to /dev/null
          }
        });
        inputStream.close();
        LOG.debug("TSA agent to initialized on port {}", port);
        break;
      } catch (IOException ioe) {
        LOG.debug("Waiting for TSA agent to initialize on port {}... (#{})", port, i);
        ThreadUtil.reallySleep(1000L);
      }
    }
  }

}
