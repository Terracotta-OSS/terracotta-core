package com.tc.object;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.object.msg.InterestListenerMessageFactory;
import com.tc.object.msg.RegisterInterestListenerMessage;
import com.tc.object.msg.ServerInterestMessage;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Eugene Shelestovich
 */
public class ServerInterestListenerManagerImpl implements ServerInterestListenerManager {

  private static final TCLogger LOG = TCLogging.getLogger(ServerInterestListenerManagerImpl.class);

  protected final ConcurrentMap<String, InterestDestination> namedDestinations =
      new ConcurrentHashMap<String, InterestDestination>();
  protected final InterestListenerMessageFactory messageFactory;
  protected final GroupID stripeId;

  public ServerInterestListenerManagerImpl(final InterestListenerMessageFactory messageFactory, final GroupID stripeId) {
    this.messageFactory = messageFactory;
    this.stripeId = stripeId;
  }

  @Override
  public void dispatchInterest(final ServerInterestMessage message) {
    final String cacheName = message.getCacheName();
    LOG.info("Server notification message has been received. Type: "
             + message.getInterestType() + ", key: " + message.getKey()
             + ", cache: " + cacheName);

    final InterestDestination destination = namedDestinations.get(cacheName);
    if (destination == null) {
      throw new IllegalStateException("Could not find cache by name: " + cacheName);
    }
    destination.handleInterest(message.getInterestType(), message.getKey());
  }

  @Override
  public void registerL1CacheListener(final InterestDestination destination, final Set<InterestType> listenTo) {
    sendMessage(destination.getDestinationName(), stripeId, listenTo);
    namedDestinations.putIfAbsent(destination.getDestinationName(), destination);
  }

  @Override
  public void unregisterL1CacheListener(final InterestDestination destination) {
    //sendMessage(destination.getDestinationName(), stripeId, listenTo);
    namedDestinations.remove(destination.getDestinationName());
  }

  protected void sendMessage(final String destinationName, GroupID stripeId, final Set<InterestType> listenTo) {
    final RegisterInterestListenerMessage msg = messageFactory.newRegisterInterestListenerMessage(stripeId);
    msg.setDestination(destinationName);
    msg.setInterestTypes(listenTo);
    msg.send();
  }
}
