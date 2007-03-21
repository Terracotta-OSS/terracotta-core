/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.cglib_2_1_3.object.config;

import org.terracotta.modules.cglib_2_1_3.object.dna.impl.CGLibBulkBeanInstance;
import org.terracotta.modules.cglib_2_1_3.object.dna.impl.CGLibFactoryInstance;

import com.tc.object.config.ChangeApplicatorSpec;
import com.tc.object.config.ModuleSpec;


public class CGLibModuleSpec implements ModuleSpec {
  private final ChangeApplicatorSpec changeAppSpec;
  
  public CGLibModuleSpec(ChangeApplicatorSpec changeAppSpec) {
    this.changeAppSpec = changeAppSpec;
  }
  
  public ChangeApplicatorSpec getChangeApplicatorSpec() {
    return this.changeAppSpec;
  }

  public Class getPeerClass(Class clazz) {
    if (isImplementCglibFactory(clazz)) {
      return CGLibFactoryInstance.class;
    } else if (isCglibBulkBean(clazz)) {
      return CGLibBulkBeanInstance.class;
    }
    return clazz;
  }

  public boolean isUseNonDefaultConstructor(Class clazz) {
    return (isImplementCglibFactory(clazz) || isCglibBulkBean(clazz));
  }
  
  public boolean isPortableClass(Class clazz) {
    return (isImplementCglibFactory(clazz) || isCglibBulkBean(clazz));
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
