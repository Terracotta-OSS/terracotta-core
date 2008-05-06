/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;

public class ProjectNature implements IProjectNature {
  public static final String NATURE_ID = ProjectNature.class.getName();
  
  private IProject m_project;
	
  public ProjectNature() {/**/}
  
  public IProject getProject()  {
    return m_project;
  }

  public void setProject(IProject project) {
    m_project = project;
  }

  public void configure() {/**/}
  public void deconfigure() {/**/}
}
