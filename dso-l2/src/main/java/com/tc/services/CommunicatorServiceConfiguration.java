package com.tc.services;

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ServiceConfiguration;

public class CommunicatorServiceConfiguration implements ServiceConfiguration<ClientCommunicator> {
  @Override
  public Class<ClientCommunicator> getServiceType() {
    return ClientCommunicator.class;
  }
}
