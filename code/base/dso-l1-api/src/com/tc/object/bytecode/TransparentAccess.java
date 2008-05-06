/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import java.util.Map;

/**
 * This interface is used to allow *physically* managed objects to be read and updated
 */
public interface TransparentAccess {

  public void __tc_getallfields(Map map);

  public void __tc_setfield(String name, Object value);

  // These two methods are called from the TC instrumented version of the
  // reflection classes (java.lang.reflect.Field, etc)
  public Object __tc_getmanagedfield(String name);

  public void __tc_setmanagedfield(String name, Object value);
}
