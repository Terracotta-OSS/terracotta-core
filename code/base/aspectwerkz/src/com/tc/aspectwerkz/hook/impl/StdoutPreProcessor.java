/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.hook.impl;

import com.tc.aspectwerkz.hook.ClassPreProcessor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A simple implementation of class preprocessor. <p/>It does not modify the bytecode. It just prints on stdout some
 * messages.
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 */
public class StdoutPreProcessor implements ClassPreProcessor {
  /**
   * Classloaders repository, based on a synchronized weak hashmap key = classloader value = List of URL[]
   * representing the local search path for .class files of the classloader value is completed at each class loading
   */
  private static Map classloaders;

  /**
   * ms interval betwee classloader hierarchy printing
   */
  private static final long stepms = 15000;

  private static transient long lastPrinted = 0;

  private void log(String s) {
    System.out.println(Thread.currentThread().getName() + ": StdoutPreProcessor: " + s);
  }

  public void initialize() {
    log("initialize");
    log("loaded by " + this.getClass().getClassLoader());
    classloaders = Collections.synchronizedMap(new WeakHashMap());

    // register the classloader - except bootstrapCL
    registerClassLoader(this.getClass().getClassLoader(), this.getClass().getName());
  }

  public byte[] preProcess(String klass, byte[] abyte, ClassLoader caller) {
    // emulate a -verbose:class mode
    klass = klass.replace('.', '/') + ".class";
    URL u = caller.getResource(klass);
    log("> " + klass + " [" + ((u == null) ? "?" : u.toString()) + "] [" + caller + "]");

    /*
    * URL uRoot = null; if (u!=null) { // u =
    * jar:file:/C:/bea/weblogic81/server/lib/weblogic.jar!/weblogic/t3/srvr/T3Srvr.class // getPath =
    * file:/C:/bea/weblogic81/server/lib/weblogic.jar!/weblogic/t3/srvr/T3Srvr.class // getFile =
    * file:/C:/bea/weblogic81/server/lib/weblogic.jar!/weblogic/t3/srvr/T3Srvr.class int i =
    * u.toString().indexOf('!'); if (i > 0) { try { uRoot = new URL(u.toString().substring(0, i)+"!/"); } catch
    * (MalformedURLException e) { ; } } }
    */
    // register the classloader
    registerClassLoader(caller, klass);

    // complete the url path of the classloader
    registerSearchPath(caller, klass);

    // dump the hierarchy if needed
    if (System.currentTimeMillis() > (lastPrinted + stepms)) {
      lastPrinted = System.currentTimeMillis();
      log("*******************************");
      log("size=" + classloaders.size());
      dumpHierarchy(null, "");
      log("*******************************");
    }
    return abyte;
  }

  /**
   * Register a weak reference on the classloader Looks for META-INF/manifest.mf resource and log a line
   *
   * @param loader
   * @param firstClassLoaded
   */
  private void registerClassLoader(ClassLoader loader, String firstClassLoaded) {
    if (loader != null) {
      if (!classloaders.containsKey(loader)) {
        // register the loader and the parent hierarchy if not already registered
        registerClassLoader(loader.getParent(), loader.getClass().getName());
        registerSearchPath(loader.getParent(), loader.getClass().getName());
        URL u = null;

        // *** THIS IS NOT WORKING for other than .class files
        // *** since manifest.mf can be several time
        // *** but getResource follow parent first delegation model
        // try to locate the first META-INF/manifest.mf (should be the aw.xml)
        // case sensitive of META-INF, meta-inf, Meta-Inf, Meta-inf ? YES IT IS
        //u = loader.getResource("META-INF/MANIFEST.MF");
        // *** THIS could be enough for early registration
        // add resources in some struct, allow multiple deploy of same resource
        // if in parallel CL hierarchy - got it ?
        try {
          //@todo - case sensitive stuff is dangerous
          // we could merge all aw.xml in META-INF and WEB-INF and meta-inf ...
          Enumeration ue = loader.getResources("META-INF/MANIFEST.MF");
          if (ue.hasMoreElements()) {
            log("--- in scope for " + loader);
          }
          while (ue.hasMoreElements()) {
            log("--- " + ue.nextElement().toString());
          }
        } catch (IOException e) {
          ;
        }

        // register this loader
        log("****" + loader + " [" + ((u == null) ? "?" : u.toString()) + "] [" + firstClassLoaded + ']');
        classloaders.put(loader, new ArrayList());
      }

      // register search path based on firstClassLoaded
    }
  }

  /**
   * Dumps on stdout the registered classloader hierarchy child of "parent" Using the depth to track recursivity level
   */
  private void dumpHierarchy(ClassLoader parent, String depth) {
    // do a copy of the registered CL to allow access on classloaders structure
    List cl = new ArrayList(classloaders.keySet());
    ClassLoader current = null;
    for (Iterator i = cl.iterator(); i.hasNext();) {
      current = (ClassLoader) i.next();
      if (current.getParent() == parent) {
        log(depth + current + '[' + classloaders.get(current));

        // handcheck for duplicate path (?)
        List path = (List) classloaders.get(current);
        ClassLoader currentParent = current.getParent();
        while (currentParent != null) {
          for (Iterator us = path.iterator(); us.hasNext();) {
            URL u = (URL) us.next();
            if (((List) classloaders.get(currentParent)).contains(u)) {
              log("!!!! duplicate detected for " + u + " in " + current);
            }
          }
          currentParent = currentParent.getParent();
        }
        dumpHierarchy(current, depth + "  ");
      }
    }
  }

  private void registerSearchPath(final ClassLoader loader, final String klass) {
    // bootCL
    if (loader == null) {
      return;
    }

    // locate the klass
    String klassFile = klass.replace('.', '/') + ".class";
    URL uKlass = loader.getResource(klassFile);
    if (uKlass == null) {
      return;
    }

    // retrieve the location root (jar/zip or directory)
    URL uRoot = null;
    int i = uKlass.toString().indexOf('!');
    if (i > 0) {
      // jar/zip
      try {
        // remove the jar: zip: prefix
        //@todo !! zip: seems to be BEA specific
        uRoot = (new File(uKlass.toString().substring(4, i))).getCanonicalFile().toURL();

        //uRoot = new URL(uKlass.toString().substring(0, i)+"!/");
      } catch (MalformedURLException e) {
        e.printStackTrace();
        return;
      } catch (IOException e2) {
        e2.printStackTrace();
        return;
      }
    } else {
      // directory
      i = uKlass.toString().indexOf(klassFile);
      try {
        uRoot = (new File(uKlass.toString().substring(0, i))).getCanonicalFile().toURL();
      } catch (MalformedURLException e) {
        e.printStackTrace();
        return;
      } catch (IOException e2) {
        e2.printStackTrace();
        return;
      }
    }

    // check if the location is not in a parent
    ClassLoader parent = loader.getParent();
    while (parent != null) {
      if (((List) classloaders.get(parent)).contains(uRoot)) {
        return;
      }
      parent = parent.getParent();
    }

    // add the location if not already registered
    // @todo !! not thread safe
    List path = (List) classloaders.get(loader);
    if (!path.contains(uRoot)) {
      log("adding path " + uRoot + " to " + loader);
      path.add(uRoot);
    }
  }

  public static void main(String[] args) throws Exception {
    URL u = new URL(
            "jar:file:/C:/bea/user_projects/domains/mydomain/myserver/.wlnotdelete/gallery/gallery-rar.jar!/"
    );

    // differ from a "/./"
    URL u2 = new URL(
            "jar:file:/C:/bea/user_projects/domains/mydomain/./myserver/.wlnotdelete/gallery/gallery-rar.jar!/"
    );
    if (u.sameFile(u2)) {
      System.out.println("same");
    } else {
      System.out.println("differ");
    }
  }
}