/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tcclient.cluster;

public class NodeImpl implements NodeInternal, Comparable<NodeImpl> {

  private final String             id;
  private final long               channelId;

  public NodeImpl(String id, long channelId) {
    this.id = id;
    this.channelId = channelId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public long getChannelId() {
    return channelId;
  }

  @Override
  public String toString() {
    return id.toString();
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (null == obj) { return false; }
    if (getClass() != obj.getClass()) { return false; }
    NodeImpl other = (NodeImpl) obj;
    if (null == id) {
      return null == other.id;
    } else {
      return id.equals(other.id);
    }
  }

  @Override
  public int compareTo(NodeImpl other) {
    return id.compareTo(other.id);
  }
}
