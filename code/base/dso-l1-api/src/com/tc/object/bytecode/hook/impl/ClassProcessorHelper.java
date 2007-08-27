/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.net.NIOWorkarounds;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.hook.ClassLoaderPreProcessorImpl;
import com.tc.object.bytecode.hook.ClassPostProcessor;
import com.tc.object.bytecode.hook.ClassPreProcessor;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.loaders.StandardClassProvider;
import com.tc.text.Banner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Helper class called by the modified version of java.lang.ClassLoader
 */
public class ClassProcessorHelper {

  // Directory where Terracotta jars (and dependencies) can be found
  private static final String                TC_INSTALL_ROOT_SYSPROP = "tc.install-root";

  // Property to indicate whether the Terracotta classloader is active
  private static final String                TC_ACTIVE_SYSPROP       = "tc.active";

  // NOTE: This is not intended to be a public/documented system property,
  // it is for dev use only. It is NOT for QA or customer use
  private static final String                TC_CLASSPATH_SYSPROP    = "tc.classpath";

  // Used for converting resource names into class names
  private static final String                CLASS_SUFFIX            = ".class";
  private static final int                   CLASS_SUFFIX_LENGTH     = CLASS_SUFFIX.length();

  private static final boolean               GLOBAL_MODE_DEFAULT     = true;

  public static final boolean                USE_GLOBAL_CONTEXT;

  private static final State                 initState               = new State();

  private static final ThreadLocal           inStaticInitializer     = new ThreadLocal();

  private static final String                tcInstallRootSysProp    = System.getProperty(TC_INSTALL_ROOT_SYSPROP);

  // This map should only hold a weak reference to the loader (key).
  // If we didn't we'd prevent loaders from being GC'd
  private static final Map                   contextMap              = new WeakHashMap();

  private static final StandardClassProvider globalProvider          = new StandardClassProvider();

  private static final URL[]                 tcClassPath;

  private static URLClassLoader              tcLoader;
  private static DSOContext                  gloalContext;

  private static final boolean               TRACE;
  private static final PrintStream           TRACE_STREAM;

  static {
    // Make sure that the DSOContext class is loaded before using the
    // TC functionalities. This is needed for the IBM JDK when Hashtable is
    // instrumented for auto-locking in the bootjar.
    Class dsocontext_class = DSOContext.class;

    inStaticInitializer.set(new Object());

    try {
      String global = System.getProperty("tc.dso.globalmode", null);
      if (global != null) {
        USE_GLOBAL_CONTEXT = Boolean.valueOf(global).booleanValue();
      } else {
        USE_GLOBAL_CONTEXT = GLOBAL_MODE_DEFAULT;
      }

      // This avoids a deadlock (see LKC-853, LKC-1387)
      java.security.Security.getProviders();

      // Workaround bug in NIO on solaris 10
      NIOWorkarounds.solaris10Workaround();

      tcClassPath = buildTerracottaClassPath();

      // See if we should trace or not -- if so grab System.[out|err] and keep a local reference to it. Applications
      // like WebSphere like to intercept this later and we can get caught in a loop and get a stack overflow
      final String traceOutput = System.getProperty("l1.classloader.trace.output", "none");
      PrintStream ts = null;
      if (traceOutput != null) {
        if ("stdout".equals(traceOutput)) {
          ts = System.out;
        } else if ("stderr".equals(traceOutput)) {
          ts = System.err;
        }
      }
      TRACE_STREAM = ts;
      TRACE = TRACE_STREAM != null;
    } catch (Throwable t) {
      Util.exit(t);
      throw new AssertionError(); // this has to be here to make the compiler happy
    } finally {
      inStaticInitializer.set(null);
    }
  }

  private static URLClassLoader createTCLoader() {
    return new URLClassLoader(tcClassPath, null);
  }

  public static URL getTCResource(String name, ClassLoader cl) {
    String className = null;
    if (name.endsWith(CLASS_SUFFIX)) {
      className = name.substring(0, name.length() - CLASS_SUFFIX_LENGTH).replace('/', '.');
    }

    URL resource = getClassResource(className, cl);

    if (null == resource) {
      if (!isAWRuntimeDependency(className)) { return null; }

      try {
        resource = tcLoader.findResource(name); // getResource() would cause an endless loop
      } catch (Exception e) {
        resource = null;
      }
    }

    return resource;
  }

  public static byte[] getTCClass(String name, ClassLoader cl) throws ClassNotFoundException {
    URL resource = getClassResource(name, cl);

    if (null == resource) {
      if (!isAWRuntimeDependency(name)) { return null; }

      resource = tcLoader.findResource(name.replace('.', '/') + ".class"); // getResource() would cause an endless loop
    }

    if (null == resource) { return null; }

    return getResourceBytes(resource);
  }

  private static byte[] getResourceBytes(URL url) throws ClassNotFoundException {
    InputStream is = null;
    try {
      is = url.openStream();
      byte[] b = new byte[is.available()];
      int len = 0;
      int n;
      while ((n = is.read(b, len, b.length - len)) > 0) {
        len += n;
        if (len < b.length) {
          byte[] c = new byte[b.length + 1000];
          System.arraycopy(b, 0, c, 0, len);
          b = c;
        }
      }
      if (len == b.length) { return b; }
      byte[] c = new byte[len];
      System.arraycopy(b, 0, c, 0, len);
      return c;
    } catch (Exception e) {
      throw new ClassNotFoundException("Unable to load " + url.toString() + "; " + e.toString(), e);
    } finally {
      try {
        is.close();
      } catch (Exception ex) {
        // ignore
      }
    }
  }

  private static URL getClassResource(String name, ClassLoader cl) {
    if (name != null) {
      DSOContext context = getContext(cl);
      if (context != null) { return context.getClassResource(name); }
    }

    return null;
  }

  private static boolean isAWRuntimeDependency(String name) {
    if (null == name) { return false; }
    return name.startsWith("com.tcspring.");
    // || name.startsWith("com.tc.aspectwerkz.definition.deployer.AspectModule")
    // || name.equals("com.tc.aspectwerkz.aspect.AspectContainer")
    // || name.equals("com.tc.aspectwerkz.aspect.AbstractAspectContainer");
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
      URL[] systemURLS = ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs();
      return (URL[]) systemURLS.clone();
    }

    File tcLib = new File(tcHomeDir, "lib");
    if (!tcLib.exists() || !tcLib.isDirectory() || !tcLib.canRead()) {
      Banner.errorBanner("Terracotta lib directory [" + tcLib.getAbsolutePath()
                         + "] is not accessible. This value is based off of the system property "
                         + TC_INSTALL_ROOT_SYSPROP);
      Util.exit();
    }

    File[] entries = tcLib.listFiles(new JarFilter());

    if (entries.length == 0) {
      Banner.errorBanner("Absolutely no .jar files found in Terracotta common lib directory ["
                         + tcLib.getAbsolutePath() + "]. Please check the value of your " + TC_INSTALL_ROOT_SYSPROP
                         + " system property");
      Util.exit();
    }

    URL[] rv = new URL[entries.length];
    for (int i = 0; i < entries.length; i++) {
      String jar = entries[i].getAbsolutePath().replace(File.separatorChar, '/');
      rv[i] = new URL("file", "", jar);
    }
    return rv;
  }

  private static String slurpFile(String path) throws IOException {
    FileInputStream in = new FileInputStream(path);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    try {
      in = new FileInputStream(path);
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
    if (tcClasspath.startsWith("file:///")) {
      tcClasspath = slurpFile(tcClasspath.substring("file://".length()));
    }

    String[] parts = tcClasspath.split(File.pathSeparator);
    ArrayList urls = new ArrayList();

    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
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

  private static Method getContextMethod(String name, Class[] args) throws ClassNotFoundException,
      NoSuchMethodException {
    Class c = tcLoader.loadClass("com.tc.object.bytecode.hook.impl.DSOContextImpl");
    return c.getDeclaredMethod(name, args);
  }

  private static void init() {
    if (initState.attemptInit()) {
      try {
        tcLoader = createTCLoader();

        // do this before doing anything with the TC loader
        initTCLogging();

        if (USE_GLOBAL_CONTEXT) {
          gloalContext = createGlobalContext();
        }

        initState.initialized();

        System.setProperty(TC_ACTIVE_SYSPROP, Boolean.TRUE.toString());
      } catch (Throwable t) {
        t.printStackTrace();
        handleError(t);
        throw new AssertionError(); // shouldn't get here
      }
    }
  }

  private static void initTCLogging() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
      InvocationTargetException {
    // This code is here because users can set various Log4J properties that will, for example, cause Log4J to try
    // to use arbitrary classes as appenders. If users try to use one of their own classes as an appender, we'll try
    // to load it in our classloader and fail in fairly unpleasant ways.
    //
    // Yes, saving and restoring a system property really sucks, but there isn't really a better way to do it. Users
    // can request that Log4J read config from an arbitrary URL otherwise, and there's no way to intercept that at
    // all. As a result, this seems like a better solution.
    //
    // See LKC-1974 for more details.

    String oldDefaultInitOverrideValue = null;

    try {
      oldDefaultInitOverrideValue = System.setProperty("log4j.defaultInitOverride", "true");
      Class loggerClass = tcLoader.loadClass("org.apache.log4j.Logger");
      Method theMethod = loggerClass.getDeclaredMethod("getRootLogger", new Class[0]);
      theMethod.invoke(null, (Object[]) null);
    } finally {
      if (oldDefaultInitOverrideValue == null) {
        System.getProperties().remove("log4j.defaultInitOverride");
      } else {
        System.setProperty("log4j.defaultInitOverride", oldDefaultInitOverrideValue);
      }
    }
  }

  public static void registerGlobalLoader(NamedClassLoader loader) {
    if (!USE_GLOBAL_CONTEXT) { throw new IllegalStateException("Not global DSO mode"); }
    if (TRACE) traceNamedLoader(loader);
    globalProvider.registerNamedLoader(loader);
  }

  public static void shutdown() {
    if (!USE_GLOBAL_CONTEXT) { throw new IllegalStateException("Not global DSO mode"); }
    try {
      if (gloalContext != null) {
        gloalContext.getManager().stop();
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public static boolean isDSOSessions(String appName) {
    appName = ("/".equals(appName)) ? "ROOT" : appName;
    init();
    try {
      Method m = getContextMethod("isDSOSessions", new Class[] { String.class });
      boolean rv = ((Boolean) m.invoke(null, new Object[] { appName })).booleanValue();
      return rv;
    } catch (Throwable t) {
      handleError(t);
      throw new AssertionError(); // shouldn't get here
    }
  }

  // used by test framework only
  public static void setContext(ClassLoader loader, DSOContext context) {
    if (USE_GLOBAL_CONTEXT) { throw new IllegalStateException("DSO Context is global in this VM"); }

    if ((loader == null) || (context == null)) {
      // bad dog
      throw new IllegalArgumentException("Loader and/or context may not be null");
    }

    synchronized (contextMap) {
      contextMap.put(loader, context);
    }
  }

  // used by test framework only
  public static Manager getManager(ClassLoader caller) {
    if (USE_GLOBAL_CONTEXT) { return gloalContext.getManager(); }

    DSOContext context;
    synchronized (contextMap) {
      context = (DSOContext) contextMap.get(caller);
    }
    if (context == null) { return null; }
    return context.getManager();
  }

  public static DSOContext getContext(ClassLoader cl) {
    if (USE_GLOBAL_CONTEXT) return gloalContext;

    synchronized (contextMap) {
      return (DSOContext) contextMap.get(cl);
    }
  }

  private static DSOContext createGlobalContext() {
    try {
      Method m = getContextMethod("createGlobalContext", new Class[] { ClassProvider.class });
      DSOContext context = (DSOContext) m.invoke(null, new Object[] { globalProvider });
      context.getManager().init();
      return context;
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(-1);
      throw new AssertionError(); // shouldn't get here
    }
  }

  /**
   * byte code instrumentation of class loaded <br>
   * XXX::NOTE:: Donot optimize to return same input byte array if the class is instrumented (I cant imagine why we
   * would). ClassLoader checks the returned byte array to see if the class is instrumented or not to maintain the
   * offset.
   * 
   * @see ClassLoaderPreProcessorImpl
   */
  public static byte[] defineClass0Pre(ClassLoader caller, String name, byte[] b, int off, int len, ProtectionDomain pd) {
    if (inStaticInitializer()) { return b; }
    if (skipClass(caller)) { return b; }

    // needed for JRockit
    name = (name != null) ? name.replace('/', '.') : null;

    if (TRACE) traceLookup(caller, name);

    if (isAWDependency(name)) { return b; }
    if (isDSODependency(name)) { return b; }

    init();
    if (!initState.isInitialized()) { return b; }

    ManagerUtil.enable();

    ClassPreProcessor preProcessor = getPreProcessor(caller);
    if (preProcessor == null) { return b; }

    return preProcessor.preProcess(name, b, off, len, caller);
  }

  private static boolean skipClass(ClassLoader caller) {
    return (caller == tcLoader);
  }

  public static void defineClass0Post(Class clazz, ClassLoader caller) {
    if (inStaticInitializer()) { return; }

    ClassPostProcessor postProcessor = getPostProcessor(caller);
    if (!initState.isInitialized()) { return; }

    if (skipClass(caller)) { return; }

    if (postProcessor == null) { return; }

    postProcessor.postProcess(clazz, caller);
  }

  private static boolean inStaticInitializer() {
    return inStaticInitializer.get() != null;
  }

  public static Manager getGlobalManager() {
    return gloalContext.getManager();
  }

  private static ClassPreProcessor getPreProcessor(ClassLoader caller) {
    if (USE_GLOBAL_CONTEXT) { return gloalContext; }

    synchronized (contextMap) {
      return (ClassPreProcessor) contextMap.get(caller);
    }
  }

  private static ClassPostProcessor getPostProcessor(ClassLoader caller) {
    if (USE_GLOBAL_CONTEXT) { return gloalContext; }

    synchronized (contextMap) {
      return (ClassPostProcessor) contextMap.get(caller);
    }
  }

  public static boolean isAWDependency(final String className) {
    return (className == null)
           || className.endsWith("_AWFactory")// TODO AVF refactor
           || className.endsWith(TransformationConstants.JOIN_POINT_CLASS_SUFFIX)
           || className.startsWith("com.tc.aspectwerkz.") || className.startsWith("com.tc.asm.")
           || className.startsWith("com.tc.jrexx.") || className.startsWith("org.dom4j.")
           || className.startsWith("org.xml.sax.") || className.startsWith("javax.xml.parsers.")
           || className.startsWith("sun.reflect.Generated"); // issue on J2SE 5 reflection - AW-245
  }

  public static boolean isDSODependency(final String className) {
    return false;
    // return (className == null) || className.startsWith("DO_NOT_USE.") || className.startsWith("com.tc.")
    // || className.startsWith("org.w3c.dom.") || className.startsWith("org.apache.log4j.")
    // || className.startsWith("org.apache.commons.io.") || className.startsWith("org.apache.commons.lang.")
    // || className.startsWith("org.apache.commons.logging.") || className.startsWith("javax.xml.")
    // || className.startsWith("org.apache.xmlbeans.") || className.startsWith("org.apache.xerces.");
  }

  public static int getSessionLockType(String appName) {
    return gloalContext.getSessionLockType(appName);
  }

  private static void traceNamedLoader(final NamedClassLoader ncl) {
    trace("loader[" + ncl + "] of type[" + ncl.getClass().getName() + "] registered as["
          + ncl.__tc_getClassLoaderName() + "]");
  }

  private static void traceLookup(final ClassLoader cl, final String clazz) {
    trace("loader[" + cl + "] of type[" + cl.getClass().getName() + "] looking for class[" + clazz + "]");
  }

  private static void trace(final String msg) {
    TRACE_STREAM.println("<TRACE> TC classloading: " + msg);
    TRACE_STREAM.flush();
  }

  public static class JarFilter implements FileFilter {
    public boolean accept(File pathname) {
      return pathname.isFile() && pathname.getAbsolutePath().toLowerCase().endsWith(".jar");
    }
  }

  public static class State {
    private final int NOT_INTIALIZED = 0;
    private final int INITIALIZING   = 1;
    private final int INITIALIZED    = 2;
    private int       state          = NOT_INTIALIZED;

    public synchronized boolean attemptInit() {
      if (state == NOT_INTIALIZED) {
        state = INITIALIZING;
        return true;
      }
      return false;
    }

    public synchronized void initialized() {
      if (state != INITIALIZING) { throw new IllegalStateException("State was " + state); }
      state = INITIALIZED;
    }

    public synchronized boolean isInitialized() {
      return state == INITIALIZED;
    }
  }

}
