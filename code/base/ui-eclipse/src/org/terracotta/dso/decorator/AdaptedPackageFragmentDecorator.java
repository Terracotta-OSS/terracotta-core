/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.decorator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

/**
 * Adorns Java types that are instrumented.
 * 
 * The adornment appears in the Package Explorer amd Outline view.
 * 
 * @see org.eclipse.jface.viewers.LabelProvider
 * @see org.terracotta.dso.ConfigurationHelper.isAdaptable
 */

public class AdaptedPackageFragmentDecorator extends LabelProvider
  implements ILightweightLabelDecorator
{
  private static final ImageDescriptor
    m_imageDesc = ImageDescriptor.createFromURL(
      AdaptedPackageFragmentDecorator.class.getResource(
        "/com/tc/admin/icons/installed_ovr.gif"));

  public static final String
    DECORATOR_ID = "org.terracotta.dso.adaptedPackageFragmentDecorator";

  public void decorate(Object element, IDecoration decoration) {
    TcPlugin         plugin   = TcPlugin.getDefault();
    IPackageFragment fragment = (IPackageFragment)element;
    IProject         project  = fragment.getJavaProject().getProject();
  
    if(plugin.hasTerracottaNature(project)) {
      ConfigurationHelper config = plugin.getConfigurationHelper(project);

      if(config != null && config.isAdaptable(fragment)) {
        decoration.addOverlay(m_imageDesc);
      }
    }
  }

  public static void updateDecorators() {
    TcPlugin.getDefault().updateDecorator(DECORATOR_ID);
  }
}
