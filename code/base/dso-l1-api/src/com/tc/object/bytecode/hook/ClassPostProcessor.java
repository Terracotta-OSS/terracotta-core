/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.hook;

public interface ClassPostProcessor {

  public void postProcess(Class clazz, ClassLoader caller);

}
