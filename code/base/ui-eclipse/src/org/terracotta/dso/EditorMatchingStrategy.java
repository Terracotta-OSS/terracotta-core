/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;

public class EditorMatchingStrategy implements IEditorMatchingStrategy {
  public boolean matches(IEditorReference editorRef, IEditorInput input) {
    if(input instanceof IFileEditorInput) {
      TcPlugin plugin  = TcPlugin.getDefault();
      IFile    file    = ((IFileEditorInput)input).getFile();
      IProject project = file.getProject();
      
      return file.getFullPath().getFileExtension().equals("xml") &&
         plugin.hasTerracottaNature(project) &&
         file.equals(plugin.getConfigurationFile(project));
    }
    
    return false;
  }
}
