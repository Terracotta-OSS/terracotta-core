/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.dijon.DictionaryResource;
import org.eclipse.core.commands.common.EventManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.BinaryMember;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.osgi.framework.BundleContext;
import org.terracotta.dso.actions.ActionUtil;
import org.terracotta.dso.actions.IProjectAction;
import org.terracotta.dso.decorator.AdaptedModuleDecorator;
import org.terracotta.dso.decorator.AdaptedPackageFragmentDecorator;
import org.terracotta.dso.decorator.AdaptedTypeDecorator;
import org.terracotta.dso.decorator.AutolockedDecorator;
import org.terracotta.dso.decorator.ConfigFileDecorator;
import org.terracotta.dso.decorator.DistributedMethodDecorator;
import org.terracotta.dso.decorator.ExcludedModuleDecorator;
import org.terracotta.dso.decorator.ExcludedTypeDecorator;
import org.terracotta.dso.decorator.NameLockedDecorator;
import org.terracotta.dso.decorator.RootDecorator;
import org.terracotta.dso.decorator.ServerRunningDecorator;
import org.terracotta.dso.decorator.TransientDecorator;
import org.terracotta.dso.editors.ConfigurationEditor;
import org.terracotta.dso.wizards.ProjectWizard;

import com.tc.admin.common.InputStreamDrainer;
import com.tc.config.Loader;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * The Terracotta plugin.  The whole enchilada centers around this singleton.
 *
 * The primary duties of this class are to manage the configuration information
 * associated with a Terracotta Eclipse project.
 * 
 * Configuration information is stored as session properties in the project, which
 * are not persistent across sessions.  The config information is made up of:
 *
 *  1) the config file XML document
 *  2) the parsed form of the XML document, aka the config object
 *  3) the line length information of the config document
 *  3) a configuration helper, used by various popup actions
 *  
 *  After successfully parsing the config document, items 1-3 above are
 *  serialized to the plugin's private working area. Subsequent lookups are
 *  first attempted on the serialized form.  When the config data changes, the
 *  serialized form is cleared.
 *  @see loadConfiguration(IProject)
 *  
 *  When there are errors instantiating the config document, those XmlErrors are
 *  turned into SAXMarkers on the document, which appear as error indicators in
 *  the config document editor.
 *  @see handleXmlErrors(IFile, LineLengths, Iterator)
 *  
 *  At times, it is necessary to inspect a module to determine where to add
 *  annotations denoting configuration-related Java elements, such as a particular
 *  method being locked.
 *  @see inspect(ICompilationUnit).
 *  
 *  Another responsibility of this class is to launch the server using the config
 *  information, additional boot jar, etc.
 *  @see launchServer(IJavaProject, String, String, String)
 *  
 *  Finally, this class provides methods for updating the annotations used to indicate
 *  various states related to the config information, such as a field being a root.
 *  @see updateDecorators()
 *   
 * @see org.eclipse.ui.plugin.AbstractUIPlugin
 * @see com.terracottatech.config.TerracottaDomainConfigurationDocument
 * @see com.terracottatech.config.TerracottaDomainConfigurationDocument.TerracottaDomainConfiguration
 */

public class TcPlugin extends AbstractUIPlugin
  implements QualifiedNames,
             IJavaLaunchConfigurationConstants,
             TcPluginStatusConstants
{
  private static TcPlugin           m_plugin;
  private DictionaryResource        m_topRes;
  private Loader                    m_configLoader;
  private CompilationUnitVisitor    m_compilationUnitVisitor;
  private ResourceListener          m_resourceListener;
  private ResourceDeltaVisitor      m_resourceDeltaVisitor;
  private XmlOptions                m_xmlOptions;
  private DecoratorUpdateAction     m_decoratorUpdateAction;
  private ArrayList<IProjectAction> m_projectActionList;
  private BootClassHelper           m_bootClassHelper;
  
  public  static final String PLUGIN_ID     = "org.terracotta.dso";
  private static final String RESOURCE_FILE = "Resources.xml";
  
  public static final String DEFAULT_CONFIG_FILENAME = "tc-config.xml";
  public static final String DEFAULT_SERVER_OPTIONS  = "-Xms256m -Xmx256m";
  
  public TcPlugin() {
    super();
  }

  public static String getPluginId() {
    return PLUGIN_ID;
  }

  public BootClassHelper getBootClassHelper() {
    return m_bootClassHelper;
  }
  
  public boolean isBootClass(final ICompilationUnit module) {
    if(m_bootClassHelper != null) {
      return module != null ? isBootClass(module.findPrimaryType()) : false;
    }
    return false;
  }
  
  public boolean isBootClass(final IClassFile classFile) {
    if(m_bootClassHelper != null && classFile != null) {
      try {
        return isBootClass(classFile.getType());
      } catch(JavaModelException jme) {/**/}
    }
    return false;
  }
  
  public boolean isBootClass(final IType type) {
    if(m_bootClassHelper != null && type != null) {
      return isBootClass(PatternHelper.getFullyQualifiedName(type));
    }
    return false;
  }
  
  public boolean isBootClass(String name) {
    return m_bootClassHelper != null ? m_bootClassHelper.isAdaptable(name) : false;
  }
  
  public void registerProjectAction(IProjectAction action) {
    if(action != null) {
      m_projectActionList.add(action);
    }
  }
  
  public void notifyProjectActions(IProject project) {
    Iterator iter = m_projectActionList.iterator();
    
    while(iter.hasNext()) {
      ((IProjectAction)iter.next()).update(project);
    }
  }
  
  private void addResourceChangeListener() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(
      m_resourceListener,
      IResourceChangeEvent.POST_BUILD);
  }
  
  private IPath m_location;
  
  public IPath getLocation() {
    return m_location;
  }

  public IPath getLibDirPath() {
    return getLocation().append("lib");
  }
  
  public void start(BundleContext context) throws Exception {
    super.start(context);

    m_plugin = this;
    m_configLoader = new Loader();
    
    // TODO: after we remove 3.1 support, change to FileLocator.resolve
    URL url = Platform.resolve(context.getBundle().getEntry("/"));
    m_location = new Path(url.getPath()).removeTrailingSeparator();

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch(Exception e) {
      /**/
    }
    
    InputStream is = null;
    try {
      is       = getClass().getResourceAsStream(RESOURCE_FILE);
      m_topRes = DictionaryResource.load(is);
      is       = null;
    } catch(Exception e) {
      throw new RuntimeException("Error loading resource file '"+RESOURCE_FILE+"'", e);
    } finally {
      if(is != null) {
        IOUtils.closeQuietly(is);
      }
    }

    JavaCore.addElementChangedListener(
      new ElementChangedListener(),
      ElementChangedEvent.POST_RECONCILE);
    
    m_resourceListener = new ResourceListener();
    addResourceChangeListener();
    
    IPath tcInstallPath = getLocation();
    if(tcInstallPath != null) {
      System.setProperty("tc.install", tcInstallPath.toString());
    }
    
    m_xmlOptions = createXmlOptions();
    m_projectActionList = new ArrayList<IProjectAction>();
    
    IAdapterManager manager = Platform.getAdapterManager();
    IAdapterFactory factory = new JavaElementAdapter();
    manager.registerAdapters(factory, IField.class);
    manager.registerAdapters(factory, IType.class);
    manager.registerAdapters(factory, IMethod.class);
    manager.registerAdapters(factory, IClassFile.class);

    // TODO: REMOVE the following when 3.1 is no longer supported
    // SourceMethod and BinaryMember are internal types
    manager.registerAdapters(factory, SourceMethod.class);
    manager.registerAdapters(factory, BinaryMember.class);
    
    try {
      m_bootClassHelper = BootClassHelper.initialize();
    } catch(Exception e) {
      buildBootJarForThisVM();
    }
  }

  private void buildBootJarForThisVM() {
    new Thread() {
      public void run() {
        try {
          IPath tcJarPath   = TcPlugin.getDefault().getLibDirPath().append("tc.jar");
          File  bootJarFile = BootJarHelper.getHelper().getBootJarFileForThisVM();
    
          System.setProperty("tc.install-root", getLocation().toOSString());
          
          String[] args = {System.getProperty("eclipse.vm"),
                           "-cp",
                           tcJarPath.toOSString(),
                           "com.tc.object.tools.BootJarTool",
                           "-q",
                           "-o",
                           bootJarFile.getAbsolutePath()};
    
          Process p = Runtime.getRuntime().exec(args);
          
          InputStreamDrainer outReader = new InputStreamDrainer(p.getInputStream());
          InputStreamDrainer errReader = new InputStreamDrainer(p.getErrorStream());
          
          outReader.start();
          errReader.start();
          
          while(true) {
            try {
              p.waitFor();
              break;
            } catch(InterruptedException ie) {/**/}
            
            try {Thread.sleep(1000);} catch(InterruptedException ie) {/**/}
          }
    
          m_bootClassHelper = BootClassHelper.initialize();
        } catch(Throwable t) {
          openError("Plugin Initialization Error", t);
        }
      }
    }.start();
  }
  
  public void stop(BundleContext context) throws Exception {
    ServerTracker.getDefault().shutdownAllServers();
    super.stop(context);
    m_plugin = null;
  }

  public static TcPlugin getDefault() {
    if(m_plugin == null) {
      new TcPlugin();
    }
    return m_plugin;
  }

  public DictionaryResource getResources() {
    return m_topRes;
  }

  public void setup(IProject project, String configFilePath, String serverOpts) {
    clearConfigurationSessionProperties(project);
    setConfigurationFilePath(project, configFilePath);
    setPersistentProperty(project, SERVER_OPTIONS, serverOpts);
    reloadConfiguration(project);    
  }

  public void addTerracottaNature(IJavaProject currentProject) {
    IWorkbench       workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window    = workbench.getActiveWorkbenchWindow();
    Shell            shell     = window != null ? window.getShell() : null;

    try {
      ProjectWizard wizard = new ProjectWizard(currentProject);
      WizardDialog  dialog = new WizardDialog(shell, wizard);
  
      wizard.setWindowTitle("Terracotta Project Wizard");
      dialog.open();
    }
    catch(Exception e) {
      MessageDialog.openInformation(
        shell,
        "Terracotta",
        "Cannot add Terracotta nature:\n" +
        ActionUtil.getStatusMessages(e));
    }
    finally {
      if(shell != null) shell.setCursor(null);
    }
  }

  public void removeTerracottaNature(IJavaProject javaProject) {
    IWorkbench       workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window    = workbench.getActiveWorkbenchWindow();
    Shell            shell     = window != null ? window.getShell() : null;

    try {
      IRunnableWithProgress op = new TCNatureRemover(javaProject);
      new ProgressMonitorDialog(shell).run(false, false, op);
    } catch(InvocationTargetException e) {
      openError("Cannot remove Terracotta nature", e.getCause());
    } catch(InterruptedException e) {/**/}  
  }

  class TCNatureRemover implements IRunnableWithProgress {
    IJavaProject m_javaProject;
    
    TCNatureRemover(IJavaProject javaProject) {
      m_javaProject = javaProject;
    }
    
    public void run(IProgressMonitor monitor)
      throws InvocationTargetException
    {
      try {
        monitor.beginTask("Removing project nature", 4);
        IProject            project     = m_javaProject.getProject();
        IProjectDescription description = project.getDescription();
        String[]            natures     = description.getNatureIds();
        ArrayList<String>   natureList  = new ArrayList<String>();
        
        for(int i = 0; i < natures.length; i++) {
          if(!natures[i].equals(ProjectNature.NATURE_ID)) {
            natureList.add(natures[i]);
          }
        }
        description.setNatureIds(natureList.toArray(new String[0]));
        project.setDescription(description, monitor);
        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        monitor.worked(1);
        
        monitor.beginTask("Closing configuration editor", 4);
        closeConfigurationEditor(project);
        monitor.worked(2);
        
        monitor.beginTask("Stopping server", 4);
        final ServerTracker tracker = ServerTracker.getDefault();
        if(tracker.anyRunning(m_javaProject)) {
          Display.getDefault().syncExec(new Runnable() {
            public void run() {
              tracker.shutdownAllServers();
            }
          });
        }
        monitor.worked(3);
        
        monitor.beginTask("Removing markers", 4);
        IPackageFragment[] fragments = m_javaProject.getPackageFragments();
        ICompilationUnit[] cus;
        IResource          file;
        
        for(int i = 0; i < fragments.length; i++) {
          if(fragments[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
            cus = fragments[i].getCompilationUnits();
            
            for(int j = 0; j < cus.length; j++) {
              file = cus[j].getResource(); 
              monitor.subTask(file.getLocation().toString());
              file.deleteMarkers("org.terracotta.dso.baseMarker", true, IResource.DEPTH_ZERO);
            }
          }
        }
        
        IFile configFile = getConfigurationFile(m_javaProject.getProject());
        if(configFile != null && configFile.exists()) {
          clearSAXMarkers(configFile);
        }
        
        clearConfigProblemMarkers(project);
        clearConfigurationSessionProperties(project);
        notifyProjectActions(project);
        updateDecorators();
        fireConfigurationChange(project);
        
        monitor.worked(4);
      } catch(CoreException e) {
        throw new InvocationTargetException(e);
      }
    }
  }
  
  public ILaunch launchServer(IJavaProject javaProject, String projectName, String serverName)
    throws CoreException
  {
    IProject                 project     = javaProject.getProject();
    ILaunchManager           manager     = DebugPlugin.getDefault().getLaunchManager();
    String                   id          = ID_JAVA_APPLICATION;
    ILaunchConfigurationType type        = manager.getLaunchConfigurationType(id);
    String                   installPath = getLocation().makeAbsolute().toOSString();
    
    ILaunchConfigurationWorkingCopy wc = type.newInstance(null, projectName+"."+serverName);
        
    wc.setAttribute(ATTR_PROJECT_NAME, project.getName());
    wc.setAttribute(ATTR_MAIN_TYPE_NAME, "com.tc.server.TCServerMain");
      
    IFile configFile = getConfigurationFile(project);
    
    if(configFile != null) {
      String configPath = configFile.getLocation().toOSString();
      String vmargs     = " -Dtc.install-root=\""+installPath+"\""+
                          " -Dtc.config=\""+configPath+"\""+
                          " -Dtc.server.name=\""+serverName+"\""+
                          " -Dcom.sun.management.jmxremote"+
                          " -Dcom.sun.management.jmxremote.authenticate=false";
      String origVMArgs = wc.getAttribute(ATTR_VM_ARGUMENTS, "") + " " + getServerOptions(project) + " ";
      
      wc.setAttribute(ATTR_VM_ARGUMENTS, vmargs + origVMArgs);
      wc.setAttribute(ATTR_CLASSPATH_PROVIDER, "org.terracotta.dso.classpathProvider");
      wc.setAttribute(ATTR_WORKING_DIRECTORY,
                      project.getLocation().append("terracotta").makeAbsolute().toOSString());
      
      return wc.launch(ILaunchManager.DEBUG_MODE, null);
    }
    else {
      System.out.println("No config file specified.  Set in project properties");
      return null;
    }
  }
  
  public String getAnyServerName(IProject project) {
    TcConfig config = getConfiguration(project);
    
    if(config != null) {
      Servers servers = config.getServers();
      
      if(servers != null) {
        Server[] serverArr = servers.getServerArray();
        
        for(int i = 0; i < serverArr.length; i++) {
          String name = serverArr[i].getName();
          
          if(name != null && name.length() > 0) {
            return name;
          }
        }
      }
    }

    return "default";
  }
  
  public int getJmxPort(IProject project, String name) {
    TcConfig config = getConfiguration(project);
    
    if(config != null) {
      Servers servers = config.getServers();
      
      if(servers != null) {
        Server[] serverArr = servers.getServerArray();
        Server   server;
        
        for(int i = 0; i < serverArr.length; i++) {
          server = serverArr[i];
          
          if(name.equals(server.getName())) {
            return server.getJmxPort();
          }
        }
      }
    }

    return -1;
  }

  private File getSerializedFile(IFile file) {
    File result = getStateLocation().append(file.getLocation()).toFile();
    
    if(!result.exists()) {
      result.getParentFile().mkdirs();
    }
    
    return result;
  }

  /**
   * Returns the file to which the config document, config object, and
   * line lengths are serialized. This file lives in the plugin's private
   * working area.
   */
  private File getSerializedConfigFile(IProject project) {
    IFile configFile = getConfigurationFile(project);
    return configFile != null ? getSerializedFile(configFile) : null;
  }
  
  private void clearSerializedFile(IFile file) {
    File serialFile = getSerializedFile(file);
    
    if(serialFile.exists()) {
      serialFile.delete();
    }
  }
  
  /**
   * Remove the serialized form of the config information.
   */
  private void clearSerializedConfigFile(IProject project) {
    IFile configFile = getConfigurationFile(project);
    
    if(configFile != null && configFile.exists()) {
      clearSerializedFile(configFile);
    }
  }
  
  private void moveSerializedFile(IFile fromFile, IFile toFile) {
    getSerializedFile(fromFile).renameTo(getSerializedFile(toFile));
  }
  
  /**
   * Test for the existance of, and load, the serialized form our the config
   * information.  Both the config and the LineLengths objects are stored under
   * the name of the config file in the plugin working area, federated by the project
   * name.
   */
  private boolean testLoadSerializedConfigFile(IProject project) {
    LineLengths lineLengths;
    TcConfig    config;
    
    File  serialFile = getSerializedConfigFile(project);
    IFile configFile = getConfigurationFile(project);
    
    if(serialFile != null       &&
       configFile != null &&
       serialFile.exists()      &&
      (serialFile.lastModified() > configFile.getLocalTimeStamp()))
    {
      ObjectInputStream ois = null;
      
      try {
        ois         = new ObjectInputStream(new FileInputStream(serialFile));
        lineLengths = (LineLengths)ois.readObject();
        config      = (TcConfig)ois.readObject();
        
        ois.close();
        ois = null;
        
        setSessionProperty(project, CONFIGURATION_LINE_LENGTHS, lineLengths);
        setSessionProperty(project, CONFIGURATION, config);
        
        return true;
      } catch(Exception e) {
        clearSerializedConfigFile(project);
      } finally {
        if(ois != null) {
          IOUtils.closeQuietly(ois);
        }
      }
    }
    
    return false;
  }

  /**
   * Serialize out the config information to the plugin's private work area.
   */
  private void writeSerializedConfigFile(
    IProject    project,
    LineLengths lineLengths,
    TcConfig    config)
  {
    File               file = getSerializedConfigFile(project);
    ObjectOutputStream oos  = null;
    
    try {
      oos = new ObjectOutputStream(new FileOutputStream(file));

      oos.writeObject(lineLengths);
      oos.writeObject(config);
      
      oos.close();
      oos = null;
    } catch(Exception e) {
      clearSerializedConfigFile(project);
    } finally {
      if(oos != null) {
        IOUtils.closeQuietly(oos);
      }
    }
  }
  
  /**
   * Instantiate the config information, either from the serialized form, or
   * directly from the config document.
   */
  private void loadConfiguration(IProject project)
    throws XmlException,
           IOException,
           CoreException,
           ConcurrentModificationException
  {
    if(!project.isOpen()) {
      return;
    }
    
    IFile configFile = getConfigurationFile(project);

    if(configFile != null) {
      IPath path   = configFile.getLocation();
      File  file   = path.toFile();
      List  errors = new ArrayList();
      
      if(m_configLoader.testIsOld(file)) {
        m_configLoader.updateToCurrent(file);
      }
      
      if(!configFile.isSynchronized(IResource.DEPTH_ZERO)) {
        configFile.refreshLocal(IResource.DEPTH_ZERO, null);
      }
  
      if(testLoadSerializedConfigFile(project)) {
        return;
      }
      
      LineLengths lineLengths = new LineLengths(configFile);
      setSessionProperty(project, CONFIGURATION_LINE_LENGTHS, lineLengths);
  
      clearSAXMarkers(configFile);
      
      // The following line may throw XmlException if the doc is not well-formed.
      // That's why LineLengths are setup earlier, they are used by the catching
      // block to apply SAXMarkers to the config IFile.  Otherwise, it's done
      // below.
      
      TcConfigDocument doc;
      TcConfig         config;
      
      doc    = m_configLoader.parse(file, m_xmlOptions);
      config = doc.getTcConfig();
      
      m_xmlOptions.setErrorListener(errors);
      if(!doc.validate(m_xmlOptions)) {
        handleXmlErrors(configFile, lineLengths, errors.iterator());
      }
      m_xmlOptions.setErrorListener(null);
      
      setSessionProperty(project, CONFIGURATION, config);
  
      if(config != null) {
        getConfigurationHelper(project).validateAll();
        JavaSetupParticipant.inspectAll();
        updateDecorators();
        fireConfigurationChange(project);
        writeSerializedConfigFile(project, lineLengths, config);
      }
    }
  }

  /**
   * Instantiate the config information from the passed-in text.
   */
  private void loadConfiguration(IProject project, String xmlText)
    throws XmlException,
           ConcurrentModificationException,
           IOException
  {
    IFile configFile = getConfigurationFile(project);
    
    if(configFile != null) {
      List errors = new ArrayList();
  
      LineLengths lineLengths = new LineLengths(new StringReader(xmlText));
      setSessionProperty(project, CONFIGURATION_LINE_LENGTHS, lineLengths);
  
      clearSAXMarkers(configFile);
    
      // The following line may throw XmlException if the doc is not well-formed.
      // That's why LineLengths are setup earlier, they are used by the catching
      // block to apply SAXMarkers to the config IFile.  Otherwise, it's done
      // below.
    
      TcConfigDocument doc;
      TcConfig         config;
  
      doc    = m_configLoader.parse(xmlText, m_xmlOptions);
      config = doc.getTcConfig();
    
      m_xmlOptions.setErrorListener(errors);
      if(!doc.validate(m_xmlOptions)) {
        handleXmlErrors(configFile, lineLengths, errors.iterator());
      }
      m_xmlOptions.setErrorListener(null);
    
      setSessionProperty(project, CONFIGURATION, config);
  
      if(config != null) {
        getConfigurationHelper(project).validateAll();
        JavaSetupParticipant.inspectAll();
        updateDecorators();
        fireConfigurationChange(project);
        writeSerializedConfigFile(project, lineLengths, config);
      }
    }
  }

  public void fileMoved(IFile file, IPath movedFromPath) {
    IProject project   = file.getProject();
    IPath    path      = file.getProjectRelativePath();   
    String   extension = path.getFileExtension();
    IPath    fromPath  = movedFromPath.removeFirstSegments(1);
    
    if(extension.equals("xml")) {
      String configPath = getConfigurationFilePath(project);
      
      if(fromPath.toString().equals(configPath)) {
        setConfigurationFilePath(project, path.toString());
        moveSerializedFile(project.getFile(fromPath), file);
      }
    }
  }

  public void replaceConfigText(IProject project, String oldText, String newText) {
    ConfigurationEditor editor = getConfigurationEditor(project);
    IDocument           doc;
    
    if(editor != null) {
      doc = editor.getDocument();
    }
    else {
      try {
        IFile  configFile = getConfigurationFile(project);
        String content    = IOUtils.toString(configFile.getContents());
      
        doc = new Document(content);
      } catch(Exception e) {
        openError("Problem handling refactor", e);
        return;
      }
    }
    
    FindReplaceDocumentAdapter finder = new FindReplaceDocumentAdapter(doc);
    int                        offset = 0;
    IRegion                    region;
    
    try {
      while((region = finder.find(offset, "\\Q"+oldText+"\\E", true, true, true, false)) != null) {
        region = finder.replace(newText, false);
        offset = region.getOffset()+region.getLength();
      }
      
      IFile       configFile = getConfigurationFile(project);
      InputStream stream     = new ByteArrayInputStream(doc.get().getBytes());

      configFile.setContents(stream, true, true, null);
      JavaSetupParticipant.inspectAll();
    } catch(Exception e) {
      openError("Problem handling refactor", e);
      return;
    }
  }
  
  /**
   * Called when a new project is created and it has a leftover Terracotta
   * nature. This can happen because it doesn't appear to be possible to
   * do anything useful with IResourceChangeEvent.PRE_DELETE because the
   * workspace is locked then... and after, the project doesn't exists
   * anymore.
   */
  public void staleProjectAdded(IProject project) {
    try {
      IProjectDescription description = project.getDescription();
      String[]            natures     = description.getNatureIds();
      String[]            newNatures  = new String[natures.length - 1];
      
      for(int i = 0; i < natures.length; i++) {
        if(!natures[i].equals(ProjectNature.NATURE_ID)) {
          newNatures[i] = natures[i];
        }
      }
      description.setNatureIds(newNatures);
      project.setDescription(description, null);
      updateDecorator("org.terracotta.dso.projectDecorator");
    } catch(CoreException ce) {
      ce.printStackTrace();
    }
  }
  
  public void fileRemoved(IFile file) {
    IPath  path      = file.getProjectRelativePath();   
    String extension = path.getFileExtension();
    
    if(extension.equals("java")) {
      clearSerializedFile(file);
      getConfigurationHelper(file.getProject()).validateAll();
    }
    else if(extension.equals("xml")) {
      final IProject project    = file.getProject();
      IFile          configFile = getConfigurationFile(project);
      
      if(file.equals(configFile)) {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            removeTerracottaNature(JavaCore.create(project));
          }
        });
      }
    }
  }
  
  /**
   * Sets the new config information from the passed-in text.  This is invoked
   * by the ConfigurationEditor after the user modifies its contents manually.
   */
  public void setConfigurationFromString(IProject project, String xmlText)
    throws IOException
  {
    try {
      loadConfiguration(project, xmlText);
    } catch(XmlException e) {
      LineLengths lineLengths = getConfigurationLineLengths(project);
      handleXmlException(getConfigurationFile(project), lineLengths, e);
    }
  }
  
  /**
   * Return the config document's line length information, which might not exist.
   */
  public LineLengths getConfigurationLineLengths(IProject project) {
    return (LineLengths)
      getSessionProperty(project, CONFIGURATION_LINE_LENGTHS);
  }

  public ConfigurationHelper getConfigurationHelper(IProject project) {
    ConfigurationHelper helper = (ConfigurationHelper)
      getSessionProperty(project, CONFIGURATION_HELPER);
    
    if(helper == null) {
      helper = new ConfigurationHelper(project);
    }
    
    return helper;
  }

  public synchronized TcConfig getConfiguration(IProject project) {
    TcConfig config = (TcConfig)getSessionProperty(project, CONFIGURATION);
    
    if(config == null) {
      try {
        loadConfiguration(project);
        config = (TcConfig)getSessionProperty(project, CONFIGURATION);
      } catch(XmlException e) {
        LineLengths lineLengths = getConfigurationLineLengths(project);
        handleXmlException(getConfigurationFile(project), lineLengths, e);
      } catch(Exception e) {
        /**/
      } catch(NoClassDefFoundError noClassDef) {
        noClassDef.printStackTrace();
      }

      if(config == null) {
        config = TcConfig.Factory.newInstance();
        setSessionProperty(project, CONFIGURATION, config);
      }
    }
    
    return config;
  }
  
  public void handleXmlException(
    IFile        configFile,
    LineLengths  lineLengths,
    XmlException e)
  {
    if(configFile != null) {
      handleXmlErrors(configFile, lineLengths, e.getErrors().iterator());
    }
  }

  public void handleXmlErrors(
    IFile       configFile,
    LineLengths lineLengths,
    Iterator    errors)
  {
    try {
      XmlError error;
        
      while(errors.hasNext()) {
        error = (XmlError)errors.next();

        HashMap<String, Object> map      = new HashMap<String, Object>();
        int                     line     = error.getLine();
        int                     col      = error.getColumn();
        String                  msg      = error.getMessage();
        int                     severity = XmlError2IMarkerSeverity(error.getSeverity());
        int                     start    = lineLengths.offset(line-1);
        int                     end      = start + ((col == -1) ? lineLengths.lineSize(line-1) : col);
        
        MarkerUtilities.setMessage(map, msg);
        MarkerUtilities.setLineNumber(map, line);
        MarkerUtilities.setCharStart(map, start);
        MarkerUtilities.setCharEnd(map, end);
          
        map.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_HIGH));
        map.put(IMarker.SEVERITY, new Integer(severity));
        map.put(IMarker.LOCATION, "line " + line);
            
        MarkerUtilities.createMarker(configFile, map, "org.terracotta.dso.SAXMarker");
      }
    } catch(CoreException ce) {
      openError("Error handling XML error", ce);
    }
  }
    
  private int XmlError2IMarkerSeverity(int severity) {
    switch(severity) {
      case XmlError.SEVERITY_ERROR:   return IMarker.SEVERITY_ERROR;
      case XmlError.SEVERITY_INFO:    return IMarker.SEVERITY_INFO;
      case XmlError.SEVERITY_WARNING: return IMarker.SEVERITY_WARNING;
      default:                        return IMarker.SEVERITY_INFO;
    }
  }

  ConfigurationEditor[] getConfigurationEditors(IProject project) {
    ArrayList<ConfigurationEditor> list = new ArrayList<ConfigurationEditor>();
    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    
    for(int i = 0; i < windows.length; i++) {
      IWorkbenchPage[] pages = windows[i].getPages();
      
      for(int j = 0; j < pages.length; j++) {
        IEditorReference[] editorRefs = pages[j].getEditorReferences();
        
        for(int k = 0; k < editorRefs.length; k++) {
          IEditorPart editorPart = editorRefs[k].getEditor(false);
          
          if(editorPart != null) {
            if(editorPart instanceof ConfigurationEditor) {
              ConfigurationEditor configEditor = (ConfigurationEditor)editorPart;
              IFile file = ((IFileEditorInput)editorPart.getEditorInput()).getFile();
              
              if(file.getProject().equals(project)) {
                list.add(configEditor);
              }
            }
          }
        }
      }
    }
    return list.toArray(new ConfigurationEditor[0]);
  }
  
  public void reloadConfiguration(IProject project) {
    clearConfigurationSessionProperties(project);
    getConfiguration(project);
    
    ConfigurationEditor[] configEditors = getConfigurationEditors(project);
    for(int i = 0; i < configEditors.length; i++) {
      IFileEditorInput fileEditorInput = (IFileEditorInput)configEditors[i].getEditorInput();
      IFile file = fileEditorInput.getFile();
      
      configEditors[i].newInputFile(file);
    }
  }
  
  public synchronized void clearConfigurationSessionProperties(
    IProject project)
  {
    IFile file = getConfigurationFile(project);
    if(file != null && file.exists()) {
      setPersistentProperty(file, ACTIVE_CONFIGURATION_FILE, null);
    }
    
    setSessionProperty(project, CONFIGURATION_LINE_LENGTHS, null);
    setSessionProperty(project, CONFIGURATION_FILE, null);
    setSessionProperty(project, CONFIGURATION, null);

    clearSerializedConfigFile(project);
    
    setConfigurationFileDirty(project, Boolean.FALSE);
  }

  public void setConfigurationFileDirty(
    IProject project,
    Boolean  dirty)
  {
    if(dirty.booleanValue()) {
      clearSerializedConfigFile(project);
    }
    
    setSessionProperty(project, IS_DIRTY, dirty);
  }

  public boolean isConfigurationFileDirty(IProject project) {
    if(project.isOpen()) {
      Boolean dirty = (Boolean)getSessionProperty(project, IS_DIRTY);
      return dirty != null ? dirty.booleanValue() : false;
    }
    return false;
  }

  public void saveConfiguration(IProject project) {
    IFile            configFile;
    TcConfig         config;
    TcConfigDocument configDoc;
    XmlOptions       opts;
    InputStream      stream;
    
    opts       = getXmlOptions();
    configFile = getConfigurationFile(project);
    config     = getConfiguration(project);
    configDoc  = TcConfigDocument.Factory.newInstance();
    stream     = null;
    
    try {
      if(config != null) {
        configDoc.setTcConfig(config);
        stream = configDoc.newInputStream(opts);
        configFile.setContents(stream, true, true, null);
        stream = null;
        
        getConfigurationHelper(project).validateAll();
        JavaSetupParticipant.inspectAll();
        updateDecorators();
        fireConfigurationChange(project);
      }
    } catch(Exception e) {
      openError("Error saving '" + configFile.getName() + "'", e);
    } finally {
      if(stream != null) {
        IOUtils.closeQuietly(stream);
      }
    }
  }

  public void saveConfigurationQuietly(IProject project) {
    IFile            configFile;
    TcConfig         config;
    TcConfigDocument configDoc;
    XmlOptions       opts;
    InputStream      inStream;
    FileWriter       outWriter;
    
    opts       = getXmlOptions();
    configFile = getConfigurationFile(project);
    config     = getConfiguration(project);
    configDoc  = TcConfigDocument.Factory.newInstance();
    inStream   = null;
    outWriter  = null;
    
    try {
      if(config != null) {
        configDoc.setTcConfig(config);
        
        inStream  = configDoc.newInputStream(opts);
        outWriter = new FileWriter(configFile.getLocation().toOSString());

        CopyUtils.copy(inStream, outWriter);
        
        getConfigurationHelper(project).validateAll();
        JavaSetupParticipant.inspectAll();
        updateDecorators();
        fireConfigurationChange(project);
      }
    } catch(Exception e) {
      openError("Error saving '" + configFile.getName() + "'", e);
    } finally {
      IOUtils.closeQuietly(inStream);
      IOUtils.closeQuietly(outWriter);
    }
  }

  public void setSessionProperty(
    IProject      project,
    QualifiedName name,
    Object        value)
  {
    if(project != null && project.exists() && project.isOpen()) {
      try {
        project.setSessionProperty(name, value);
      } catch(CoreException ce) {
        ce.printStackTrace();
      }
    }
  }

  public Object getSessionProperty(
    IProject      project,
    QualifiedName name)
  {
    if(project != null && project.exists() && project.isOpen()) {
      try {
        return project.getSessionProperty(name);
      } catch(CoreException ce) {
        ce.printStackTrace();
      }
    }
    
    return null;
  }

  public void setPersistentProperty(
    IResource     resource,
    QualifiedName name,
    String        value)
  {
    if(resource != null && resource.exists() ) {
      try {
        resource.setPersistentProperty(name, value);
      } catch(CoreException ce) {
        ce.printStackTrace();
      }
    }
  }

  public String getPersistentProperty(
    IResource     resource,
    QualifiedName name)
  {
    if(resource != null && resource.exists()) {
      try {
        return resource.getPersistentProperty(name);
      } catch(CoreException ce) {
        return null;
      }
    }
    
    return null;
  }

  public ConfigurationEditor getConfigurationEditor(IProject project) {
    ConfigurationEditor[] configEditors = getConfigurationEditors(project);
    IFile configFile = getConfigurationFile(project);
    
    for(int i = 0; i < configEditors.length; i++) {
      IFileEditorInput fileEditorInput = (IFileEditorInput)configEditors[i].getEditorInput();
      IFile file = fileEditorInput.getFile();
      
      if(file.equals(configFile)) {
        return configEditors[i];
      }
    }

    return null;
  }

  public ConfigurationEditor ensureConfigurationEditor(IProject project) {
    ConfigurationEditor editor = getConfigurationEditor(project);
    
    if(editor == null) {
      try {
        editor = openConfigurationEditor(project);
      } catch(PartInitException pie) {
        openError("Unable to open configuration editor", pie);
      }
    }
    
    return editor;
  }
 
  public void setConfigurationFilePath(IProject project, String path) {
    String oldPath = getConfigurationFilePath(project);
    IFile file = oldPath != null ? project.getFile(oldPath) : null;
    if(file != null && file.exists()) {
      clearSAXMarkers(file);
      setPersistentProperty(file, ACTIVE_CONFIGURATION_FILE, null);
    }
    
    setPersistentProperty(project, CONFIGURATION_FILE_PATH, path);

    file = project.getFile(new Path(path));
    setSessionProperty(project, CONFIGURATION_FILE, file);  
    setPersistentProperty(file, ACTIVE_CONFIGURATION_FILE, "true");
    
    ConfigurationEditor configEditor = getConfigurationEditor(project);
    if(false && configEditor != null) {
      configEditor.newInputFile(file);
    }
  }
  
  public String getConfigurationFilePath(IProject project) {
    return getPersistentProperty(project, CONFIGURATION_FILE_PATH);   
  }
    
  public IFile getConfigurationFile(IProject project) {
    IFile file = (IFile)getSessionProperty(project, CONFIGURATION_FILE);
    
    if(file == null) {
      String path = getConfigurationFilePath(project);
      
      if(path != null) {
        file = project.getFile(new Path(path));
        setSessionProperty(project, CONFIGURATION_FILE, file);
        setPersistentProperty(file, ACTIVE_CONFIGURATION_FILE, "true");
      }
    }
    
    return file;
  }

  public void setServerOptions(IProject project, String opts) {
    setPersistentProperty(project, SERVER_OPTIONS, opts);
  }
  
  public String getServerOptions(IProject project) {
    String options = getPersistentProperty(project, SERVER_OPTIONS);
    return options != null ? options : "";
  }
  
  public boolean hasTerracottaNature(IJavaElement element) {
    return hasTerracottaNature(element.getJavaProject().getProject());
  }

  public boolean hasTerracottaNature(IProject project) {
    try {
      return project != null && project.hasNature(ProjectNature.NATURE_ID);
    } catch(CoreException ce) {/**/}
    return false;
  }

  public void inspect(final ICompilationUnit cu) {
    if(cu != null) {
      getCompilationUnitVisitor().inspect(cu);
    }
  }
  
  private CompilationUnitVisitor getCompilationUnitVisitor() {
    if(m_compilationUnitVisitor == null) {
      m_compilationUnitVisitor = new CompilationUnitVisitor();
    }
    return m_compilationUnitVisitor;
  }
  
  private ResourceDeltaVisitor getResourceDeltaVisitor() {
    if(m_resourceDeltaVisitor == null) {
      m_resourceDeltaVisitor = new ResourceDeltaVisitor();
    }
    return m_resourceDeltaVisitor;
  }
  
  class ResourceListener implements IResourceChangeListener {
    public void resourceChanged(final IResourceChangeEvent event) {
      switch(event.getType()) {
        case IResourceChangeEvent.POST_BUILD: {
          try {
            event.getDelta().accept(getResourceDeltaVisitor());
          } catch(CoreException ce) {
            openError("Error handling resource change event for '"+
              event.getResource().getName()+"'", ce);
          }
          break;
        }
      }
    }
  }
  
  public void updateDecorator(final String id) {
    updateDecorators(new String[] {id});
  }

  private DecoratorUpdateAction getDecoratorUpdateAction() {
    if(m_decoratorUpdateAction == null) {
      m_decoratorUpdateAction = new DecoratorUpdateAction();
    }
    return m_decoratorUpdateAction;
  }
  
  public void updateDecorators(String[] ids) {
    DecoratorUpdateAction updater = getDecoratorUpdateAction();
    
    updater.setDecorators(ids);
    Display.getDefault().asyncExec(updater);
  }

  class DecoratorUpdateAction implements Runnable {
    IDecoratorManager m_decoratorManager;
    String[]          m_decorators;
    
    DecoratorUpdateAction() {
      m_decoratorManager = PlatformUI.getWorkbench().getDecoratorManager();
    }
    
    void setDecorators(String[] ids) {
      m_decorators = ids;
    }
    
    public void run() {
      for(int i = 0; i < m_decorators.length; i++) {
        m_decoratorManager.update(m_decorators[i]);
      }
    }
  }
  
  private static final String[] DECORATOR_IDS = {
    "org.terracotta.dso.projectDecorator",
    ConfigFileDecorator.DECORATOR_ID,
    ServerRunningDecorator.DECORATOR_ID,
    AdaptedModuleDecorator.DECORATOR_ID,
    AdaptedTypeDecorator.DECORATOR_ID,
    AdaptedPackageFragmentDecorator.DECORATOR_ID,
    ExcludedTypeDecorator.DECORATOR_ID,
    ExcludedModuleDecorator.DECORATOR_ID,
    DistributedMethodDecorator.DECORATOR_ID,
    NameLockedDecorator.DECORATOR_ID,
    AutolockedDecorator.DECORATOR_ID,
    RootDecorator.DECORATOR_ID,
    TransientDecorator.DECORATOR_ID,
  };                                                 

  public void updateDecorators() {
    updateDecorators(DECORATOR_IDS);
  }
  
  public boolean hasSAXMarkers(IResource res)
    throws CoreException
  {
    if(res != null && res.exists()) {
      IMarker[] markers = res.findMarkers("org.terracotta.dso.SAXMarker", false, IResource.DEPTH_ZERO);
      return markers.length > 0;
    }
    return false;
  }

  public void clearSAXMarkers(IResource res) {
    if(res != null && res.exists()) {
      try {
        res.deleteMarkers("org.terracotta.dso.SAXMarker", false, IResource.DEPTH_ZERO);
      } catch(CoreException ce) {
        openError("Error clearing SAX markers on file'"+res.getName()+"'", ce);
      }
    }
  }
  
  public boolean hasProblemMarkers(IResource res)
    throws CoreException
  {
    if(res != null && res.exists()) {
      IMarker[] markers = res.findMarkers("org.eclipse.core.resources.problemmarker",
                                          true, IResource.DEPTH_ZERO);
      return markers.length > 0;
    }
    return false;
  }
  
  public void clearConfigProblemMarkers(IProject project) {
    try {
      IFile configFile = getConfigurationFile(project);

      if(configFile != null && configFile.exists()) {
        configFile.deleteMarkers("org.terracotta.dso.ConfigProblemMarker", true, IResource.DEPTH_ZERO);
      }
    } catch(CoreException ce) {/**/}
  }
  
  public void clearConfigProblemMarkersOfType(IProject project, String markerType) {
    try {
      IFile configFile = getConfigurationFile(project);

      if(configFile != null && configFile.exists()) {
        configFile.deleteMarkers(markerType, false, IResource.DEPTH_ZERO);
      }
    } catch(CoreException ce) {/**/}
  }
  
  public static ImageDescriptor getImageDescriptor(String path) {
    return AbstractUIPlugin.imageDescriptorFromPlugin(getPluginId(), path);
  }

  public ConfigurationEditor openConfigurationEditor(IProject project)
    throws PartInitException
  {
    IWorkbench       workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow wbWin     = workbench.getActiveWorkbenchWindow();
    
    if(wbWin != null) {
      IWorkbenchPage wbPage = wbWin.getActivePage();

      if(wbPage != null) {
        IFile           configFile      = getConfigurationFile(project);
        FileEditorInput fileEditorInput = new FileEditorInput(configFile);
        IEditorPart     editorPart      = wbPage.findEditor(fileEditorInput);
        
        if(editorPart != null) {
          if(editorPart instanceof ConfigurationEditor) {
            wbPage.activate(editorPart);
            return (ConfigurationEditor)editorPart;
          }
          else {
            wbPage.closeEditor(editorPart, true);
          }
        }
        
        if(configFile != null) {
          String configEditorID = "editors.configurationEditor";
          
          return (ConfigurationEditor)
            IDE.openEditor(wbPage, configFile, configEditorID, false);
        }
      }
    }
    
    return null;
  }
  
  public void closeConfigurationEditor(IProject project) {
    ConfigurationEditor configEditor = getConfigurationEditor(project); 
    
    if(configEditor != null) {
      configEditor.closeEditor();
    }
  }
  
  private XmlOptions createXmlOptions() {
    XmlOptions opts = new XmlOptions();
    
    opts.setLoadLineNumbers();
    opts.setSavePrettyPrint();
    opts.setSavePrettyPrintIndent(2);
    opts.remove(XmlOptions.LOAD_STRIP_WHITESPACE);
    opts.remove(XmlOptions.LOAD_STRIP_COMMENTS);
    //opts.remove(XmlOptions.VALIDATE_ON_SET);

    return opts;
  }
  
  public XmlOptions getXmlOptions() {
    return m_xmlOptions;
  }

  public void openError(final String msg, final Throwable t) {
    ILog log = getLog();
    log.log(new Status(IStatus.ERROR, getPluginId(), INTERNAL_ERROR, msg, t));
  }
  
  public void openError(final String msg) {
    if(SwingUtilities.isEventDispatchThread()) {
      Display.getDefault().syncExec(new Runnable() {
        public void run(){
          MessageDialog.openError(null, "Terracotta Plugin", msg);
        }
      });
    }
    else {
      MessageDialog.openError(null, "Terracotta Plugin", msg);
    }
  }
  
  public boolean continueWithConfigProblems(final IProject project) throws CoreException {
    IFile configFile = getConfigurationFile(project);

    setSessionProperty(project, CONFIG_PROBLEM_CONTINUE, null);

    if(hasProblemMarkers(configFile)) {
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          Shell  shell = Display.getDefault().getActiveShell();
          String title = "Terracotta";
          String msg   = "The are problems with the Terracotta configuration. Continue?";
          
          if(MessageDialog.openConfirm(shell, title, msg)) {
            setSessionProperty(project, CONFIG_PROBLEM_CONTINUE, Boolean.TRUE);
          }
        }
      });
      
      return getSessionProperty(project, CONFIG_PROBLEM_CONTINUE) == Boolean.TRUE;
    }
    
    return true;
  }

  public static Display getStandardDisplay() {
    Display display = Display.getCurrent();
    if (display == null) {
      display= Display.getDefault();
    }
    return display;   
  } 
  
  public void fireConfigurationChange(IProject project) {
    m_configurationListeners.fireConfigurationChange(project);
  }
  
  class ConfigurationManager extends EventManager {
    void addConfigurationListener(IConfigurationListener listener) {
      addListenerObject(listener);
    }
    
    void removeConfigurationListener(IConfigurationListener listener) {
      removeListenerObject(listener);
    }
    
    void fireConfigurationChange(IProject project) {
      Object[] listeners = getListeners();
      
      if(listeners != null) {
        for(int i = 0; i < listeners.length; i++) {
          ((IConfigurationListener)listeners[i]).configurationChanged(project);
        }
      }
    }
  }
  
  private ConfigurationManager m_configurationListeners = new ConfigurationManager();
  
  public void addConfigurationListener(IConfigurationListener listener) {
    m_configurationListeners.addConfigurationListener(listener);
  }

  public void removeConfigurationListener(IConfigurationListener listener) {
    m_configurationListeners.removeConfigurationListener(listener);
  }
  
  public static Image createImage(String path) {
    return new JavaElementImageDescriptor(getImageDescriptor(path), 0, new Point(16, 16)).createImage(false);
  }
}
