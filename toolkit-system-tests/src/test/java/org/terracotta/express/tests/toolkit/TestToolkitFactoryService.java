/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.toolkit;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitInstantiationException;
import org.terracotta.toolkit.api.ToolkitFactoryService;

import java.util.Properties;

public class TestToolkitFactoryService implements ToolkitFactoryService {

  private final static String     TERRACOTTA_TOOLKIT_TYPE     = "terracotta";

  @Override
  public boolean canHandleToolkitType(String type, String subName) {
    return TERRACOTTA_TOOLKIT_TYPE.equals(type);
  }

  @Override
  public Toolkit createToolkit(String type, String subName, Properties properties) throws ToolkitInstantiationException {
   
    throw new ToolkitInstantiationException("There were some problem in creating toolkit");
  }
}
