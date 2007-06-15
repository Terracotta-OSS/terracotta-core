/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.EventMulticaster;
import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.LockLevel;

public class LockAttributesDialog extends MessageDialog {

  private static final String    TITLE = "Specify Named-Lock Attributes";
  private final Shell            m_parentShell;
  private final EventMulticaster m_valueListener;
  private Layout                 m_layout;

  public LockAttributesDialog(Shell shell, String message) {
    super(shell, TITLE, null, message, MessageDialog.NONE, new String[] {
      IDialogConstants.OK_LABEL,
      IDialogConstants.CANCEL_LABEL }, 0);
    this.m_parentShell = shell;
    this.m_valueListener = new EventMulticaster();
  }
  
  public void addValueListener(UpdateEventListener listener) {
    m_valueListener.addListener(listener);
  }

  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setSize(400, 180);
    SWTUtil.placeDialogInCenter(m_parentShell, shell);
  }

  protected Control createCustomArea(Composite parent) {
    m_layout = new Layout(parent);
    return parent;
  }

  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      LockLevel.Enum lockLevel = null;
      if (m_layout.m_read.getSelection()) lockLevel = LockLevel.READ;
      if (m_layout.m_write.getSelection()) lockLevel = LockLevel.WRITE;
      m_valueListener.fireUpdateEvent(new UpdateEvent(new Object[] { m_layout.m_name.getText(), lockLevel }));
    }
    super.buttonPressed(buttonId);
  }

  // --------------------------------------------------------------------------------

  private static class Layout {
    private static final String NAME  = "Name";
    private static final String TYPE  = "Type";
    private static final String READ  = "Read";
    private static final String WRITE = "Write";
    private final Text          m_name;
    private final Button        m_read;
    private final Button        m_write;

    private Layout(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginHeight = 5;
      gridLayout.marginWidth = 5;
      comp.setLayout(gridLayout);
      comp.setLayoutData(new GridData(GridData.FILL_BOTH));

      Group nameGroup = new Group(comp, SWT.BORDER);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginHeight = 5;
      gridLayout.marginWidth = 5;
      nameGroup.setText(NAME);
      nameGroup.setLayout(gridLayout);
      nameGroup.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
      this.m_name = new Text(nameGroup, SWT.BORDER);
      m_name.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      Group typeGroup = new Group(comp, SWT.BORDER);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginHeight = 5;
      gridLayout.marginWidth = 5;
      typeGroup.setText(TYPE);
      typeGroup.setLayout(gridLayout);
      this.m_read = new Button(typeGroup, SWT.RADIO);
      m_read.setText(READ);
      m_write = new Button(typeGroup, SWT.RADIO);
      m_write.setText(WRITE);
      m_write.setSelection(true);
    }
  }
}
