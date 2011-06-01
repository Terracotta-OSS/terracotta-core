/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tcclient.cluster.ClusterInternalEventsContext;
import com.tcclient.cluster.DsoClusterEventsNotifier;

/**
 * Handler firing the dso cluster internal events to the listeners
 */
public class ClusterInternalEventsHandler extends AbstractEventHandler {

  private final DsoClusterEventsNotifier dsoClusterEventsNotifier;

  public ClusterInternalEventsHandler(final DsoClusterEventsNotifier eventsNotifier) {
    this.dsoClusterEventsNotifier = eventsNotifier;
  }

  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof ClusterInternalEventsContext) {
      handleClusterInternalEvents((ClusterInternalEventsContext) context);
    } else {
      throw new AssertionError("Unknown Context " + context);
    }
  }

  private void handleClusterInternalEvents(ClusterInternalEventsContext context) {
    dsoClusterEventsNotifier.notifyDsoClusterListener(context.getEventType(), context.getEvent(),
                                                      context.getDsoClusterListener());
  }
}
