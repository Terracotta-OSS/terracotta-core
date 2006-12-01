/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.terracotta.dso.ProjectNature;
import org.terracotta.dso.TcPlugin;
import com.terracottatech.configV2.ConfigurationModel;
import com.terracottatech.configV2.Server;
import com.terracottatech.configV2.Servers;
import com.terracottatech.configV2.System;
import com.terracottatech.configV2.TcConfigDocument;
import com.terracottatech.configV2.TcConfigDocument.TcConfig;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProjectWizard extends Wizard {
  private SetupWizardPage m_page;
  private IJavaProject    m_javaProject;
  private boolean         m_cancelled;

  public ProjectWizard(IJavaProject javaProject) {
    super();
    
    m_javaProject = javaProject;
    m_cancelled   = false;
    
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
      public void run(IProgressMonitor monitor)
        throws InvocationTargetException
      {
        try {
          doFinish(monitor);
        } catch(CoreException e) {
          throw new InvocationTargetException(e);
        } finally {
          monitor.done();
        }
      }
    };
    
    return op;
 }
  
  public boolean performFinish() {
    if(m_cancelled) {
      return true;
    }
    
    try {
      getContainer().run(false, true, getWorker());
    } catch(InterruptedException e) {
      return false;
    } catch(InvocationTargetException e) {
      Throwable cause = e.getTargetException();
      TcPlugin.getDefault().openError("Problem setting up project", cause);
      return false;
    }
    
    return true;
  }
  
  private void handleProblem(String msg, Throwable t, IProgressMonitor monitor)
    throws CoreException
  {
    t.printStackTrace();
    monitor.setCanceled(true);
    if(!(t instanceof CoreException)) {
      t = new CoreException(JavaUIStatus.createError(-1, msg, t));
    }
    throw (CoreException)t;    
  }
  
  public void doFinish(IProgressMonitor monitor)
    throws CoreException
  {
    String step = "Adding Terracotta nature";
    
    monitor.beginTask(step, IProgressMonitor.UNKNOWN);
    try {
      addTerracottaNature(monitor);
    } catch(Throwable t) {
      handleProblem(step, t, monitor);
    }
    monitor.worked(1);
    
    monitor.subTask(step = "Creating Terracotta folder");
    try {
      createTerracottaFolder(monitor);
    } catch(Throwable t) {
      handleProblem(step, t, monitor);
    }
    monitor.worked(2);

    monitor.subTask(step = "Inspecting classes");
    try {
      inspectProject(monitor);
    } catch(Throwable t) {
      handleProblem(step, t, monitor);
    }
    monitor.worked(3);

    TcPlugin.getDefault().updateDecorators();
    TcPlugin.getDefault().notifyProjectActions(m_javaProject.getProject());
    
    final IWorkbench       workbench = PlatformUI.getWorkbench();
    final IWorkbenchWindow window    = workbench.getActiveWorkbenchWindow();
    
    if(window instanceof ApplicationWindow) {
      ApplicationWindow appWin = (ApplicationWindow)window;
      String            msg    = "Finished adding Terracotta Nature.";
      
      appWin.setStatus(msg);
    }
  }

  private void addTerracottaNature(IProgressMonitor monitor)
    throws CoreException
  {
    IProject            proj        = m_javaProject.getProject();
    IProjectDescription description = proj.getDescription();
    String[]            natures     = description.getNatureIds();
    String[]            newNatures  = new String[natures.length + 1];
    
    java.lang.System.arraycopy(natures, 0, newNatures, 0, natures.length);

    newNatures[natures.length] = ProjectNature.NATURE_ID;
    description.setNatureIds(newNatures);
    proj.refreshLocal(IResource.DEPTH_ZERO, monitor);
    proj.setDescription(description, monitor);
  }
  
  private String getDomainConfigurationPath() {
    if(m_page == null) {
      return TcPlugin.DEFAULT_CONFIG_FILENAME;
    } else {
      return m_page.getDomainConfigurationPath();
    }
  }
  
  private String getServerOptions() {
    if(m_page == null) {
      return TcPlugin.DEFAULT_SERVER_OPTIONS;
    } else {
      return m_page.getServerOptions();
    }
 }
  
  private void createTerracottaFolder(IProgressMonitor monitor)
    throws CoreException
  {
    TcPlugin plugin     = TcPlugin.getDefault();
    IProject proj       = m_javaProject.getProject();
    String   configPath = getDomainConfigurationPath();
    String   serverOpts = getServerOptions();
    IFolder  folder     = proj.getFolder("terracotta");
    
    plugin.setup(proj, configPath, serverOpts);
    
    if(!folder.exists()) {
      folder.create(true, true, monitor);
    }

    /**
     * Make sure the terracotta artifact directory isn't considered
     * a package fragment.
     */
    IPath             relPath   = folder.getProjectRelativePath();
    IPath             exclusion = relPath.addTrailingSeparator();
    ArrayList         list      = new ArrayList();
    IClasspathEntry[] entries   = m_javaProject.getRawClasspath();
    IClasspathEntry   entry;
    
    for(int i = 0; i < entries.length; i++) {
      entry = entries[i];
      
      if(entry.getEntryKind() == IClasspathEntry.CPE_SOURCE &&
         entry.getPath().equals(m_javaProject.getPath()))
      {
        List exclusions = new ArrayList(Arrays.asList(entry.getExclusionPatterns()));

        exclusions.add(exclusion);
        entry = JavaCore.newSourceEntry(entry.getPath(),
                                        entry.getInclusionPatterns(),
                                        (IPath[])exclusions.toArray(new IPath[0]),
                                        entry.getOutputLocation(),
                                        entry.getExtraAttributes());
      }
      list.add(entry);
    }
    
    entries = (IClasspathEntry[])list.toArray(new IClasspathEntry[0]);
    m_javaProject.setRawClasspath(entries, monitor);

    /**
     * Ensure a config file exists.
     */
    if(configPath == null || configPath.length() == 0) {
      configPath = TcPlugin.DEFAULT_CONFIG_FILENAME;
    }
    if(!configPath.endsWith(".xml")) {
      configPath = configPath.concat(".xml");
    }
    final IFile configFile = proj.getFile(new Path(configPath));
    if(!configFile.exists()) {
      InputStream is = null;
      
      ensureParent(configFile);
      try {
        XmlOptions xmlOpts = plugin.getXmlOptions();
        
        is = createTemplateConfigDoc().newInputStream(xmlOpts);                
        configFile.create(is, true, monitor);
      } catch(CoreException ce) {
        String  step   = "Creating default Terracotta config file";
        IStatus status = JavaUIStatus.createError(-1, step, ce);
        
        IOUtils.closeQuietly(is);
        throw new CoreException(status);
      } finally {
        IOUtils.closeQuietly(is);
      }
    }
    
    m_javaProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
  }

  private static void ensureParent(IFile file) throws CoreException {
    if(!file.exists()) {
      IContainer parent = file.getParent();
    
      if(!parent.exists()) {
        ensureParent(parent);
      }
    }
  }
  
  private static void ensureParent(IContainer container) throws CoreException {
    if(!container.exists()) {
      IContainer parent = container.getParent();
    
      if(!parent.exists()) {
        ensureParent(parent);
      }

      if(container instanceof IFolder) {
        ((IFolder)container).create(true, true, null);
      }
    }
  }
  
  private void inspectProject(IProgressMonitor monitor)
    throws JavaModelException,
           CoreException
  {
    TcPlugin           plugin    = TcPlugin.getDefault();
    IPackageFragment[] fragments = m_javaProject.getPackageFragments();
    IPackageFragment   fragment;
    ICompilationUnit[] cus;
    
    for(int i = 0; i < fragments.length; i++) {
      fragment = fragments[i];
      
      if(fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
        cus = fragment.getCompilationUnits();
        
        for(int j = 0; j < cus.length; j++) {
          monitor.subTask(cus[j].getResource().getLocation().toString());
          plugin.inspect(cus[j]);
        }
      }
    }
  }
  
  private static TcConfigDocument createTemplateConfigDoc() {
    TcConfigDocument doc     = TcConfigDocument.Factory.newInstance();
    TcConfig         config  = doc.addNewTcConfig();
    System           system  = config.addNewSystem();
    Servers          servers = config.addNewServers();
    Server           server  = servers.addNewServer();

    system.setConfigurationModel(ConfigurationModel.DEVELOPMENT);

    server.setName("localhost");
    server.setDsoPort(9510);
    server.setJmxPort(9520);
    server.setData("terracotta/server-data");
    server.setLogs("terracotta/server-logs");
    
    config.addNewClients().setLogs("terracotta/client-logs");
    
    return doc;
  }

  public boolean canFinish() {
    return m_page.isPageComplete();
  }
}
