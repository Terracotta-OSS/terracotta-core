package com.tc.object;

import java.util.Properties;
import java.util.ServiceLoader;

public interface ClientBuilderFactory {

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


  String CLIENT_BUILDER_TYPE = "client.builder.type";

  enum ClientBuilderType {
    TERRACOTTA, DIAGNOSTIC;


    public static ClientBuilderType of(String name) {
      for (ClientBuilderType type : values()) {
        if (type.name().equalsIgnoreCase(name)) {
          return type;
        }
      }
      throw new IllegalArgumentException("Couldn't find enum with name " + name);
    }
  }

  ClientBuilder create(Properties connectionProperties);
}
