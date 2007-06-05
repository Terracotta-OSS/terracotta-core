/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import com.tc.text.Banner;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SessionsHelper {

  // For dev/test use only, not intended for customer usage
  private static final String TC_SESSION_CLASSPATH = "tc.session.classpath";

  private SessionsHelper() {
    //
  }

  public static String[] injectClasses(String[] s) {
    List classPaths = new ArrayList(Arrays.asList(s));

    String tcSessionCP = System.getProperty(TC_SESSION_CLASSPATH);
    if (tcSessionCP != null) {
      tcSessionCP = tcSessionCP.replace('\\', '/'); // for windows
      String[] paths = tcSessionCP.split(File.pathSeparator);
      for (int i = 0; i < paths.length; i++) {
        String path = paths[i];
        if (!path.endsWith("/")) {
          path += "/";
        }
        if (!path.startsWith("/")) {
          path = "/" + path;
        }
        classPaths.add(path);
      }
    } else {
      File installRoot = ClassProcessorHelper.getTCInstallDir(false);
      File sessionsLib = new File(installRoot, "lib");
      File tcSessionLib = new File(sessionsLib, "session");
      File tcSessionJar = new File(tcSessionLib, "tc-session.jar");
      if (tcSessionJar.exists() && tcSessionJar.isFile() && tcSessionJar.canRead()) {
        classPaths.add(tcSessionJar.getAbsolutePath());
      } else {
        Banner.errorBanner(tcSessionJar.getAbsolutePath() + " does not exist, or is not an accessible directory");
        Util.exit();
      }
    }
    s = new String[classPaths.size()];
    classPaths.toArray(s);
    return s;
  }

  public static void injectClasses(ClassLoader loader) throws Exception {
    final String[] sessionsClasspaths = injectClasses(new String[0]);
    if (!invokeAddURLMethodIfPresent(loader, sessionsClasspaths)) {
      if (!invokeAddPathsMethodIfPresent(loader, sessionsClasspaths)) {
        AssertionError ae = new AssertionError("SessionsHelper.injectClasses() cannot recognize classloader of type: "
                                               + loader.getClass().getName());
        throw ae;
      }
    }
  }

  private static boolean invokeAddPathsMethodIfPresent(ClassLoader loader, String[] classPaths) {
    try {
      Method m = loader.getClass().getDeclaredMethod("addPaths", new Class[] { String[].class });
      m.setAccessible(true);
      m.invoke(loader, new Object[] { classPaths });
      return true;
    } catch (SecurityException e) {
      return false;
    } catch (NoSuchMethodException e) {
      return false;
    } catch (IllegalArgumentException e) {
      return false;
    } catch (IllegalAccessException e) {
      return false;
    } catch (InvocationTargetException e) {
      return false;
    }
  }

  private static boolean invokeAddURLMethodIfPresent(ClassLoader loader, String[] classPaths) {
    Method m;
    try {
      m = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
      m.setAccessible(true);
      for (int pos = 0; pos < classPaths.length; ++pos) {
        m.invoke(loader, new Object[] { new URL("file", "", classPaths[pos]) });
      }
      return true;
    } catch (SecurityException e) {
      return false;
    } catch (NoSuchMethodException e) {
      return false;
    } catch (IllegalArgumentException e) {
      return false;
    } catch (MalformedURLException e) {
      return false;
    } catch (IllegalAccessException e) {
      return false;
    } catch (InvocationTargetException e) {
      return false;
    }
  }

}
