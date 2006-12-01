/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.DefaultCellEditor;

public class XCellEditor extends DefaultCellEditor {
  public XCellEditor(XCheckBox checkBox) {
    super(checkBox);
  }

  public XCellEditor(XTextField textField) {
    super(textField);
  }

  public XCellEditor(XComboBox combo) {
    super(combo);
  }
}
