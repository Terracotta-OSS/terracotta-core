/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
    ACTIVE_CONFIGURATION_FILE =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "ActiveDomainConfigFile");

  public static final QualifiedName
    CONFIGURATION =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "DomainConfig");

  public static final QualifiedName
    BOOT_CLASS_HELPER =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "BootClassHelper");
  
  public static final QualifiedName
    MODULES_CONFIGURATION =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "ModulesConfig");
  
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
    AUTO_START_SERVER_OPTION =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "AutoStartServerOption");
  
  public static final QualifiedName
    CONFIG_PROBLEM_CONTINUE =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "ConfigProblemContinueOption");

  public static final QualifiedName
    WARN_CONFIG_PROBLEMS_OPTION =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "WarnConfigProblemsOption");
  
  public static final QualifiedName
    QUERY_RESTART_OPTION =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "QueryRestartOption");
  
  public static final QualifiedName
    BOOT_JAR_PRODUCT_VERSION =
      new QualifiedName(TERRACOTTA_QUALIFIER,
                        "BootJarProductVersion");
}
