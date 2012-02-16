/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import com.tc.net.NIOWorkarounds;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.hook.ClassPostProcessor;
import com.tc.object.bytecode.hook.ClassPreProcessor;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.text.Banner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.LogManager;

/**
 * Helper class called by the modified version of java.lang.ClassLoader
 */
public class ClassProcessorHelper {

  // Directory where Terracotta jars (and dependencies) can be found
  private static final String     TC_INSTALL_ROOT_SYSPROP = "tc.install-root";

  // Property to indicate whether the Terracotta classloader is active
  private static final String     TC_ACTIVE_SYSPROP       = "tc.active";

  // NOTE: This is not intended to be a public/documented system property,
  // it is for dev use only. It is NOT for QA or customer use
  private static final String     TC_CLASSPATH_SYSPROP    = "tc.classpath";

  private static final State      initState               = new State();

  private static final String     tcInstallRootSysProp    = System.getProperty(TC_INSTALL_ROOT_SYSPROP);

  // This map should only hold a weak reference to the loader (key).
  // If we didn't we'd prevent loaders from being GC'd
  private static final Map        contextMap              = new WeakHashMap();

  private static URLClassLoader   tcLoader;
  private static DSOContext       globalContext;

  private static volatile boolean systemLoaderInitialized = false;

  private static URLClassLoader createTCLoader() throws Exception {
    return new URLClassLoader(buildTerracottaClassPath(), null);
  }

  private static void handleError(Throwable t) {
    if (t instanceof InvocationTargetException) {
      t = ((InvocationTargetException) t).getTargetException();
    }

    if (t instanceof RuntimeException) { throw (RuntimeException) t; }
    if (t instanceof Error) { throw (Error) t; }

    throw new RuntimeException(t);
  }

  static File getTCInstallDir(boolean systemClassPathAllowed) {
    if (tcInstallRootSysProp == null) {
      if (systemClassPathAllowed) {
        try {
          ClassLoader.getSystemClassLoader().loadClass("com.tc.object.NotInBootJar");
          return null;
        } catch (ClassNotFoundException cnfe) {
          // ignore
        }
      }

      Banner.errorBanner("Terracotta home directory is not set. Please set it with -D" + TC_INSTALL_ROOT_SYSPROP
                         + "=<path-to-Terracotta-install>");
      Util.exit();
    }

    File tcInstallDir = new File(tcInstallRootSysProp);

    if (!tcInstallDir.exists() || !tcInstallDir.isDirectory() || !tcInstallDir.canRead()) {
      Banner.errorBanner("Terracotta install directory [" + tcInstallDir.getAbsolutePath()
                         + "] is not accessible. This value is set via system property " + TC_INSTALL_ROOT_SYSPROP);
      Util.exit();
    }

    return tcInstallDir;
  }

  private static URL[] buildTerracottaClassPath() throws Exception {
    if (System.getProperty(TC_CLASSPATH_SYSPROP) != null) { return buildDevClassPath(); }

    File tcHomeDir = getTCInstallDir(true);
    if (tcHomeDir == null) {
      @SuppressWarnings("restriction")
      ClassLoader classPathLoader = sun.misc.Launcher.getLauncher().getClassLoader();
      URL[] systemURLS = ((URLClassLoader) classPathLoader).getURLs();
      return systemURLS.clone();
    }

    File tcLib = new File(tcHomeDir, "lib");
    if (!tcLib.exists() || !tcLib.isDirectory() || !tcLib.canRead()) {
      Banner.errorBanner("Terracotta lib directory [" + tcLib.getAbsolutePath()
                         + "] is not accessible. This value is based off of the system property "
                         + TC_INSTALL_ROOT_SYSPROP);
      Util.exit();
    }

    File[] entries = tcLib.listFiles(new TcCommonLibQualifier());
    if (entries.length == 0) {
      Banner.errorBanner("Absolutely no .jar files or resources directory found in Terracotta common lib directory ["
                         + tcLib.getAbsolutePath() + "]. Please check the value of your " + TC_INSTALL_ROOT_SYSPROP
                         + " system property");
      Util.exit();
    }

    URL[] rv = new URL[entries.length];
    for (int i = 0; i < entries.length; i++) {
      String entry = entries[i].getCanonicalPath().replace(File.separatorChar, '/');
      if (entries[i].isDirectory()) entry += "/";
      rv[i] = new URL("file", "", entry);
    }

    return rv;
  }

  private static String slurpFile(String path) throws IOException {
    URL url = new URL(path);
    InputStream in = url.openStream();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    try {
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf, 0, buf.length)) >= 0) {
        bos.write(buf, 0, len);
      }
    } finally {
      try {
        in.close();
      } catch (IOException ioe) {
        // ignore
      }

      try {
        bos.close();
      } catch (IOException ioe) {
        // ignore
      }
    }

    return new String(bos.toByteArray());
  }

  private static URL[] buildDevClassPath() throws MalformedURLException, IOException {
    // For development use only. This is handy since you can put the eclipse/ant build output directories
    // here and not bother creating a tc.jar every time you change some source

    String tcClasspath = System.getProperty(TC_CLASSPATH_SYSPROP);
    if (tcClasspath.startsWith("file:/")) {
      tcClasspath = slurpFile(tcClasspath);
    }

    String[] parts = tcClasspath.split(File.pathSeparator);
    ArrayList urls = new ArrayList();

    for (String part : parts) {
      if (part.length() > 0) {

        File file = new File(part);
        part = file.getAbsolutePath().replace(File.separatorChar, '/');

        if (!part.startsWith("/")) {
          part = "/" + part;
        }

        if (file.isDirectory()) {
          if (!part.endsWith("/")) {
            part = part + "/";
          }
        }

        urls.add(new URL("file", "", part));
      }
    }

    return (URL[]) urls.toArray(new URL[urls.size()]);
  }

  public static void initialize() {
    if (initState.attemptInit()) {
      try {
        tcLoader = createTCLoader();

        // do this before doing anything with the TC loader
        initTCLogging();

        initState.initialized();

        System.setProperty(TC_ACTIVE_SYSPROP, Boolean.TRUE.toString());
      } catch (Throwable t) {
        t.printStackTrace();
        handleError(t);
        throw new AssertionError(); // shouldn't get here
      }
    }
  }

  private static void initTCLogging() throws Exception {
    Class loggerClass = tcLoader.loadClass("com.tc.logging.Log4jSafeInit");
    Method theMethod = loggerClass.getMethod("init", new Class[0]);
    theMethod.invoke(null, (Object[]) null);
  }

  /**
   * WARNING: Used by test framework only
   * 
   * @param loader Loader
   * @param context DSOContext
   */
  public static void setContext(ClassLoader loader, DSOContext context) {
    if ((loader == null) || (context == null)) {
      // bad dog
      throw new IllegalArgumentException("Loader and/or context may not be null");
    }

    synchronized (contextMap) {
      contextMap.put(loader, context);
    }
  }

  /**
   * WARNING: used by test framework only
   */
  public static Manager getManager(ClassLoader caller) {
    DSOContext context;
    synchronized (contextMap) {
      context = (DSOContext) contextMap.get(caller);
    }
    if (context == null) { return null; }
    return context.getManager();
  }

  /**
   * Get the DSOContext for this classloader
   * 
   * @param cl Loader
   * @return Context
   */
  public static DSOContext getContext(ClassLoader cl) {
    synchronized (contextMap) {
      return (DSOContext) contextMap.get(cl);
    }
  }

  /**
   * byte code instrumentation of class loaded <br>
   * XXX::NOTE:: Do NOT optimize to return same input byte array if the class was instrumented (I can't imagine why we
   * would). Our instrumentation in java.lang.ClassLoader checks the returned byte array to see if the class is
   * instrumented or not to maintain the array offset.
   * 
   * @param caller Loader defining class
   * @param name Class name
   * @param b Data
   * @param off Offset into b
   * @param len Length of class data
   * @param pd Protection domain for class
   * @return Modified class array
   */
  public static byte[] defineClass0Pre(ClassLoader caller, String name, byte[] b, int off, int len, ProtectionDomain pd) {
    if (skipClass(caller)) { return b; }

    // needed for JRockit
    name = (name != null) ? name.replace('/', '.') : null;

    if (isAWDependency(name)) { return b; }

    /*
     * This current initialization strategy has one slight shortcoming. JVMTI agents cannot attach while initialization
     * is in progress.
     */
    initialize();

    if (!initState.isInitialized()) { return b; }

    ManagerUtil.enable();

    ClassPreProcessor preProcessor = getPreProcessor(caller);
    if (preProcessor == null) { return b; }

    return preProcessor.preProcess(name, b, off, len, caller);
  }

  private static boolean skipClass(ClassLoader caller) {
    return (caller == tcLoader);
  }

  /**
   * Post process class during definition
   * 
   * @param clazz Class being defined
   * @param caller Classloader doing definition
   */
  public static void defineClass0Post(Class clazz, ClassLoader caller) {
    ClassPostProcessor postProcessor = getPostProcessor(caller);
    if (!initState.isInitialized()) { return; }

    if (skipClass(caller)) { return; }

    if (postProcessor == null) { return; }

    postProcessor.postProcess(clazz, caller);
  }

  /**
   * @return Global Manager
   */
  public static Manager getGlobalManager() {
    return globalContext.getManager();
  }

  private static ClassPreProcessor getPreProcessor(ClassLoader caller) {
    synchronized (contextMap) {
      return (ClassPreProcessor) contextMap.get(caller);
    }
  }

  private static ClassPostProcessor getPostProcessor(ClassLoader caller) {
    synchronized (contextMap) {
      return (ClassPostProcessor) contextMap.get(caller);
    }
  }

  /**
   * Check whether this is an AspectWerkz dependency
   * 
   * @param className Class name
   * @return True if AspectWerkz dependency
   */
  public static boolean isAWDependency(final String className) {
    return (className == null)
           || className.endsWith("_AWFactory")// TODO AVF refactor
           || className.endsWith("___AW_JoinPoint") || className.startsWith("com.tc.aspectwerkz.")
           || className.startsWith("com.tc.asm.") || className.startsWith("com.tc.backport175.")
           || className.startsWith("com.tc.jrexx.") || className.startsWith("org.dom4j.")
           || className.startsWith("org.xml.sax.") || className.startsWith("javax.xml.parsers.")
           || className.startsWith("sun.reflect.Generated"); // issue on J2SE 5 reflection - AW-245
  }

  public static void systemLoaderInitialized() {
    // This avoids a deadlock (see LKC-853, LKC-1387)
    java.security.Security.getProviders();

    // Avoid another deadlock (DEV-1047, DEV-1301)
    // Looking at the sun-jdk17 sources, this deadlock shouldn't happen there
    LogManager.getLogManager();

    // Workaround bug in NIO on solaris 10
    NIOWorkarounds.solaris10Workaround();

    systemLoaderInitialized = true;
  }

  /**
   * File filter for lib/*.jar files and lib/resources directory
   */
  public static class TcCommonLibQualifier implements FileFilter {
    public boolean accept(File pathname) {
      return (pathname.isDirectory() && pathname.getName().equals("resources"))
             || (pathname.isFile() && pathname.getAbsolutePath().toLowerCase().endsWith(".jar"));
    }
  }

  /**
   * ClassProcessorHelper initialization state
   */
  public static final class State {
    private static final int NOT_INITIALIZED = 0;
    private static final int INITIALIZING    = 1;
    private static final int INITIALIZED     = 2;

    private int              state           = NOT_INITIALIZED;

    final synchronized boolean attemptInit() {
      if ((state == NOT_INITIALIZED) && systemLoaderInitialized && !creatingPlatformMBeanServer()) {
        state = INITIALIZING;
        return true;
      }
      return false;
    }

    private static boolean creatingPlatformMBeanServer() {
      // quick and mildly dirty fix for CDV-1415
      StackTraceElement[] stack = new Throwable().getStackTrace();
      for (StackTraceElement frame : stack) {
        if (frame.getClassName().equals("java.lang.management.ManagementFactory")
            && frame.getMethodName().equals("getPlatformMBeanServer")) { return true; }
      }

      return false;
    }

    final synchronized void initialized() {
      if (state != INITIALIZING) { throw new IllegalStateException("State was " + state); }
      state = INITIALIZED;
    }

    final synchronized boolean isInitialized() {
      return state == INITIALIZED;
    }

  }

}
