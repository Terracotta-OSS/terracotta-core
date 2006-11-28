/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.bytecode.hook;

public interface ClassPostProcessor {

  public void postProcess(Class clazz, ClassLoader caller);

}
