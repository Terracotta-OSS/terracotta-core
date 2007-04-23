/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.hibernate_3_1_2.object.config;

import org.terracotta.modules.hibernate_3_1_2.object.dna.impl.HibernateProxyInstance;

import com.tc.object.config.ChangeApplicatorSpec;
import com.tc.object.config.ModuleSpec;

public class HibernateModuleSpec implements ModuleSpec {
  private final ChangeApplicatorSpec changeAppSpec;
  
  public HibernateModuleSpec(ChangeApplicatorSpec changeAppSpec) {
    this.changeAppSpec = changeAppSpec;
  }
  
  public ChangeApplicatorSpec getChangeApplicatorSpec() {
    return this.changeAppSpec;
  }

  public Class getPeerClass(Class clazz) {
    if (isHibernateProxy(clazz)) {
      return HibernateProxyInstance.class;
    }
    return clazz;
  }

  public boolean isUseNonDefaultConstructor(Class clazz) {
    return isHibernateProxy(clazz);
  }
  
  public boolean isPortableClass(Class clazz) {
    return isHibernateProxy(clazz);
  }
  
  private static boolean isHibernateProxy(Class clazz) {
    if (HibernateProxyInstance.class.getName().equals(clazz.getName())) { return true; }
    
    Class[] interfaces = clazz.getInterfaces();
    for (int i=0; i<interfaces.length; i++) {
      if (interfaces[i].getName().equals("org.hibernate.proxy.HibernateProxy")) {
        return true;
      }
    }
    return false;

  }

}
