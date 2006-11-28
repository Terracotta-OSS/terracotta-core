/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

/**
 * A TreeNode that represents an IFile.
 * 
 * @see ProjectNode
 * @see IResourceNode
 */

public class ProjectFileNode extends ProjectNode implements IResourceNode {
  private IFile m_file;
  
  public ProjectFileNode(IFile file) {
    super(file);
    m_file = file;
  }

  public String toString() {
    return m_file.getName();
  }

  public IResource getResource() {
    return getFile();
  }
  
  public IFile getFile() {
    return m_file;
  }
}
