/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.config.schema.dynamic.ObjectArrayConfigItem;
import com.tc.util.Assert;
import com.tc.util.stringification.OurStringBuilder;

/**
 * Contains the information from the L2s that L1 needs.
 */
public interface L2ConfigForL1 {

  public static class L2Data {
    private final String host;
    private final int    dsoPort;

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

    public boolean equals(Object that) {
      if (!(that instanceof L2Data)) return false;
      L2Data thatData = (L2Data) that;
      return new EqualsBuilder().append(this.host, thatData.host)
          .append(this.dsoPort, thatData.dsoPort).isEquals();
    }

    public int hashCode() {
      return new HashCodeBuilder().append(this.host).append(this.dsoPort).toHashCode();
    }

    public String toString() {
      return new OurStringBuilder(this).append("host", this.host)
          .append("DSO port", this.dsoPort).toString();
    }
  }

  ObjectArrayConfigItem l2Data();

}
