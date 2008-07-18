/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XObjectTable;

import java.awt.Color;
import java.text.DateFormat;
import java.util.Date;

public class GCStatsTable extends XObjectTable {
  private static final BaseRenderer START_TIME_RENDERER      = new BaseRenderer(DateFormat
                                                                 .getDateTimeInstance(DateFormat.SHORT,
                                                                                      DateFormat.MEDIUM));
  private static final LongRenderer UNDEFINED_VALUE_RENDERER = new UndefinedValueRenderer();

  public GCStatsTable() {
    super();
    setDefaultRenderer(Long.class, UNDEFINED_VALUE_RENDERER);
    setDefaultRenderer(Date.class, START_TIME_RENDERER);
  }

  private static class UndefinedValueRenderer extends LongRenderer {
    public void setValue(Object value) {
      if (!(value instanceof Long)) {
        setText(value != null ? value.toString() : "");
        return;
      }
      Long l = (Long) value;
      if (l.longValue() == -1) {
        setText("---");
        getComponent().setBackground(Color.lightGray);
      } else {
        super.setValue(value);
      }
    }
  }

  // no sorting allowed
  
  public int getSortColumn() {return -1;}
  protected void loadSortPrefs() {/**/}
  protected void storeSortPrefs() {/**/}
}
