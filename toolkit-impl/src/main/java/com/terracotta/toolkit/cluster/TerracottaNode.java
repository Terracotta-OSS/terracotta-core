/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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