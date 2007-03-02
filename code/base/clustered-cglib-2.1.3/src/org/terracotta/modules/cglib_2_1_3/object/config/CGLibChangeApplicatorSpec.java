/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.cglib_2_1_3.object.config;

import org.terracotta.modules.cglib_2_1_3.object.dna.impl.CGLibBulkBeanInstance;
import org.terracotta.modules.cglib_2_1_3.object.dna.impl.CGLibFactoryInstance;

import com.tc.object.config.ChangeApplicatorSpec;


public class CGLibChangeApplicatorSpec implements ChangeApplicatorSpec {
  private final static String CGLIB_FACTORY_APPLICATOR_CLASS_NAME   = "org.terracotta.modules.cglib_2_1_3.object.applicator.CglibProxyApplicator";
  private final static String CGLIB_BULK_BEAN_APPLICATOR_CLASS_NAME = "org.terracotta.modules.cglib_2_1_3.object.applicator.CglibBulkBeanApplicator";

  private final ClassLoader   classLoader;

  public CGLibChangeApplicatorSpec(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public Class getChangeApplicator(Class clazz) {
    String changeApplicatorClassName = null;
    if (isImplementCglibFactory(clazz)) {
      changeApplicatorClassName = CGLIB_FACTORY_APPLICATOR_CLASS_NAME;
    } else if (isCglibBulkBean(clazz)) {
      changeApplicatorClassName = CGLIB_BULK_BEAN_APPLICATOR_CLASS_NAME;
    }
    if (changeApplicatorClassName != null) {
      try {
        if (classLoader == null) {
          return Class.forName(changeApplicatorClassName);
        } else {
          return Class.forName(changeApplicatorClassName, false, classLoader);
        }
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
    return null;
  }

  private static boolean isImplementCglibFactory(Class clazz) {
    if (CGLibFactoryInstance.class.getName().equals(clazz.getName())) { return true; }
    
    Class[] interfaces = clazz.getInterfaces();
    for (int i = 0; i < interfaces.length; i++) {
      if (interfaces[i].getName().equals("net.sf.cglib.proxy.Factory")) { return true; }
    }
    return false;
  }

  private static boolean isCglibBulkBean(Class clazz) {
    if (CGLibBulkBeanInstance.class.getName().equals(clazz.getName())) { return true; }
    
    Class superClass = clazz.getSuperclass();
    if (superClass == null) { return false; }
    return "net.sf.cglib.beans.BulkBean".equals(superClass.getName());
  }

}
