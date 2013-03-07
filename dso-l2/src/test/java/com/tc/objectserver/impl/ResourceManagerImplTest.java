package com.tc.objectserver.impl;

import org.junit.Before;
import org.junit.Test;

import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ResourceManagerThrottleMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.ResourceManager;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class ResourceManagerImplTest {

  private ResourceManagerThrottleMessage throttleMessage;
  private MessageChannel channel;
  private ResourceManager resourceManager;
  private DSOChannelManager channelManager;
  private GroupID groupID;

  @Before
  public void setUp() throws Exception {
    throttleMessage = mock(ResourceManagerThrottleMessage.class);
    channel = mock(MessageChannel.class);
    channelManager = mock(DSOChannelManager.class);
    when(channelManager.getActiveChannels()).thenReturn(new MessageChannel[] { channel });
    when(channel.createMessage(any(TCMessageType.class))).thenReturn(throttleMessage);
    groupID = new GroupID(1);
    resourceManager = new ResourceManagerImpl(channelManager, groupID);
  }

  @Test
  public void testInvalidThrottle() throws Exception {
    try {
      resourceManager.setThrottle(-1.0f);
      fail("Expected an IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
    verify(channelManager, never()).getActiveChannels();
    verify(channel, never()).createMessage(TCMessageType.RESOURCE_MANAGER_THROTTLE_STATE_MESSAGE);
    verify(throttleMessage, never()).initialize(groupID, false, 0.5f);
    verify(throttleMessage, never()).send();
  }

  @Test
  public void testThrottle() throws Exception {
    resourceManager.setThrottle(0.5f);
    verify(channelManager).getActiveChannels();
    verify(channel).createMessage(TCMessageType.RESOURCE_MANAGER_THROTTLE_STATE_MESSAGE);
    verify(throttleMessage).initialize(groupID, false, 0.5f);
    verify(throttleMessage).send();
  }

  @Test
  public void testThrowException() throws Exception {
    resourceManager.setRestricted();
    verify(channelManager).getActiveChannels();
    verify(channel).createMessage(TCMessageType.RESOURCE_MANAGER_THROTTLE_STATE_MESSAGE);
    verify(throttleMessage).initialize(groupID, true, 0.0f);
    verify(throttleMessage).send();
  }

  @Test
  public void testRepeatedBroadcast() throws Exception {
    resourceManager.setThrottle(0.1f);
    resourceManager.setThrottle(0.2f);
    resourceManager.setThrottle(0.2f);
    verify(throttleMessage, times(1)).initialize(groupID, false, 0.1f);
    verify(throttleMessage, times(1)).initialize(groupID, false, 0.2f);
    verify(throttleMessage, times(2)).send();
  }

  @Test
  public void testClear() throws Exception {
    resourceManager.setThrottle(0.1f);
    resourceManager.setRestricted();
    resourceManager.resetState();
    resourceManager.resetState();
    verify(throttleMessage, times(1)).initialize(groupID, false, 0.1f);
    verify(throttleMessage, times(1)).initialize(groupID, true, 0.0f);
    verify(throttleMessage, times(1)).initialize(groupID, false, 0.0f);
    verify(throttleMessage, times(3)).send();
  }
}
