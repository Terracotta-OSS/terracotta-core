/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

public final class DSOChangeApplicatorSpec implements ChangeApplicatorSpec {
  private final String      changeApplicatorClassName;
  private final ClassLoader classLoader;

  public DSOChangeApplicatorSpec(String changeApplicatorClassName) {
    this(changeApplicatorClassName, null);
  }

  public DSOChangeApplicatorSpec(String changeApplicatorClassName, ClassLoader classLoader) {
    this.changeApplicatorClassName = changeApplicatorClassName;
    this.classLoader = classLoader;
  }

  public final Class getChangeApplicator(Class clazz) {
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

}
