/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.loaders;

public interface ClassProvider {

  Class getClassFor(String className, String loaderDesc) throws ClassNotFoundException;

  String getLoaderDescriptionFor(Class clazz);

  ClassLoader getClassLoader(String loaderDesc);

  String getLoaderDescriptionFor(ClassLoader loader);
}
