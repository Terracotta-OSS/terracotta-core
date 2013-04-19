package com.tc.objectserver.impl;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.InterestType;
import com.tc.object.msg.EvictionInterestMessage;
import com.tc.object.msg.ExpirationInterestMessage;
import com.tc.object.msg.RegisterInterestListenerMessage;
import com.tc.object.msg.UnregisterInterestListenerMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.interest.EvictionInterest;
import com.tc.objectserver.interest.ExpirationInterest;
import com.tc.objectserver.interest.InterestListenerSupport;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Sends L2 eviction and expiration events to interested L1 clients within the same cluster.
 *
 * @author Eugene Shelestovich
 */
public final class InClusterInterestNotifier extends InterestListenerSupport {

  private final CopyOnWriteArraySet<ClientID> clientsToNotify = new CopyOnWriteArraySet<ClientID>();
  private final DSOChannelManager channelManager;

  public InClusterInterestNotifier(final DSOChannelManager channelManager) {
    this.channelManager = channelManager;
  }

  @Override
  public void onEviction(final EvictionInterest interest) {
    for (ClientID clientID : clientsToNotify) {
      try {
        final MessageChannel channel = channelManager.getActiveChannel(clientID);
        final EvictionInterestMessage msg = (EvictionInterestMessage)channel.createMessage(
            TCMessageType.EVICTION_INTEREST_MESSAGE);
        msg.setKey(interest.getKey());
        msg.send();
      } catch (NoSuchChannelException e) {
        // ignore disconnected client
        clientsToNotify.remove(clientID);
      }
    }
  }

  @Override
  public void onExpiration(final ExpirationInterest interest) {
    for (ClientID clientID : clientsToNotify) {
      try {
        final MessageChannel channel = channelManager.getActiveChannel(clientID);
        final ExpirationInterestMessage msg = (ExpirationInterestMessage)channel.createMessage(
            TCMessageType.EXPIRATION_INTEREST_MESSAGE);
        msg.setKey(interest.getKey());
        msg.send();
      } catch (NoSuchChannelException e) {
        // ignore disconnected client
        clientsToNotify.remove(clientID);
      }
    }

  }

  public void subscribe(final ClientID clientId, String destination, Set<InterestType> interestTypes) {
    clientsToNotify.add(clientId);
  }

  public void unsubscribe(final ClientID clientId) {
    clientsToNotify.remove(clientId);
  }
}
