package com.tc.object;

import com.terracotta.diagnostic.DiagnosticClientBuilder;

import java.util.Arrays;
import java.util.Properties;

public class StandardClientBuilderFactory implements ClientBuilderFactory {
  @Override
  public ClientBuilder create(Properties connectionProperties) {
    Object clientBuilderTypeValue = connectionProperties.get(CLIENT_BUILDER_TYPE);
    if (clientBuilderTypeValue instanceof ClientBuilderType) {
      ClientBuilderType connectionType = (ClientBuilderType)clientBuilderTypeValue;
      if (connectionType == ClientBuilderType.TERRACOTTA) {
        return new StandardClientBuilder(connectionProperties);
      } else if (connectionType == ClientBuilderType.DIAGNOSTIC) {
        return new DiagnosticClientBuilder(connectionProperties);
      } else {
        throw new IllegalArgumentException(connectionType + " is not a valid client builder type, valid client " +
                                           "builder types " + Arrays.toString(ClientBuilderType.values()));
      }
    } else {
      throw new IllegalArgumentException("Received invalid value (" + clientBuilderTypeValue + ") for property "
                                         + CLIENT_BUILDER_TYPE + ", valid client builder types " +
                                         Arrays.toString(ClientBuilderType.values()));
    }
  }
}
