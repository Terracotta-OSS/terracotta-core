/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.object.bytecode.ClassAdapterFactory;

import java.net.URL;

public interface StandardDSOClientConfigHelper {

  // HACK: available only in StandardDSOClientConfigHelper

  void allowCGLIBInstrumentation();

  // HACK: duplicated from DSOApplicationConfig

  void addRoot(String rootName, String rootFieldName);

  void addIncludePattern(String classPattern);

  void addWriteAutolock(String methodPattern);

  void addReadAutolock(String methodPattern);

  void addIncludePattern(String classname, boolean honorTransient);
  
  void addAutoLockExcludePattern(String expression);

  // HACK: duplicated from DSOClientConfigHelper

  TransparencyClassSpec getOrCreateSpec(String className);

  TransparencyClassSpec getOrCreateSpec(String className, String applicator);

  void addCustomAdapter(String name, ClassAdapterFactory adapterFactory);

  void addClassReplacement(final String originalClassName, final String replacementClassName,
                           final URL replacementResource);

  void addClassResource(final String className, final URL resource);

  void addIncludePattern(String expression, boolean honorTransient, boolean oldStyleCallConstructorOnLoad,
                         boolean honorVolatile);

  void addAutolock(String methodPattern, ConfigLockLevel type);

}
