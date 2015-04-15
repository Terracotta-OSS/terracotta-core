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
package com.terracotta.toolkit.cluster;

import org.terracotta.toolkit.cluster.ClusterNode;

import com.tcclient.cluster.DsoNode;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class TerracottaNode implements ClusterNode {

  private final String id;
  private final String ip;
  private final String hostname;

  public TerracottaNode(DsoNode node) {
    this.id = node.getId();
    this.ip = node.getIp();
    this.hostname = node.getHostname();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public InetAddress getAddress() {
    try {
      return InetAddress.getByAddress(hostname, InetAddress.getByName(ip).getAddress());
    } catch (UnknownHostException uhe) {
      // This should never occur, since the ip address passed here will always be of legal length.
      throw new RuntimeException(uhe);
    }
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TerracottaNode) {
      TerracottaNode tcNodeImpl = (TerracottaNode) obj;
      return this.id.equals(tcNodeImpl.id);
    }
    return false;
  }

  @Override
  public String toString() {
    return "TerracottaNode [id=" + id + ", ip=" + ip + ", hostname=" + hostname + "]";
  }
}