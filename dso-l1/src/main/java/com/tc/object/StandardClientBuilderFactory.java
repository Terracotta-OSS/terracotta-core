package com.tc.object;

import com.tc.diagnostic.DiagnosticClientBuilder;

import java.util.Properties;

public class StandardClientBuilderFactory implements ClientBuilderFactory {
  @Override
  public ClientBuilder create(Properties connectionProperties) {
    ConnectionType connectionType = (ConnectionType) connectionProperties.get(CONNECTION_TYPE_PROPERTY);
    if (connectionType == ConnectionType.TERRACOTTA) {
      return new StandardClientBuilder(connectionProperties);
    } else if (connectionType == ConnectionType.DIAGNOSTIC) {
      return new DiagnosticClientBuilder(connectionProperties);
    } else {
      throw new IllegalArgumentException(connectionType + " is not a valid connection type");
    }
  }
}
