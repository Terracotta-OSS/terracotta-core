/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.resources.IProject;

public class MultiChangeSignaller {
  public boolean rootsChanged              = false;
  public boolean namedLocksChanged         = false;
  public boolean autolocksChanged          = false;
  public boolean bootClassesChanged        = false;
  public boolean transientFieldsChanged    = false;
  public boolean distributedMethodsChanged = false;
  public boolean includeRulesChanged       = false;
  public boolean excludeRulesChanged       = false;

  public void signal(IProject project) {
    TcPlugin plugin = TcPlugin.getDefault();

    if (rootsChanged) plugin.fireRootsChanged(project);
    if (namedLocksChanged) plugin.fireNamedLocksChanged(project);
    if (autolocksChanged) plugin.fireAutolocksChanged(project);
    if (bootClassesChanged) plugin.fireBootClassesChanged(project);
    if (transientFieldsChanged) plugin.fireTransientFieldsChanged(project);
    if (distributedMethodsChanged) plugin.fireDistributedMethodsChanged(project);
    if (includeRulesChanged) plugin.fireIncludeRulesChanged(project);
    if (excludeRulesChanged) plugin.fireExcludeRulesChanged(project);
    
    if (plugin.getConfigurationEditor(project) == null) {
      plugin.saveConfiguration(project);
    }
  }
}
