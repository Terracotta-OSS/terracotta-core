/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.hibernate_3_1_2.object.config;

import com.tc.hibernate.HibernateProxyInstance;
import com.tc.object.config.ChangeApplicatorSpec;

public class HibernateChangeApplicatorSpec implements ChangeApplicatorSpec {
  private final static String HIBERNATE_PROXY_APPLICATOR_CLASS_NAME   = "org.terracotta.modules.hibernate_3_1_2.object.applicator.HibernateProxyApplicator";

  private final ClassLoader   classLoader;

  public HibernateChangeApplicatorSpec(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public Class getChangeApplicator(Class clazz) {
    String changeApplicatorClassName = null;
    if (isHibernateProxy(clazz)) {
      changeApplicatorClassName = HIBERNATE_PROXY_APPLICATOR_CLASS_NAME;
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

  private static boolean isHibernateProxy(Class clazz) {
    if (HibernateProxyInstance.class.getName().equals(clazz.getName())) { return true; }
    
    Class[] interfaces = clazz.getInterfaces();
    for (int i=0; i<interfaces.length; i++) {
      if (interfaces[i].getName().equals("org.hibernate.proxy.HibernateProxy")) {
        return true;
      }
    }
    return false;

  }}
