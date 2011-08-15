/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
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
import org.eclipse.debug.core.ILaunchConfiguration;
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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
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
import org.terracotta.dso.dialogs.ConfigProblemsDialog;
import org.terracotta.dso.editors.ConfigurationEditor;
import org.terracotta.dso.wizards.ProjectWizard;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.bundles.EmbeddedOSGiEventHandler;
import com.tc.bundles.EmbeddedOSGiRuntime;
import com.tc.bundles.Resolver;
import com.tc.bundles.ResolverUtils;
import com.tc.config.Loader;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.util.JarResourceLoader;
import com.tc.plugins.ModulesLoader;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.ServerConstants;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;
import com.terracottatech.config.BindPort;
import com.terracottatech.config.Client;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Terracotta plugin. The whole enchilada centers around this singleton. The primary duties of this class are to
 * manage the configuration information associated with a Terracotta Eclipse project. Configuration information is
 * stored as session properties in the project, which are not persistent across sessions. The config information is made
 * up of: 1) the config file XML document 2) the parsed form of the XML document, aka the config object 3) the line
 * length information of the config document 3) a configuration helper, used by various actions.
 * 
 * @see loadConfiguration(IProject) When there are errors instantiating the config document, those XmlErrors are turned
 *      into SAXMarkers on the document, which appear as error indicators in the config document editor.
 * @see handleXmlErrors(IFile, LineLengths, Iterator) At times, it is necessary to inspect a module to determine where
 *      to add annotations denoting configuration-related Java elements, such as a particular method being locked.
 * @see inspect(ICompilationUnit). Another responsibility of this class is to launch the server using the config
 *      information, additional boot jar, etc.
 * @see launchServer(IJavaProject, String, String, String) Finally, this class provides methods for updating the
 *      annotations used to indicate various states related to the config information, such as a field being a root.
 * @see updateDecorators()
 * @see org.eclipse.ui.plugin.AbstractUIPlugin
 * @see com.terracottatech.config.TCConfigDocument
 * @see com.terracottatech.config.TCConfigDocument.TCConfig
 */

public class TcPlugin extends AbstractUIPlugin implements QualifiedNames, IJavaLaunchConfigurationConstants,
    TcPluginStatusConstants {
  private static TcPlugin                 m_plugin;
  private Loader                          m_configLoader;
  private CompilationUnitVisitor          m_compilationUnitVisitor;
  private ResourceListener                m_resourceListener;
  private ResourceDeltaVisitor            m_resourceDeltaVisitor;
  private XmlOptions                      m_xmlOptions;
  private DecoratorUpdateAction           m_decoratorUpdateAction;
  private ArrayList<IProjectAction>       m_projectActionList;
  private final ConfigurationEventManager m_configurationEventManager;

  public static final String              PLUGIN_ID                           = "org.terracotta.dso";

  public static final String              DEFAULT_CONFIG_FILENAME             = "tc-config.xml";
  public static final String              DEFAULT_SERVER_OPTIONS              = "-Xms256m -Xmx256m";
  public static final boolean             DEFAULT_AUTO_START_SERVER_OPTION    = false;
  public static final boolean             DEFAULT_WARN_CONFIG_PROBLEMS_OPTION = true;
  public static final boolean             DEFAULT_QUERY_RESTART_OPTION        = true;

  public static final String              SERVER_NAME_LAUNCH_ATTR             = "server.name";
  public static final String              SERVER_HOST_LAUNCH_ATTR             = "server.host";
  public static final String              SERVER_JMX_PORT_LAUNCH_ATTR         = "server.jmx-port";
  public static final String              SERVER_DSO_PORT_LAUNCH_ATTR         = "server.dso-port";

  public static final Server              DEFAULT_SERVER_INSTANCE             = createDefaultServerInstance();

  public TcPlugin() {
    super();
    if (m_plugin != null) throw new IllegalStateException("Plugin already instantiated.");
    m_plugin = this;
    m_configurationEventManager = new ConfigurationEventManager();
  }

  public static String getPluginId() {
    return PLUGIN_ID;
  }

  private static Server createDefaultServerInstance() {
    Server server = Server.Factory.newInstance();
    server.setName("default");
    server.setHost("localhost");
    BindPort jmxPort = BindPort.Factory.newInstance();
    jmxPort.setBind("0.0.0.0");
    jmxPort.setIntValue(9520);
    server.addNewJmxPort();
    server.setJmxPort(jmxPort);

    BindPort dsoPort = BindPort.Factory.newInstance();
    dsoPort.setBind("0.0.0.0");
    dsoPort.setIntValue(9510);
    server.addNewDsoPort();
    server.setDsoPort(dsoPort);
    server.setBind("0.0.0.0");
    return server;
  }

  public boolean isBootClass(final IProject project, final ICompilationUnit module) {
    return module != null && isBootClass(project, module.findPrimaryType());
  }

  public boolean isBootClass(final IProject project, final IClassFile classFile) {
    try {
      return classFile != null && isBootClass(project, classFile.getType());
    } catch (Exception e) {/**/
    }
    return false;
  }

  public boolean isBootClass(final IProject project, final IType type) {
    return isBootClass(project, PatternHelper.getFullyQualifiedName(type));
  }

  public boolean isBootClass(final IProject project, String name) {
    BootClassHelper bch = getBootClassHelper(project);
    return bch != null ? bch.isAdaptable(name) : false;
  }

  public void registerProjectAction(IProjectAction action) {
    if (action != null) {
      m_projectActionList.add(action);
    }
  }

  public void notifyProjectActions(IProject project) {
    Iterator iter = m_projectActionList.iterator();

    while (iter.hasNext()) {
      ((IProjectAction) iter.next()).update(project);
    }
  }

  private void addResourceChangeListener() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(m_resourceListener, IResourceChangeEvent.POST_BUILD);
  }

  private IPath m_location;

  public IPath getLocation() {
    return m_location;
  }

  public IPath getLibDirPath() {
    return getLocation().append("lib");
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    m_configLoader = new Loader();

    // TODO: after we remove 3.1 support, change to FileLocator.resolve
    URL url = Platform.resolve(context.getBundle().getEntry("/"));
    m_location = new Path(url.getPath()).removeTrailingSeparator();

    JavaCore.addElementChangedListener(new ElementChangedListener(), ElementChangedEvent.POST_RECONCILE);

    m_resourceListener = new ResourceListener();
    addResourceChangeListener();

    IPath tcInstallPath = getLocation();
    if (tcInstallPath != null) {
      String installRootProp = "tc.install-root";
      String installRoot = System.getProperty(installRootProp);
      if (installRoot == null) {
        System.setProperty(installRootProp, tcInstallPath.toString());
      }
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
    // manager.registerAdapters(factory, SourceMethod.class);
    // manager.registerAdapters(factory, BinaryMember.class);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    ServerTracker.getDefault().shutdownAllServers();
    super.stop(context);
    m_plugin = null;
  }

  public static TcPlugin getDefault() {
    return m_plugin;
  }

  public void setup(IProject project, String configFilePath) {
    boolean isConfigEditorVisible = getConfigurationEditor(project) != null;

    if (isConfigEditorVisible) closeConfigurationEditor(project);

    clearConfigurationSessionProperties(project);
    setConfigurationFilePath(project, configFilePath);
    reloadConfiguration(project);

    if (isConfigEditorVisible) ensureConfigurationEditor(project);
  }

  public void setup(IProject project, String configFilePath, String serverOpts) {
    setPersistentProperty(project, SERVER_OPTIONS, serverOpts);
    setup(project, configFilePath);
  }

  public void setup(IFile configFile) {
    setup(configFile.getProject(), configFile.getProjectRelativePath().toString());
  }

  public static IWorkbenchWindow getActiveWorkbenchWindow() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
  }

  public static Shell getActiveWorkbenchShell() {
    IWorkbenchWindow window = getActiveWorkbenchWindow();
    if (window != null) { return window.getShell(); }
    return null;
  }

  public static IWorkbenchPage getActivePage() {
    IWorkbenchWindow w = getActiveWorkbenchWindow();
    if (w != null) { return w.getActivePage(); }
    return null;
  }

  public void addTerracottaNature(IJavaProject currentProject) {
    Shell shell = getActiveWorkbenchShell();

    try {
      ProjectWizard wizard = new ProjectWizard(currentProject);
      WizardDialog dialog = new WizardDialog(shell, wizard);

      wizard.setWindowTitle("Terracotta Project Wizard");
      dialog.open();
    } catch (Exception e) {
      MessageDialog.openInformation(shell, "Terracotta",
                                    "Cannot add Terracotta nature:\n" + ActionUtil.getStatusMessages(e));
    } finally {
      if (shell != null) shell.setCursor(null);
    }
  }

  public void removeTerracottaNature(IJavaProject javaProject) {
    Shell shell = getActiveWorkbenchShell();

    try {
      IRunnableWithProgress op = new TCNatureRemover(javaProject);
      new ProgressMonitorDialog(shell).run(true, false, op);
    } catch (InvocationTargetException e) {
      openError("Cannot remove Terracotta nature", e.getCause());
    } catch (InterruptedException e) {/**/
    }
  }

  class TCNatureRemover implements IRunnableWithProgress {
    IJavaProject m_javaProject;

    TCNatureRemover(IJavaProject javaProject) {
      m_javaProject = javaProject;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException {
      try {
        monitor.beginTask("Removing project nature", 4);
        IProject project = m_javaProject.getProject();
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();
        ArrayList<String> natureList = new ArrayList<String>();

        for (int i = 0; i < natures.length; i++) {
          if (!natures[i].equals(ProjectNature.NATURE_ID)) {
            natureList.add(natures[i]);
          }
        }
        description.setNatureIds(natureList.toArray(new String[0]));
        project.setDescription(description, monitor);
        // project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        monitor.worked(1);

        monitor.beginTask("Closing configuration editor", 4);
        closeConfigurationEditor(project);
        monitor.worked(2);

        monitor.beginTask("Stopping server", 4);
        final ServerTracker tracker = ServerTracker.getDefault();
        if (tracker.anyRunning(m_javaProject)) {
          Display.getDefault().syncExec(new Runnable() {
            public void run() {
              tracker.shutdownAllServers();
            }
          });
        }
        monitor.worked(3);

        monitor.beginTask("Removing markers", 4);
        project.deleteMarkers("org.terracotta.dso.baseMarker", true, IResource.DEPTH_INFINITE);

        IFile configFile = getConfigurationFile(m_javaProject.getProject());
        if (configFile != null && configFile.exists()) {
          clearSAXMarkers(configFile);
        }

        clearConfigurationSessionProperties(project);
        notifyProjectActions(project);
        fireConfigurationChange(project);

        monitor.worked(4);
      } catch (CoreException e) {
        throw new InvocationTargetException(e);
      }
    }
  }

  public ILaunch launchServer(IJavaProject javaProject, String projectName, String serverName, Server server,
                              IProgressMonitor monitor) throws CoreException {
    IProject project = javaProject.getProject();
    if (!validateConfigurationFile(project)) { return null; }

    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    String id = ID_JAVA_APPLICATION;
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(id);
    String installPath = getLocation().makeAbsolute().toOSString();

    ILaunchConfigurationWorkingCopy wc = type.newInstance(null, projectName + "." + serverName);

    wc.setAttribute(ATTR_PROJECT_NAME, project.getName());
    wc.setAttribute(ATTR_MAIN_TYPE_NAME, ServerConstants.SERVER_MAIN_CLASS_NAME);
    wc.setAttribute(SERVER_NAME_LAUNCH_ATTR, server.getName());
    wc.setAttribute(SERVER_HOST_LAUNCH_ATTR, server.getHost());
    BindPort dsoBindPort = server.getDsoPort();
    int dsoPort = dsoBindPort != null ? dsoBindPort.getIntValue() : 9510;
    wc.setAttribute(SERVER_DSO_PORT_LAUNCH_ATTR, dsoPort);
    BindPort jmxBindPort = server.getJmxPort();
    int jmxPort = jmxBindPort != null ? jmxBindPort.getIntValue() : (dsoPort + 10);
    wc.setAttribute(SERVER_JMX_PORT_LAUNCH_ATTR, jmxPort);

    IFile configFile = getConfigurationFile(project);

    if (configFile != null) {
      String configPath = configFile.getLocation().toOSString();
      String vmargs = " -Dtc.install-root=\"" + installPath + "\"" + " -Dtc.config=\"" + configPath + "\""
                      + " -Dtc.server.name=\"" + serverName + "\"" + " -Dcom.sun.management.jmxremote"
                      + " -Dcom.sun.management.jmxremote.authenticate=false";
      String origVMArgs = wc.getAttribute(ATTR_VM_ARGUMENTS, "") + " " + getServerOptions(project) + " ";

      wc.setAttribute(ATTR_VM_ARGUMENTS, vmargs + origVMArgs);
      wc.setAttribute(ATTR_CLASSPATH_PROVIDER, "org.terracotta.dso.classpathProvider");
      wc.setAttribute(ATTR_WORKING_DIRECTORY, project.getLocation().makeAbsolute().toOSString());

      return wc.launch(ILaunchManager.RUN_MODE, monitor);
    } else {
      System.out.println("No config file specified.  Set in project properties");
      return null;
    }
  }

  public Server getServerByName(IProject project, String name) {
    TcConfig config = getConfiguration(project);

    if (config != null) {
      Servers servers = config.getServers();

      if (servers != null) {
        for (Server server : servers.getServerArray()) {
          String serverName = ParameterSubstituter.substitute(server.getName());
          if (serverName != null && serverName.equals(name)) { return server; }
        }
      }
    }

    return null;
  }

  private Server createLaunchServer(ILaunch launch) {
    ILaunchConfiguration launchConfig = launch.getLaunchConfiguration();
    String name = "", host = "";
    int jmxPort = 0, dsoPort = 0;

    try {
      name = launchConfig.getAttribute(SERVER_NAME_LAUNCH_ATTR, "");
      host = launchConfig.getAttribute(SERVER_HOST_LAUNCH_ATTR, "");
      jmxPort = launchConfig.getAttribute(SERVER_JMX_PORT_LAUNCH_ATTR, 9520);
      dsoPort = launchConfig.getAttribute(SERVER_DSO_PORT_LAUNCH_ATTR, 9510);
    } catch (CoreException ce) {
      // this can't happen because server launch configurations are never persisted
    }

    Server server = Server.Factory.newInstance();
    server.setName(name);
    server.setHost(host);
    if (jmxPort > 0) {
      BindPort jmxBindPort = BindPort.Factory.newInstance();
      jmxBindPort.setBind("0.0.0.0");
      jmxBindPort.setIntValue(jmxPort);
      server.addNewJmxPort();
      server.setJmxPort(jmxBindPort);
    }
    if (dsoPort > 0) {
      BindPort dsoBindPort = BindPort.Factory.newInstance();
      dsoBindPort.setBind("0.0.0.0");
      dsoBindPort.setIntValue(dsoPort);
      server.addNewDsoPort();
      server.getDsoPort().setIntValue(dsoPort);
    }

    return server;
  }

  public boolean areEquivalentServers(Server server1, Server server2) {
    if (server1 != null && server2 != null) {
      if (StringUtils.equals(server1.getName(), server2.getName())
          && StringUtils.equals(server1.getHost(), server2.getHost()) && server1.getJmxPort() == server2.getJmxPort()
          && server1.getDsoPort() == server2.getDsoPort()) { return true; }
    }
    return false;
  }

  public Server getLaunchedServer(IProject project, ILaunch launch) {
    TcConfig config = getConfiguration(project);

    if (config != null) {
      Servers servers = config.getServers();

      if (servers != null) {
        Server launchServer = createLaunchServer(launch);

        for (Server server : servers.getServerArray()) {
          Server serverCopy = (Server) server.copy();
          replacePatterns(serverCopy);
          if (areEquivalentServers(launchServer, serverCopy)) { return server; }
        }
      }
    }

    return null;
  }

  /**
   * This should not be called with a configuration server or any patterns will be overwritten with whatever values
   * apply at call-time. Clone the configuration server first: Server serverCopy = (Server) server.copy(); Further,
   * identity comparison should not be used on servers, rather use areEquivalentServers(Server1, Server2).
   */
  public void replacePatterns(Server server) {
    if (server != null) {
      if (server.isSetName()) {
        server.setName(ParameterSubstituter.substitute(server.getName()));
      }
      if (server.isSetHost()) {
        server.setHost(ParameterSubstituter.substitute(server.getHost()));
      }
      if (server.isSetBind()) {
        server.setBind(ParameterSubstituter.substitute(server.getBind()));
      }
    }
  }

  public Server getAnyServer(IProject project) {
    TcConfig config = getConfiguration(project);

    if (config != null) {
      Servers servers = config.getServers();

      if (servers != null) {
        Server[] serverArray = servers.getServerArray();

        if (serverArray.length > 0) {
          Server server = (Server) serverArray[0].copy();
          replacePatterns(server);
          return server;
        }
      }
    }

    return DEFAULT_SERVER_INSTANCE;
  }

  public static String getServerName(Server server) {
    if (server == null) return null;
    String name;
    if (server.isSetName()) {
      name = server.getName();
    } else {
      int dsoPort = server.isSetDsoPort() ? server.getDsoPort().getIntValue() : 9510;
      name = server.getHost() + ":" + dsoPort;
    }
    return name;
  }

  /**
   * Instantiate the config information, either from the serialized form, or directly from the config document.
   */
  private void loadConfiguration(IProject project) throws Exception, ConcurrentModificationException {
    if (!project.isOpen()) { return; }

    IFile configFile = getConfigurationFile(project);

    if (configFile != null) {
      IPath path = configFile.getLocation();
      File file = path.toFile();
      List errors = new ArrayList();

      if (!configFile.isSynchronized(IResource.DEPTH_ZERO)) {
        ignoreNextConfigChange();
        configFile.refreshLocal(IResource.DEPTH_ZERO, null);
      }

      LineLengths lineLengths = new LineLengths(configFile);
      setSessionProperty(project, CONFIGURATION_LINE_LENGTHS, lineLengths);

      clearSAXMarkers(configFile);

      // The following line may throw XmlException if the doc is not well-formed.
      // That's why LineLengths are setup earlier, they are used by the catching
      // block to apply SAXMarkers to the config IFile. Otherwise, it's done
      // below.

      TcConfigDocument doc;
      TcConfig config;

      doc = m_configLoader.parse(file, m_xmlOptions);
      config = doc.getTcConfig();
      setSessionProperty(project, CONFIGURATION, config);

      m_xmlOptions.setErrorListener(errors);
      if (!doc.validate(m_xmlOptions)) {
        handleXmlErrors(configFile, lineLengths, errors.iterator());
      }
      m_xmlOptions.setErrorListener(null);

      clearModulesConfiguration(project);
      Client client = config.getClients();
      if (client != null && client.isSetModules() && client.getModules().sizeOfModuleArray() > 0) {
        ModulesConfiguration modulesConfig = createModulesConfiguration(project);
        initModules(client.getModules(), modulesConfig);
      }

      setBootClassHelper(project, new BootClassHelper(JavaCore.create(project)));
    }
  }

  private void clearModulesConfiguration(IProject project) {
    setSessionProperty(project, MODULES_CONFIGURATION, null);
  }

  private ModulesConfiguration createModulesConfiguration(IProject project) {
    ModulesConfiguration modulesConfig = new ModulesConfiguration();
    setSessionProperty(project, MODULES_CONFIGURATION, modulesConfig);
    return modulesConfig;
  }

  public ModulesConfiguration getModulesConfiguration(IProject project) {
    return (ModulesConfiguration) getSessionProperty(project, MODULES_CONFIGURATION);
  }

  private void initModules(final Modules modules, final ModulesConfiguration modulesConfig) {
    EmbeddedOSGiRuntime osgiRuntime = null;

    try {
      Modules modulesCopy = (Modules) modules.copy();
      Module[] origModules = modulesCopy.getModuleArray();

      for (Module origModule : origModules) {
        Modules tmpModules = (Modules) modulesCopy.copy();
        tmpModules.setModuleArray(new Module[] { origModule });
        osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(tmpModules);
        String[] repositories = ResolverUtils.urlsToStrings(osgiRuntime.getRepositories());
        final Resolver resolver = new Resolver(repositories, ProductInfo.getInstance().version(), ProductInfo
            .getInstance().timApiVersion());
        Module[] allModules = tmpModules.getModuleArray();
        ModuleInfo origModuleInfo = modulesConfig.getOrAdd(origModule);

        // forge default modules to be resolved
        resolver.resolve(new Module[] {});

        for (Module module : allModules) {
          ModuleInfo moduleInfo = modulesConfig.getOrAdd(module);
          try {
            moduleInfo.setLocation(resolver.resolve(module));
            for (URL location : resolver.getResolvedURLs()) {
              osgiRuntime.installBundle(location);
            }
          } catch (BundleException be) {
            moduleInfo.setError(be);
            origModuleInfo.setError(be);
          }
        }
        osgiRuntime.shutdown();
      }

      osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(modulesCopy);
      final Resolver resolver = new Resolver(ResolverUtils.urlsToStrings(osgiRuntime.getRepositories()), ProductInfo
          .getInstance().version(), ProductInfo.getInstance().timApiVersion());
      Module[] allModules = modulesCopy.getModuleArray();

      resolver.resolve(allModules);

      DSOClientConfigHelper configHelper = new FakeDSOClientConfigHelper();
      final Map<Bundle, URL> bundleURLs = new HashMap<Bundle, URL>();
      for (URL location : resolver.getResolvedURLs()) {
        try {
          Bundle b = osgiRuntime.installBundle(location);
          bundleURLs.put(b, location);
        } catch (BundleException be) {
          be.printStackTrace();
        }
      }
      configHelper.recordBundleURLs(bundleURLs);

      osgiRuntime.registerService("com.tc.object.config.StandardDSOClientConfigHelper", configHelper, new Hashtable());
      osgiRuntime.registerService(TCLogger.class.getName(), TCLogging.getLogger(TerracottaConfiguratorModule.class),
                                  new Hashtable());
      osgiRuntime.registerService(TCProperties.class.getName(), TCPropertiesImpl.getProperties(), new Hashtable());

      osgiRuntime.startBundles(resolver.getResolvedURLs(), new EmbeddedOSGiEventHandler() {
        public void callback(final Object payload) throws BundleException {
          Assert.assertTrue(payload instanceof Bundle);
          Bundle bundle = (Bundle) payload;
          if (bundle != null) {
            loadModuleConfiguration(bundle, modulesConfig);
          }
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (osgiRuntime != null) {
        osgiRuntime.shutdown();
      }
    }
  }

  private void loadModuleConfiguration(final Bundle bundle, final ModulesConfiguration modulesConfig)
      throws BundleException {
    ModuleInfo moduleInfo = modulesConfig.associateBundle(bundle);
    if (moduleInfo == null) return;

    final String[] paths = ModulesLoader.getConfigPath(bundle);
    for (final String configPath : paths) {
      InputStream is = null;

      try {
        is = JarResourceLoader.getJarResource(new URL(bundle.getLocation()), configPath);
      } catch (MalformedURLException murle) {
        moduleInfo.setError(new BundleException("Unable to create URL from: " + bundle.getLocation(), murle));
      } catch (IOException ioe) {
        moduleInfo.setError(new BundleException("Unable to extract " + configPath + " from URL: "
                                                + bundle.getLocation(), ioe));
      }

      if (is == null) {
        continue;
      }

      try {
        final DsoApplication application = DsoApplication.Factory.parse(is);
        if (application != null) {
          ArrayList errors = new ArrayList();
          XmlOptions opts = new XmlOptions();
          opts.setErrorListener(errors);
          if (!application.validate(opts)) {
            StringBuffer sb = new StringBuffer("Bundle XML fragment invalid");
            if (errors.size() > 0) {
              sb.append(": ");
              Object error = errors.get(0);
              if (error instanceof XmlError) {
                sb.append(((XmlError) error).getMessage());
              } else {
                sb.append(error.toString());
              }
              if (errors.size() > 1) {
                int remainingErrors = errors.size() - 1;
                sb.append(MessageFormat.format(", ({0} more)", remainingErrors));
              }
            }
            moduleInfo.setError(new BundleException(sb.toString()));
          }
          modulesConfig.setModuleApplication(moduleInfo, application);
        }
      } catch (Exception e) {
        moduleInfo.setError(new BundleException("Failed to parse bundle XML fragment", e));
      } finally {
        IOUtils.closeQuietly(is);
      }
    }
  }

  public BootClassHelper getBootClassHelper(IProject project) {
    BootClassHelper bch = (BootClassHelper) getSessionProperty(project, BOOT_CLASS_HELPER);
    if (bch == null) {
      setBootClassHelper(project, bch = new BootClassHelper(JavaCore.create(project)));
    }
    return bch;
  }

  public void setBootClassHelper(IProject project, BootClassHelper helper) {
    setSessionProperty(project, BOOT_CLASS_HELPER, helper);
  }

  /**
   * Instantiate the config information from the passed-in text.
   */
  private void loadConfiguration(IProject project, String xmlText) throws Exception, ConcurrentModificationException,
      IOException {
    IFile configFile = getConfigurationFile(project);

    if (configFile != null) {
      List errors = new ArrayList();

      LineLengths lineLengths = new LineLengths(new StringReader(xmlText));
      setSessionProperty(project, CONFIGURATION_LINE_LENGTHS, lineLengths);

      clearSAXMarkers(configFile);

      // The following line may throw XmlException if the doc is not well-formed.
      // That's why LineLengths are setup earlier, they are used by the catching
      // block to apply SAXMarkers to the config IFile. Otherwise, it's done
      // below.

      TcConfigDocument doc;
      TcConfig config;

      doc = m_configLoader.parse(xmlText, m_xmlOptions);
      config = doc.getTcConfig();
      setSessionProperty(project, CONFIGURATION, config);

      m_xmlOptions.setErrorListener(errors);
      if (!doc.validate(m_xmlOptions)) {
        handleXmlErrors(configFile, lineLengths, errors.iterator());
      }
      m_xmlOptions.setErrorListener(null);

      clearModulesConfiguration(project);
      Client client = config.getClients();
      if (client != null && client.isSetModules() && client.getModules().sizeOfModuleArray() > 0) {
        ModulesConfiguration modulesConfig = createModulesConfiguration(project);
        initModules(client.getModules(), modulesConfig);
      }

      setBootClassHelper(project, new BootClassHelper(JavaCore.create(project)));
    }
  }

  public void fileMoved(IFile file, IPath movedFromPath) {
    IProject project = file.getProject();
    IPath path = file.getProjectRelativePath();
    String extension = path.getFileExtension();
    IPath fromPath = movedFromPath.removeFirstSegments(1);

    if (extension.equals("xml")) {
      String configPath = getConfigurationFilePath(project);

      if (fromPath.toString().equals(configPath)) {
        setConfigurationFilePath(project, path.toString());
      }
    }
  }

  public void replaceConfigText(IProject project, String oldText, String newText) {
    ConfigurationEditor editor = getConfigurationEditor(project);
    IDocument doc;

    if (editor != null) {
      doc = editor.getDocument();
    } else {
      try {
        IFile configFile = getConfigurationFile(project);
        String content = IOUtils.toString(configFile.getContents());
        doc = new Document(content);
      } catch (Exception e) {
        openError("Problem handling refactor", e);
        return;
      }
    }

    FindReplaceDocumentAdapter finder = new FindReplaceDocumentAdapter(doc);
    int offset = 0;
    IRegion region;

    try {
      while ((region = finder.find(offset, "\\Q" + oldText + "\\E", true, true, true, false)) != null) {
        region = finder.replace(newText, false);
        offset = region.getOffset() + region.getLength();
      }

      IFile configFile = getConfigurationFile(project);
      InputStream stream = new ByteArrayInputStream(doc.get().getBytes());

      configFile.setContents(stream, true, true, null);
      JavaSetupParticipant.inspectAll();
    } catch (Exception e) {
      openError("Problem handling refactor", e);
      return;
    }
  }

  /**
   * Called when a new project is created and it has a leftover Terracotta nature. This can happen because it doesn't
   * appear to be possible to do anything useful with IResourceChangeEvent.PRE_DELETE because the workspace is locked
   * then... and after, the project doesn't exists anymore.
   */
  public void staleProjectAdded(IProject project) {
    try {
      IProjectDescription description = project.getDescription();
      String[] natures = description.getNatureIds();
      String[] newNatures = new String[natures.length - 1];

      for (int i = 0; i < natures.length; i++) {
        if (!natures[i].equals(ProjectNature.NATURE_ID)) {
          newNatures[i] = natures[i];
        }
      }
      description.setNatureIds(newNatures);
      project.setDescription(description, null);
      updateDecorator("org.terracotta.dso.projectDecorator");
    } catch (CoreException ce) {
      /**/
    }
  }

  public void fileRemoved(IFile file) {
    IPath path = file.getProjectRelativePath();
    String extension = path.getFileExtension();

    if (extension.equals("java")) {
      getConfigurationHelper(file.getProject()).validateAll();
    } else if (extension.equals("xml")) {
      final IProject project = file.getProject();
      IFile configFile = getConfigurationFile(project);

      if (file.equals(configFile)) {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            removeTerracottaNature(JavaCore.create(project));
          }
        });
      }
    }
  }

  /**
   * Sets the new config information from the passed-in text. This is invoked by the ConfigurationEditor after the user
   * modifies its contents manually.
   */
  public void setConfigurationFromString(IProject project, String xmlText) throws Exception {
    TcConfig config = null;

    try {
      loadConfiguration(project, xmlText);
    } catch (XmlException xmle) {
      LineLengths lineLengths = getConfigurationLineLengths(project);
      handleXmlException(getConfigurationFile(project), lineLengths, xmle);
    } catch (Exception e) {
      openError("Loading configuration", e);
    }
    config = (TcConfig) getSessionProperty(project, CONFIGURATION);
    if (config == null) {
      config = BAD_CONFIG;
      setSessionProperty(project, CONFIGURATION, config);
    } else {
      getConfigurationHelper(project).validateAll();
    }
    fireConfigurationChange(project);
  }

  /**
   * Return the config document's line length information, which might not exist.
   */
  public LineLengths getConfigurationLineLengths(IProject project) {
    return (LineLengths) getSessionProperty(project, CONFIGURATION_LINE_LENGTHS);
  }

  public ConfigurationHelper getConfigurationHelper(IProject project) {
    ConfigurationHelper helper = (ConfigurationHelper) getSessionProperty(project, CONFIGURATION_HELPER);

    if (helper == null) {
      helper = new ConfigurationHelper(project);
      setSessionProperty(project, CONFIGURATION_HELPER, helper);
    }

    return helper;
  }

  public static final TcConfig BAD_CONFIG = createTemplateConfigDoc().getTcConfig();

  public static TcConfigDocument createTemplateConfigDoc() {
    TcConfigDocument doc = TcConfigDocument.Factory.newInstance();
    TcConfig config = doc.addNewTcConfig();
    Servers servers = config.addNewServers();
    Server server = servers.addNewServer();

    server.setHost("%i");
    server.setName("localhost");

    BindPort dsoPort = BindPort.Factory.newInstance();
    dsoPort.setBind("0.0.0.0");
    dsoPort.setIntValue(9510);
    server.addNewDsoPort();
    server.setDsoPort(dsoPort);

    BindPort jmxPort = BindPort.Factory.newInstance();
    jmxPort.setBind("0.0.0.0");
    jmxPort.setIntValue(9520);
    server.addNewJmxPort();
    server.setJmxPort(jmxPort);

    server.setData("terracotta/server-data");
    server.setLogs("terracotta/server-logs");
    server.setStatistics("terracotta/cluster-statistics");

    Client clients = config.addNewClients();
    clients.setLogs("terracotta/client-logs");

    return doc;
  }

  public TcConfig getConfiguration(IProject project) {
    if (project == null) return null;

    boolean fireChanged = false;
    TcConfig config = null;
    synchronized (project) {
      config = (TcConfig) getSessionProperty(project, CONFIGURATION);

      if (config == null) {
        try {
          loadConfiguration(project);
          fireChanged = true;
        } catch (XmlException e) {
          LineLengths lineLengths = getConfigurationLineLengths(project);
          handleXmlException(getConfigurationFile(project), lineLengths, e);
        } catch (Exception e) {
          openError("Loading configuration", e);
        } catch (NoClassDefFoundError noClassDef) {
          openError("Loading configuration", noClassDef);
        }
        config = (TcConfig) getSessionProperty(project, CONFIGURATION);
        if (config == null) {
          config = BAD_CONFIG;
          setSessionProperty(project, CONFIGURATION, config);
          fireChanged = true;
        }
      }
    }

    if (fireChanged) {
      if (config != BAD_CONFIG) {
        getConfigurationHelper(project).validateAll();
      }
      fireConfigurationChange(project);
    }

    return config;
  }

  public void handleXmlException(IFile configFile, LineLengths lineLengths, XmlException e) {
    if (configFile != null) {
      handleXmlErrors(configFile, lineLengths, e.getErrors().iterator());
    }
  }

  public void handleXmlErrors(IFile configFile, LineLengths lineLengths, Iterator errors) {
    try {
      XmlError error;

      while (errors.hasNext()) {
        error = (XmlError) errors.next();

        HashMap<String, Object> map = new HashMap<String, Object>();
        int line = error.getLine();
        int col = error.getColumn();
        String msg = error.getMessage();
        int severity = XmlError2IMarkerSeverity(error.getSeverity());
        int start = lineLengths.offset(line - 1);
        int end = start + ((col == -1) ? lineLengths.lineSize(line - 1) : col);

        MarkerUtilities.setMessage(map, msg);
        MarkerUtilities.setLineNumber(map, line);
        MarkerUtilities.setCharStart(map, start);
        MarkerUtilities.setCharEnd(map, end);

        map.put(IMarker.PRIORITY, Integer.valueOf(IMarker.PRIORITY_HIGH));
        map.put(IMarker.SEVERITY, Integer.valueOf(severity));
        map.put(IMarker.LOCATION, "line " + line);

        MarkerUtilities.createMarker(configFile, map, "org.terracotta.dso.SAXMarker");
      }
    } catch (CoreException ce) {
      openError("Error handling XML error", ce);
    }
  }

  private int XmlError2IMarkerSeverity(int severity) {
    switch (severity) {
      case XmlError.SEVERITY_ERROR:
        return IMarker.SEVERITY_ERROR;
      case XmlError.SEVERITY_INFO:
        return IMarker.SEVERITY_INFO;
      case XmlError.SEVERITY_WARNING:
        return IMarker.SEVERITY_WARNING;
      default:
        return IMarker.SEVERITY_INFO;
    }
  }

  ConfigurationEditor[] getConfigurationEditors(IProject project) {
    ArrayList<ConfigurationEditor> list = new ArrayList<ConfigurationEditor>();
    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();

    for (IWorkbenchWindow window : windows) {
      IWorkbenchPage[] pages = window.getPages();

      for (IWorkbenchPage page : pages) {
        IEditorReference[] editorRefs = page.getEditorReferences();

        for (IEditorReference editorRef : editorRefs) {
          IEditorPart editorPart = editorRef.getEditor(false);

          if (editorPart != null) {
            if (editorPart instanceof ConfigurationEditor) {
              ConfigurationEditor configEditor = (ConfigurationEditor) editorPart;
              IFile file = ((IFileEditorInput) editorPart.getEditorInput()).getFile();

              if (file.getProject().equals(project)) {
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
    if (project == null) return;

    synchronized (project) {
      clearConfigurationSessionProperties(project);
      getConfiguration(project);
    }

    ConfigurationEditor[] configEditors = getConfigurationEditors(project);
    for (ConfigurationEditor configEditor : configEditors) {
      IFileEditorInput fileEditorInput = (IFileEditorInput) configEditor.getEditorInput();
      IFile file = fileEditorInput.getFile();
      configEditor.newInputFile(file);
    }
  }

  public void clearConfigurationSessionProperties(IProject project) {
    if (project == null) return;

    synchronized (project) {
      IFile file = getConfigurationFile(project);
      if (file != null && file.exists()) {
        setSessionProperty(file, ACTIVE_CONFIGURATION_FILE, null);
      }

      setSessionProperty(project, CONFIGURATION_LINE_LENGTHS, null);
      setSessionProperty(project, CONFIGURATION_FILE, null);
      setSessionProperty(project, CONFIGURATION, null);

      setConfigurationFileDirty(project, Boolean.FALSE);
    }
  }

  public void setConfigurationFileDirty(IProject project, Boolean dirty) {
    if (project == null) return;

    synchronized (project) {
      setSessionProperty(project, IS_DIRTY, dirty);
    }
  }

  public boolean isConfigurationFileDirty(IProject project) {
    if (project == null) throw new IllegalArgumentException("Project is null");

    synchronized (project) {
      if (project.isOpen()) {
        Boolean dirty = (Boolean) getSessionProperty(project, IS_DIRTY);
        return dirty != null ? dirty.booleanValue() : false;
      }
      return false;
    }
  }

  public void ignoreNextConfigChange() {
    ResourceDeltaVisitor rdv = getResourceDeltaVisitor();
    rdv.fIgnoreNextConfigChange = true;
  }

  public void saveConfiguration(IProject project) {
    IFile configFile = getConfigurationFile(project);
    if (configFile != null) {
      InputStream stream = null;

      try {
        TcConfig config = getConfiguration(project);
        if (config != null) {
          ignoreNextConfigChange();

          TcConfigDocument configDoc = TcConfigDocument.Factory.newInstance();
          configDoc.setTcConfig(config);
          stream = configDoc.newInputStream(getXmlOptions());
          configFile.setContents(stream, true, true, null);
          stream = null;

          getConfigurationHelper(project).validateAll();
          fireConfigurationChange(project);
        }
      } catch (Exception e) {
        openError("Error saving '" + configFile.getName() + "'", e);
      } finally {
        if (stream != null) {
          IOUtils.closeQuietly(stream);
        }
      }
    }
  }

  public void saveConfigurationQuietly(IProject project) {
    IFile configFile;
    TcConfig config;
    TcConfigDocument configDoc;
    XmlOptions opts;
    InputStream inStream;
    FileWriter outWriter;

    opts = getXmlOptions();
    configFile = getConfigurationFile(project);
    config = getConfiguration(project);
    configDoc = TcConfigDocument.Factory.newInstance();
    inStream = null;
    outWriter = null;

    try {
      if (config != null) {
        configDoc.setTcConfig(config);

        inStream = configDoc.newInputStream(opts);
        outWriter = new FileWriter(configFile.getLocation().toOSString());

        CopyUtils.copy(inStream, outWriter);
      }
    } catch (Exception e) {
      openError("Error saving '" + configFile.getName() + "'", e);
    } finally {
      IOUtils.closeQuietly(inStream);
      IOUtils.closeQuietly(outWriter);
    }
  }

  public void setSessionProperty(IResource res, QualifiedName name, Object value) {
    if (res != null && res.exists()) {
      try {
        res.setSessionProperty(name, value);
      } catch (CoreException ce) {
        /**/
      }
    }
  }

  public Object getSessionProperty(IResource res, QualifiedName name) {
    if (res != null && res.exists()) {
      try {
        return res.getSessionProperty(name);
      } catch (CoreException ce) {
        /**/
      }
    }

    return null;
  }

  public void setPersistentProperty(IResource resource, QualifiedName name, String value) {
    if (resource != null && resource.exists()) {
      try {
        resource.setPersistentProperty(name, value);
      } catch (CoreException ce) {
        /**/
      }
    }
  }

  public String getPersistentProperty(IResource resource, QualifiedName name) {
    if (resource != null && resource.exists()) {
      try {
        return resource.getPersistentProperty(name);
      } catch (CoreException ce) {
        return null;
      }
    }

    return null;
  }

  public ConfigurationEditor getConfigurationEditor(IProject project) {
    ConfigurationEditor[] configEditors = getConfigurationEditors(project);
    IFile configFile = getConfigurationFile(project);

    for (ConfigurationEditor configEditor : configEditors) {
      IFileEditorInput fileEditorInput = (IFileEditorInput) configEditor.getEditorInput();
      IFile file = fileEditorInput.getFile();

      if (file.equals(configFile)) { return configEditor; }
    }

    return null;
  }

  public ConfigurationEditor ensureConfigurationEditor(IProject project) {
    ConfigurationEditor editor = getConfigurationEditor(project);

    if (editor == null) {
      try {
        editor = openConfigurationEditor(project);
      } catch (PartInitException pie) {
        openError("Unable to open configuration editor", pie);
      }
    }

    return editor;
  }

  public void setConfigurationFilePath(IProject project, String path) {
    String oldPath = getConfigurationFilePath(project);
    IFile file = oldPath != null ? project.getFile(oldPath) : null;
    if (file != null && file.exists()) {
      clearSAXMarkers(file);
      setSessionProperty(file, ACTIVE_CONFIGURATION_FILE, null);
    }

    setPersistentProperty(project, CONFIGURATION_FILE_PATH, path);

    file = project.getFile(new Path(path));
    setSessionProperty(project, CONFIGURATION_FILE, file);
    setSessionProperty(file, ACTIVE_CONFIGURATION_FILE, "true");
  }

  public String getConfigurationFilePath(IProject project) {
    return getPersistentProperty(project, CONFIGURATION_FILE_PATH);
  }

  public IFile getConfigurationFile(IProject project) {
    IFile file = (IFile) getSessionProperty(project, CONFIGURATION_FILE);

    if (file == null) {
      String path = getConfigurationFilePath(project);

      if (path != null) {
        file = project.getFile(new Path(path));
        setSessionProperty(project, CONFIGURATION_FILE, file);
        setSessionProperty(file, ACTIVE_CONFIGURATION_FILE, "true");
      }
    }

    return file;
  }

  public boolean validateConfigurationFile(final IProject project) {
    final AtomicBoolean result = new AtomicBoolean(true);
    IFile configFile = getConfigurationFile(project);
    if (configFile == null) {
      setConfigurationFilePath(project, TcPlugin.DEFAULT_CONFIG_FILENAME);
      configFile = getConfigurationFile(project);
    }
    if (!configFile.exists()) {
      final Display display = Display.getDefault();
      display.syncExec(new Runnable() {
        public void run() {
          try {
            if (!project.isSynchronized(IResource.DEPTH_INFINITE)) {
              project.refreshLocal(IResource.DEPTH_INFINITE, null);
            }
          } catch (CoreException ce) {
            /**/
          }
          Shell shell = TcPlugin.getActiveWorkbenchShell();
          String pageId = "org.terracotta.dso.ui.propertyPages.PropertyPage";
          IJavaProject javaProject = JavaCore.create(project);
          PreferenceDialog prefDialog = PreferencesUtil.createPropertyDialogOn(shell, javaProject, pageId, null, null);
          result.set(prefDialog.open() == Window.OK);
        }
      });
    }
    final String markerType = "org.terracotta.dso.ConfigFileNotFoundMarker";
    if (result.get()) {
      try {
        project.deleteMarkers(markerType, false, IResource.DEPTH_ZERO);
      } catch (CoreException ce) {
        /**/
      }
    } else {
      final HashMap<String, Object> map = new HashMap<String, Object>();
      MarkerUtilities.setMessage(map, "Config file not found");
      map.put(IMarker.PRIORITY, Integer.valueOf(IMarker.PRIORITY_HIGH));
      map.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_ERROR));
      map.put(IMarker.LOCATION, configFile.getFullPath().toString());
      final Display display = Display.getDefault();
      display.syncExec(new Runnable() {
        public void run() {
          try {
            MarkerUtilities.createMarker(project, map, markerType);
          } catch (CoreException ce) {
            openError("Problem creating marker '" + markerType + "'", ce);
          }
        }
      });
    }
    return result.get();
  }

  public void setServerOptions(IProject project, String opts) {
    setPersistentProperty(project, SERVER_OPTIONS, opts);
  }

  public String getServerOptions(IProject project) {
    String options = getPersistentProperty(project, SERVER_OPTIONS);
    return options != null ? options : "";
  }

  public void setAutoStartServerOption(IProject project, boolean autoStartServer) {
    setPersistentProperty(project, AUTO_START_SERVER_OPTION, Boolean.toString(autoStartServer));
  }

  public boolean getAutoStartServerOption(IProject project) {
    String option = getPersistentProperty(project, AUTO_START_SERVER_OPTION);
    return option != null ? Boolean.parseBoolean(option) : DEFAULT_AUTO_START_SERVER_OPTION;
  }

  public void setWarnConfigProblemsOption(IProject project, boolean warnConfigProblems) {
    setPersistentProperty(project, WARN_CONFIG_PROBLEMS_OPTION, Boolean.toString(warnConfigProblems));
  }

  public boolean getWarnConfigProblemsOption(IProject project) {
    String option = getPersistentProperty(project, WARN_CONFIG_PROBLEMS_OPTION);
    return option != null ? Boolean.parseBoolean(option) : DEFAULT_WARN_CONFIG_PROBLEMS_OPTION;
  }

  public void setQueryRestartOption(IProject project, boolean queryRestart) {
    setPersistentProperty(project, QUERY_RESTART_OPTION, Boolean.toString(queryRestart));
  }

  public boolean getQueryRestartOption(IProject project) {
    String option = getPersistentProperty(project, QUERY_RESTART_OPTION);
    return option != null ? Boolean.parseBoolean(option) : DEFAULT_QUERY_RESTART_OPTION;
  }

  public boolean hasTerracottaNature(IJavaElement element) {
    return hasTerracottaNature(element.getJavaProject().getProject());
  }

  public boolean hasTerracottaNature(IProject project) {
    try {
      return project != null && project.hasNature(ProjectNature.NATURE_ID);
    } catch (CoreException ce) {/**/
    }
    return false;
  }

  public void inspect(final ICompilationUnit cu) {
    if (cu != null) {
      getCompilationUnitVisitor().inspect(cu);
    }
  }

  private CompilationUnitVisitor getCompilationUnitVisitor() {
    if (m_compilationUnitVisitor == null) {
      m_compilationUnitVisitor = new CompilationUnitVisitor();
    }
    return m_compilationUnitVisitor;
  }

  private ResourceDeltaVisitor getResourceDeltaVisitor() {
    if (m_resourceDeltaVisitor == null) {
      m_resourceDeltaVisitor = new ResourceDeltaVisitor();
    }
    return m_resourceDeltaVisitor;
  }

  class ResourceListener implements IResourceChangeListener {
    public void resourceChanged(final IResourceChangeEvent event) {
      switch (event.getType()) {
        case IResourceChangeEvent.POST_BUILD: {
          try {
            event.getDelta().accept(getResourceDeltaVisitor());
          } catch (CoreException ce) {
            openError("Error handling resource change event for '" + event.getResource().getName() + "'", ce);
          }
          break;
        }
      }
    }
  }

  public void updateDecorator(final String id) {
    updateDecorators(new String[] { id });
  }

  private DecoratorUpdateAction getDecoratorUpdateAction() {
    if (m_decoratorUpdateAction == null) {
      m_decoratorUpdateAction = new DecoratorUpdateAction();
    }
    return m_decoratorUpdateAction;
  }

  public void updateDecorators(String[] ids) {
    DecoratorUpdateAction updater = getDecoratorUpdateAction();

    updater.setDecorators(ids);
    Display.getDefault().asyncExec(updater);
  }

  static class DecoratorUpdateAction implements Runnable {
    IDecoratorManager m_decoratorManager;
    String[]          m_decorators;

    DecoratorUpdateAction() {
      m_decoratorManager = PlatformUI.getWorkbench().getDecoratorManager();
    }

    void setDecorators(String[] ids) {
      m_decorators = ids;
    }

    public void run() {
      for (String decorator : m_decorators) {
        m_decoratorManager.update(decorator);
      }
    }
  }

  private static final String[] DECORATOR_IDS = { "org.terracotta.dso.projectDecorator",
      ConfigFileDecorator.DECORATOR_ID, ServerRunningDecorator.DECORATOR_ID, AdaptedModuleDecorator.DECORATOR_ID,
      AdaptedTypeDecorator.DECORATOR_ID, AdaptedPackageFragmentDecorator.DECORATOR_ID,
      ExcludedTypeDecorator.DECORATOR_ID, ExcludedModuleDecorator.DECORATOR_ID,
      DistributedMethodDecorator.DECORATOR_ID, NameLockedDecorator.DECORATOR_ID, AutolockedDecorator.DECORATOR_ID,
      RootDecorator.DECORATOR_ID, TransientDecorator.DECORATOR_ID, };

  public void updateDecorators() {
    updateDecorators(DECORATOR_IDS);
  }

  public boolean hasSAXMarkers(IResource res) throws CoreException {
    if (res != null && res.exists()) {
      IMarker[] markers = res.findMarkers("org.terracotta.dso.SAXMarker", false, IResource.DEPTH_ZERO);
      return markers.length > 0;
    }
    return false;
  }

  public void clearSAXMarkers(IResource res) {
    if (res != null && res.exists()) {
      try {
        res.deleteMarkers("org.terracotta.dso.SAXMarker", false, IResource.DEPTH_ZERO);
      } catch (CoreException ce) {
        openError("Error clearing SAX markers on file'" + res.getName() + "'", ce);
      }
    }
  }

  public boolean hasProblemMarkers(IResource res) throws CoreException {
    if (res != null && res.exists()) {
      IMarker[] markers = res.findMarkers("org.eclipse.core.resources.problemmarker", true, IResource.DEPTH_ZERO);
      return markers.length > 0;
    }
    return false;
  }

  public void clearConfigProblemMarkers(IProject project) {
    try {
      IFile configFile = getConfigurationFile(project);

      if (configFile != null && configFile.exists()) {
        configFile.deleteMarkers("org.terracotta.dso.ConfigProblemMarker", true, IResource.DEPTH_ZERO);
      }
    } catch (CoreException ce) {/**/
    }
  }

  public void clearConfigProblemMarkersOfType(IProject project, String markerType) {
    try {
      IFile configFile = getConfigurationFile(project);

      if (configFile != null && configFile.exists()) {
        configFile.deleteMarkers(markerType, false, IResource.DEPTH_ZERO);
      }
    } catch (CoreException ce) {/**/
    }
  }

  public static ImageDescriptor getImageDescriptor(String path) {
    return AbstractUIPlugin.imageDescriptorFromPlugin(getPluginId(), path);
  }

  public ConfigurationEditor openConfigurationEditor(IProject project) throws PartInitException {
    IWorkbenchPage wbPage = getActivePage();

    if (wbPage != null) {
      IFile configFile = getConfigurationFile(project);
      FileEditorInput fileEditorInput = new FileEditorInput(configFile);
      IEditorPart editorPart = wbPage.findEditor(fileEditorInput);

      if (editorPart != null) {
        if (editorPart instanceof ConfigurationEditor) {
          wbPage.activate(editorPart);
          return (ConfigurationEditor) editorPart;
        } else {
          wbPage.closeEditor(editorPart, true);
        }
      }

      if (configFile != null) {
        String configEditorID = "editors.configurationEditor";

        return (ConfigurationEditor) IDE.openEditor(wbPage, configFile, configEditorID, false);
      }
    }

    return null;
  }

  public void closeConfigurationEditor(IProject project) {
    final ConfigurationEditor configEditor = getConfigurationEditor(project);

    if (configEditor != null) {
      if (Display.getCurrent() == null) {
        Display.getDefault().syncExec(new Runnable() {
          public void run() {
            configEditor.closeEditor();
          }
        });
      } else {
        configEditor.closeEditor();
      }
    }
  }

  private XmlOptions createXmlOptions() {
    XmlOptions opts = new XmlOptions();

    opts.setLoadLineNumbers();
    opts.setSavePrettyPrint();
    opts.setSavePrettyPrintIndent(2);
    opts.remove(XmlOptions.LOAD_STRIP_WHITESPACE);
    opts.remove(XmlOptions.LOAD_STRIP_COMMENTS);

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
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        ErrorDialog.showError(msg);
      }
    });
  }

  public boolean continueWithConfigProblems(final IProject project) throws CoreException {
    IFile configFile = getConfigurationFile(project);

    setSessionProperty(project, CONFIG_PROBLEM_CONTINUE, null);

    if (getWarnConfigProblemsOption(project) && hasProblemMarkers(configFile)) {
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          Shell shell = Display.getDefault().getActiveShell();
          ConfigProblemsDialog dialog = new ConfigProblemsDialog(shell, project);

          if (dialog.open() == IDialogConstants.OK_ID) {
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
      display = Display.getDefault();
    }
    return display;
  }

  public void fireConfigurationChange(IProject project) {
    m_configurationEventManager.fireConfigurationChange(project);
    if (hasTerracottaNature(project)) {
      JavaSetupParticipant.inspectAll();
    }
    updateDecorators();
  }

  public void fireServerChanged(IProject project, int index) {
    m_configurationEventManager.fireServerChanged(project, index);
  }

  public void fireServersChanged(IProject project) {
    m_configurationEventManager.fireServersChanged(project);
  }

  public void fireRootChanged(IProject project, int index) {
    m_configurationEventManager.fireRootChanged(project, index);
    JavaSetupParticipant.inspectAll();
    updateDecorator(RootDecorator.DECORATOR_ID);
  }

  public void fireRootsChanged(IProject project) {
    m_configurationEventManager.fireRootsChanged(project);
    JavaSetupParticipant.inspectAll();
    updateDecorator(RootDecorator.DECORATOR_ID);
  }

  public void fireNamedLockChanged(IProject project, int index) {
    m_configurationEventManager.fireNamedLockChanged(project, index);
    JavaSetupParticipant.inspectAll();
    updateDecorator(NameLockedDecorator.DECORATOR_ID);
  }

  public void fireNamedLocksChanged(IProject project) {
    m_configurationEventManager.fireNamedLocksChanged(project);
    JavaSetupParticipant.inspectAll();
    updateDecorator(NameLockedDecorator.DECORATOR_ID);
  }

  public void fireAutolockChanged(IProject project, int index) {
    m_configurationEventManager.fireAutolockChanged(project, index);
    JavaSetupParticipant.inspectAll();
    updateDecorator(AutolockedDecorator.DECORATOR_ID);
  }

  public void fireAutolocksChanged(IProject project) {
    m_configurationEventManager.fireAutolocksChanged(project);
    JavaSetupParticipant.inspectAll();
    updateDecorator(AutolockedDecorator.DECORATOR_ID);
  }

  public void fireIncludeRuleChanged(IProject project, int index) {
    m_configurationEventManager.fireIncludeRuleChanged(project, index);
    JavaSetupParticipant.inspectAll();
    updateDecorators(new String[] { AdaptedModuleDecorator.DECORATOR_ID, AdaptedTypeDecorator.DECORATOR_ID,
        AdaptedPackageFragmentDecorator.DECORATOR_ID });
  }

  public void fireIncludeRulesChanged(IProject project) {
    m_configurationEventManager.fireIncludeRulesChanged(project);
    JavaSetupParticipant.inspectAll();
    updateDecorators(new String[] { AdaptedModuleDecorator.DECORATOR_ID, AdaptedTypeDecorator.DECORATOR_ID,
        AdaptedPackageFragmentDecorator.DECORATOR_ID });
  }

  public void fireExcludeRuleChanged(IProject project, int index) {
    m_configurationEventManager.fireExcludeRuleChanged(project, index);
    JavaSetupParticipant.inspectAll();
    updateDecorators(new String[] { ExcludedTypeDecorator.DECORATOR_ID, ExcludedModuleDecorator.DECORATOR_ID });
  }

  public void fireExcludeRulesChanged(IProject project) {
    m_configurationEventManager.fireExcludeRulesChanged(project);
    JavaSetupParticipant.inspectAll();
    updateDecorators(new String[] { ExcludedTypeDecorator.DECORATOR_ID, ExcludedModuleDecorator.DECORATOR_ID });
  }

  public void fireInstrumentationRulesChanged(IProject project) {
    m_configurationEventManager.fireInstrumentationRulesChanged(project);
    JavaSetupParticipant.inspectAll();
    updateDecorators(new String[] { AdaptedModuleDecorator.DECORATOR_ID, AdaptedTypeDecorator.DECORATOR_ID,
        AdaptedPackageFragmentDecorator.DECORATOR_ID, ExcludedTypeDecorator.DECORATOR_ID,
        ExcludedModuleDecorator.DECORATOR_ID });
  }

  public void fireTransientFieldsChanged(IProject project) {
    m_configurationEventManager.fireTransientFieldsChanged(project);
    JavaSetupParticipant.inspectAll();
    updateDecorator(TransientDecorator.DECORATOR_ID);
  }

  public void fireTransientFieldChanged(IProject project, int index) {
    m_configurationEventManager.fireTransientFieldChanged(project, index);
    JavaSetupParticipant.inspectAll();
    updateDecorator(TransientDecorator.DECORATOR_ID);
  }

  public void fireBootClassesChanged(IProject project) {
    m_configurationEventManager.fireBootClassesChanged(project);
    JavaSetupParticipant.inspectAll();
  }

  public void fireBootClassChanged(IProject project, int index) {
    m_configurationEventManager.fireBootClassChanged(project, index);
    JavaSetupParticipant.inspectAll();
  }

  public void fireDistributedMethodsChanged(IProject project) {
    m_configurationEventManager.fireDistributedMethodsChanged(project);
    updateDecorator(DistributedMethodDecorator.DECORATOR_ID);
    JavaSetupParticipant.inspectAll();
  }

  public void fireDistributedMethodChanged(IProject project, int index) {
    m_configurationEventManager.fireDistributedMethodChanged(project, index);
    updateDecorator(DistributedMethodDecorator.DECORATOR_ID);
    JavaSetupParticipant.inspectAll();
  }

  public void fireClientChanged(IProject project) {
    m_configurationEventManager.fireClientChanged(project);
  }

  public void fireModuleRepoChanged(IProject project, int index) {
    m_configurationEventManager.fireModuleRepoChanged(project, index);
  }

  public void fireModuleReposChanged(IProject project) {
    m_configurationEventManager.fireModuleReposChanged(project);
  }

  public void fireModuleChanged(IProject project, int index) {
    m_configurationEventManager.fireModuleChanged(project, index);
  }

  public void fireModulesChanged(IProject project) {
    m_configurationEventManager.fireModulesChanged(project);
  }

  public void addConfigurationListener(IConfigurationListener listener) {
    m_configurationEventManager.addConfigurationListener(listener);
  }

  public void removeConfigurationListener(IConfigurationListener listener) {
    m_configurationEventManager.removeConfigurationListener(listener);
  }

  public static Image createImage(String path) {
    return new JavaElementImageDescriptor(getImageDescriptor(path), 0, new Point(16, 16)).createImage(false);
  }

  public static Image createImage(URL path) {
    return new JavaElementImageDescriptor(ImageDescriptor.createFromURL(path), 0, new Point(16, 16)).createImage(false);
  }

  public String configDocumentAsString(TcConfigDocument configDoc) {
    InputStream is = configDoc.newInputStream(getXmlOptions());
    StringWriter writer = new StringWriter();
    try {
      CopyUtils.copy(is, writer);
    } catch (IOException ioe) {/**/
    }
    return writer.toString();
  }
}

class ErrorDialog extends MessageDialog {
  ErrorDialog(String msg) {
    super(null, "Terracotta Plugin", null, msg, MessageDialog.ERROR, new String[] { IDialogConstants.OK_LABEL }, 0);
  }

  static void showError(String msg) {
    ErrorDialog dialog = new ErrorDialog(msg);
    dialog.setShellStyle(dialog.getShellStyle() | SWT.RESIZE);
    dialog.open();
  }
}
