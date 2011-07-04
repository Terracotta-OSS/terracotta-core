/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.decorator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.terracotta.dso.TcPlugin;

/**
 * Adorns the DSO config file. The adornment appears in the Package Explorer amd Outline view.
 */

public class ConfigFileDecorator extends LabelProvider implements ILightweightLabelDecorator {
  private static final ImageDescriptor m_imageDesc  = ImageDescriptor.createFromURL(ConfigFileDecorator.class
                                                        .getResource("/com/tc/admin/icons/installed_ovr.gif"));

  public static final String           DECORATOR_ID = "org.terracotta.dso.configFileDecorator";

  public void decorate(Object element, IDecoration decoration) {
    TcPlugin plugin = TcPlugin.getDefault();
    IFile file = (IFile) element;
    IProject project = file.getProject();

    if (plugin != null && plugin.hasTerracottaNature(project)) {
      IFile configFile = plugin.getConfigurationFile(project);

      if (file.equals(configFile)) {
        decoration.addOverlay(m_imageDesc);
      }
    }
  }

  public static void updateDecorators() {
    TcPlugin plugin = TcPlugin.getDefault();
    if (plugin != null) {
      plugin.updateDecorator(DECORATOR_ID);
    }
  }
}
