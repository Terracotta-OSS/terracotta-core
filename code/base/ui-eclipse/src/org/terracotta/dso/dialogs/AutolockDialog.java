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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import com.tc.util.event.EventMulticaster;
import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.LockLevel;

public class AutolockDialog extends MessageDialog {

  private static final String    TITLE = "Specify Autolock Attributes";
  private final EventMulticaster m_valueListener;
  private Layout                 m_layout;

  public AutolockDialog(Shell shell, String message) {
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
      LockLevel.Enum lockLevel;
      if (m_layout.m_read.getSelection()) lockLevel = LockLevel.READ;
      else if (m_layout.m_write.getSelection()) lockLevel = LockLevel.WRITE;
      else if (m_layout.m_synchronousWrite.getSelection()) lockLevel = LockLevel.SYNCHRONOUS_WRITE;
      else lockLevel = LockLevel.CONCURRENT;

      m_valueListener.fireUpdateEvent(new UpdateEvent(new Object[] { m_layout.m_autoSync.getSelection(), lockLevel }));
    }
    super.buttonPressed(buttonId);
  }

  // --------------------------------------------------------------------------------

  private static class Layout {
    private static final String AUTO_SYNCHRONIZE  = "Auto-synchronize";
    private static final String TYPE              = "Type";
    private static final String READ              = "Read";
    private static final String WRITE             = "Write";
    private static final String CONCURRENT        = "Concurrent";
    private static final String SYNCHRONOUS_WRITE = "Synchronous-write";
    private final Button        m_autoSync;
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

      m_autoSync = new Button(comp, SWT.CHECK);
      m_autoSync.setText(AUTO_SYNCHRONIZE);
      m_autoSync.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
    }
  }
}
