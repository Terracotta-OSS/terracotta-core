/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.hook;

import com.tc.object.bytecode.Manager;

import java.util.Collection;

public interface DSOContext extends ClassProcessor {

  public static final String CLASS = "com/tc/object/bytecode/hook/DSOContext";
  public static final String TYPE  = "L" + CLASS + ";";

  public Manager getManager();

  public Collection getDSOSpringConfigHelpers();

  public void addInclude(String expression, boolean callConstructorOnLoad, String lockExpression);

  public void addTransient(String beanClassName, String fieldName);

  public int getSessionLockType(String appName);

}
