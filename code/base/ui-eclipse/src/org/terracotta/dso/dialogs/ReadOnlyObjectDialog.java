/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.dialogs;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.terracotta.dso.TcPlugin;

import com.tc.admin.common.AbstractResolutionAction;
import com.tc.admin.common.AbstractWorkState;
import com.tc.admin.common.ReadOnlyWorkState;
import com.tc.object.appevent.AbstractLockEvent;
import com.tc.object.appevent.ReadOnlyObjectEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class ReadOnlyObjectDialog extends AbstractLockDialog {

  public ReadOnlyObjectDialog(Shell parentShell, ReadOnlyObjectEvent event) {
    super(parentShell, "Attempt to modify shared data in a read lock", event);
  }

  protected AbstractWorkState createWorkState(AbstractLockEvent lockEvent) {
    return new ReadOnlyWorkState((ReadOnlyObjectEvent) lockEvent);
  }

  // private ReadOnlyWorkState getReadOnlyWorkState() {
  // return (ReadOnlyWorkState) getWorkState();
  // }

  // private ReadOnlyObjectEvent getReadOnlyObjectEvent() {
  // return (ReadOnlyObjectEvent) getApplicationEvent();
  // }

  // private ReadOnlyObjectEventContext getReadOnlyObjectEventContext() {
  // return getReadOnlyObjectEvent().getReadOnlyObjectEventContext();
  // }

  protected String getIssueName() {
    return "Read-locked shared object modification";
  }

  protected String getDialogSettingsSectionName() {
    return TcPlugin.PLUGIN_ID + ".READONLY_OBJECT_DIALOG_SECTION"; //$NON-NLS-1$
  }

  protected Control createCustomArea(Composite parent) {
    Control customArea = super.createCustomArea(parent);

    return customArea;
  }

  protected AbstractResolutionAction[] createActions(AbstractWorkState workState) {
    AbstractResolutionAction[] actions = super.createActions(workState);
    ArrayList list = new ArrayList(Arrays.asList(actions));

    list.add(new AddLockAction());

    return (AbstractResolutionAction[]) list.toArray(new AbstractResolutionAction[0]);
  }
}
