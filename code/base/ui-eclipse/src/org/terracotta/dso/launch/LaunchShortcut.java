/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.launch;

import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

public class LaunchShortcut implements ILaunchShortcut {
  private ILaunchShortcut fDelegate;

  public LaunchShortcut() {
    try {
      fDelegate = (ILaunchShortcut) Class.forName(LaunchShortcut.class.getName() + "33").newInstance();
    } catch (Throwable t) {
      try {
        fDelegate = (ILaunchShortcut) Class.forName(LaunchShortcut.class.getName() + "34").newInstance();
      } catch (Throwable t2) {
        throw new RuntimeException(t2);
      }
    }
  }

  public void launch(ISelection selection, String mode) {
    fDelegate.launch(selection, mode);
  }

  public void launch(IEditorPart editor, String mode) {
    fDelegate.launch(editor, mode);
  }
}
