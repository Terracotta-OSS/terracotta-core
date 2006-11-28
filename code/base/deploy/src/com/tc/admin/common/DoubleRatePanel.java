package com.tc.admin.common;

import javax.management.ObjectName;
import com.tc.admin.ConnectionContext;

public class DoubleRatePanel extends DoubleStatisticPanel {
  public DoubleRatePanel(
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
