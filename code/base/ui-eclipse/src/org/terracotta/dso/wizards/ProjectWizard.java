/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.wizards;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlOptions;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.BootClassHelper;
import org.terracotta.dso.ProjectNature;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.actions.BuildBootJarAction;
import org.terracotta.ui.util.TcUIStatus;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

public class ProjectWizard extends Wizard {
  private SetupWizardPage m_page;
  private IJavaProject    m_javaProject;
  private boolean         m_cancelled;

  public ProjectWizard(IJavaProject javaProject) {
    super();

    m_javaProject = javaProject;
    m_cancelled = false;

    setNeedsProgressMonitor(true);
  }

  public boolean performCancel() {
    m_cancelled = true;
    return super.performCancel();
  }

  public void addPages() {
    addPage(m_page = new SetupWizardPage(m_javaProject));
  }

  public IRunnableWithProgress getWorker() {
    IRunnableWithProgress op = new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor) throws InvocationTargetException {
        try {
          doFinish(monitor);
        } catch (CoreException e) {
          throw new InvocationTargetException(e);
        } finally {
          monitor.done();
        }
      }
    };

    return op;
  }

  public boolean performFinish() {
    if (m_cancelled) { return true; }

    try {
      getContainer().run(false, true, getWorker());
    } catch (InterruptedException e) {
      return false;
    } catch (InvocationTargetException e) {
      Throwable cause = e.getTargetException();
      TcPlugin.getDefault().openError("Problem setting up project", cause);
      return false;
    }

    return true;
  }

  private void handleProblem(String msg, Throwable t, IProgressMonitor monitor) throws CoreException {
    t.printStackTrace();
    monitor.setCanceled(true);
    if (!(t instanceof CoreException)) {
      t = new CoreException(TcUIStatus.createError(-1, msg, t));
    }
    throw (CoreException) t;
  }

  public void doFinish(IProgressMonitor monitor) throws CoreException {
    int stepIndex = 1;
    String step = "Adding Terracotta nature";

    monitor.beginTask(step, IProgressMonitor.UNKNOWN);
    try {
      addTerracottaNature(monitor);
    } catch (Throwable t) {
      handleProblem(step, t, monitor);
    }
    monitor.worked(stepIndex++);

    monitor.subTask(step = "Creating Terracotta folder");
    try {
      ensureConfigExists(monitor);
    } catch (Throwable t) {
      handleProblem(step, t, monitor);
    }
    monitor.worked(stepIndex++);

    if (!BootClassHelper.canGetBootTypes(m_javaProject)) {
      monitor.subTask(step = "Building BootJar");
      try {
        buildBootJar(monitor);
      } catch (Throwable t) {
        handleProblem(step, t, monitor);
      }
      monitor.worked(stepIndex++);
    }

    // monitor.subTask(step = "Inspecting classes");
    // try {
    // inspectProject(monitor);
    // } catch(Throwable t) {
    // handleProblem(step, t, monitor);
    // }
    // monitor.worked(stepIndex++);

    TcPlugin.getDefault().updateDecorators();
    TcPlugin.getDefault().notifyProjectActions(m_javaProject.getProject());

    final IWorkbench workbench = PlatformUI.getWorkbench();
    final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

    if (window instanceof ApplicationWindow) {
      ApplicationWindow appWin = (ApplicationWindow) window;
      String msg = "Finished adding Terracotta Nature.";

      appWin.setStatus(msg);
    }
  }

  private void addTerracottaNature(IProgressMonitor monitor) throws CoreException {
    IProject proj = m_javaProject.getProject();
    IProjectDescription description = proj.getDescription();
    String[] natures = description.getNatureIds();
    String[] newNatures = new String[natures.length + 1];

    java.lang.System.arraycopy(natures, 0, newNatures, 0, natures.length);

    newNatures[natures.length] = ProjectNature.NATURE_ID;
    description.setNatureIds(newNatures);
    proj.refreshLocal(IResource.DEPTH_ZERO, monitor);
    proj.setDescription(description, monitor);
  }

  private String getDomainConfigurationPath() {
    if (m_page == null) {
      return TcPlugin.DEFAULT_CONFIG_FILENAME;
    } else {
      return m_page.getDomainConfigurationPath();
    }
  }

  private String getServerOptions() {
    if (m_page == null) {
      return TcPlugin.DEFAULT_SERVER_OPTIONS;
    } else {
      return m_page.getServerOptions();
    }
  }

  private void ensureConfigExists(IProgressMonitor monitor) throws CoreException {
    TcPlugin plugin = TcPlugin.getDefault();
    IProject project = m_javaProject.getProject();
    String configPath = getDomainConfigurationPath();
    String serverOpts = getServerOptions();

    if (configPath == null || configPath.length() == 0) {
      configPath = TcPlugin.DEFAULT_CONFIG_FILENAME;
    }
    if (!configPath.endsWith(".xml")) {
      configPath = configPath.concat(".xml");
    }
    final IFile configFile = project.getFile(new Path(configPath));
    if (!configFile.exists()) {
      InputStream is = null;

      ensureParent(configFile);
      try {
        XmlOptions xmlOpts = plugin.getXmlOptions();
        is = TcPlugin.createTemplateConfigDoc().newInputStream(xmlOpts);
        configFile.create(is, true, monitor);
      } catch (CoreException ce) {
        String step = "Creating default Terracotta config file";
        IStatus status = JavaUIStatus.createError(-1, step, ce);
        IOUtils.closeQuietly(is);
        throw new CoreException(status);
      } finally {
        IOUtils.closeQuietly(is);
      }
    }

    m_javaProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
    plugin.setup(project, configPath, serverOpts);
  }

  private static void ensureParent(IFile file) throws CoreException {
    if (!file.exists()) {
      IContainer parent = file.getParent();
      if (!parent.exists()) {
        ensureParent(parent);
      }
    }
  }

  private static void ensureParent(IContainer container) throws CoreException {
    if (!container.exists()) {
      IContainer parent = container.getParent();
      if (!parent.exists()) {
        ensureParent(parent);
      }
      if (container instanceof IFolder) {
        ((IFolder) container).create(true, true, null);
      }
    }
  }

  // private void inspectProject(IProgressMonitor monitor) throws JavaModelException, CoreException {
  // TcPlugin plugin = TcPlugin.getDefault();
  // IPackageFragment[] fragments = m_javaProject.getPackageFragments();
  // IPackageFragment fragment;
  // ICompilationUnit[] cus;
  //
  // for (int i = 0; i < fragments.length; i++) {
  // fragment = fragments[i];
  //
  // if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
  // cus = fragment.getCompilationUnits();
  //
  // for (int j = 0; j < cus.length; j++) {
  // monitor.subTask(cus[j].getResource().getLocation().toString());
  // plugin.inspect(cus[j]);
  // }
  // }
  // }
  // }

  private void buildBootJar(IProgressMonitor monitor) {
    BuildBootJarAction bbja = new BuildBootJarAction(m_javaProject);
    bbja.run((IAction) null);
  }

  public boolean canFinish() {
    return m_page.isPageComplete();
  }
}
