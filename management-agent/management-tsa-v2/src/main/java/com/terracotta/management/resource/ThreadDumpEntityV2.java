/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
