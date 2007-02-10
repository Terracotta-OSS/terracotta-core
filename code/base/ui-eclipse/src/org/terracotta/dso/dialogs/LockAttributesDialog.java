/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.dialogs;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.dijon.ButtonGroup;
import org.dijon.Container;
import org.dijon.ContainerResource;
import org.dijon.DictionaryResource;
import org.dijon.TextField;

import org.terracotta.dso.PatternHelper;
import org.terracotta.dso.TcPlugin;
import com.tc.admin.common.XAbstractAction;
import com.terracottatech.config.LockLevel;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

/**
 * Given an IMethod display a form letting the user specify the lock's name and level. 
 */

public class LockAttributesDialog extends MessageDialog {
  private static ContainerResource m_panelRes;
  
  private TextField   m_nameField;
  private ButtonGroup m_typeGroup;
  private IMethod     m_method;
  
  private static final String ENTER_CMD = "ENTER";
  
  static {
    DictionaryResource topRes = TcPlugin.getDefault().getResources();
    m_panelRes = (ContainerResource)topRes.findComponent("LockAttributesPanel"); 
  }
  
  public LockAttributesDialog(Shell parentShell, IMethod method) {
    super(parentShell, "Specify Named-Lock Attributes", null,
          PatternHelper.getExecutionPattern(method),
          MessageDialog.NONE,
          new String[] {IDialogConstants.OK_LABEL,
                        IDialogConstants.CANCEL_LABEL}, 0);
    m_method = method;
  }
  
  protected Control createCustomArea(Composite parent) {
    Composite composite = new Composite(parent, SWT.EMBEDDED);
    
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    Frame frame = SWT_AWT.new_Frame(composite);
  
    JRootPane rootPane = new JRootPane();
    Panel     root     = new Panel(new BorderLayout());
    Container panel    = new Container(m_panelRes);

    frame.add(root);
    root.add(rootPane);
    rootPane.getContentPane().add(panel);

    m_nameField = (TextField)panel.findComponent("NameField");
    m_nameField.setText(m_method.getElementName());
    
    m_typeGroup = (ButtonGroup)panel.findComponent("TypeGroup");
    m_typeGroup.setSelectedIndex(1);

    panel.getActionMap().put(ENTER_CMD, new XAbstractAction() {
      public void actionPerformed(ActionEvent ae) {
        acceptValues();
      }
    });
    
    InputMap inputMap = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), ENTER_CMD);

    frame.setVisible(true);

    return composite;
  }
  
  private void acceptValues() {
    getShell().getDisplay().syncExec(new Runnable() {
      public void run(){
        buttonPressed(IDialogConstants.OK_ID);
      }
    });
  }
  
  /*
   * Return the user-specified lock name.
   */
  public String getLockName() {
    return m_nameField.getText();
  }
  
  /*
   * Return the user-specified lock level:
   *  LockLevel.READ
   *  LockLevel.WRITE
   */
  public LockLevel.Enum getLockLevel() {
    return "WriteButton".equals(m_typeGroup.getSelected()) ?
        LockLevel.WRITE : LockLevel.READ;
  }
}
