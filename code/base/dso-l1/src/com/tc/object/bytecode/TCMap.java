/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;


public interface TCMap {

  public void __tc_applicator_put(Object key, Object value);

  public void __tc_applicator_remove(Object key);
}
