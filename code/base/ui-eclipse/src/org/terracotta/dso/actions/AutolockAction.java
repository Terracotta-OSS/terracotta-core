/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.swt.widgets.Shell;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.PatternHelper;
import org.terracotta.dso.dialogs.AutolockDialog;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.LockLevel;

/**
 * Mark the currently selected IMethod as being autolocked.
 * 
 * @see org.terracotta.dso.ConfigurationHelper.isAutolocked
 * @see org.terracotta.dso.ConfigurationHelper.ensureAutolocked
 * @see org.terracotta.dso.ConfigurationHelper.ensureNotAutolocked
 */

public class AutolockAction extends BaseAction {
  public AutolockAction() {
    super("Autolock", AS_CHECK_BOX);
  }
  
  public void setJavaElement(IJavaElement element) {
    super.setJavaElement(element);
    setChecked(getConfigHelper().isAutolocked(element));
  }
  
  public void performAction() {
    final ConfigurationHelper helper = getConfigHelper();
    
    if(isChecked()) {
      Shell shell = ActionUtil.findSelectedEditorPart().getSite().getShell();
      final String pattern = PatternHelper.getExecutionPattern(m_element);
      AutolockDialog dialog = new AutolockDialog(shell, pattern);
      dialog.addValueListener(new UpdateEventListener() {
        public void handleUpdate(UpdateEvent e) {
          Object[] values = (Object[]) e.data;
          boolean autoSync = (Boolean)values[0];
          LockLevel.Enum level = (LockLevel.Enum) values[1];
          Autolock lock = helper.addNewAutolock(pattern, level);
          lock.setAutoSynchronized(autoSync);
        }
      });
      dialog.open();
    }
    else {
      helper.ensureNotAutolocked(m_element);
    }

    inspectCompilationUnit();
  }
}
