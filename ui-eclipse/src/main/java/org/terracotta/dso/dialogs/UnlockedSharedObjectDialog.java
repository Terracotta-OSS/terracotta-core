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
import com.tc.admin.common.UnlockedSharedWorkState;
import com.tc.object.appevent.AbstractLockEvent;
import com.tc.object.appevent.UnlockedSharedObjectEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class UnlockedSharedObjectDialog extends AbstractLockDialog {

  public UnlockedSharedObjectDialog(Shell parentShell, UnlockedSharedObjectEvent event) {
    super(parentShell, "Attempt to modify shared data outside a lock", event);
  }

  protected AbstractWorkState createWorkState(AbstractLockEvent lockEvent) {
    return new UnlockedSharedWorkState((UnlockedSharedObjectEvent) lockEvent);
  }

  // private UnlockedSharedWorkState getUnlockedSharedObjectWorkState() {
  // return (UnlockedSharedWorkState) getWorkState();
  // }
  //
  // private UnlockedSharedObjectEvent getUnlockedSharedObjectEvent() {
  // return (UnlockedSharedObjectEvent) getApplicationEvent();
  // }
  //
  // private UnlockedSharedObjectEventContext getUnlockedSharedObjectEventContext() {
  // return getUnlockedSharedObjectEvent().getUnlockedSharedObjectEventContext();
  // }

  protected String getIssueName() {
    return "Unlocked shared object access";
  }

  protected String getDialogSettingsSectionName() {
    return TcPlugin.PLUGIN_ID + ".UNLOCKED_SHARED_OBJECT_DIALOG_SECTION"; //$NON-NLS-1$
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
