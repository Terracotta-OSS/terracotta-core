/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.terracotta.modules.tool.ModuleReport;
import org.terracotta.modules.tool.Modules;
import org.terracotta.modules.tool.config.ConfigAnnotation;

import com.google.inject.Inject;
import com.google.inject.name.Named;

abstract class ModuleOperatorCommand extends AbstractCommand {

  @Inject
  @Named(ConfigAnnotation.MODULES_INSTANCE)
  protected Modules      modules;

  @Inject
  @Named(ConfigAnnotation.MODULEREPORT_INSTANCE)
  protected ModuleReport report;

}
