/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import javax.management.ObjectName;
import com.tc.admin.ConnectionContext;

public class RatePanel extends CountStatisticPanel {
  public RatePanel(
    ConnectionContext cc,
    ObjectName        bean,
    String            statName,
    String            header,
    String            xLabel,
    String            yLabel)
  {
    super(cc);

    setBean(bean);
    setStatisticName(statName);
    setSeriesName(header);
    setTimeAxisLabel(xLabel);
    setValueAxisLabel(yLabel);
  }
}
