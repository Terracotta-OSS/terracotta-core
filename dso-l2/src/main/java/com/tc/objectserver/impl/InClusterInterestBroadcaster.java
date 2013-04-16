package com.tc.objectserver.impl;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.interest.EvictionInterest;
import com.tc.objectserver.interest.ExpirationInterest;
import com.tc.objectserver.interest.InterestListenerSupport;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Eugene Shelestovich
 */
public class InClusterInterestBroadcaster extends InterestListenerSupport {

  private final CopyOnWriteArraySet<ClientID> clientsToNotify = new CopyOnWriteArraySet<ClientID>();
  private final DSOChannelManager channelManager;

  public InClusterInterestBroadcaster(final DSOChannelManager channelManager) {
    this.channelManager = channelManager;
  }

  @Override
  public void onEviction(final EvictionInterest interest) {
    for (ClientID clientID : clientsToNotify) {
      try {
        final MessageChannel channel = channelManager.getActiveChannel(clientID);
        //TODO: send EvictionNotificationMessage
        //final ClusterMembershipMessage cmm = (ClusterMembershipMessage)channel
        //    .createMessage(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE);
        //cmm.initialize(eventType, nodeID, channels);
        //cmm.send();
      } catch (NoSuchChannelException e) {
        // ignore disconnected client
        clientsToNotify.remove(clientID);
      }
    }
  }

  @Override
  public void onExpiration(final ExpirationInterest interest) {

  }

  public void addClient(final ClientID clientId) {
    clientsToNotify.add(clientId);
  }

  public void removeClient(final ClientID clientId) {
    clientsToNotify.remove(clientId);
  }
}
