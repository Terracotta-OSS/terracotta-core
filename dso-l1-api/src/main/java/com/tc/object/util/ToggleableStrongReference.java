/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.util;

public interface ToggleableStrongReference {

  public void strongRef(Object obj);

  public void clearStrongRef();

}
