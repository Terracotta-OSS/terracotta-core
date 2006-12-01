/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.loaders;

public interface ClassProvider {

  Class getClassFor(String className, String loaderDesc) throws ClassNotFoundException;

  String getLoaderDescriptionFor(Class clazz);

  ClassLoader getClassLoader(String loaderDesc);

  String getLoaderDescriptionFor(ClassLoader loader);
}
