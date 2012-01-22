/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.util.Assert;
import com.tc.util.stringification.OurStringBuilder;

/**
 * Contains the information from the L2s that L1 needs.
 */
public interface L2ConfigForL1 {
  public static class L2Data {
    private final String host;
    private final int    dsoPort;
    private int          groupId = -1;
    private String       groupName;

    public L2Data(String host, int dsoPort) {
      Assert.assertNotBlank(host);
      this.host = host;
      this.dsoPort = dsoPort;
    }

    public String host() {
      return this.host;
    }

    public int dsoPort() {
      return this.dsoPort;
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

    public boolean equals(Object that) {
      if (!(that instanceof L2Data)) return false;
      L2Data thatData = (L2Data) that;
      return new EqualsBuilder().append(this.host, thatData.host).append(this.dsoPort, thatData.dsoPort).isEquals();
    }

    public int hashCode() {
      return new HashCodeBuilder().append(this.host).append(this.dsoPort).toHashCode();
    }

    public String toString() {
      return new OurStringBuilder(this).append("host", this.host).append("DSO port", this.dsoPort).toString();
    }
  }

  L2Data[] l2Data();

  L2Data[][] getL2DataByGroup();

}
