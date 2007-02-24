/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.plugins.iBatis_2_2_0.object.config;

import org.terracotta.plugins.iBatis_2_2_0.object.dna.impl.IBatisAccessPlanInstance;

import com.tc.object.config.ChangeApplicatorSpec;

public class IBatisChangeApplicatorSpec implements ChangeApplicatorSpec {
  private final static String IBATIS_ACCESS_PLAN_APPLICATOR_CLASS_NAME   = "org.terracotta.plugins.iBatis_2_2_0.object.applicator.IBatisAccessPlanApplicator";

  private final ClassLoader   classLoader;

  public IBatisChangeApplicatorSpec(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public Class getChangeApplicator(Class clazz) {
    String changeApplicatorClassName = null;
    if (isIBatisAccessPlan(clazz)) {
      changeApplicatorClassName = IBATIS_ACCESS_PLAN_APPLICATOR_CLASS_NAME;
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

  public static boolean isIBatisAccessPlan(Class clazz) {
    if (IBatisAccessPlanInstance.class.getName().equals(clazz.getName())) { return true; }
    Class superclass = clazz.getSuperclass();
    if (superclass == null) { return false; }
    if (superclass.getName().equals("com.ibatis.sqlmap.engine.accessplan.BaseAccessPlan")) { return true; }
    return false;

  }
}
