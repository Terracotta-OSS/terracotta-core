/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.distrunner;

import com.tcsimulator.ClientSpec;
import com.tcsimulator.distrunner.NullServerSpec;
import com.tcsimulator.distrunner.ServerSpec;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ArgParser {

  private final Collection    clientSpecs          = new LinkedList();
  private final SpecFactory   specFactory;
  private final Collection    testServerSpecs      = new LinkedList();
  private final ServerSpec    controlServerSpec;
  private final String        testClassname;
  private final int           intensity;
  private final int           testServerStartMode;
  private final String        javaHome;
  private static final String CLASS_NAME_ARG       = "classname";
  private static final String INTENSITY_ARG        = "intensity";
  private static final String CLIENT_ARG           = "client";
  private static final String TEST_SERVER_ARG      = "server";
  private static final String CONTROL_SERVER_ARG   = "control";
  private static final String BACKUP_SERVER_ARG    = "backup";
  private static final String VM_COUNT_ARG         = "vm.count";
  private static final String EXECUTION_COUNT_ARG  = "execution.count";
  private static final String JVM_OPTS_ARG         = "jvm.args";
  private static final String CACHE_COUNT_ARG      = "config.dso.server.cache";
  private static final String HTTP_PORT_ARG        = "config.dso.http.port";
  private static final String JMX_PORT_ARG         = "config.dso.jmx.port";
  private static final String DSO_PORT_ARG         = "config.dso.dso.port";

  private static final String DSO_START_ARG        = "dsostart";
  private static final String DSO_START_TRUE_ARG   = "true";
  private static final String JAVA_HOME_ARG        = "javahome";
  private static final String UNDEFINED            = "undefined";
  private static final String UNDEFINED_NUMBER     = "-1";

  public static final int     DSO_START            = 0;
  public static final int     NON_DSO_START        = 1;
  // default port values of control server... for test server, add 1... for back up, add 2
  public static final int     DEFAULT_HTTP_PORT    = 9515;
  public static final int     DEFAULT_JMX_PORT     = 9520;
  public static final int     DEFAULT_DSO_PORT     = 9510;
  // default cache count for all servers
  public static final int     DEFAULT_SERVER_CACHE = 200000;

  public ArgParser(String[] args, SpecFactory specFactory, boolean serverSpecRequired, boolean controlServerSpecRequired)
      throws ArgException {
    this.specFactory = specFactory;
    if (args == null || args.length == 0) { throw new ArgException("No arguments specified"); }
    ServerSpec theControlServerSpec = new NullServerSpec();
    String theTestClassname = "";
    int theIntensity = -1;
    int theTestServerStartMode = -1;
    String theJavaHome = "";
    try {
      for (int i = 0; i < args.length; i++) {
        if (args[i].startsWith(CLIENT_ARG)) {
          parseClientSpecs(args[i]);
        } else if (args[i].startsWith(TEST_SERVER_ARG) || args[i].startsWith(CONTROL_SERVER_ARG)
                   || args[i].startsWith(BACKUP_SERVER_ARG)) {
          theControlServerSpec = parseServerSpecs(args[i]);
        } else if (args[i].startsWith(CLASS_NAME_ARG)) {
          if (!theTestClassname.equals("")) { throw new ArgException("More than one test classname specified."); }
          String[] nvPair = args[i].split("=");
          if (nvPair.length != 2) { throw new ArgException("Malformed test classname argument."); }
          theTestClassname = nvPair[1];
        } else if (args[i].startsWith(INTENSITY_ARG)) {
          if (theIntensity >= 0) { throw new ArgException("More than one intensity specified."); }
          String[] nvPair = args[i].split("=");
          if (nvPair.length != 2) { throw new ArgException("Malformed intensity argument."); }
          if (!nvPair[1].equals(UNDEFINED)) {
            theIntensity = Integer.parseInt(nvPair[1]);
          }
        } else if (args[i].startsWith(DSO_START_ARG)) {
          String[] nvPair = args[i].split("=");
          if (nvPair.length != 2) { throw new ArgException("Malformed dso start argument."); }
          if (nvPair[1].equalsIgnoreCase(DSO_START_TRUE_ARG)) {
            theTestServerStartMode = DSO_START;
          } else {
            theTestServerStartMode = NON_DSO_START;
          }
        } else if (args[i].startsWith(JAVA_HOME_ARG)) {
          String[] nvPair = args[i].split("=");
          if (nvPair.length != 2) { throw new ArgException("Malformed java home argument."); }
          theJavaHome = nvPair[1];
        }
      }
    } catch (URISyntaxException e) {
      throw new ArgException(e);
    }
    this.testClassname = theTestClassname; // if not specified, then value is "undefined"
    this.intensity = theIntensity; // if not specified, then value is -1
    this.testServerStartMode = theTestServerStartMode;
    if (theControlServerSpec.isNull() && controlServerSpecRequired) { throw new ArgException(
                                                                                             "No control server spec specified."); }
    this.controlServerSpec = theControlServerSpec;
    if (testServerSpecs.isEmpty() && serverSpecRequired) { throw new ArgException("No server spec specified."); }
    this.javaHome = theJavaHome;
  }

  private ServerSpec parseServerSpecs(String arg) throws ArgException, URISyntaxException {
    ServerSpec theControlServerSpec = new NullServerSpec();
    String[] specDescriptions = arg.split(";");
    for (int i = 0; i < specDescriptions.length; i++) {
      ServerSpec csSpec = parseServerSpec(new URI(specDescriptions[i]));
      if (!csSpec.isNull() && theControlServerSpec.isNull()) {
        theControlServerSpec = csSpec;
      } else if (!csSpec.isNull()) { throw new ArgException("More than one control server spec specified."); }
    }
    return theControlServerSpec;
  }

  private ServerSpec parseServerSpec(URI uri) throws ArgException {
    String hostname = uri.getHost();
    String testHome = uri.getPath();
    int cache = 0;
    int jmxPort = 0;
    int dsoPort = 0;
    List parsedJvmOpts = new ArrayList();
    int undefNumber = ArgParser.getUndefinedNumber();
    String typeName = uri.getScheme();
    int type = -1;

    if (typeName.startsWith(CONTROL_SERVER_ARG)) {
      type = 0;
    } else if (typeName.startsWith(TEST_SERVER_ARG)) {
      type = 1;
    } else if (typeName.startsWith(BACKUP_SERVER_ARG)) {
      type = 2;
    }

    try {
      cache = Integer.parseInt(getValueFromQuery(CACHE_COUNT_ARG, UNDEFINED_NUMBER, uri.getQuery()));
      if (cache == undefNumber) {
        cache = ArgParser.DEFAULT_SERVER_CACHE;
      }
    } catch (NumberFormatException e) {
      throw new ArgException("Unable to parse " + CACHE_COUNT_ARG + ": " + uri);
    }
    try {
      jmxPort = Integer.parseInt(getValueFromQuery(JMX_PORT_ARG, UNDEFINED_NUMBER, uri.getQuery()));
      if (jmxPort == undefNumber) {
        jmxPort = ArgParser.DEFAULT_JMX_PORT + type;
      }
    } catch (NumberFormatException e) {
      throw new ArgException("Unable to parse " + JMX_PORT_ARG + ": " + uri);
    }
    try {
      dsoPort = Integer.parseInt(getValueFromQuery(DSO_PORT_ARG, UNDEFINED_NUMBER, uri.getQuery()));
      if (dsoPort == undefNumber) {
        dsoPort = ArgParser.DEFAULT_DSO_PORT + type;
      }
    } catch (NumberFormatException e) {
      throw new ArgException("Unable to parse " + DSO_PORT_ARG + ": " + uri);
    }
    String[] jvmOpts = getValueFromQuery(JVM_OPTS_ARG, UNDEFINED, uri.getQuery()).split(",");
    for (int i = 0; i < jvmOpts.length; i++) {
      parsedJvmOpts.add(jvmOpts[i]);
    }

    switch (type) {
      case 0:
        return specFactory.newServerSpec(hostname, testHome, cache, jmxPort, dsoPort, parsedJvmOpts,
                                         ServerSpec.CONTROL_SERVER);
      case 1:
        testServerSpecs.add(specFactory.newServerSpec(hostname, testHome, cache, jmxPort, dsoPort, parsedJvmOpts,
                                                      ServerSpec.TEST_SERVER));
        return new NullServerSpec();
      case 2:
        testServerSpecs.add(specFactory.newServerSpec(hostname, testHome, cache, jmxPort, dsoPort, parsedJvmOpts,
                                                      ServerSpec.BACKUP_SERVER));
        return new NullServerSpec();
      default:
        throw new AssertionError("Attempting to parse unrecognizable server type!");
    }
  }

  private void parseClientSpecs(String arg) throws URISyntaxException, NumberFormatException, ArgException {
    String[] specDescriptions = arg.split(";");
    for (int i = 0; i < specDescriptions.length; i++) {
      parseClientSpec(new URI(specDescriptions[i]));
    }
  }

  private void parseClientSpec(URI uri) throws ArgException {
    String hostname = uri.getHost();
    String testHome = uri.getPath();
    int vmCount = 0;
    int executionCount = 0;
    List parsedJvmOpts = new ArrayList();

    try {
      vmCount = Integer.parseInt(getValueFromQuery(VM_COUNT_ARG, "1", uri.getQuery()));
    } catch (NumberFormatException e) {
      throw new ArgException("Unable to parse " + VM_COUNT_ARG + ": " + uri);
    }
    try {
      executionCount = Integer.parseInt(getValueFromQuery(EXECUTION_COUNT_ARG, "1", uri.getQuery()));
    } catch (NumberFormatException e) {
      throw new ArgException("Unable to parse " + EXECUTION_COUNT_ARG + ": " + uri);
    }
    String[] jvmOpts = getValueFromQuery(JVM_OPTS_ARG, UNDEFINED, uri.getQuery()).split(",");
    for (int i = 0; i < jvmOpts.length; i++) {
      parsedJvmOpts.add(jvmOpts[i]);
    }
    clientSpecs.add(specFactory.newClientSpec(hostname, testHome, vmCount, executionCount, parsedJvmOpts));
  }

  public int getIntensity() {
    return intensity;
  }

  public Collection getClientSpecs() {
    return clientSpecs;
  }

  public Collection getServerSpecs() {
    return this.testServerSpecs;
  }

  // TODO: this is temp way to get active test server
  public ServerSpec getServerSpec() {
    return (ServerSpec) this.testServerSpecs.iterator().next();
  }

  public ServerSpec getControlServerSpec() {
    return this.controlServerSpec;
  }

  public String getTestClassname() {
    return this.testClassname;
  }

  public boolean getTestServerStartMode() {
    if (this.testServerStartMode == DSO_START) { return true; }
    return false;
  }

  public String getJavaHome() {
    String fileSeparator = System.getProperty("file.separator");

    return this.javaHome + fileSeparator + "bin" + fileSeparator + "java";
  }

  private static String getValueFromQuery(String name, String defaultValue, String query) throws ArgException {
    List results = getValuesFromQuery(name, query);
    if (results.isEmpty()) { return defaultValue; }
    if (results.size() > 1) {
      throw new ArgException("More than one value for name (" + name + "): " + query);
    } else {
      return (String) results.get(0);
    }
  }

  private static List getValuesFromQuery(String name, String query) {
    List rv = new LinkedList();
    if (query != null) {
      String[] pairs = query.split("&");
      for (int i = 0; i < pairs.length; i++) {
        String pair = pairs[i];
        int split = pair.indexOf("=");
        String[] nv = new String[] { pair.substring(0, split), pair.substring(split + 1, pair.length()) };
        if (name.equals(nv[0])) {
          rv.add(nv[1]);
        }
      }
    }
    return rv;
  }

  public static String getArgumentForTestClassName(String testClassName) {
    return CLASS_NAME_ARG + "=" + testClassName;
  }

  public static String getArgumentForClientSpec(ClientSpec cSpec) {
    StringBuffer jopts = new StringBuffer();
    for (Iterator i = cSpec.getJvmOpts().iterator(); i.hasNext();) {
      jopts.append(i.next());
      if (i.hasNext()) {
        jopts.append(",");
      }
    }
    StringBuffer result = new StringBuffer();
    result.append(CLIENT_ARG + "://" + cSpec.getHostName() + cSpec.getTestHome() + "?" + VM_COUNT_ARG + "="
                  + cSpec.getVMCount() + "&" + EXECUTION_COUNT_ARG + "=" + cSpec.getExecutionCount());
    if (!jopts.toString().equals(UNDEFINED)) {
      result.append("&" + JVM_OPTS_ARG + "=" + jopts.toString());
    }
    return result.toString();
  }

  public static Collection getArgumentsForClientSpecs(Collection clientSpecs) {
    List result = new ArrayList();
    for (Iterator i = clientSpecs.iterator(); i.hasNext();) {
      result.add(ArgParser.getArgumentForClientSpec((ClientSpec) i.next()));
    }
    return result;
  }

  public static String getArgumentForIntensity(int intensity) {
    return INTENSITY_ARG + "=" + intensity;
  }

  public static String getArgumentForServerSpec(ServerSpec sSpec) {
    int type = sSpec.getType();
    String token = null;
    switch (type) {
      case ServerSpec.CONTROL_SERVER:
        token = CONTROL_SERVER_ARG;
        break;
      case ServerSpec.TEST_SERVER:
        token = TEST_SERVER_ARG;
        break;
      case ServerSpec.BACKUP_SERVER:
        token = BACKUP_SERVER_ARG;
    }

    StringBuffer jopts = new StringBuffer();
    for (Iterator i = sSpec.getJvmOpts().iterator(); i.hasNext();) {
      jopts.append(i.next());
      if (i.hasNext()) {
        jopts.append(",");
      }
    }
    StringBuffer result = new StringBuffer();
    result.append(token + "://" + sSpec.getHostName() + sSpec.getTestHome() + "?config.dso.server.cache="
                  + sSpec.getCache() + "&config.dso.jmx.port=" + sSpec.getJmxPort() + "&config.dso.dso.port="
                  + sSpec.getDsoPort());
    if (!jopts.toString().equals(UNDEFINED)) {
      result.append("&" + JVM_OPTS_ARG + "=" + jopts.toString());
    }
    return result.toString();
  }

  public static Collection getArgumentsForServerSpecs(Collection serverSpecs) {
    List result = new ArrayList();
    for (Iterator i = serverSpecs.iterator(); i.hasNext();) {
      result.add(ArgParser.getArgumentForServerSpec((ServerSpec) i.next()));
    }
    return result;
  }

  public static String getUndefinedString() {
    return UNDEFINED;
  }

  public static int getUndefinedNumber() {
    return Integer.parseInt(UNDEFINED_NUMBER);
  }

  public static final String usage() {
    StringBuffer buf = new StringBuffer("Setup for distributed test runner.\nUsage:");

    buf.append("\n\njava ");
    buf.append("<client spec>[;<client spec>;<client spec>...]");
    buf.append(" <server spec>[;<server spec>...]");
    buf.append(" " + CLASS_NAME_ARG + "=<test application classname>");
    buf.append(" " + INTENSITY_ARG + "=<integer describing intensity>");
    buf.append("\n\nclient spec: " + CLIENT_ARG + "://<hostname>/path/to/test/home[?[" + VM_COUNT_ARG + "=<vm count>]");
    buf.append("[&" + EXECUTION_COUNT_ARG + "=<execution count>][&" + JVM_OPTS_ARG + "=<jvm args>]]\n");
    buf.append("server spec: {" + TEST_SERVER_ARG + " " + CONTROL_SERVER_ARG + "}://<hostname>/path/to/test/home");
    buf.append("[?[" + JVM_OPTS_ARG + "=<jvm args>][&" + CACHE_COUNT_ARG + "=<cache count>]");
    buf.append("[&" + HTTP_PORT_ARG + "=<http port>][&" + JMX_PORT_ARG + "=<jmx port>]");
    buf.append("[&" + DSO_PORT_ARG + "=<dso port>]]\n");
    buf.append("\t" + VM_COUNT_ARG + ": how many JVMs to start on this client\n");
    buf.append("\t" + EXECUTION_COUNT_ARG + ": the number of application instances to start per vm\n");
    buf.append("\t" + JVM_OPTS_ARG + ": jvm options to be used when running this process\n");
    buf.append("\t" + TEST_SERVER_ARG + ": DSO server used to run the test apps\n");
    buf.append("\t" + CONTROL_SERVER_ARG + ": DSO server used to manage the distribute test framework\n");
    return buf.toString();
  }
}
