/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.object.logging.RuntimeLogger;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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

  public StandardClassProvider(final RuntimeLogger runtimeLogger) {
    this.runtimeLogger = runtimeLogger;
  }

  public ClassLoader getClassLoader(final LoaderDescription desc) {
    final ClassLoader rv = lookupLoader(desc);
    if (rv == null) { throw new IllegalArgumentException("No registered loader for description: " + desc); }
    return rv;
  }

  public Class getClassFor(final String className, final LoaderDescription desc) throws ClassNotFoundException {
    final ClassLoader loader = lookupLoader(desc);

    if (loader == null) { throw new ClassNotFoundException(
                                                           "Detected different clustered applications trying to share the same Terracotta root. "
                                                               + "See the \"/app-groups\" section in the Terracotta Configuration Guide and Reference "
                                                               + "(http://www.terracotta.org/kit/reflector?kitID="
                                                               + ProductInfo.getInstance().kitID()
                                                               + "&pageID=ConfigGuideAndRef) for more "
                                                               + "information on how to configure application groups."); }

    // debugging
    // StringBuilder sb = new StringBuilder();
    // sb.append("APPGROUPS: SCP.getClassFor([").append(className);
    // sb.append("], [").append(desc.toString()).append("]) -> loader [");
    // sb.append(((NamedClassLoader)loader).__tc_getClassLoaderName()).append("]");
    // System.out.println(sb.toString());

    try {
      return Class.forName(className, false, loader);
    } catch (final ClassNotFoundException e) {
      if (loader instanceof BytecodeProvider) {
        final BytecodeProvider provider = (BytecodeProvider) loader;
        final byte[] bytes = provider.__tc_getBytecodeForClass(className);
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
  public void registerNamedLoader(final NamedClassLoader loader, String appGroup) {
    final String name = getName(loader);

    final WeakReference<NamedClassLoader> prevRef;

    // Ensure postcondition that we never have an empty string in appGroup or name
    if (null == name || "".equals(name)) { throw new IllegalArgumentException("Loader name must be non-empty"); }
    if ("".equals(appGroup)) {
      appGroup = null;
    }

    synchronized (this) {
      prevRef = this.loaders.put(name, new WeakReference<NamedClassLoader>(loader));
      this.loaderDescriptions.put(name, new LoaderDescription(appGroup, name));

      if (appGroup != null) {
        Set<String> descs = this.appGroups.get(appGroup);
        if (descs == null) {
          descs = new HashSet<String>();
          this.appGroups.put(appGroup, descs);
        }
        descs.add(name);

        // Adding a loader to an app group could change any child relationships in the group
        updateAllChildRelationships(appGroup);
      } else {
        this.loaderChildren.put(name, Collections.EMPTY_SET);

        if (ISOLATION.equals(name)) {
          this.isolationClassLoader = loader;
        }
      }

    }

    final NamedClassLoader prev = prevRef == null ? null : (NamedClassLoader) prevRef.get();

    if (this.runtimeLogger.getNamedLoaderDebug()) {
      this.runtimeLogger.namedLoaderRegistered(loader, name, appGroup, prev);
    }
  }

  public LoaderDescription getLoaderDescriptionFor(final Class clazz) {
    return getLoaderDescriptionFor(clazz.getClassLoader());
  }

  public LoaderDescription getLoaderDescriptionFor(final ClassLoader loader) {
    if (loader == null) { return BOOT_DESC; }
    if (loader instanceof NamedClassLoader) {
      final String name = getName((NamedClassLoader) loader);
      return this.loaderDescriptions.get(name);
    }
    throw handleMissingLoader(loader);
  }

  private static String getName(final NamedClassLoader loader) {
    final String name = loader.__tc_getClassLoaderName();
    if (name == null || name.length() == 0) { throw new AssertionError("Invalid name [" + name + "] from loader "
                                                                       + loader); }
    return name;
  }

  private RuntimeException handleMissingLoader(final ClassLoader loader) {
    if ("org.apache.jasper.servlet.JasperLoader".equals(loader.getClass().getName())) {
      // try to give a better error message if you're trying to share a JSP
      return new RuntimeException("JSP instances (and inner classes there of) cannot be distributed, loader = "
                                  + loader);
    }
    return new RuntimeException("No loader description for " + loader);
  }

  private boolean isBootLoader(final String desc) {
    // EXT and SYSTEM get registered at startup like normal loaders; no need to special-case
    return BOOT.equals(desc); // || EXT.equals(desc) || SYSTEM.equals(desc);
  }

  private ClassLoader lookupLoader(final LoaderDescription desc) {
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
  private ClassLoader lookupLoaderWithAppGroup(final LoaderDescription desc) {
    // Testing support: allow substitution of IsolationClassLoader for system classloader,
    // unless they were explicitly registered within app-groups.
    if (SYSTEM.equals(desc.name()) && null == desc.appGroup()) {
      if (null != this.isolationClassLoader) { return (ClassLoader) this.isolationClassLoader; }
    } else if (ISOLATION.equals(desc.name()) && null == desc.appGroup()
               && null == this.loaderDescriptions.get(desc.name())) { return SystemLoaderHolder.loader; }

    while (true) {
      ClassLoader loader;

      // if (the DNA specifies an app-group,
      // and there is a loader that exactly matches both the app-group and the name,
      // and there is exactly one loader registered in that app-group that is a *child* of the exact match) {
      // use the child;
      // }
      Set<String> appGroupLoaders = null;
      if (desc.appGroup() != null) {
        appGroupLoaders = this.appGroups.get(desc.appGroup());
        if (appGroupLoaders != null && appGroupLoaders.contains(desc.name())) {
          final Set<String> children = this.loaderChildren.get(desc.name());
          // Clean up GC'ed children before deciding that there is exactly one
          ClassLoader firstChild = null;
          boolean exactlyOne = false;
          for (final String child : children) {
            loader = lookupLoaderByName(child);
            Assert.assertNotNull(loader); // invariant: loaderChildren only contains valid loader names
            if (loader != REMOVED) {
              if (firstChild == null) {
                // keep a ref so it doesn't get GC'ed before we come back to it
                firstChild = loader;
                exactlyOne = true;
              } else {
                // no point in looking further; there are at least two non-GC'ed children
                exactlyOne = false;
                break;
              }
            }
          }
          if (exactlyOne) { return firstChild; }

          // there might not be an observable parent/child relationship. If there is exactly one loader in the app-grpip
          // that is not a "standard" loader, select it
          final Set<String> copy = new HashSet<String>(appGroupLoaders);
          for (final Iterator<String> iter = copy.iterator(); iter.hasNext();) {
            final String name = iter.next();
            if (name.startsWith(Namespace.STANDARD_NAMESPACE)) {
              iter.remove();
            }
          }
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
      if (loader != null) { return loader; }

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
  private void updateAllChildRelationships(final String appGroup) {
    final Set<String> descs = this.appGroups.get(appGroup);
    final Map<NamedClassLoader, Set<NamedClassLoader>> loaderToChildren = new HashMap<NamedClassLoader, Set<NamedClassLoader>>();

    // For each loader in the appgroup, add an empty set to loaderToChildren.
    // This way childToParents.keys() identifies all the loaders in the app group.
    for (final String desc : descs) {
      final ClassLoader loader = lookupLoaderByName(desc);
      if (loader != null) {
        loaderToChildren.put((NamedClassLoader) loader, new HashSet<NamedClassLoader>());
      }
    }

    // For each loader in the appgroup, find any parents also in the group, and add it as a child
    for (final NamedClassLoader loader : loaderToChildren.keySet()) {
      ClassLoader parent = ((ClassLoader) loader).getParent();
      while (parent != null) {
        final Set<NamedClassLoader> children = loaderToChildren.get(parent);
        if (children != null) {
          children.add(loader);
        }
        parent = parent.getParent();
      }
    }

    // Update the loaderChildren map
    for (final Map.Entry<NamedClassLoader, Set<NamedClassLoader>> entry : loaderToChildren.entrySet()) {
      final String desc = getName(entry.getKey());
      final Set<NamedClassLoader> children = entry.getValue();
      final Set<String> childrenNames = new HashSet<String>();
      for (final NamedClassLoader child : children) {
        childrenNames.add(getName(child));
      }
      this.loaderChildren.put(desc, childrenNames);
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
  private ClassLoader lookupLoaderByName(final String name) {
    final ClassLoader rv;
    final WeakReference<NamedClassLoader> ref = this.loaders.get(name);
    if (ref != null) {
      rv = (ClassLoader) ref.get();
      if (rv == null) {
        removeFromMaps(name);
        return REMOVED;
      }
    } else {
      rv = null;
    }
    return rv;
  }

  /**
   * A previously registered loader has been released (a WeakReference to it has been found to contain null); so remove
   * all other traces to it.
   * <p>
   * This method must be externally synchronized on 'this'.
   */
  private void removeFromMaps(final String name) {
    this.loaders.remove(name);
    this.loaderDescriptions.remove(name);
    this.loaderChildren.remove(name);
    for (final Entry<String, Set<String>> entry : this.appGroups.entrySet()) {
      final Set<String> names = entry.getValue();
      final boolean removed = names.remove(name);
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
    if (this.loaderDescriptions.size() != this.loaders.size()) { return "Map sizes differ: loaderDescriptions.size() = "
                                                                        + this.loaderDescriptions.size()
                                                                        + ", loaders.size() = " + this.loaders.size(); }
    if (this.loaderChildren.size() != this.loaders.size()) { return "Map sizes differ: loaderChildren.size() = "
                                                                    + this.loaderChildren.size()
                                                                    + ", loaders.size() = " + this.loaders.size(); }

    // Every entry in every value of loaderChildren should be the name of another loader
    for (final Entry<String, Set<String>> entry : this.loaderChildren.entrySet()) {
      final Set<String> children = entry.getValue();
      for (final String child : children) {
        if (!this.loaders.containsKey(child)) { return "loaderChildren[" + entry.getKey()
                                                       + "] contains unrecognized child [" + child + "]"; }
      }
    }

    // Every value of appGroups should be a Set<String>, and every value in the set should
    // be the name of another loader
    for (final Entry<String, Set<String>> entry : this.appGroups.entrySet()) {
      final Set<String> loadersInGroup = entry.getValue();
      if (loadersInGroup == null) { return "appGroups[" + entry.getKey() + "] contained a null value"; }
      for (final String name : loadersInGroup) {
        if (!this.loaders.containsKey(name)) { return "appGroups[" + entry.getKey()
                                                      + "] pointed to unrecognized name [" + name + "]"; }
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
    for (final WeakReference<NamedClassLoader> ref : this.loaders.values()) {
      if (null == ref.get()) { return true; }
    }
    return false;
  }

}
