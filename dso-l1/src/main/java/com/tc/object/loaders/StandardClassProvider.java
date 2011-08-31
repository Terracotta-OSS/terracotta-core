/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.object.logging.RuntimeLogger;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Standard ClassProvider, using named classloaders and aware of boot, extension, and system classloaders.
 */
public class StandardClassProvider implements ClassProvider {

  private static final String                                BOOT               = Namespace
                                                                                    .getStandardBootstrapLoaderName();
  // private static final String EXT = Namespace.getStandardExtensionsLoaderName();
  private static final String                                SYSTEM             = Namespace
                                                                                    .getStandardSystemLoaderName();
  private static final String                                ISOLATION          = Namespace.getIsolationLoaderName();

  private static final LoaderDescription                     BOOT_DESC          = new LoaderDescription(null, BOOT);

  private static final boolean                               DEBUG              = TCPropertiesImpl
                                                                                    .getProperties()
                                                                                    .getBoolean(TCPropertiesConsts.APP_GROUPS_DEBUG);

  // Modifications to the following fields must be done atomically. We achieve this
  // by synchronizing on 'this' all methods that access them.

  /** Maps loader name -> loader */
  private final Map<String, WeakReference<NamedClassLoader>> loaders            = new HashMap<String, WeakReference<NamedClassLoader>>();

  /** Maps loader name -> LoaderDescription */
  private final Map<String, LoaderDescription>               loaderDescriptions = new HashMap<String, LoaderDescription>();

  /** Maps loader name -> set of child loader names. */
  private final Map<String, Set<String>>                     loaderChildren     = new HashMap<String, Set<String>>();

  /** Maps appGroup -> set of loader names */
  private final Map<String, Set<String>>                     appGroups          = new HashMap<String, Set<String>>();

  /**
   * If an IsolationClassLoader has been registered, without an explicit app-group, save it here so it can be
   * substituted for the system classloader.
   */
  private NamedClassLoader                                   isolationClassLoader;

  // END fields requiring atomicity

  private final RuntimeLogger                                runtimeLogger;

  public StandardClassProvider(RuntimeLogger runtimeLogger) {
    this.runtimeLogger = runtimeLogger;
  }

  public ClassLoader getClassLoader(LoaderDescription desc) {
    final ClassLoader rv = lookupLoader(desc);
    if (rv == null) { throw new IllegalArgumentException("No registered loader for description: " + desc); }
    return rv;
  }

  private static void debug(String msg) {
    if (!DEBUG) { throw new AssertionError(); }
    System.err.println("APP_GROUP_DEBUG: [" + Thread.currentThread().getName() + "]: " + msg);
  }

  public Class getClassFor(final String className, LoaderDescription desc) throws ClassNotFoundException {
    final ClassLoader loader = lookupLoader(desc);

    if (loader == null) { throw new ClassNotFoundException(
                                                           "Detected different clustered applications trying to share the same Terracotta root. "
                                                               + "See the \"/app-groups\" section in the Terracotta Configuration Guide and Reference "
                                                               + "(http://www.terracotta.org/kit/reflector?kitID="
                                                               + ProductInfo.getInstance().kitID()
                                                               + "&pageID=ConfigGuideAndRef) for more "
                                                               + "information on how to configure application groups. [class: "
                                                               + className + ", loaderDesc: " + desc + "]"); }

    // debugging
    // StringBuilder sb = new StringBuilder();
    // sb.append("APPGROUPS: SCP.getClassFor([").append(className);
    // sb.append("], [").append(desc.toString()).append("]) -> loader [");
    // sb.append(((NamedClassLoader)loader).__tc_getClassLoaderName()).append("]");
    // System.out.println(sb.toString());

    try {
      return Class.forName(className, false, loader);
    } catch (ClassNotFoundException e) {
      if (loader instanceof BytecodeProvider) {
        BytecodeProvider provider = (BytecodeProvider) loader;
        byte[] bytes = provider.__tc_getBytecodeForClass(className);
        if (bytes != null && bytes.length != 0) { return AsmHelper.defineClass(loader, bytes, className); }
      }
      throw e;
    }
  }

  /**
   * @param loader must implement both ClassLoader and NamedClassLoader
   * @param appGroup an appGroup to support sharing roots between apps, or null if no sharing is desired. The empty
   *        string will be replaced with null.
   */
  public void registerNamedLoader(NamedClassLoader loader, String appGroup) {
    final String name = getName(loader);

    final WeakReference<NamedClassLoader> prevRef;

    // Ensure postcondition that we never have an empty string in appGroup or name
    if (null == name || "".equals(name)) { throw new IllegalArgumentException("Loader name must be non-empty"); }
    if ("".equals(appGroup)) {
      appGroup = null;
    }

    synchronized (this) {
      prevRef = loaders.put(name, new WeakReference<NamedClassLoader>(loader));
      loaderDescriptions.put(name, new LoaderDescription(appGroup, name));

      if (appGroup != null) {
        Set<String> descs = appGroups.get(appGroup);
        if (descs == null) {
          descs = new HashSet<String>();
          appGroups.put(appGroup, descs);
        }
        descs.add(name);

        // Adding a loader to an app group could change any child relationships in the group
        updateAllChildRelationships(appGroup);
      } else {
        loaderChildren.put(name, Collections.EMPTY_SET);

        if (ISOLATION.equals(name)) {
          isolationClassLoader = loader;
        }
      }

    }

    NamedClassLoader prev = prevRef == null ? null : (NamedClassLoader) prevRef.get();

    if (DEBUG || runtimeLogger.getNamedLoaderDebug()) {
      runtimeLogger.namedLoaderRegistered(loader, name, appGroup, prev);
    }
  }

  public LoaderDescription getLoaderDescriptionFor(Class clazz) {
    return getLoaderDescriptionFor(clazz.getClassLoader());
  }

  public LoaderDescription getLoaderDescriptionFor(ClassLoader loader) {
    if (loader == null) { return BOOT_DESC; }
    if (loader instanceof NamedClassLoader) {
      String name = getName((NamedClassLoader) loader);
      return loaderDescriptions.get(name);
    }
    throw handleMissingLoader(loader);
  }

  private static String getName(NamedClassLoader loader) {
    String name = loader.__tc_getClassLoaderName();
    if (name == null || name.length() == 0) { throw new AssertionError("Invalid name [" + name + "] from loader "
                                                                       + loader); }
    return name;
  }

  private RuntimeException handleMissingLoader(ClassLoader loader) {
    if ("org.apache.jasper.servlet.JasperLoader".equals(loader.getClass().getName())) {
      // try to give a better error message if you're trying to share a JSP
      return new RuntimeException("JSP instances (and inner classes there of) cannot be distributed, loader = "
                                  + loader);
    }
    return new RuntimeException("No loader description for " + loader);
  }

  private boolean isBootLoader(String desc) {
    // EXT and SYSTEM get registered at startup like normal loaders; no need to special-case
    return BOOT.equals(desc); // || EXT.equals(desc) || SYSTEM.equals(desc);
  }

  private ClassLoader lookupLoader(LoaderDescription desc) {
    if (isBootLoader(desc.name())) {
      return SystemLoaderHolder.loader;
    } else {
      return lookupLoaderWithAppGroup(desc);
    }
  }

  /**
   * Look up a loader by name, falling back to other loaders in the same app-group if necessary.
   * <p>
   * This method must be externally synchronized on 'this'.
   */
  private ClassLoader lookupLoaderWithAppGroup(LoaderDescription desc) {
    if (DEBUG) debug("Starting lookup for loader description: " + desc);

    // Testing support: allow substitution of IsolationClassLoader for system classloader,
    // unless they were explicitly registered within app-groups.
    if (SYSTEM.equals(desc.name()) && null == desc.appGroup()) {
      if (null != isolationClassLoader) { return (ClassLoader) isolationClassLoader; }
    } else if (ISOLATION.equals(desc.name()) && null == desc.appGroup() && null == loaderDescriptions.get(desc.name())) { return SystemLoaderHolder.loader; }

    while (true) {
      ClassLoader loader;

      Set<String> appGroupLoaders = null;
      if (desc.appGroup() != null) {
        appGroupLoaders = appGroups.get(desc.appGroup());

        if (DEBUG) {
        if (appGroupLoaders != null) {
            debug("Loaders in app-group: " + appGroupLoaders);
          } else {
            debug("Zero loaders present in app-group");
          }
        }

        if (appGroupLoaders != null) {
          // if (the DNA specifies an app-group,
          // and there is a loader that exactly matches both the app-group and the name,
          // and there is exactly one loader registered in that app-group that is a *child* of the exact match) {
          // use the child;
          // }
          if (appGroupLoaders.contains(desc.name())) {
            Set<String> children = loaderChildren.get(desc.name());
            if (DEBUG) debug("loaderChildren: " + children);

            // Clean up GC'ed children before deciding that there is exactly one
            ClassLoader firstChild = null;
            boolean exactlyOne = false;
            for (String child : children) {
              loader = lookupLoaderByName(child);
              Assert.assertNotNull(loader); // invariant: loaderChildren only contains valid loader names
              if (loader != REMOVED) {
                if (firstChild == null) {
                  // keep a ref so it doesn't get GC'ed before we come back to it
                  firstChild = loader;
                  exactlyOne = true;
                  if (DEBUG) debug("Found first possible child: " + child);
                } else {
                  // no point in looking further; there are at least two non-GC'ed children
                  exactlyOne = false;
                  if (DEBUG) debug("Found another child: " + child);
                  break;
                }
              }
            }

            if (exactlyOne) { return firstChild; }
          }

          if (DEBUG) debug("Could not pick/find a child loader");

          // there might not be an observable parent/child relationship. If there is exactly
          // one loader in the app-group that is not a "standard" loader, select it
          Set<String> copy = new HashSet<String>(appGroupLoaders);
          for (Iterator<String> iter = copy.iterator(); iter.hasNext();) {
            String name = iter.next();
            if (name.startsWith(Namespace.STANDARD_NAMESPACE)) {
              iter.remove();
            }
          }

          if (DEBUG) debug("After remove: " + copy);
          if (copy.size() == 1) {
            loader = lookupLoaderByName(copy.iterator().next());
            if (loader == REMOVED) {
              continue;
            }
            return loader;
          }
        }
      }

      // else if (there is a loader that matches the registered name) {
      // use it;
      // }
      loader = lookupLoaderByName(desc.name());
      if (loader == REMOVED) {
        continue;
      }
      if (loader != null) {
        if (DEBUG) debug("Loader found for name " + desc.name() + " is " + loader);
        return loader;
      } else {
        if (DEBUG) debug("No loader found for name: " + desc.name());
      }

      // else if (the DNA specifies an app-group
      // and there is exactly one loader that matches the app-group) {
      // use it;
      // }
      if (appGroupLoaders != null && appGroupLoaders.size() == 1) {
        loader = lookupLoaderByName(appGroupLoaders.iterator().next());
        if (loader == REMOVED) {
          continue;
        }
      }

      return loader;
    }

  }

  /**
   * A new loader has been added to the app group, so update the loaderChildren map for every loader in the app group.
   * For each loader, if it has exactly one loader also in the app group that is its child, enter that in the map;
   * otherwise enter a null.
   * <p>
   * This method must be externally synchronized on 'this'.
   * 
   * @param appGroup must be non-null and non-empty
   */
  private void updateAllChildRelationships(String appGroup) {
    Set<String> descs = appGroups.get(appGroup);
    Map<NamedClassLoader, Set<NamedClassLoader>> loaderToChildren = new HashMap<NamedClassLoader, Set<NamedClassLoader>>();

    // For each loader in the appgroup, add an empty set to loaderToChildren.
    // This way childToParents.keys() identifies all the loaders in the app group.
    for (String desc : descs) {
      ClassLoader loader = lookupLoaderByName(desc);
      if (loader != null) {
        loaderToChildren.put((NamedClassLoader) loader, new HashSet<NamedClassLoader>());
      }
    }

    // For each loader in the appgroup, find any parents also in the group, and add it as a child
    for (NamedClassLoader loader : loaderToChildren.keySet()) {
      ClassLoader parent = ((ClassLoader) loader).getParent();
      while (parent != null) {
        Set<NamedClassLoader> children = loaderToChildren.get(parent);
        if (children != null) {
          children.add(loader);
        }
        parent = parent.getParent();
      }
    }

    // Update the loaderChildren map
    for (Map.Entry<NamedClassLoader, Set<NamedClassLoader>> entry : loaderToChildren.entrySet()) {
      String desc = getName(entry.getKey());
      Set<NamedClassLoader> children = entry.getValue();
      Set<String> childrenNames = new HashSet<String>();
      for (NamedClassLoader child : children) {
        childrenNames.add(getName(child));
      }
      loaderChildren.put(desc, childrenNames);
    }
  }

  private static class RemovedClassLoader extends ClassLoader { /* */
  }

  /** sentinel to indicate that a GC'ed classloader has been removed from maps */
  private static final RemovedClassLoader REMOVED = new RemovedClassLoader();

  /**
   * Look up a loader by name alone, ignoring app-group.
   * <p>
   * This method must be externally synchronized on 'this'.
   * 
   * @return null if the requested loader has not been registered, or {@link #REMOVED} if the requested loader was
   *         registered but has been GC'ed.
   */
  private ClassLoader lookupLoaderByName(String name) {
    final ClassLoader rv;
    WeakReference<NamedClassLoader> ref = loaders.get(name);
    if (ref != null) {
      rv = (ClassLoader) ref.get();
      if (rv == null) {
        removeFromMaps(name);
        return REMOVED;
      }
    } else {
      rv = null;
    }

    if (DEBUG) debug("Lookup for name [" + name + "] returning " + rv);
    return rv;
  }

  /**
   * A previously registered loader has been released (a WeakReference to it has been found to contain null); so remove
   * all other traces to it.
   * <p>
   * This method must be externally synchronized on 'this'.
   */
  private void removeFromMaps(String name) {
    loaders.remove(name);
    loaderDescriptions.remove(name);
    loaderChildren.remove(name);
    for (Entry<String, Set<String>> entry : appGroups.entrySet()) {
      Set<String> names = entry.getValue();
      boolean removed = names.remove(name);
      if (removed) {
        // this loader was in an appGroup, so update that group's uniqueChild status
        updateAllChildRelationships(entry.getKey());
      }
    }
  }

  public static class SystemLoaderHolder {
    final static ClassLoader loader = ClassLoader.getSystemClassLoader();
  }

  /**
   * Verify integrity of data structures (should be used only by test code)
   * 
   * @return null if all is well, or a descriptive string if there is a problem
   */
  synchronized String checkIntegrity() {
    // loaderDescriptions, loaderChildren, and loaders should each have exactly one entry per loader
    if (loaderDescriptions.size() != loaders.size()) { return "Map sizes differ: loaderDescriptions.size() = "
                                                              + loaderDescriptions.size() + ", loaders.size() = "
                                                              + loaders.size(); }
    if (loaderChildren.size() != loaders.size()) { return "Map sizes differ: loaderChildren.size() = "
                                                          + loaderChildren.size() + ", loaders.size() = "
                                                          + loaders.size(); }

    // Every entry in every value of loaderChildren should be the name of another loader
    for (Entry<String, Set<String>> entry : loaderChildren.entrySet()) {
      Set<String> children = entry.getValue();
      for (String child : children) {
        if (!loaders.containsKey(child)) { return "loaderChildren[" + entry.getKey()
                                                  + "] contains unrecognized child [" + child + "]"; }
      }
    }

    // Every value of appGroups should be a Set<String>, and every value in the set should
    // be the name of another loader
    for (Entry<String, Set<String>> entry : appGroups.entrySet()) {
      Set<String> loadersInGroup = entry.getValue();
      if (loadersInGroup == null) { return "appGroups[" + entry.getKey() + "] contained a null value"; }
      for (String name : loadersInGroup) {
        if (!loaders.containsKey(name)) { return "appGroups[" + entry.getKey() + "] pointed to unrecognized name ["
                                                 + name + "]"; }
      }
    }

    return null;
  }

  /**
   * Check to see whether there are any dangling weak references in the loaders map. Used only by test code.
   * 
   * @return true if dangling weak references are found
   */
  synchronized boolean checkWeakReferences() {
    for (WeakReference<NamedClassLoader> ref : loaders.values()) {
      if (null == ref.get()) { return true; }
    }
    return false;
  }

}
