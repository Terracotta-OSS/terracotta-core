package org.terracotta.test.util;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.google.common.collect.Sets;
import com.tc.config.test.schema.ConfigHelper;
import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;
import com.tc.test.setup.GroupsData;
import com.tc.text.Banner;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class TestBaseUtil {
  private static final String  SEP                       = File.pathSeparator;
  private static final String  MAVEN_LOCAL_REPO = getMavenLocalRepo();
  private static final Pattern ARTIFACT_PATTERN = Pattern.compile("^\\s*([^:]+):([^:]+):([^:]+):([^:]+):(.+)$");

  public static void removeDuplicateJvmArgs(List<String> jvmArgs) {
    Map<String, String> argsMap = new HashMap<String, String>();
    Set<String> argSet = new HashSet<String>();
    for (String arg : jvmArgs) {
      if (arg.indexOf("=") > 0) {
        String key = arg.substring(0, arg.indexOf("="));
        String value = arg.substring(arg.indexOf("=") + 1);
        argsMap.put(key, value);
      } else {
        // for args that does not have value
        argSet.add(arg);
      }
    }

    jvmArgs.clear();
    for (Entry<String, String> entry : argsMap.entrySet()) {
      jvmArgs.add(entry.getKey() + "=" + entry.getValue());
    }
    for (String arg : argSet) {
      jvmArgs.add(arg);
    }
  }

  public static String getTerracottaURL(GroupsData[] groupsData, boolean useTsaProxy) {
    StringBuilder tcUrl = new StringBuilder();
    for (GroupsData groupData : groupsData) {
      for (int i = 0; i < groupData.getServerCount(); i++) {
        tcUrl.append(ConfigHelper.HOST + ":" + (useTsaProxy ? groupData.getProxyTsaPort(i) : groupData.getTsaPort(i))
                     + ",");
      }
    }

    return tcUrl.substring(0, tcUrl.lastIndexOf(","));
  }

  public static String getFileContents(File f) throws IOException {
    FileInputStream in = null;
    try {
      in = new FileInputStream(f);
      return IOUtils.toString(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  public static String jarFor(Class c) {
    ProtectionDomain protectionDomain = c.getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    if (codeSource != null) {
      URL url = codeSource.getLocation();
      String path = url.getPath();
      if (Os.isWindows() && path.startsWith("/")) {
        path = path.substring(1);
      }
      return URLDecoder.decode(path);
    } else {
      return jarFromClassResource(c);
    }
  }

  private static String jarFromClassResource(Class c) {
    URL clsUrl = c.getResource(c.getSimpleName() + ".class");
    if (clsUrl != null) {
      try {
        URLConnection conn = clsUrl.openConnection();
        if (conn instanceof JarURLConnection) {
          JarURLConnection connection = (JarURLConnection) conn;
          return connection.getJarFileURL().getFile();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    throw new AssertionError("returning null for " + c.getName());
  }

  public static void setupVerboseGC(List<String> jvmArgs, File verboseGcOutputFile) {
    if (Vm.isJRockit()) {
      jvmArgs.add("-Xverbose:gcpause,gcreport");
      jvmArgs.add("-Xverboselog:" + verboseGcOutputFile.getAbsolutePath());
    } else if (Vm.isIBM()) {
      jvmArgs.add("-Xverbosegclog:" + verboseGcOutputFile.getAbsolutePath());
    } else {
      jvmArgs.add("-Xloggc:" + verboseGcOutputFile.getAbsolutePath());
      jvmArgs.add("-XX:+PrintGCTimeStamps");
      jvmArgs.add("-XX:+PrintGCDetails");
    }
  }

  public static void setHeapSizeArgs(List<String> jvmArgs, int minHeap, int maxHeap, int directMemorySize,
                                     boolean overrideParams) {
    Iterator<String> i = jvmArgs.iterator();
    List<String> illegalArgs = new ArrayList<String>();
    while (i.hasNext()) {
      String arg = i.next();
      if (arg.startsWith("-Xmx") || arg.startsWith("-Xms") || arg.startsWith("-XX:MaxDirectMemorySize")) {
        if (overrideParams) {
          i.remove();
          continue;
        }
        illegalArgs.add(arg);
      }
    }
    if (!illegalArgs.isEmpty()) { throw new AssertionError(
                                                           "Invalid use of jvmArgs - "
                                                               + illegalArgs
                                                               + " - use testConfig.get[Client/L2]Config().set[MaxHeap/MinHeap/DirectMemorySize]() instead!"); }
    jvmArgs.add("-Xms" + minHeap + "m");
    jvmArgs.add("-Xmx" + maxHeap + "m");

    if (directMemorySize > 0) jvmArgs.add("-XX:MaxDirectMemorySize=" + directMemorySize + "m");
  }

  public static void configureOffHeap(TestConfig testConfig, int maxDirectMemory, int offHeapDataSize) {
    testConfig.getL2Config().setOffHeapEnabled(true);
    testConfig.getL2Config().setDirectMemorySize(maxDirectMemory);
    testConfig.getL2Config().setMaxOffHeapDataSize(offHeapDataSize);
    testConfig.setRestartable(true);

    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_SKIP_JVMARG_CHECK, "true");
    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_OBJECTDB_INITIAL_DATASIZE, "1m");
    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_OBJECTDB_TABLESIZE, "1");
    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_OBJECTDB_CONCURRENCY, "16");

    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_MAX_PAGE_SIZE, "10k");
    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_MIN_PAGE_SIZE, "10k");
    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_MAP_TABLESIZE, "1");
  }

  public static void enableL2Reconnect(TestConfig testConfig) {

    testConfig.addTcProperty(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED, "true");

    // for windows, it takes 10 seconds to restart proxy port
    if (Os.isWindows()) {
      testConfig.addTcProperty(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT, "" + 20000);
    }
  }

  public static void enableL1Reconnect(TestConfig testConfig) {
    testConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
    testConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS, "20000");
  }

  public static void enabledL1ProxyConnection(TestConfig testConfig) {
    testConfig.getL2Config().setProxyTsaPorts(true);
    testConfig.getL2Config().setManualProxycontrol(true);
    testConfig.addTcProperty(TCPropertiesConsts.L1_L2_CONFIG_VALIDATION_ENABLED, "false");
  }

  public static void enableOOOLogging(TestConfig testConfig) {
    testConfig.getClientConfig().addExtraClientJvmArg("-Dooo.logging.enabled=true");
  }

  public static void enableCallStackTrace(TestConfig testConfig) {
    testConfig.getClientConfig().addExtraClientJvmArg("-Dstack.trace.enabled=true");
  }

  public static String getThreadDump() {
    final String newline = System.getProperty("line.separator", "\n");
    StringBuffer rv = new StringBuffer();
    // tbean may contain entries for some threads which are already dead
    ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
    for (long id : tbean.getAllThreadIds()) {
      // if id is the id of a thread which is already dead
      // we'll get tinfo == null in the next line
      ThreadInfo tinfo = tbean.getThreadInfo(id, Integer.MAX_VALUE);
      if (tinfo == null) {
        continue;
      }
      rv.append("Thread name: " + tinfo.getThreadName()).append("-" + id).append(newline);
      for (StackTraceElement e : tinfo.getStackTrace()) {
        rv.append("    at " + e).append(newline);
      }
      rv.append(newline);
    }
    return rv.toString();
  }

  public static List<ThreadInfo> getThreadDumpAsList() {
    List<ThreadInfo> list = new ArrayList();
    ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
    for (long id : tbean.getAllThreadIds()) {
      ThreadInfo tinfo = tbean.getThreadInfo(id, Integer.MAX_VALUE);
      list.add(tinfo);
    }
    return list;
  }

  public static void dumpHeap(String dumpName) {
    try {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      String hotSpotDiagName = "com.sun.management:type=HotSpotDiagnostic";
      ObjectName name = new ObjectName(hotSpotDiagName);
      String operationName = "dumpHeap";

      new File("heapDumps").mkdirs();
      File tempFile = new File("heapDumps/" + dumpName + "_" + (System.currentTimeMillis()) + ".hprof");
      tempFile.delete();
      String dumpFilename = tempFile.getAbsolutePath();

      Object[] params = new Object[] { dumpFilename, Boolean.TRUE };
      String[] signature = new String[] { String.class.getName(), boolean.class.getName() };
      try {
        mbs.invoke(name, operationName, params, signature);
      } catch (InstanceNotFoundException e) {
        System.out.println("heap dump failed: " + e);
        return;
      }

      System.out.println("dumped heap in file " + dumpFilename);
    } catch (Exception e) {
      System.out.println("Caught exception while trying to heap dump:");
      e.printStackTrace();
    }
  }

  public static List<String> jarsFromMavenDependenciesList(URL resourceList) {
    if (resourceList == null) { throw new NullPointerException("resourceList is null"); }

    List<String> jarList = new ArrayList<String>();
    InputStream in = null;
    try {
      in = resourceList.openStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      String line = null;
      while ((line = reader.readLine()) != null) {
        Matcher m = ARTIFACT_PATTERN.matcher(line);
        if (m.matches() && ("compile".equals(m.group(5)) || "runtime".equals(m.group(5)))) {
          String jarFile = constructMavenLocalFile(m.group(1), m.group(2), m.group(3), m.group(4));
          jarList.add(jarFile);
        }
      }
      return jarList;
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  private static String constructMavenLocalFile(String groupId, String artifactId, String type, String version) {
    File artifact = new File(MAVEN_LOCAL_REPO, groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/"
                                               + artifactId + "-" + version + "." + type);
    if (!artifact.exists()) { throw new AssertionError("Can't find Maven artifact: " + groupId + ":" + artifactId + ":"
                                                       + type + ":" + version); }
    try {
      return artifact.getCanonicalFile().getAbsolutePath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getMavenLocalRepo() {
    String base = System.getProperty("maven.repo.local");
    try {
      if (base == null) {
        base = System.getProperty("localMavenRepository");
        if (base == null) base = new File(System.getProperty("user.home"), "/.m2/repository").getCanonicalPath();
      }
      File settingsXml = new File(System.getProperty("user.home"), "/.m2/settings.xml");
      if (settingsXml.exists()) {
        // check if people have moved local repo
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(settingsXml);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("localRepository");
        if (nodeList.getLength() > 0) {
          base = nodeList.item(0).getTextContent();
        }
      }
      return base;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * <p>
   * return list of dependencies of given artifact coordinates either ee or os this method will first look for devmode
   * depenendencies list from resource if not found, it will use the provided class to look up the jar
   * </p>
   * <p>
   * Currently, only Ehcache and toolkit runtime jars are possible to load this way
   * </p>
   */
  private static List<String> getDevmodeAwareDependenciesOf(String groupId, String opensourceArtifactId,
                                                            String enterpriseArtifactId, Class<?> c) {
    String artifactId = enterpriseArtifactId;
    URL devmodeResource = TestBaseUtil.class.getResource("/META-INF/devmode/" + groupId + "/" + enterpriseArtifactId
                                                         + "/dependencies.txt");
    if (devmodeResource == null) {
      artifactId = opensourceArtifactId;
      devmodeResource = TestBaseUtil.class.getResource("/META-INF/devmode/" + groupId + "/" + opensourceArtifactId
                                                       + "/dependencies.txt");
    }
    List<String> deps = new ArrayList<String>();
    if (devmodeResource != null) {
      Banner.infoBanner("devmode jar is being used for " + artifactId);
      deps = jarsFromMavenDependenciesList(devmodeResource);
    } else {
      deps.add(jarFor(c));
    }
    return deps;
  }

  public static List<String> getEhcacheDependencies(Class<?> someEhcacheClass) {
    return getDevmodeAwareDependenciesOf("net.sf.ehcache", "ehcache", "ehcache-ee", someEhcacheClass);
  }

  public static List<String> getToolkitRuntimeDependencies(Class<?> someToolkitClass) {
    return getDevmodeAwareDependenciesOf("org.terracotta", "terracotta-toolkit-runtime",
                                         "terracotta-toolkit-runtime-ee", someToolkitClass);
  }

  public static List<String> getTerracottaCoreDependencies(Class<?> someTCCoreClass) {
    return getDevmodeAwareDependenciesOf("org.terracotta", "terracotta", "terracotta-ee", someTCCoreClass);
  }

  public static String constructClassPath(String... jars) {
    return constructClassPath(Arrays.asList(jars));
  }

  public static String constructClassPath(Collection<String> jars) {
    StringBuilder builder = new StringBuilder();
    for (String jar : Sets.newLinkedHashSet(jars)) {
      builder.append(SEP).append(jar);
    }
    return builder.toString();
  }
}
