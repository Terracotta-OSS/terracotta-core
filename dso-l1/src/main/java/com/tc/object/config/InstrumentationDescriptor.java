/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.reflect.ClassInfo;

/**
 * Describes the configuration policy for instrumentation of classes.
 *
 * @author orion
 */
interface InstrumentationDescriptor {

  /**
   * Returns the name of the method to call after loading an object from the server, or null if none was defined in
   * config.
   */
  public String getOnLoadMethodIfDefined();

  /**
   * Returns the body of the script to execute after loading an object from the server, or null if none was defined in
   * config.
   */
  public String getOnLoadScriptIfDefined();

  public boolean isCallConstructorOnLoad();

  /**
   * @return true if the class should be instrumented so that the java transient keyword is honored.
   */
  public boolean isHonorTransient();

  /**
   * @return true if the class should be instrumented so that the java volatile keyword is honored.
   */
  public boolean isHonorVolatile();

  /**
   * @return true if the class name matches this descriptor.
   */
  public boolean matches(ClassInfo classInfo);

  /**
   * @return true if this is an explicit include, false otherwise. It is possible for both isInclude() and isExclude()
   *         to return false in the case of the null implementation. This means that it is the default policy and wasn't
   *         explicity defined as an include or an exclude.
   */
  public boolean isInclude();

  /**
   * @return true if this is an explicit exclude, false otherwise. It is possible for both isInclude() and isExclude()
   *         to return false in the case of the null implementation. This means that it is the default policy and wasn't
   *         explicity defined as an include or an exclude.
   */
  public boolean isExclude();

}