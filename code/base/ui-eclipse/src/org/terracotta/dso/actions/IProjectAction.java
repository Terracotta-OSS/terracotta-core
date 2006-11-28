/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.resources.IProject;

public interface IProjectAction {
  void update(IProject project);
}
