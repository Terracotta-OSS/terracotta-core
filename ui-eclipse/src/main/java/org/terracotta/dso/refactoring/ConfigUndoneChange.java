/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.refactoring;

import org.apache.xmlbeans.XmlOptions;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.terracotta.dso.TcPlugin;

import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

public class ConfigUndoneChange extends Change {
  private TcConfig fConfig;
  private IProject fProject;
    
  public ConfigUndoneChange(IProject project, TcConfig config) {
    super();
    fProject = project;
    fConfig  = config;
  }
  
  public Object getModifiedElement() {
    return null;
  }
  
  public String getName() {
    return "TCConfigUndoneUpdate";
  }
  
  public void initializeValidationData(IProgressMonitor pm) {/**/}
  
  public RefactoringStatus isValid(IProgressMonitor pm)
    throws OperationCanceledException
  {
    return new RefactoringStatus();
  }
  
  public Change perform(IProgressMonitor pm) {
    TcPlugin plugin = TcPlugin.getDefault();
    TcConfig config = (TcConfig)plugin.getConfiguration(fProject).copy();
    
    try {
      XmlOptions xmlOpts = plugin.getXmlOptions();
      TcConfigDocument doc = TcConfigDocument.Factory.newInstance(xmlOpts);
      doc.setTcConfig(fConfig);
      plugin.setConfigurationFromString(fProject, plugin.configDocumentAsString(doc));
    } catch(Exception e) {
      e.printStackTrace();
    }
    
    // create the undo change
    return new ConfigUndoneChange(fProject, config);
  }
}
