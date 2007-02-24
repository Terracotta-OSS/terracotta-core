/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;


public interface PluginSpec {
  
  public ChangeApplicatorSpec getChangeApplicatorSpec();
  
  public boolean isUseNonDefaultConstructor(Class clazz);
  
  public Class getPeerClass(Class clazz);
  
  public boolean isPortableClass(Class clazz);
}
