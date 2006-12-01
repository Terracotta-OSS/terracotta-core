/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.DerivedConfigItem;
import com.tc.net.core.ConnectionInfo;
import com.tc.util.Assert;
import com.tc.util.stringification.OurStringBuilder;

/**
 * Returns a {@link ConnectionInfo} array from the L2 data.
 */
public class ConnectionInfoConfigItem extends DerivedConfigItem {
  public ConnectionInfoConfigItem(ConfigItem l2DataConfigItem) {
    super(new ConfigItem[] { l2DataConfigItem });
  }

  protected Object createValueFrom(ConfigItem[] fromWhich) {
    Assert.eval(fromWhich.length == 1);

    L2Data[] l2Data = (L2Data[]) fromWhich[0].getObject();
    ConnectionInfo[] out = new ConnectionInfo[l2Data.length];

    for (int i = 0; i < out.length; ++i) {
      out[i] = new ConnectionInfo(l2Data[i].host(), l2Data[i].dsoPort());
    }

    return out;
  }

  public String toString() {
    return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE).appendSuper(super.toString()).toString();
  }
}