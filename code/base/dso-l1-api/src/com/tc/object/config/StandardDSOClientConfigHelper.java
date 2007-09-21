/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.object.bytecode.ClassAdapterFactory;

import java.net.URL;

public interface StandardDSOClientConfigHelper {

  public static final String UNSAFE_CLASSADAPTER_FACTORY    = "com.tc.object.bytecode.UnsafeAdapter";

  public static final String DSOUNSAFE_CLASSADAPTER_FACTORY = "com.tc.object.bytecode.DSOUnsafeAdapter";

  // HACK: available only in StandardDSOClientConfigHelper

  void allowCGLIBInstrumentation();

  void addAspectModule(String pattern, String moduleName);

  // HACK: duplicated from DSOApplicationConfig

  void addRoot(String rootName, String rootFieldName);

  void addIncludePattern(String classPattern);

  void addWriteAutolock(String methodPattern);

  void addReadAutolock(String methodPattern);

  void addIncludePattern(String classname, boolean honorTransient);

  void addAutoLockExcludePattern(String expression);

  void addPermanentExcludePattern(String pattern);

  void addNonportablePattern(String pattern);

  LockDefinition createLockDefinition(String name, ConfigLockLevel level);

  void addLock(String methodPattern, LockDefinition lockDefinition);

  // HACK: duplicated from DSOClientConfigHelper

  TransparencyClassSpec getOrCreateSpec(String className);

  TransparencyClassSpec getOrCreateSpec(String className, String applicator);

  void addCustomAdapter(String name, String factoryName);

  void addCustomAdapter(String name, ClassAdapterFactory adapterFactory);

  void addClassReplacement(final String originalClassName, final String replacementClassName,
                           final URL replacementResource);

  void addClassResource(final String className, final URL resource);

  void addIncludePattern(String expression, boolean honorTransient, boolean oldStyleCallConstructorOnLoad,
                         boolean honorVolatile);

  void addAutolock(String methodPattern, ConfigLockLevel type);

}
