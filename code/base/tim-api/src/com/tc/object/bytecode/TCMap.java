/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import java.util.Collection;

public interface TCMap {

  public void __tc_applicator_put(Object key, Object value);

  public void __tc_applicator_remove(Object key);

  public void __tc_applicator_clear();

  public void __tc_remove_logical(Object key);

  public void __tc_put_logical(Object key, Object value);

  public Collection __tc_getAllLocalEntriesSnapshot();

  public Collection __tc_getAllEntriesSnapshot();
}
