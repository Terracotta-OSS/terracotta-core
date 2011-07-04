/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.tc.util.event.EventMulticaster;
import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.LockLevel;

public class NamedLockDialog extends MessageDialog {

  private static final String    TITLE = "Specify Named-Lock Attributes";
  private final EventMulticaster m_valueListener;
  private Layout                 m_layout;

  public NamedLockDialog(Shell shell, String message) {
    super(shell, TITLE, null, message, MessageDialog.NONE, new String[] {
      IDialogConstants.OK_LABEL,
      IDialogConstants.CANCEL_LABEL }, 0);
    this.m_valueListener = new EventMulticaster();
  }

  public void addValueListener(UpdateEventListener listener) {
    m_valueListener.addListener(listener);
  }

  protected Control createCustomArea(Composite parent) {
    m_layout = new Layout(parent);
    return parent;
  }

  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      String lockName = m_layout.m_name.getText();
      if(lockName == null || (lockName = lockName.trim()) == null || lockName.length() == 0) {
        Display.getCurrent().beep();
        m_layout.m_name.forceFocus();
        return;
      }
      LockLevel.Enum lockLevel;
      if (m_layout.m_read.getSelection()) lockLevel = LockLevel.READ;
      else if (m_layout.m_write.getSelection()) lockLevel = LockLevel.WRITE;
      else if (m_layout.m_synchronousWrite.getSelection()) lockLevel = LockLevel.SYNCHRONOUS_WRITE;
      else lockLevel = LockLevel.CONCURRENT;

      m_valueListener.fireUpdateEvent(new UpdateEvent(new Object[] { lockName, lockLevel }));
    }
    super.buttonPressed(buttonId);
  }

  // --------------------------------------------------------------------------------

  private static class Layout {
    private static final String NAME              = "Name";
    private static final String TYPE              = "Type";
    private static final String READ              = "Read";
    private static final String WRITE             = "Write";
    private static final String CONCURRENT        = "Concurrent";
    private static final String SYNCHRONOUS_WRITE = "Synchronous-write";
    private final Text          m_name;
    private final Button        m_read;
    private final Button        m_write;
    private final Button        m_synchronousWrite;
    private final Button        m_concurrent;

    private Layout(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      comp.setLayout(new GridLayout());
      comp.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

      Group typeGroup = new Group(comp, SWT.NONE);
      typeGroup.setText(TYPE);
      typeGroup.setLayout(new GridLayout(4, false));
      m_read = new Button(typeGroup, SWT.RADIO);
      m_read.setText(READ);
      m_write = new Button(typeGroup, SWT.RADIO);
      m_write.setText(WRITE);
      m_write.setSelection(true);
      m_synchronousWrite = new Button(typeGroup, SWT.RADIO);
      m_synchronousWrite.setText(SYNCHRONOUS_WRITE);
      m_concurrent = new Button(typeGroup, SWT.RADIO);
      m_concurrent.setText(CONCURRENT);

      Group nameGroup = new Group(comp, SWT.NONE);
      nameGroup.setText(NAME);
      nameGroup.setLayout(new GridLayout());
      nameGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      Label requiredLabel = new Label(nameGroup, SWT.LEFT);
      requiredLabel.setText("* - required field");
      requiredLabel.setForeground(requiredLabel.getDisplay().getSystemColor(SWT.COLOR_RED));
      m_name = new Text(nameGroup, SWT.BORDER);
      m_name.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }
  }
}
