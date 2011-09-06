/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.util;

public interface ToggleableStrongReference {

  public void strongRef(Object obj);

  public void clearStrongRef();

}
