package com.tc.objectserver.context;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.tx.TxnObjectGrouping;

import java.util.Collection;

/**
 * @author tim
 */
public class FlushApplyCommitContext implements MultiThreadedEventContext {
  private final TxnObjectGrouping grouping;

  public FlushApplyCommitContext(final TxnObjectGrouping grouping) {
    this.grouping = grouping;
  }

  public Collection<ManagedObject> getObjectsToRelease() {
    return grouping.getObjects();
  }

  @Override
  public Object getKey() {
    return grouping;
  }
}
