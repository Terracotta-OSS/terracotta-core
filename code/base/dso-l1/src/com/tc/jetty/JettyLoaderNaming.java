/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.jetty;

import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.loaders.Namespace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JettyLoaderNaming {

  public static void nameAndRegisterClasspathLoader(ClassLoader loader) {
    check(loader);
    ((NamedClassLoader) loader).__tc_setClassLoaderName(Namespace.createLoaderName(Namespace.JETTY_NAMESPACE, loader
        .getClass().getName()));
    register((NamedClassLoader) loader);
  }

  public static void nameAndRegisterWebAppLoader(ClassLoader loader) {
    check(loader);

    String contextPath = getContextPath(loader);

    ((NamedClassLoader) loader).__tc_setClassLoaderName(Namespace.createLoaderName(Namespace.JETTY_NAMESPACE,
                                                                                   "path:" + contextPath));
    register((NamedClassLoader) loader);
  }

  private static String getContextPath(ClassLoader loader) {
    try {
      Method getContext = loader.getClass().getMethod("getContext", new Class[] {});
      Object context = getContext.invoke(loader, new Object[] {});

      Method getContextPath = context.getClass().getMethod("getContextPath", new Class[] {});
      return (String) getContextPath.invoke(context, new Object[] {});
    } catch (Throwable t) {
      if (t instanceof InvocationTargetException) {
        t = ((InvocationTargetException) t).getTargetException();
      }

      throw new RuntimeException(t);
    }
  }

  private static void check(ClassLoader loader) {
    if (loader == null) { throw new NullPointerException("loader is null"); }
    if (!(loader instanceof NamedClassLoader)) {
      //
      throw new IllegalArgumentException("Missing NamedClassLoader interface, type " + loader.getClass().getName());
    }
  }

  private static void register(NamedClassLoader loader) {
    ClassProcessorHelper.registerGlobalLoader(loader);
  }
}
