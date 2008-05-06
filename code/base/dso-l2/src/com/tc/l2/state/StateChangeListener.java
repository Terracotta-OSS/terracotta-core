/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.state;

import com.tc.l2.context.StateChangedEvent;

public interface StateChangeListener {

  public void l2StateChanged(StateChangedEvent sce);

}
