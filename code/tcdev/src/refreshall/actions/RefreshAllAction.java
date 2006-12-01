/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package refreshall.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Our sample action implements workbench action delegate. The action proxy will
 * be created by the workbench and shown in the UI. When the user tries to use
 * the action, this delegate will be created and execution will be delegated to
 * it.
 *
 * @see IWorkbenchWindowActionDelegate
 */
public class RefreshAllAction implements IWorkbenchWindowActionDelegate {
    private IWorkbenchWindow window;

    private IWorkspace workspace;

    /**
     * The constructor.
     */
    public RefreshAllAction() {
    }

    /**
     * The action has been activated. The argument of the method represents the
     * 'real' action sitting in the workbench UI.
     *
     * @throws CoreException
     *
     * @see IWorkbenchWindowActionDelegate#run
     */
    public void run(IAction action) {
        boolean autoOn = isAutoBuildEnabled();
        if (autoOn)
            toggleAutomaticBuild(false);
        try {
            doRefresh(action);
        } finally {
            if (autoOn)
                toggleAutomaticBuild(true);
        }
    }

    public void doRefresh(IAction action) {
        final IProject[] projects = workspace.getRoot().getProjects();
        if (projects.length == 0)
            return;

        ProgressMonitorDialog progress = new ProgressMonitorDialog(window
                .getShell());

        IRunnableWithProgress run = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
                monitor.beginTask("Refreshing all projects", projects.length);

                for (int i = 0; i < projects.length; i++) {
                    IProject project = projects[i];
                    monitor.subTask(project.getName());
                    try {
                        project.refreshLocal(IResource.DEPTH_INFINITE, null);
                    } catch (CoreException e) {
                        throw new RuntimeException(e);
                    }
                    monitor.worked(1);
                }

                monitor.done();
            }
        };

        try {
            progress.run(true, true, run);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private boolean isAutoBuildEnabled() {
        return workspace.getDescription().isAutoBuilding();
    }

    private void toggleAutomaticBuild(boolean on) {
        IWorkspaceDescription desc = workspace.getDescription();
        desc.setAutoBuilding(on);
        try {
            workspace.setDescription(desc);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Selection in the workbench has been changed. We can change the state of
     * the 'real' action here if we want, but this can only happen after the
     * delegate has been created.
     *
     * @see IWorkbenchWindowActionDelegate#selectionChanged
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

    /**
     * We can use this method to dispose of any system resources we previously
     * allocated.
     *
     * @see IWorkbenchWindowActionDelegate#dispose
     */
    public void dispose() {
    }

    /**
     * We will cache window object in order to be able to provide parent shell
     * for the message dialog.
     *
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow window) {
        this.window = window;
        this.workspace = ResourcesPlugin.getWorkspace();
    }
}