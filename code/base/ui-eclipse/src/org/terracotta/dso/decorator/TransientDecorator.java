/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.decorator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IField;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

/**
 * Adorns Java fields that are not to be included in a shared object graph. The adornment appears in the Package
 * Explorer amd Outline view.
 * 
 * @see org.eclipse.jface.viewers.LabelProvider
 * @see org.terracotta.dso.TcPlugin.hasTerracottaNature
 * @see org.terracotta.dso.ServerTracker.isRunning
 */

public class TransientDecorator extends LabelProvider implements ILightweightLabelDecorator {
  private static final ImageDescriptor m_imageDesc  = ImageDescriptor.createFromURL(TransientDecorator.class
                                                        .getResource("/com/tc/admin/icons/transient_ovr.gif"));

  public static final String           DECORATOR_ID = "org.terracotta.dso.transientDecorator";

  public void decorate(Object element, IDecoration decoration) {
    TcPlugin plugin = TcPlugin.getDefault();
    IField field = (IField) element;
    IProject project = field.getJavaProject().getProject();

    if (plugin != null && plugin.hasTerracottaNature(project)) {
      ConfigurationHelper config = plugin.getConfigurationHelper(project);

      if (config != null && config.isTransient(field)) {
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
