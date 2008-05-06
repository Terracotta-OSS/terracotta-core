/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.launch;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.terracotta.dso.TcPlugin;

public interface IDSOLaunchConfigurationConstants extends IJavaLaunchConfigurationConstants {
  public static final String ID_SERVER_SPEC = TcPlugin.getPluginId() + ".serverSpec";
  public static final String ID_CONFIG_SERVER_SPEC = TcPlugin.getPluginId() + ".configServerSpec";
  public static final String ID_CONFIG_FILE_SPEC = TcPlugin.getPluginId() + ".configFileSpec";
}
