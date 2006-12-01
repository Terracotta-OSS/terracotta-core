/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public class PropertyTable extends XTable {
  private PropertyTableModel m_model;
 
  public PropertyTable() {
    super();
  }
  
  public PropertyTable(PropertyTableModel model) {
    super();
    setModel(m_model = model);
  }

  protected TableModel createDefaultDataModel() {
    return new PropertyTableModel();
}

  public void setModel(TableModel model) {
    if(!(model instanceof PropertyTableModel)) {
      throw new IllegalArgumentException("Must be a PropertyTableModel");
    }
    super.setModel(m_model = (PropertyTableModel)model);
  }
  
  public PropertyTableModel getPropertyModel() {
    return (PropertyTableModel)getModel();
  }
  
  public TableCellEditor getCellEditor(int row, int column) {
    switch(column) {
      case PropertyTableModel.VALUE_COLUMN:
        return getDefaultEditor(m_model.getRowClass(row));
    }

    return super.getCellEditor(row, column);
  }

  public TableCellRenderer getCellRenderer(int row, int column) {
    switch(column) {
      case PropertyTableModel.VALUE_COLUMN:
        return getDefaultRenderer(m_model.getRowClass(row));
    }

    return super.getCellRenderer(row, column);
  }
}
