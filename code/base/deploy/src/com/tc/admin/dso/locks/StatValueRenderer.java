/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.admin.common.XTable.LongRenderer;

/**
 * Table renderer used in the Locks UI. Displays a undefined value (-1) as na.
 */
public class StatValueRenderer extends LongRenderer {
  public void setValue(Object value) {
    if (!(value instanceof Long)) {
      setText(value != null ? value.toString() : "");
      return;
    }
    Long l = (Long) value;
    if (l.longValue() == -1) {
      setText("na");
    } else {
      super.setValue(value);
    }
  }
}
