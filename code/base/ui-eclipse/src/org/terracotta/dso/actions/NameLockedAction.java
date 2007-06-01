/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.terracotta.dso.PatternHelper;
import org.terracotta.dso.dialogs.LockAttributesDialog;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.LockLevel;

/**
 * Marks the currently selected method as being name-locked. Creates a default name based on the simple name of the
 * method and sets the lock-type to WRITE.
 * 
 * @see org.eclipse.jdt.core.IMethod
 * @see BaseAction
 * @see org.terracotta.dso.ConfigurationHelper.isNameLocked
 * @see org.terracotta.dso.ConfigurationHelper.ensureNameLocked
 * @see org.terracotta.dso.ConfigurationHelper.ensureNotNameLocked
 */

public class NameLockedAction extends BaseAction {
  public NameLockedAction() {
    super("Name Locked", AS_CHECK_BOX);
  }

  public void setJavaElement(IJavaElement element) {
    super.setJavaElement(element);
    setChecked(getConfigHelper().isNameLocked(element));
  }

  public void performAction(Event event) {
    if (isChecked()) {
      Shell shell = ActionUtil.findSelectedEditorPart().getSite().getShell();
      LockAttributesDialog dialog = new LockAttributesDialog(shell, PatternHelper.getExecutionPattern(m_element));
      dialog.addValueListener(new UpdateEventListener() {
        public void handleUpdate(UpdateEvent e) {
          Object[] values = (Object[]) e.data;
          getConfigHelper().ensureNameLocked(m_element, (String) values[0], (LockLevel.Enum) values[1]);
        }
      });
      dialog.open();
    } else {
      getConfigHelper().ensureNotNameLocked(m_element);
    }

    inspectCompilationUnit();
  }
}
