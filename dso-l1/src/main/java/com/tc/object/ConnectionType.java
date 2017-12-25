package com.tc.object;

public enum ConnectionType {
  DIAGNOSTIC, TERRACOTTA;

  public static ConnectionType of(String name) {
    for (ConnectionType type : values()) {
      if (type.name().equalsIgnoreCase(name)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Could not find enum with name: " + name);
  }
}
