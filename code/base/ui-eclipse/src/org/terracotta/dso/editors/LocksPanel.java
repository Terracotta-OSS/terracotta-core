/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.xmlbeans.StringEnumAbstractBase;
import org.eclipse.core.resources.IProject;

import org.dijon.Button;
import org.dijon.ComboBox;
import org.dijon.ComboModel;
import org.dijon.ContainerResource;
import org.dijon.DefaultCellEditor;

import com.tc.admin.common.XObjectTableModel;
import com.tc.admin.common.XTable;
import com.tc.admin.common.XTableCellRenderer;
import org.terracotta.dso.editors.chooser.MethodChooser;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.LockLevel;
import com.terracottatech.config.Locks;
import com.terracottatech.config.NamedLock;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class LocksPanel extends ConfigurationEditorPanel
  implements TableModelListener
{
  private IProject                 m_project;
  private DsoApplication           m_dsoApp;
  private Locks                    m_locks;
  private XTable                   m_autoLockTable;
  private AutoLocksModel           m_autoLocksModel;
  private Button                   m_addAutoLockButton;
  private ActionListener           m_addAutoLockListener;
  private Button                   m_removeAutoLockButton;
  private ActionListener           m_removeAutoLockListener;
  private ListSelectionListener    m_autoLocksListener;
  private XTable                   m_namedLockTable;
  private NamedLocksModel          m_namedLocksModel;
  private Button                   m_addNamedLockButton;
  private ActionListener           m_addNamedLockListener;
  private Button                   m_removeNamedLockButton;
  private ActionListener           m_removeNamedLockListener;
  private ListSelectionListener    m_namedLocksListener;
  private MethodChooser            m_methodChooser;
  private AutoLockChooserListener  m_autoLockChooserListener;
  private NamedLockChooserListener m_namedLockChooserListener;
  
  public LocksPanel() {
    super();
  }
  
  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_autoLockTable = (XTable)findComponent("AutoLockTable");
    m_autoLockTable.setModel(m_autoLocksModel = new AutoLocksModel());
    m_autoLocksListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) {
        if(!lse.getValueIsAdjusting()) {
          int[] sel = m_autoLockTable.getSelectedRows();
          m_removeAutoLockButton.setEnabled(sel != null && sel.length > 0);
        }
      }
    };    
    m_autoLockTable.setDefaultRenderer(StringEnumAbstractBase.class, new LockLevelRenderer());
    m_autoLockTable.setDefaultEditor(StringEnumAbstractBase.class, new LockLevelEditor());

    m_addAutoLockButton = (Button)findComponent("AddAutoLockButton");
    m_addAutoLockListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        MethodChooser chsr = getAutoLockChooser();
        
        chsr.setup(m_project);
        chsr.center(LocksPanel.this.getAncestorOfClass(Frame.class));
        chsr.setVisible(true);
      }
    };

    m_removeAutoLockButton = (Button)findComponent("RemoveAutoLockButton");
    m_removeAutoLockListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        int[] selection = m_autoLockTable.getSelectedRows();
        int   row;
        
        for(int i = selection.length-1; i >= 0; i--) {
          row = selection[i];
          
          ensureLocks().removeAutolock(row);
          m_autoLocksModel.remove(row);
          m_autoLocksModel.fireTableRowsDeleted(row, row);
        }
      }
    };
    
    m_namedLockTable = (XTable)findComponent("NamedLockTable");
    m_namedLockTable.setModel(m_namedLocksModel = new NamedLocksModel());
    m_namedLocksListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) {
        if(!lse.getValueIsAdjusting()) {
          int[] sel = m_namedLockTable.getSelectedRows();
          m_removeNamedLockButton.setEnabled(sel != null && sel.length > 0);
        }
      }
    };
    m_namedLockTable.setDefaultRenderer(StringEnumAbstractBase.class, new LockLevelRenderer());
    m_namedLockTable.setDefaultEditor(StringEnumAbstractBase.class, new LockLevelEditor());
    
    m_addNamedLockButton = (Button)findComponent("AddNamedLockButton");
    m_addNamedLockListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        MethodChooser chsr = getNamedLockChooser();
        
        chsr.setup(m_project);
        chsr.center(LocksPanel.this.getAncestorOfClass(Frame.class));
        chsr.setVisible(true);
      }
    };
    
    m_removeNamedLockButton = (Button)findComponent("RemoveNamedLockButton");
    m_removeNamedLockListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        int[] selection = m_namedLockTable.getSelectedRows();
        int   row;
        
        for(int i = selection.length-1; i >= 0; i--) {
          row = selection[i];

          m_namedLocksModel.remove(row);
          ensureLocks().removeNamedLock(row);
          m_namedLocksModel.fireTableRowsDeleted(row, row);
        }
      }
    };
  }
  
  private AutoLockChooserListener getAutoLockChooserListener() {
    if(m_autoLockChooserListener == null) {
      m_autoLockChooserListener = new AutoLockChooserListener();
    }
    
    return m_autoLockChooserListener;
  }
  
  private MethodChooser getAutoLockChooser() {
    if(m_methodChooser == null) {
      Frame owner = (Frame)getAncestorOfClass(Frame.class);
      m_methodChooser = new MethodChooser(owner);
    }
    m_methodChooser.setListener(getAutoLockChooserListener());
    
    return m_methodChooser;
  }

  private NamedLockChooserListener getNamedLockChooserListener() {
    if(m_namedLockChooserListener == null) {
      m_namedLockChooserListener = new NamedLockChooserListener();
    }
    
    return m_namedLockChooserListener;
  }
  
  private MethodChooser getNamedLockChooser() {
    if(m_methodChooser == null) {
      Frame owner = (Frame)getAncestorOfClass(Frame.class);
      m_methodChooser = new MethodChooser(owner);
    }
    m_methodChooser.setListener(getNamedLockChooserListener());
    
    return m_methodChooser;
  }

  class AutoLockChooserListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String[] expressions = m_methodChooser.getMethodExpressions();
      
      for(int i = 0; i < expressions.length; i++) {
        internalAddAutolock(expressions[i]);
      }
    }
  }

  class NamedLockChooserListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String[] expressions = m_methodChooser.getMethodExpressions();
      
      for(int i = 0; i < expressions.length; i++) {
        internalAddNamedLock(expressions[i]);
      }
    }
  }

  public boolean hasAnySet() {
    return m_locks != null && 
          (m_locks.sizeOfAutolockArray()  > 0 ||
           m_locks.sizeOfNamedLockArray() > 0);
  }
 
  private Locks ensureLocks() {
    if(m_locks == null) {
      ensureXmlObject();
    }
    return m_locks;
  }
  
  public void ensureXmlObject() {
    super.ensureXmlObject();
    
    if(m_locks == null) {
      removeListeners();
      m_locks = m_dsoApp.addNewLocks();
      updateChildren();
      addListeners();
    }
  }
  
  private void syncModel() {
    if(!hasAnySet() && m_dsoApp.getLocks() != null) {
      m_dsoApp.unsetLocks();
      m_locks = null;
      fireXmlObjectStructureChanged(m_dsoApp);
    }

    setDirty();
  }

  private void addListeners() {
    m_autoLockTable.getSelectionModel().addListSelectionListener(m_autoLocksListener);
    m_addAutoLockButton.addActionListener(m_addAutoLockListener);    
    m_removeAutoLockButton.addActionListener(m_removeAutoLockListener);
    m_autoLocksModel.addTableModelListener(this);
    
    m_namedLockTable.getSelectionModel().addListSelectionListener(m_namedLocksListener);
    m_addNamedLockButton.addActionListener(m_addNamedLockListener);    
    m_removeNamedLockButton.addActionListener(m_removeNamedLockListener);
    m_namedLocksModel.addTableModelListener(this);
  }
  
  private void removeListeners() {
    m_autoLockTable.getSelectionModel().removeListSelectionListener(m_autoLocksListener);
    m_addAutoLockButton.removeActionListener(m_addAutoLockListener);    
    m_removeAutoLockButton.removeActionListener(m_removeAutoLockListener);
    m_autoLocksModel.removeTableModelListener(this);
    
    m_namedLockTable.getSelectionModel().removeListSelectionListener(m_namedLocksListener);
    m_addNamedLockButton.removeActionListener(m_addNamedLockListener);    
    m_removeNamedLockButton.removeActionListener(m_removeNamedLockListener);
    m_namedLocksModel.removeTableModelListener(this);
  }
  
  public void tableChanged(TableModelEvent e) {
    syncModel();
  }

  public void updateChildren() {
    m_autoLocksModel.setAutoLocks(m_locks != null ? m_locks.getAutolockArray() : null);
    m_namedLocksModel.setNamedLocks(m_locks != null ? m_locks.getNamedLockArray() : null);
  }
  
  public void updateModel() {
    removeListeners();
    updateChildren();
    addListeners();
  }
  
  public void setup(IProject project, DsoApplication dsoApp) {
    setEnabled(true);
    removeListeners();

    m_project = project;
    m_dsoApp  = dsoApp;
    m_locks   = m_dsoApp != null ? m_dsoApp.getLocks() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();
    
    m_project = null;
    m_dsoApp  = null;
    m_locks   = null;

    m_autoLocksModel.clear();
    m_namedLocksModel.clear();
    
    setEnabled(false);
  }
  
  private static final String[] AUTO_LOCK_FIELDS  = {
    "MethodExpression", "LockLevel"
  };
  
  private static final String[] AUTO_LOCK_HEADERS = {
    "Method expression", "Lock level"
  };
                                                  
  class AutoLocksModel extends XObjectTableModel {
    public AutoLocksModel() {
      super(Autolock.class, AUTO_LOCK_FIELDS, AUTO_LOCK_HEADERS);
    }
    
    public void setAutoLocks(Autolock[] autoLocks) {
      set(autoLocks);
      fireTableDataChanged();
    }
    
    public boolean hasEditor(Class type) {
      return StringEnumAbstractBase.class.isAssignableFrom(type);
    }
  }

  private static final String[] NAMED_LOCK_FIELDS  = {
    "LockName", AUTO_LOCK_FIELDS[0], AUTO_LOCK_FIELDS[1]
  };
                                                   
  private static final String[] NAMED_LOCK_HEADERS = {
    "Lock name", AUTO_LOCK_HEADERS[0], AUTO_LOCK_HEADERS[1]
  };
                                                                                                   
  class NamedLocksModel extends XObjectTableModel {
    public NamedLocksModel() {
      super(NamedLock.class, NAMED_LOCK_FIELDS, NAMED_LOCK_HEADERS);
    }
    
    public void setNamedLocks(NamedLock[] namedLocks) {
      set(namedLocks);
      fireTableDataChanged();
    }
    
    public boolean hasEditor(Class type) {
      return StringEnumAbstractBase.class.isAssignableFrom(type);
    }
  }
  
  static class LockLevelRenderer extends XTableCellRenderer.UIResource {
    public LockLevelRenderer() {
      super();
    }

    public void setValue(Object value) {
      setText((value == null) ? "" : value.toString());
    }
  }
  
  static class LockLevelEditor extends DefaultCellEditor {
    private static StringEnumAbstractBase[] choices = {
      LockLevel.READ, LockLevel.WRITE, LockLevel.CONCURRENT
    };
    
    private static ComboBox m_editor = new ComboBox(new ComboModel(choices));
    
    public LockLevelEditor() {
      super(m_editor);
    }
  }
  
  private void internalAddAutolock(String expression) {
    internalAddAutolock(expression, LockLevel.WRITE);
  }

  private void internalAddAutolock(String expression, LockLevel.Enum level) {
    Locks    locks = ensureLocks();
    Autolock lock  = locks.addNewAutolock();
    
    lock.setMethodExpression(expression);
    lock.setLockLevel(level);
    m_autoLocksModel.add(lock);

    int row = m_autoLocksModel.getRowCount()-1;
    m_autoLocksModel.fireTableRowsInserted(row, row);
    m_autoLockTable.setRowSelectionInterval(row, row);
  }

  private void internalAddNamedLock(String expression) {
    internalAddNamedLock(expression, "NewLock", LockLevel.WRITE);
  }

  private void internalAddNamedLock(
    String         expression,
    String         name,
    LockLevel.Enum level)
  {
    Locks     locks = ensureLocks();
    NamedLock lock  = locks.addNewNamedLock();
    
    lock.setMethodExpression(expression);
    lock.setLockName(name);
    lock.setLockLevel(level);
    m_namedLocksModel.add(lock);

    int row = m_namedLocksModel.getRowCount()-1;
    m_namedLocksModel.fireTableRowsInserted(row, row);
    m_namedLockTable.setRowSelectionInterval(row, row);
  }
}
