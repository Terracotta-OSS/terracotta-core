/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

public class ConfigSpec {
  public static enum Type {
    SERVER, FILE;
  }

  private String fSpec;
  private Type   fType;

  public ConfigSpec(String spec, Type type) {
    fSpec = spec;
    fType = type;
  }

  public boolean isFile() {
    return fType == Type.FILE;
  }

  public boolean isServer() {
    return fType == Type.SERVER;
  }

  public String getSpec() {
    return fSpec;
  }
  
  public Type getType() {
    return fType;
  }
  
  public String toString() {
    return fSpec;
  }
}
