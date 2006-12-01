/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package refreshall;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

  // The plug-in ID
  public static final String    PLUGIN_ID = "RefreshAll";

  // The shared instance
  private static Activator      plugin;

  private IOConsole             console;

  private IOConsoleOutputStream consoleStream;

  public Activator() {
    plugin = this;
  }

  public void start(BundleContext context) throws Exception {
    console = new IOConsole("TCBuild", null);
    consoleStream = console.newOutputStream();
    final IConsoleManager mgr = ConsolePlugin.getDefault().getConsoleManager();
    mgr.addConsoles(new IConsole[] { console });
    super.start(context);
  }

  public void stop(BundleContext context) throws Exception {
    plugin = null;
    final IConsoleManager mgr = ConsolePlugin.getDefault().getConsoleManager();
    mgr.removeConsoles(new IConsole[] { console });
    console = null;
    super.stop(context);
  }

  public static Activator getDefault() {
    return plugin;
  }

  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

  public IOConsoleOutputStream getConsoleStream() {
    return consoleStream;
  }

}
