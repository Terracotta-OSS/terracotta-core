package com.tc.object;

import com.tc.net.GroupID;
import com.tc.object.msg.InterestListenerMessageFactory;
import com.tc.object.msg.RegisterInterestListenerMessage;
import com.tc.util.concurrent.ConcurrentHashMap;

import java.util.Map;
import java.util.Set;

/**
 * @author Eugene Shelestovich
 */
public class ServerInterestListenerManagerImpl implements ServerInterestListenerManager {

  protected final Map<InterestDestination, Set<InterestType>> registry =
      new ConcurrentHashMap<InterestDestination, Set<InterestType>>();
  protected final InterestListenerMessageFactory messageFactory;
  private final GroupID stripeId;

  public ServerInterestListenerManagerImpl(final InterestListenerMessageFactory messageFactory, final GroupID stripeId) {
    this.messageFactory = messageFactory;
    this.stripeId = stripeId;
  }

  @Override
  public void registerL1CacheListener(final InterestDestination destination, final Set<InterestType> listenTo) {
    registry.put(destination, listenTo);
    sendMessage(destination.getDestinationName(), stripeId, listenTo);
  }

  protected void sendMessage(final String destinationName, GroupID stripeId, final Set<InterestType> listenTo) {
    final RegisterInterestListenerMessage msg = messageFactory.newRegisterInterestListenerMessage(stripeId);
    msg.setDestinationName(destinationName);
    msg.setInterestTypes(listenTo);
    msg.send();
  }
}
