/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;
import com.tc.text.PrettyPrintable;

import java.util.Set;

public interface SyncObjectIdSet extends Set, PrettyPrintable {
  public void startPopulating();

  public void stopPopulating(ObjectIDSet fullSet);

  public ObjectIDSet snapshot();

  /**
   * A Slightly optimized methods to do add() and size() without grabbing the internal lock twice.
   * 
   * @return size if object was successfully added, else return -1.
   */
  public int addAndGetSize(ObjectID obj);
}
