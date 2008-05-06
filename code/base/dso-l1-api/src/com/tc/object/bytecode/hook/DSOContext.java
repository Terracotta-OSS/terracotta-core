/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.hook;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.object.bytecode.Manager;

import java.net.URL;
import java.util.Collection;

/**
 * The idea behind DSOContext is to encapsulate a DSO "world" in a client VM.  But this 
 * idea has not been fully realized.
 */
public interface DSOContext extends ClassProcessor {

  public static final String CLASS = "com/tc/object/bytecode/hook/DSOContext";
  public static final String TYPE  = "L" + CLASS + ";";

  /**
   * @return The Manager instance
   */
  public Manager getManager();

  /**
   * The DSOSpringConfigHelpers in the DSO
   * @return Collection of DSOSpringConfigHelper
   */
  public Collection getDSOSpringConfigHelpers();

  /**
   * Add include and lock
   * @param expression Class expression
   * @param callConstructorOnLoad True to call constructor on load
   * @param lockExpression Lock expression
   * @param classInfo Class information
   */
  public void addInclude(String expression, boolean callConstructorOnLoad, String lockExpression, ClassInfo classInfo);

  /**
   * Add transient field 
   * @param beanClassName Bean class name
   * @param fieldName
   */
  public void addTransient(String beanClassName, String fieldName);

  /**
   * Get type of locks used by sessions
   * @param appName Web app anem
   * @return Lock type
   */
  public int getSessionLockType(String appName);
  
  /**
   * Get url to class file 
   * @param className Class name
   * @return URL to class itself
   */
  public URL getClassResource(String className);

}
