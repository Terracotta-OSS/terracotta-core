/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.object.logging.RuntimeLogger;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Standard ClassProvider, using named classloaders and aware of boot, extension, and system classloaders.
 */
public class StandardClassProvider implements ClassProvider {

  private static final String BOOT    = Namespace.getStandardBootstrapLoaderName();
  // private static final String EXT     = Namespace.getStandardExtensionsLoaderName();
  // private static final String SYSTEM  = Namespace.getStandardSystemLoaderName();
  
  private static final LoaderDescription BOOT_DESC = new LoaderDescription(null, BOOT);

  // The following four maps need to be maintained in synchrony.  We achieve this
  // by synchronizing on 'this' all methods that access them.
  
  /** map loader name to loader */
  private final Map<String, WeakReference<NamedClassLoader>> loaders = 
    new HashMap<String, WeakReference<NamedClassLoader>>();
  
  /** map loader name to LoaderDescription */
  private final Map<String, LoaderDescription> loaderDescriptions =
    new HashMap<String, LoaderDescription>();
  
  /** map an appGroup to set of loader descriptions in it */
  private final Map<String, Set<String>> appGroups = new HashMap<String, Set<String>>();
  
  /** 
   * All registered loaders have an entry in this map. If the loader has exactly one child
   * (in terms of {@link ClassLoader#getParent()}) in the same app-group, the value is that child's
   * loader description; if it has zero or more than one child, the value is null.
   */
  private final Map<String, String> uniqueChildInAppGroup = new HashMap<String, String>();
  
  private final RuntimeLogger runtimeLogger;

  public StandardClassProvider(RuntimeLogger runtimeLogger) {
    this.runtimeLogger = runtimeLogger;
  }
  
  public ClassLoader getClassLoader(LoaderDescription desc) {
    final ClassLoader rv = lookupLoader(desc);
    if (rv == null) { 
      throw new IllegalArgumentException("No registered loader for description: " + desc); 
    }
    return rv;
  }

  public Class getClassFor(final String className, LoaderDescription desc) throws ClassNotFoundException {
    final ClassLoader loader = lookupLoader(desc);
    
    if (loader == null) { 
      throw new ClassNotFoundException("No registered loader for description: " + desc
                                       + ", trying to load " + className); 
    }

    // debugging
//    StringBuilder sb = new StringBuilder();
//    sb.append("APPGROUPS: SCP.getClassFor([").append(className);
//    sb.append("], [").append(desc.toString()).append("]) -> loader [");
//    sb.append(((NamedClassLoader)loader).__tc_getClassLoaderName()).append("]");
//    System.out.println(sb.toString());
    
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
   * @param appGroup an appGroup to support sharing roots between apps, or null if
   * no sharing is desired. The empty string will be replaced with null.
   */
  public void registerNamedLoader(NamedClassLoader loader, String appGroup) {
    final String name = getName(loader);
    
    final WeakReference<NamedClassLoader> prevRef;
    
    // Ensure postcondition that we never have an empty string in appGroup or name
    if (null == name || "".equals(name)) {
      throw new IllegalArgumentException("Loader name must be non-empty");
    }
    if ("".equals(appGroup)) {
      appGroup = null;
    }

    synchronized (this) {
      prevRef = loaders.put(name, new WeakReference<NamedClassLoader>(loader));
      loaderDescriptions.put(name, new LoaderDescription(appGroup, name));
      
      if (appGroup != null && appGroup.length() > 0) {
        Set<String> descs = appGroups.get(appGroup);
        if (descs == null) {
          descs = new HashSet<String>();
          appGroups.put(appGroup, descs);
        }
        descs.add(name);
        
        // Adding a loader to an app group could change any child relationships in the group
        updateAllChildRelationships(appGroup);
      } else {
        uniqueChildInAppGroup.put(name, null);
      }
      
    }

    NamedClassLoader prev = prevRef == null ? null : (NamedClassLoader) prevRef.get();

    if (runtimeLogger.getNamedLoaderDebug()) {
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

  private boolean isStandardLoader(String desc) {
    // EXT and SYSTEM get registered at startup like normal loaders; no need to special-case
    return BOOT.equals(desc); // || EXT.equals(desc) || SYSTEM.equals(desc);
  }

  private ClassLoader lookupLoader(LoaderDescription desc) {
    if (isStandardLoader(desc.name())) {
      return SystemLoaderHolder.loader;
    } else {
      return lookupLoaderWithAppGroup(desc);
    }
  }

  /**
   * Look up a loader by name alone, ignoring app-group.
   * @return null if the requested loader has not been registered.
   */
  private synchronized ClassLoader lookupLoaderByName(String name) {
    final ClassLoader rv;
    WeakReference ref = loaders.get(name);
    if (ref != null) {
      rv = (ClassLoader) ref.get();
      if (rv == null) {
        loaders.remove(name);
        loaderDescriptions.remove(name);
        // TODO: remove from appGroups and update uniqueChildInAppGroup 
      }
    } else {
      rv = null;
    }
    return rv;
  }
  
  /**
   * Look up a loader by name, falling back to other loaders in the same
   * app-group if necessary.
   */
  private ClassLoader lookupLoaderWithAppGroup(LoaderDescription desc) {
    // if (the DNA specifies an app-group, 
    //     and there is a loader that exactly matches both the app-group and the name, 
    //     and there is exactly one loader registered in that app-group that is a *child* of the exact match) { 
    //   use the child; 
    // } 
    Set<String> appGroupLoaders = null;
    if (desc.appGroup() != null) {
      appGroupLoaders = appGroups.get(desc.appGroup());
      if (appGroupLoaders != null && appGroupLoaders.contains(desc.name())) {
        String child = uniqueChildInAppGroup.get(desc.name());
        if (child != null) {
          return lookupLoaderByName(child);
        }
      }
    }
    
    // else if (there is a loader that matches the registered name) { 
    //   use it; 
    // } 
    ClassLoader namedLoader = lookupLoaderByName(desc.name());
    if (namedLoader != null) {
      return namedLoader;
    }
    
    // else if (the DNA specifies an app-group 
    //     and there is exactly one loader that matches the app-group) { 
    //   use it; 
    // } 
    if (appGroupLoaders != null && appGroupLoaders.size() == 1) {
      return lookupLoaderByName(appGroupLoaders.iterator().next());
    }
    
    return null;

  }

  /**
   * A new loader has been added to the app group, so update the uniqueChildInAppGroup
   * map for every loader in the app group. For each loader, if it has exactly one
   * loader also in the app group that is its child, enter that in the map; otherwise
   * enter a null.
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
        loaderToChildren.put((NamedClassLoader)loader, new HashSet<NamedClassLoader>());
      }
    }
    
    // For each loader in the appgroup, find any parents also in the group, and add it as a child 
    for (NamedClassLoader loader : loaderToChildren.keySet()) {
      ClassLoader parent = ((ClassLoader)loader).getParent();
      while (parent != null) {
        Set<NamedClassLoader> children = loaderToChildren.get(parent);
        if (children != null) {
          children.add(loader);
        }
        parent = parent.getParent();
      }
    }
    
    // Update the uniqueChildInAppGroup map
    for (Map.Entry<NamedClassLoader, Set<NamedClassLoader>> entry : loaderToChildren.entrySet()) {
      String desc = getName(entry.getKey());
      Set<NamedClassLoader> children = entry.getValue();
      if (children.size() == 1) {
        String childName = getName(children.iterator().next());
        uniqueChildInAppGroup.put(desc, childName);
      } else {
        uniqueChildInAppGroup.put(desc, null);
      }
    }
  }

  public static class SystemLoaderHolder {
    final static ClassLoader loader = ClassLoader.getSystemClassLoader();
  }

}
