/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.core.resources.IProject;

public class ConfigurationEventManager extends EventManager {
  void addConfigurationListener(IConfigurationListener listener) {
    addListenerObject(listener);
  }

  void removeConfigurationListener(IConfigurationListener listener) {
    removeListenerObject(listener);
  }

  void fireConfigurationChange(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).configurationChanged(project);
      }
    }
  }

  void fireRootChanged(IProject project, int index) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).rootChanged(project, index);
      }
    }
  }

  void fireRootsChanged(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).rootsChanged(project);
      }
    }
  }
  
  void fireServerChanged(IProject project, int index) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).serverChanged(project, index);
      }
    }
  }

  void fireServersChanged(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).serversChanged(project);
      }
    }
  }
  
  public void fireDistributedMethodsChanged(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).distributedMethodsChanged(project);
      }
    }
  }
  
  public void fireDistributedMethodChanged(IProject project, int index) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).distributedMethodChanged(project, index);
      }
    }
  }

  public void fireBootClassesChanged(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).bootClassesChanged(project);
      }
    }
  }
  
  public void fireBootClassChanged(IProject project, int index) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).bootClassChanged(project, index);
      }
    }
  }

  public void fireTransientFieldsChanged(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).transientFieldsChanged(project);
      }
    }
  }

  public void fireTransientFieldChanged(IProject project, int index) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).transientFieldChanged(project, index);
      }
    }
  }

  public void fireNamedLockChanged(IProject project, int index) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).namedLockChanged(project, index);
      }
    }
  }
  
  public void fireNamedLocksChanged(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).namedLocksChanged(project);
      }
    }
  }

  public void fireAutolockChanged(IProject project, int index) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).autolockChanged(project, index);
      }
    }
  }
  
  public void fireAutolocksChanged(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).autolocksChanged(project);
      }
    }
  }

  public void fireIncludeRuleChanged(IProject project, int index) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).includeRuleChanged(project, index);
      }
    }
  }
  
  public void fireIncludeRulesChanged(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).includeRulesChanged(project);
      }
    }
  }
  
  public void fireExcludeRuleChanged(IProject project, int index) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).excludeRuleChanged(project, index);
      }
    }
  }
  
  public void fireExcludeRulesChanged(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).excludeRulesChanged(project);
      }
    }
  }

  public void fireInstrumentationRulesChanged(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).instrumentationRulesChanged(project);
      }
    }
  }

  public void fireClientChanged(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).clientChanged(project);
      }
    }
  }

  public void fireModuleReposChanged(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).moduleReposChanged(project);
      }
    }
  }

  public void fireModuleRepoChanged(IProject project, int index) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).moduleRepoChanged(project, index);
      }
    }
  }

  public void fireModuleChanged(IProject project, int index) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).moduleChanged(project, index);
      }
    }
  }

  public void fireModulesChanged(IProject project) {
    Object[] listeners = getListeners();
    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        ((IConfigurationListener) listeners[i]).modulesChanged(project);
      }
    }
  }
}
