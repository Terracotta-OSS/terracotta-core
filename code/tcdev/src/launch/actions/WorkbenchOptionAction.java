package launch.actions;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import refreshall.Activator;


public class WorkbenchOptionAction implements IWorkbenchWindowActionDelegate {

	public static final String KEY = "testWith14"; 
	
	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	public void run(IAction action) {
		Preferences prefs = Activator.getDefault().getPluginPreferences();
		prefs.setValue(KEY, action.isChecked());
		Activator.getDefault().savePluginPreferences();
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
