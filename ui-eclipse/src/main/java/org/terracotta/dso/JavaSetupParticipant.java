/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.IFileBufferListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.PlatformUI;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Hackey way to see that a Java module is being loaded into an editor.
 * We might need to inspect it at load time.
 * 
 * @see org.eclipse.core.filebuffers.IDocumentSetupParticipant
 * @see TcPlugin.inspect
 */

public class JavaSetupParticipant implements IDocumentSetupParticipant {
  private static JavaSetupParticipant m_setupParticipant;
  private ArrayList<IFile>            m_fileList;
  
  public JavaSetupParticipant() {
    m_setupParticipant = this;
    m_fileList         = new ArrayList<IFile>();
    
    FileBuffers.getTextFileBufferManager().addFileBufferListener(
      new IFileBufferListener() {
        public void bufferCreated(IFileBuffer buffer) {
          IFile file = fileForBuffer(buffer);
            
          if(file != null && shouldInspect(file)) {
            m_fileList.add(file);
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Inspector(file));
          }
        }
                                                                 
        public void bufferDisposed(IFileBuffer buffer) {
          IFile file = fileForBuffer(buffer);
          
          if(file != null) {
            m_fileList.remove(file);
          }
        }

        public void underlyingFileMoved(IFileBuffer buffer, IPath path) {
          IFile    file;
          Iterator iter = m_fileList.iterator();

          while(iter.hasNext()) {
            file = (IFile)iter.next();
            
            if(!file.exists()) {
              iter.remove();
            }
          }

          m_fileList.add(FileBuffers.getWorkspaceFileAtLocation(path));
        }
        
        public void underlyingFileDeleted(IFileBuffer buffer) {
          IFile file = fileForBuffer(buffer);
          
          if(file != null) {
            m_fileList.remove(file);
          }
        }
        
        public void bufferContentAboutToBeReplaced(IFileBuffer buffer) {/**/}
        public void bufferContentReplaced(IFileBuffer buffer) {/**/}
        public void stateChanging(IFileBuffer buffer) {/**/}
        public void dirtyStateChanged(IFileBuffer buffer, boolean isDirty) {/**/}
        public void stateValidationChanged(IFileBuffer buffer, boolean isStateValidated) {/**/}
        public void stateChangeFailed(IFileBuffer buffer) {/**/}
      });
  }
  
  private IFile fileForBuffer(IFileBuffer buffer) {
    return FileBuffers.getWorkspaceFileAtLocation(buffer.getLocation());
  }
    
  private boolean shouldInspect(IFile file) {
    IPath  path      = file.getLocation();
    String extension = path.getFileExtension();
    
    return extension.equals("java") &&
      TcPlugin.getDefault().hasTerracottaNature(file.getProject());
  }
  
  public void setup(IDocument document) {/**/}
  
  static class Inspector implements Runnable {
    private IFile m_file;
    
    Inspector(IFile file) {
      m_file = file;
    }
    
    public void run() {
      if(m_file.exists()) {
        ICompilationUnit cu = JavaCore.createCompilationUnitFrom(m_file);
        
        if(cu != null) {
          TcPlugin.getDefault().inspect(cu);
        }
      }
    }
  }
  
  protected void inspectAllBuffers() {
    IFile    file;
    Iterator iter = m_fileList.iterator();
    
    while(iter.hasNext()) {
      file = (IFile)iter.next();
      
      if(file.exists()) {
        (new Inspector(file)).run();
      }
      else {
        iter.remove();
      }
    }
  }
  
  /**
   * Inspect each file associated with a Java buffer that is open.
   * This is called by ConfigurationEditor after the user had manually modified
   * the config (XML) document.  This causes the graphical adornments to be synchronized
   * with the configuration.
   */
  public static void inspectAll() {
    if(m_setupParticipant != null) {
      m_setupParticipant.inspectAllBuffers();
    }
  }
}
