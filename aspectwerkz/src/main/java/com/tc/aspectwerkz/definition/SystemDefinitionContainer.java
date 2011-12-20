/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.definition;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.HashSet;
import java.util.List;
import java.io.File;

import com.tc.aspectwerkz.transform.Properties;

/**
 * The SystemDefintionContainer maintains all the definition and is aware of the classloader hierarchy. <p/>A
 * ThreadLocal structure is used during weaving to store current classloader definition hierarchy. <p/>Due to
 * getResources() API, we maintain a perClassLoader loaded resource list so that it contains only resource defined
 * within the classloader and not its parent.
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 * @author Jonas Bon&#233;r
 */
public class SystemDefinitionContainer {

  /**
   * Map of SystemDefinition[List] per ClassLoader.
   * NOTE: null key is supported
   */
  public static final Map s_classLoaderSystemDefinitions = new WeakHashMap();
  /**
   * Map of SystemDefinition[List] per ClassLoader, with the hierarchy structure
   * NOTE: null key is supported
   */
  public static Map s_classLoaderHierarchicalSystemDefinitions = new WeakHashMap();

  /**
   * Map of SystemDefinition location (as URL[List]) per ClassLoader
   * NOTE: null key is supported
   */
  public static final Map s_classLoaderDefinitionLocations = new WeakHashMap();

  /**
   * Default location for default AspectWerkz definition file, JVM wide
   */
  public static final String URL_JVM_OPTION_SYSTEM = System.getProperty(
          "aspectwerkz.definition.file",
          "-Daspectwerkz.definition.file option is not used"
  );

  /**
   * The AOP deployment descriptor for any deployed unit Note: Tomcat 5 does not handles war/META-INF.
   */
  public static final String AOP_META_INF_XML_FILE = "META-INF/aop_xml";

  /**
   * The AOP deployment descriptor for any deployed unit in a webapp.
   */
  public static final String AOP_WEB_INF_XML_FILE = "../aop.xml";

  public static final String WEB_WEB_INF_XML_FILE = "../web.xml";

  /**
   * The UUID of the single AspectWerkz system if only one definition is used.
   */
  public static final String DEFAULT_SYSTEM = "default";
  /**
   * The path to the definition file.
   */
  public static final String DEFINITION_FILE = System.getProperty("aspectwerkz.definition.file", null);
  /**
   * The default name for the definition file.
   */
  public static final String DEFAULT_DEFINITION_FILE_NAME = "aspectwerkz.xml";

  /**
   * An internal flag to disable registration of the -Daspectwerkz.definition.file definition in the System class
   * loader. This is used only in offline mode, where these definitions are registered programmatically at the
   * compilation class loader level.
   */
  private static boolean s_disableSystemWideDefinition = false;

  private static final String VIRTUAL_SYSTEM_ID_PREFIX = "virtual_";

  /**
   * Register a new ClassLoader in the system and gather all its definition and parents definitions.
   *
   * @param loader the class loader to register
   */
  @SuppressWarnings("deprecation")
  private static void registerClassLoader(final ClassLoader loader) {
    synchronized (s_classLoaderSystemDefinitions) {
      if (s_classLoaderSystemDefinitions.containsKey(loader)) {
        return;
      }

      // else - register

      // skip boot classloader and ext classloader
      if (loader == null) {
        // by defaults, there is always the virtual definition, that has lowest precedence
        Set defaults = new HashSet();
        //TODO AVOPT
        defaults.add(SystemDefinition.createVirtualDefinitionAt(loader));
        s_classLoaderSystemDefinitions.put(loader, defaults);
        s_classLoaderDefinitionLocations.put(loader, new ArrayList());

        return;
      }

      // register parents first
      registerClassLoader(loader.getParent());

      // then register -D.. if system classloader and then all META-INF/aop.xml
      try {
        final Set definitions = new HashSet();
        final List locationOfDefinitions = new ArrayList();

        // early registration to avoid recursion
        s_classLoaderSystemDefinitions.put(loader, definitions);
        s_classLoaderDefinitionLocations.put(loader, locationOfDefinitions);

        // is this system classloader ?
        if ((loader == ClassLoader.getSystemClassLoader()) && !s_disableSystemWideDefinition) {
          // -D..file=... sysdef
          definitions.addAll(getDefaultDefinition(loader));
          locationOfDefinitions.add(new File(URL_JVM_OPTION_SYSTEM).toURL());
        }

        // causes dead lock on WLS when invoking getResource(..) - https://jira.terracotta.lan/jira/browse/LKC-2548
        // code is not needed since we never use an aop.xml file anyway
//        if (loader.getResource(WEB_WEB_INF_XML_FILE) != null) {
//          Enumeration webres = loader.getResources(AOP_WEB_INF_XML_FILE);
//          while (webres.hasMoreElements()) {
//            URL def = (URL) webres.nextElement();
//            if (isDefinedBy(loader, def)) {
//              ;
//            } else {
//              definitions.addAll(XmlParser.parseNoCache(loader, def));
//              locationOfDefinitions.add(def);
//            }
//          }
//        }
//        Enumeration res = loader.getResources(AOP_META_INF_XML_FILE);
//        while (res.hasMoreElements()) {
//          URL def = (URL) res.nextElement();
//          if (isDefinedBy(loader, def)) {
//            ;
//          } else {
//            definitions.addAll(XmlParser.parseNoCache(loader, def));
//            locationOfDefinitions.add(def);
//          }
//        }

        // there is always the virtual definition, that has lowest precedence
        SystemDefinition virtualDef = SystemDefinitionContainer.getVirtualDefinitionFor(loader);
        if (virtualDef == null) {
          definitions.add(SystemDefinition.createVirtualDefinitionAt(loader));
        }

        printDeploymentInfoFor(loader);
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
  }

  /**
   * Hotdeploy a list of SystemDefintions as defined at the level of the given ClassLoader
   * <p/>
   * Note: this is used for Offline mode.
   *
   * @param loader      ClassLoader
   * @param definitions SystemDefinitions list
   */
  public static void deployDefinitions(final ClassLoader loader, final Set definitions) {
    synchronized (s_classLoaderSystemDefinitions) {

      // make sure the classloader is known
      registerClassLoader(loader);

      //unchanged: s_classLoaderDefinitionLocations

      // propagate change by flushing hierarchical cache in all childs
      flushHierarchicalSystemDefinitionsBelow(loader);

      // update
      Set defs = (Set) s_classLoaderSystemDefinitions.get(loader);
      defs.addAll(definitions);
      printDeploymentInfoFor(loader);
    }
  }

  /**
   * Lookup for a given SystemDefinition by uuid within a given ClassLoader.
   * <p/>
   * The lookup does go thru the ClassLoader hierarchy
   *
   * @param loader ClassLoader
   * @param uuid   system uuid
   * @return SystemDefinition or null if no such defined definition
   */
  public static SystemDefinition getDefinitionFor(final ClassLoader loader, final String uuid) {
    for (Iterator defs = getDefinitionsFor(loader).iterator(); defs.hasNext();) {
      SystemDefinition def = (SystemDefinition) defs.next();
      if (def.getUuid().equals(uuid)) {
        return def;
      }
    }
    return null;
  }

  /**
   * Return the list of SystemDefinitions visible at the given ClassLoader level.
   * <p/>
   * It does handle the ClassLoader hierarchy.
   *
   * @param loader
   * @return SystemDefinitions list
   */
  public static Set getDefinitionsFor(final ClassLoader loader) {
    return getHierarchicalDefinitionsFor(loader);
  }

  /**
   * Return the list of SystemDefinitions defined at the given ClassLoader level.
   * <p/>
   * It does NOT handle the ClassLoader hierarchy.
   *
   * @param loader
   * @return SystemDefinitions list
   */
  public static Set getAllDefinitionsFor(final ClassLoader loader) {
    // make sure the classloader is registered
    registerClassLoader(loader);
    return (Set) s_classLoaderSystemDefinitions.get(loader);
  }

  /**
   * Returns the virtual system for the class loader specified.
   * <p/>
   * There is ONE and ONLY ONE virtual system per classloader ie several per classloader
   * hierachy. This definition hosts hotdeployed aspects. This method returns the
   * one corresponding to the given classloader only.
   *
   * @param loader the class loader
   * @return the virtual system
   */
  public static SystemDefinition getVirtualDefinitionFor(final ClassLoader loader) {
    // since virtual uuid is mapped to a classloader, a direct lookup on uuid is enough
    return getDefinitionFor(loader, getVirtualDefinitionUuidFor(loader));
  }

  /**
   * Returns the uuid for the virtual system definition for the given classloader
   *
   * @param loader
   * @return uuit
   */
  public static String getVirtualDefinitionUuidFor(final ClassLoader loader) {
    // handle bootclassloader with care
    int hash = loader == null ? 0 : loader.hashCode();
    StringBuffer sb = new StringBuffer(VIRTUAL_SYSTEM_ID_PREFIX);
    return sb.append(hash).toString();
  }

  /**
   * Turns on the option to avoid -Daspectwerkz.definition.file handling.
   */
  public static void disableSystemWideDefinition() {
    synchronized (s_classLoaderSystemDefinitions) {
      s_disableSystemWideDefinition = true;
    }
  }

  /**
   * Pretty printDeploymentInfo a classloader
   *
   * @param loader
   */
  public static void printDeploymentInfoFor(final ClassLoader loader) {
    if (!Properties.PRINT_DEPLOYMENT_INFO) {
      return;
    }

    StringBuffer dump = new StringBuffer("******************************************************************");
    dump.append("\n* class loader = ");

    //Note: Tomcat classLoader.toString is too verbose so we allow 120 chars.
    if ((loader != null) && (loader.toString().length() < 120)) {
      dump.append(loader.toString()).append("@").append(loader.hashCode());
    } else if (loader != null) {
      dump.append(loader.getClass().getName()).append("@").append(loader.hashCode());
    } else {
      dump.append("null");
    }

    Set defs = (Set) s_classLoaderSystemDefinitions.get(loader);
    for (Iterator it = defs.iterator(); it.hasNext();) {
      SystemDefinition def = (SystemDefinition) it.next();
      Collection aspects = def.getAspectDefinitions();
      dump.append("\n* system id = ").append(def.getUuid());
      dump.append(", ").append(aspects.size()).append(" aspects.");
      for (Iterator it2 = aspects.iterator(); it2.hasNext();) {
        AspectDefinition aspect = (AspectDefinition) it2.next();
        dump.append("\n*    aspect: " + aspect.getClassName());
      }
    }
    for (Iterator it = ((List) s_classLoaderDefinitionLocations.get(loader)).iterator(); it.hasNext();) {
      dump.append("\n* ").append(it.next());
    }
    dump.append("\n******************************************************************");
    System.out.println(dump.toString());
  }

  /**
   * Returns true if the given classloader is a child of the given parent classloader
   *
   * @param loader
   * @param parentLoader
   * @return bool
   */
  public static boolean isChildOf(final ClassLoader loader, final ClassLoader parentLoader) {
    if (loader == null) {
      if (parentLoader == null) {
        return true;
      } else {
        return false;
      }
    } else if (loader.equals(parentLoader)) {
      return true;
    } else {
      return isChildOf(loader.getParent(), parentLoader);
    }
  }


  /**
   * Returns the gathered SystemDefinition visible from a classloader.
   * <p/>
   * This method is using a cache. Caution when
   * modifying this method since when an aop.xml is loaded, the aspect classes gets loaded as well, which triggers
   * this cache, while the system is in fact not yet initialized properly. </p>
   *
   * @param loader
   * @return set with the system definitions
   */
  private static Set getHierarchicalDefinitionsFor(final ClassLoader loader) {
    synchronized (s_classLoaderSystemDefinitions) {
      // make sure the classloader is known
      registerClassLoader(loader);

      Set defs = new HashSet();
      // put it in the cache now since this method is recursive
      s_classLoaderHierarchicalSystemDefinitions.put(loader, defs);
      if (loader == null) {
        // go on to put in the cache at the end
      } else {
        ClassLoader parent = loader.getParent();
        defs.addAll(getHierarchicalDefinitionsFor(parent));
      }
      defs.addAll((Set) s_classLoaderSystemDefinitions.get(loader));

      return defs;
    }
  }

  /**
   * Check if a given resource has already been registered to a classloader and its parent hierachy
   *
   * @param loader the classloader which might define the resource
   * @param def    the resource
   * @return true if classloader or its parent defines the resource
   * @TODO what if child shares parent path?
   * @TODO What happens with smylinking and xml in jars etc ?
   * @TODO Needs test
   * @TODO No need for the s_ map
   * @TODO KICK the def map and crawl up the CL parents and redo a getResources check instead
   */
  private static boolean isDefinedBy(final ClassLoader loader, final URL def) {
    if (loader == null) {
      return false;
    }
    ArrayList defLocation = (ArrayList) s_classLoaderDefinitionLocations.get(loader);
    if (defLocation != null) {
      for (Iterator it = defLocation.iterator(); it.hasNext();) {
        URL definedDef = (URL) it.next();
        if (definedDef.sameFile(def)) {
          return true;
        }
      }
    }
    return isDefinedBy(loader.getParent(), def);
  }

  private static void flushHierarchicalSystemDefinitionsBelow(final ClassLoader loader) {
    Map classLoaderHierarchicalSystemDefinitions = new WeakHashMap();
    for (Iterator iterator = s_classLoaderHierarchicalSystemDefinitions.entrySet().iterator(); iterator.hasNext();) {
      Map.Entry entry = (Map.Entry) iterator.next();
      ClassLoader currentLoader = (ClassLoader) entry.getKey();
      if (isChildOf(currentLoader, loader)) {
        // flushed
      } else {
        classLoaderHierarchicalSystemDefinitions.put(currentLoader, entry.getValue());
      }
    }
    s_classLoaderHierarchicalSystemDefinitions = classLoaderHierarchicalSystemDefinitions;
  }

  /**
   * Returns the default defintion.
   *
   * @param loader
   * @return the default defintion
   */
  @SuppressWarnings("deprecation")
  public static Set getDefaultDefinition(final ClassLoader loader) {
    if (DEFINITION_FILE != null) {
      File file = new File(DEFINITION_FILE);
      if (file.canRead()) {
        try {
          return XmlParser.parseNoCache(loader, file.toURL());
        } catch (MalformedURLException e) {
          System.err.println("<WARN> Cannot read " + DEFINITION_FILE);
          e.printStackTrace();
        }
      } else {
        System.err.println("<WARN> Cannot read " + DEFINITION_FILE);
      }
    }
    return new HashSet();
  }

}
