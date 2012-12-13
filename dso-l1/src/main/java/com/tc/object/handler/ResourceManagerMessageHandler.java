package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.RemoteResourceManager;
import com.tc.object.msg.ResourceManagerThrottleMessage;

/**
 * @author tim
 */
public class ResourceManagerMessageHandler extends AbstractEventHandler {

  private final RemoteResourceManager remoteResourceManager;

  public ResourceManagerMessageHandler(final RemoteResourceManager remoteResourceManager) {
    this.remoteResourceManager = remoteResourceManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ResourceManagerThrottleMessage) {
      ResourceManagerThrottleMessage msg = (ResourceManagerThrottleMessage)context;
      remoteResourceManager.handleThrottleMessage(msg.getGroupID(), msg.getThrowException(), msg.getThrottle());
    } else {
      throw new AssertionError("Wrong context received in Resource Management Message sink. Type " + context.getClass());
    }
  }
}
