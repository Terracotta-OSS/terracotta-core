/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.websphere;

import com.tc.logging.TCLogger;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.loaders.Namespace;

import java.io.File;

public class WebsphereLoaderNaming {

  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[] {};
  private static final Class[]  EMPTY_CLASS_ARRAY  = new Class[] {};

  // loader name prefixes -- use a new one of these for each type of logical loader type in WAS
  private static final String   EAR                = "ear:";
  private static final String   EAR_DEPENDENCY     = "earDep:";

  public synchronized static void nameAndRegisterDependencyLoader(NamedClassLoader loader, Object earFile) {
    nameAndRegister(loader, EAR_DEPENDENCY + getEarName(earFile));
  }

  public static void registerWebAppLoader(NamedClassLoader loader, Object earFile, Object moduleRef) {
    Object webModule = invokeMethod(moduleRef, "getModule");
    Object webModuleFile = invokeMethod(moduleRef, "getModuleFile");
    Object bindings = invokeMethod(webModuleFile, "getBindings");
    String vhost = (String) invokeMethod(bindings, "getVirtualHostName");
    String contextRoot = (String) invokeMethod(webModule, "getContextRoot");

    nameAndRegister(loader, EAR + getEarName(earFile) + ":" + vhost + contextRoot);
  }

  private static void nameAndRegister(NamedClassLoader loader, String name) {
    loader.__tc_setClassLoaderName(Namespace.createLoaderName(Namespace.WEBSPHERE_NAMESPACE, name));
    ClassProcessorHelper.registerGlobalLoader(loader);
  }

  private static String getEarName(Object earFile) {
    String orig = (String) invokeMethod(earFile, "getURI");
    File dir = new File(orig);

    while (true) {
      String ear = dir.getName();

      if (ear == null) {
        RuntimeException re = new RuntimeException("Cannot determine ear name from " + orig);
        getLogger().error(re);
        throw re;
      }

      if (ear.endsWith(".ear")) { return ear; }

      dir = dir.getParentFile();
    }
  }

  private static Object invokeMethod(Object obj, String name) {
    try {
      return obj.getClass().getMethod(name, EMPTY_CLASS_ARRAY).invoke(obj, EMPTY_OBJECT_ARRAY);
    } catch (Exception e) {
      getLogger().error(e);
      throw new RuntimeException(e);
    }
  }

  private static TCLogger getLogger() {
    return ManagerUtil.getLogger(WebsphereLoaderNaming.class.getName());
  }

}
