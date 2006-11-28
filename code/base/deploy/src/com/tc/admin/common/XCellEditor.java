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
