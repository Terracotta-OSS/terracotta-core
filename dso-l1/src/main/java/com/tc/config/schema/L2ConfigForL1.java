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

/**
 * Contains the information from the L2s that L1 needs.
 */
public interface L2ConfigForL1 {
  public static class L2Data {
    private final String  host;
    private final int     tsaPort;
    private int           groupId = -1;
    private String        groupName;
    private final boolean secure;

    public L2Data(String host, int tsaPort) {
      this(host, tsaPort, false);
    }

    public L2Data(String host, int tsaPort, boolean secure) {
      Assert.assertNotBlank(host);
      this.host = host;
      this.tsaPort = tsaPort;
      this.secure = secure;
    }

    public String host() {
      return this.host;
    }

    public int tsaPort() {
      return this.tsaPort;
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

    public void setGroupName(String groupName) {
      this.groupName = groupName;
    }

    /**
     * This function could return null if no group name is specified in the tc config file
     */
    public String getGroupName() {
      return groupName;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + groupId;
      result = prime * result + ((host == null) ? 0 : host.hashCode());
      result = prime * result + tsaPort;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      L2Data other = (L2Data) obj;
      if (groupId != other.groupId) return false;
      if (host == null) {
        if (other.host != null) return false;
      } else if (!host.equals(other.host)) return false;
      if (tsaPort != other.tsaPort) return false;
      return true;
    }

    @Override
    public String toString() {
      return "L2Data [host=" + host + ", tsaPort=" + tsaPort + "]";
    }
  }

  L2Data[] l2Data();

  L2Data[][] getL2DataByGroup();

}
