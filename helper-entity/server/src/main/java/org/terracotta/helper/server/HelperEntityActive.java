package org.terracotta.helper.server;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.StateDumper;
import org.terracotta.helper.common.HelperEntityMessage;
import org.terracotta.helper.common.HelperEntityMessageType;
import org.terracotta.helper.common.HelperEntityResponse;
import org.terracotta.monitoring.PlatformService;

import java.util.HashSet;
import java.util.Set;

public class HelperEntityActive implements ActiveServerEntity<HelperEntityMessage, HelperEntityResponse>, StateDumpable {
  
  private final Set<ClientDescriptor> connectedClients = new HashSet<ClientDescriptor>();
  private final ClientCommunicator clientCommunicator;
  private final PlatformService platformService;

  public HelperEntityActive(ClientCommunicator clientCommunicator, final PlatformService platformService) {
    this.clientCommunicator = clientCommunicator;
    this.platformService = platformService;
  }

  @Override
  public void connected(final ClientDescriptor clientDescriptor) {
    connectedClients.add(clientDescriptor);
  }

  @Override
  public void disconnected(final ClientDescriptor clientDescriptor) {
    connectedClients.remove(clientDescriptor);
  }

  @Override
  public HelperEntityResponse invoke(final ClientDescriptor clientDescriptor, final HelperEntityMessage helperEntityMessage) throws EntityUserException {
    HelperEntityMessageType type = helperEntityMessage.getType();
    
    switch (type) {
      case DUMP: platformService.dumpPlatformState(); break;
      default: throw new RuntimeException("Unknown message type: " + type);      
    }
    return new HelperEntityResponse(HelperEntityMessageType.DUMP);
  }

  @Override
  public void loadExisting() {
    //Nothing to do
  }

  @Override
  public void handleReconnect(final ClientDescriptor clientDescriptor, final byte[] bytes) {
    //Nothing to do
  }

  @Override
  public void synchronizeKeyToPassive(final PassiveSynchronizationChannel<HelperEntityMessage> passiveSynchronizationChannel, final int i) {
    //Nothing to do
  }

  @Override
  public void createNew() throws ConfigurationException {
    //Nothing to do
  }

  @Override
  public void destroy() {
    connectedClients.clear();
  }

  @Override
  public void dumpStateTo(final StateDumper stateDumper) {
    for (ClientDescriptor connectedClient : connectedClients) {
      try {
        clientCommunicator.sendNoResponse(connectedClient, new HelperEntityResponse(HelperEntityMessageType.DUMP));
      } catch (MessageCodecException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
