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
package com.tc.config.schema;

import com.tc.util.Assert;
import java.net.InetSocketAddress;

/**
 * Contains the information from the L2s that L1 needs.
 */
public interface L2ConfigForL1 {
  public static final int DEFAULT_PORT = 9410;

  public static class L2Data {
  private final InetSocketAddress  address;
  private int           groupId = -1;
  private final boolean secure;
    

    public L2Data(InetSocketAddress host) {
      this(host, false);
    }

    public L2Data(InetSocketAddress host, boolean secure) {
      Assert.assertNotNull(host);
      this.address = host.getPort() <= 0 ? InetSocketAddress.createUnresolved(host.getHostString(), DEFAULT_PORT) : host;
      this.secure = secure;
    }

    public String host() {
      return this.address.getHostString();
    }

    public int tsaPort() {
      return this.address.getPort();
    }
    
    public InetSocketAddress address() {
      return this.address;
    }

    public boolean secure() {
      return secure;
    }

    public void setGroupId(int gid) {
      Assert.assertTrue(gid >= 0);
      this.groupId = gid;
    }

    public int getGroupId() {
      Assert.assertTrue(groupId > -1);
      return groupId;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + groupId;
      result = prime * result + this.address.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      L2Data other = (L2Data) obj;
      if (groupId != other.groupId) return false;
      if (!address.equals(other.address)) return false;
      return true;
    }

    @Override
    public String toString() {
      return "L2Data [address=" + address + "]";
    }
  }

  L2Data[] l2Data();

}
