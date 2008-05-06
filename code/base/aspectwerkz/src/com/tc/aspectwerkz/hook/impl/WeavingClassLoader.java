/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.hook.impl;

import sun.misc.Resource;
import sun.misc.URLClassPath;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Very basic classloader that do online weaving. <p/>This classloader can be used thru several means
 * <ul>
 * <li>as a URLClassLoader in a custom development</li>
 * <li>as a <i>MainClass </i> to allow on the fly weaving (without support for classloader hierarchy)</li>
 * </ul>
 * It can also be used for debugging step by step in any IDE
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 *         TODO rewrite based on SUN src (definePackage missing)
 */
public class WeavingClassLoader extends URLClassLoader {
  public WeavingClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  }

  protected Class findClass(String name) throws ClassNotFoundException {
    String path = name.replace('.', '/').concat(".class");
    Resource res = new URLClassPath(getURLs()).getResource(path, false);
    if (res != null) {
      //definePackage(name.substring(0, name.lastIndexOf(".")), null, null);
      try {
        byte[] b = res.getBytes();
        byte[] transformed = ClassPreProcessorHelper.defineClass0Pre(this, name, b, 0, b.length, null);
        return defineClass(name, transformed, 0, transformed.length);
      } catch (IOException e) {
        throw new ClassNotFoundException(e.getMessage());
      }
    } else {
      throw new ClassNotFoundException(name);
    }
  }

  public static void main(String[] args) throws Exception {
    String path = System.getProperty("java.class.path");
    ArrayList paths = new ArrayList();
    StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
    while (st.hasMoreTokens()) {
      paths.add((new File(st.nextToken())).getCanonicalFile().toURL());
    }

    //System.setProperty("aspectwerkz.transform.verbose", "yes");
    //System.setProperty("aspectwerkz.transform.dump", "*");
    //System.setProperty("aspectwerkz.definition.file", "...");
    // TODO check child of extension classloader instead of boot classloader
    ClassLoader cl = new WeavingClassLoader(
            (URL[]) paths.toArray(new URL[]{}), ClassLoader.getSystemClassLoader()
            .getParent()
    );
    Thread.currentThread().setContextClassLoader(cl);
    String s = args[0];
    String[] args1 = new String[args.length - 1];
    if (args1.length > 0) {
      System.arraycopy(args, 1, args1, 0, args.length - 1);
    }
    Class class1 = Class.forName(s, false, cl);
    Method method = class1.getMethod(
            "main", new Class[]{
            String[].class
    }
    );
    method.invoke(
            null, new Object[]{
            args1
    }
    );
  }
}