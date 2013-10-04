/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;


public interface Portability {

  public boolean isPortableClass(Class clazz);

  public boolean isPortableInstance(Object obj);

}
