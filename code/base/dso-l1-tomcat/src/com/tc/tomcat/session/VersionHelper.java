/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.tomcat.session;

import org.apache.catalina.Container;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.util.ServerInfo;

import java.lang.reflect.Constructor;

public class VersionHelper {
  private static final Object[] NO_ARGS           = new Object[] {};
  private static final Class[]  NO_ARGS_SIGNATURE = new Class[] {};

  private static final Version  TOMCAT_50         = new Version("tomcat", "50");
  private static final Version  TOMCAT_55         = new Version("tomcat", "55");
  private static final Version  GLASSFISH         = new Version("glassfish", "");
  private static final Version  CURRENT;

  static {
    String serverInfo = ServerInfo.getServerInfo();
    serverInfo = (serverInfo == null) ? "null" : serverInfo;

    if (serverInfo.startsWith("Sun Java System Application Server Platform Edition 9")
      || serverInfo.startsWith("Sun Java System Application Server 9")) {
      CURRENT = GLASSFISH;
    } else {
      int lastSlash = serverInfo.lastIndexOf("/");
      if (lastSlash < 0 || serverInfo.endsWith("/")) { throw new AssertionError("Cannot determine tomcat version from "
                                                                                + serverInfo); }

      String ver = serverInfo.substring(lastSlash + 1);

      if (ver.startsWith("5.0")) {
        CURRENT = TOMCAT_50;
      } else if (ver.startsWith("5.5")) {
        CURRENT = TOMCAT_55;
      } else if (ver.startsWith("6.0")) {
        CURRENT = TOMCAT_55;
      } else {
        throw new AssertionError("Cannot determine tomcat version from " + serverInfo);
      }
    }
  }

  private VersionHelper() {
    //
  }

  public static Valve createSessionValve() {
    return (Valve) createObject(CURRENT, "session.SessionValve" + CURRENT.getVersion());
  }

  public static Pipeline createTerracottaPipeline(Container container) {
    return (Pipeline) createObject(CURRENT, "TerracottaPipeline", new Class[] { Container.class },
                                   new Object[] { container });
  }

  private static final Object createObject(Version version, String clazz) {
    return createObject(version, clazz, NO_ARGS_SIGNATURE, NO_ARGS);
  }

  private static final Object createObject(Version version, String clazz, Class[] signature, Object[] params) {
    String className = "com.tc." + version.getPackageName() + "." + clazz;

    try {
      Class c = Class.forName(className);
      Constructor cstr = c.getDeclaredConstructor(signature);
      return cstr.newInstance(params);
    } catch (Exception e) {
      Error err = new AssertionError("Error creating instance of " + className);
      err.initCause(e);
      throw err;
    }
  }

  private static class Version {
    private final String version;
    private final String prefix;

    Version(String prefix, String version) {
      this.prefix = prefix;
      this.version = version;
    }

    public String getPackageName() {
      return prefix + version;
    }

    public String getVersion() {
      return version;
    }

    public String toString() {
      return version;
    }
  }

}
