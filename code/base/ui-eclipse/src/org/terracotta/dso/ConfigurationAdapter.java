/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.resources.IProject;

public class ConfigurationAdapter implements IConfigurationListener {
  public void configurationChanged(IProject project) {/**/}

  public void serverChanged(IProject project, int index) {/**/}
  public void serversChanged(IProject project) {/**/}
    
  public void rootChanged(IProject project, int index) {/**/}
  public void rootsChanged(IProject project) {/**/}

  public void distributedMethodsChanged(IProject project) {/**/}
  public void distributedMethodChanged(IProject project, int index) {/**/}

  public void bootClassesChanged(IProject project) {/**/}
  public void bootClassChanged(IProject project, int index) {/**/}

  public void transientFieldsChanged(IProject project) {/**/}
  public void transientFieldChanged(IProject project, int index) {/**/}

  public void namedLockChanged(IProject project, int index) {/**/}
  public void namedLocksChanged(IProject project) {/**/}

  public void autolockChanged(IProject project, int index) {/**/}
  public void autolocksChanged(IProject project) {/**/}

  public void includeRuleChanged(IProject project, int index) {/**/}
  public void includeRulesChanged(IProject project) {/**/}
  public void excludeRuleChanged(IProject project, int index) {/**/}
  public void excludeRulesChanged(IProject project) {/**/}
  public void instrumentationRulesChanged(IProject project) {/**/}

  public void clientChanged(IProject project) {/**/}
  public void moduleReposChanged(IProject project) {/**/}
  public void moduleRepoChanged(IProject project, int index) {/**/}
  public void moduleChanged(IProject project, int index) {/**/}
  public void modulesChanged(IProject project) {/**/}
}
