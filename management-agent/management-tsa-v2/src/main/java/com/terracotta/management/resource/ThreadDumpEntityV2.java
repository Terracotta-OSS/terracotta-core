/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

/**
 * A {@link org.terracotta.management.resource.AbstractEntityV2} representing a TSA server or client
 * thread dump from the management API.
 *
 * @author Ludovic Orban
 */
public class ThreadDumpEntityV2 extends AbstractTsaEntityV2 {

  public enum NodeType {
    CLIENT, SERVER
  }

  private String sourceId;

  private String dump;

  private NodeType nodeType;

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public void setNodeType(NodeType type) {
    nodeType = type;
  }

  public String getDump() {
    return dump;
  }

  public void setDump(String dump) {
    this.dump = dump;
  }
  
  public NodeType getNodeType() {
    return nodeType;
  }
}
