/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

/**
 * Different kit type constants for the index attribute <tc-kit>.
 */
public enum KitTypes {
  
  ENTERPRISE("enterprise"),
  OPEN_SOURCE("open source"),
  ALL("all");
  
  private String type;
  
  KitTypes(String type) {
    this.type = type;
  }
  
  public String type() {
    return this.type;
  }
}