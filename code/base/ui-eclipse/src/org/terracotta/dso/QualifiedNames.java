/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.runtime.QualifiedName;

/**
 * Defines names used to store session properties in an IResource, such as
 * the Eclipse project (IProject).
 * 
 * @see TcPlugin.getSessionProperty
 * @see TcPlugin.setSessionProperty
 */

public interface QualifiedNames {
  public static final String
   TERRACOTTA_QUALIFIER = "org.terracotta.dso";
  
  public static final QualifiedName
  CONFIGURATION_EDITOR =
    new QualifiedName(TERRACOTTA_QUALIFIER,
                      "DomainConfigEditor");

  public static final QualifiedName
    CONFIGURATION_FILE =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "DomainConfigFile");

  public static final QualifiedName
    CONFIGURATION_FILE_PATH =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "DomainConfigFilePath");

  public static final QualifiedName
    CONFIGURATION =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "DomainConfig");

  public static final QualifiedName
    CONFIGURATION_LINE_LENGTHS =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "DomainConfigLineLengths");

  public static final QualifiedName
    CONFIGURATION_HELPER =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "DomainConfigHelper");
  
  public static final QualifiedName
    IS_DIRTY =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "IsDirty");

  public static final QualifiedName
    SERVER_OPTIONS =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "ServerOptions");
  
  public static final QualifiedName
    CONFIG_PROBLEM_CONTINUE =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "ConfigProblemContinue");
}
