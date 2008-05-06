/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.decorator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

/**
 * Adorns Java modules whose primary-type is instrumented. The adornment appears in the Package Explorer amd Outline
 * view.
 * 
 * @see org.eclipse.jface.viewers.LabelProvider
 * @see org.terracotta.dso.ConfigurationHelper.isAdaptable
 */

public class AdaptedModuleDecorator extends LabelProvider implements ILightweightLabelDecorator {
  private static final ImageDescriptor m_imageDesc  = ImageDescriptor.createFromURL(AdaptedModuleDecorator.class
                                                        .getResource("/com/tc/admin/icons/installed_ovr.gif"));

  public static final String           DECORATOR_ID = "org.terracotta.dso.adaptedModuleDecorator";

  public void decorate(Object element, IDecoration decoration) {
    TcPlugin plugin = TcPlugin.getDefault();
    ICompilationUnit cu = (ICompilationUnit) element;
    IProject project = cu.getJavaProject().getProject();

    if (plugin != null && plugin.hasTerracottaNature(project)) {
      ConfigurationHelper config = plugin.getConfigurationHelper(project);

      if (config != null && (config.isAdaptable(cu) || TcPlugin.getDefault().isBootClass(project, cu))) {
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
