/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.decorator;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;

import org.terracotta.dso.ServerTracker;
import org.terracotta.dso.TcPlugin;

/**
 * Adorns Terracotta projects that have a running server.
 * 
 * The adornment appears in the Package Explorer amd Outline view.
 * 
 * @see org.eclipse.jface.viewers.LabelProvider
 * @see org.terracotta.dso.TcPlugin.hasTerracottaNature
 * @see org.terracotta.dso.ServerTracker.isRunning
 */

public class ServerRunningDecorator extends LabelProvider
  implements ILightweightLabelDecorator
{
  private static final ImageDescriptor
    m_imageDesc = ImageDescriptor.createFromURL(
        ServerRunningDecorator.class.getResource(
          "/com/tc/admin/icons/run_co.gif"));

  public static final String
    DECORATOR_ID = "org.terracotta.dso.serverRunningDecorator";

  public void decorate(Object element, IDecoration decoration) {
    IJavaProject javaProj = (IJavaProject)element;

    if(TcPlugin.getDefault().hasTerracottaNature(javaProj) &&
       ServerTracker.getDefault().isRunning(javaProj))
    {
      decoration.addOverlay(m_imageDesc);
    }
  }
  
  public static void updateDecorators() {
    TcPlugin.getDefault().updateDecorator(DECORATOR_ID);
  }
}
