package com.tc.objectserver.impl;

import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ResourceManagerThrottleMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.ResourceManager;

/**
 * @author tim
 */
public class ResourceManagerImpl implements ResourceManager {
  private final DSOChannelManager channelManager;
  private final GroupID groupID;

  private volatile boolean throwException = false;
  private volatile float throttleAmount = 0.0f;

  private boolean lastBroadcastThrowException = false;
  private float lastBroadcastThrottleAmount = 0.0f;

  public ResourceManagerImpl(final DSOChannelManager channelManager, final GroupID groupID) {
    this.channelManager = channelManager;
    this.groupID = groupID;
  }

  @Override
  public boolean isThrowException() {
    return throwException;
  }

  @Override
  public void setThrottle(final float percentage) {
    if (percentage < 0.0f || percentage > 1.0f) {
      throw new IllegalArgumentException("Ratio out of range [0.0, 1.0], actual " + percentage);
    }
    throwException = false;
    throttleAmount = percentage;
    broadcastMessage();
  }

  @Override
  public void setThrowException() {
    throttleAmount = 0.0f;
    throwException = true;
    broadcastMessage();
  }

  @Override
  public void clear() {
    throttleAmount = 0.0f;
    throwException = false;
    broadcastMessage();
  }

  private void broadcastMessage() {
    if (throwException == lastBroadcastThrowException && throttleAmount == lastBroadcastThrottleAmount) {
      // broadcast is the same as the last one, don't bother sending it.
      return;
    }
    for (MessageChannel clientChannel : channelManager.getActiveChannels()) {
      sendMessageTo(clientChannel);
    }
    lastBroadcastThrowException = throwException;
    lastBroadcastThrottleAmount = throttleAmount;
  }

  private void sendMessageTo(MessageChannel clientChannel) {
    ResourceManagerThrottleMessage throttleMessage = (ResourceManagerThrottleMessage) clientChannel.createMessage(TCMessageType.RESOURCE_MANAGER_THROTTLE_STATE_MESSAGE);
    throttleMessage.initialize(groupID, throwException, throttleAmount);
    throttleMessage.send();
  }

  @Override
  public void channelCreated(final MessageChannel channel) {
    // Send an update to any freshly connected clients so they know the throttle status.
    sendMessageTo(channel);
  }

  @Override
  public void channelRemoved(final MessageChannel channel) {
    // Nothing to do.
  }
}
