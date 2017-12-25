package com.tc.object;

import java.util.Properties;
import java.util.ServiceLoader;

public interface ClientBuilderFactory {
  String CONNECTION_TYPE_PROPERTY = "connection.type";

  ClientBuilder create(Properties connectionProperties);

  static ClientBuilderFactory get() {
    ClientBuilderFactory clientBuilderFactory = null;

    for (ClientBuilderFactory factory : ServiceLoader.load(ClientBuilderFactory.class)) {
      if (clientBuilderFactory == null) {
        clientBuilderFactory = factory;
      } else {
        throw new RuntimeException("Found multiple implementations of ClientBuilderFactory");
      }
    }

    if (clientBuilderFactory == null) {
      throw new RuntimeException("No ClientBuilderFactory implementation found");
    }

    return clientBuilderFactory;
  }
}
