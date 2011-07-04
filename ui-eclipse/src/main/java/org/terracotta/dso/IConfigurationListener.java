/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.resources.IProject;

public interface IConfigurationListener {
  void configurationChanged(IProject project);

  void serverChanged(IProject project, int index);
  void serversChanged(IProject project);
  
  void rootChanged(IProject project, int index);
  void rootsChanged(IProject project);

  void distributedMethodsChanged(IProject project);
  void distributedMethodChanged(IProject project, int index);

  void bootClassesChanged(IProject project);
  void bootClassChanged(IProject project, int index);

  void transientFieldsChanged(IProject project);
  void transientFieldChanged(IProject project, int index);

  void namedLockChanged(IProject project, int index);
  void namedLocksChanged(IProject project);

  void autolockChanged(IProject project, int index);
  void autolocksChanged(IProject project);

  void includeRuleChanged(IProject project, int index);
  void includeRulesChanged(IProject project);
  void excludeRuleChanged(IProject project, int index);
  void excludeRulesChanged(IProject project);
  void instrumentationRulesChanged(IProject project);
  
  void clientChanged(IProject project);
  void moduleReposChanged(IProject project);
  void moduleRepoChanged(IProject project, int index);
  void moduleChanged(IProject project, int index);
  void modulesChanged(IProject project);
}
