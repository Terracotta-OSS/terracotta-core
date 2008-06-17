/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.text.PrettyPrintable;

import java.util.Set;

public interface SyncObjectIdSet extends Set, PrettyPrintable {
  public void startPopulating();

  public void stopPopulating(ObjectIDSet fullSet);

  public ObjectIDSet snapshot();
}
