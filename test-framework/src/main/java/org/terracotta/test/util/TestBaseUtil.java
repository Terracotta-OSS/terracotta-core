package org.terracotta.test.util;

import org.apache.commons.io.IOUtils;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.TestConfig;
import com.tc.test.setup.GroupsData;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TestBaseUtil {

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

  public static String getTerracottaURL(GroupsData[] groupsData) {
    StringBuilder tcUrl = new StringBuilder();
    for (GroupsData groupData : groupsData) {
      for (int i = 0; i < groupData.getServerCount(); i++) {
        tcUrl.append(ConfigHelper.HOST + ":" + groupData.getDsoPort(i) + ",");
      }
    }

    return tcUrl.substring(0, tcUrl.lastIndexOf(","));
  }

  public static void cleanDirectory(File directory) {
    int count = 0;
    while (true) {
      count++;
      try {
        cleanDirectory(directory);
        break;
      } catch (Exception e) {
        System.err.println("Unable to clean up the directory - " + directory + "; Exception: " + e);
      }

      if (count > 10) {
        System.err.println("Skipping clean up for directory - " + directory);
        break;
      }

      ThreadUtil.reallySleep(2000);
    }

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
    } else {
      jvmArgs.add("-Xloggc:" + verboseGcOutputFile.getAbsolutePath());
      jvmArgs.add("-XX:+PrintGCTimeStamps");
      jvmArgs.add("-XX:+PrintGCDetails");
    }
  }

  public static void setHeapSizeArgs(List<String> jvmArgs, int minHeap, int maxHeap) {
    Iterator<String> i = jvmArgs.iterator();
    while (i.hasNext()) {
      String arg = i.next();
      if (arg.startsWith("-Xmx") || arg.startsWith("-Xms")) {
        System.err.println("Ignoring '" + arg + "'. Heap size should be set through L2Config.");
        i.remove();
      }
    }
    jvmArgs.add("-Xms" + minHeap + "m");
    jvmArgs.add("-Xmx" + maxHeap + "m");
  }

  public static void configureOffHeap(TestConfig testConfig, int maxDirectMemory, int offHeapDataSize) {
    testConfig.getL2Config().addExtraServerJvmArg("-XX:MaxDirectMemorySize=" + maxDirectMemory + "m");
    testConfig.getL2Config().setMaxOffHeapDataSize(offHeapDataSize);
    testConfig.getL2Config().setPersistenceMode(PersistenceMode.PERMANENT_STORE);

    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_SKIP_JVMARG_CHECK, "true");
    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_INITIAL_DATASIZE, "1m");
    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_TABLESIZE, "1");
    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_CONCURRENCY, "16");

    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_MAP_CACHE_MAX_PAGE_SIZE, "10k");
    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_MAP_CACHE_MIN_PAGE_SIZE, "10k");
    testConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_MAP_CACHE_TABLESIZE, "1");
  }

}
