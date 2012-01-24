/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.BaseConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.exception.ImplementMe;

public class DSOApplicationConfigObject extends BaseConfigObject implements DSOApplicationConfig {

  public DSOApplicationConfigObject(ConfigContext context) {
    super(context);
  }

  @Override
  public InstrumentedClass[] instrumentedClasses() {
    throw new ImplementMe();
  }

  @Override
  public Lock[] locks() {
    throw new ImplementMe();
  }

  @Override
  public Root[] roots() {
    throw new ImplementMe();
  }

  @Override
  public boolean supportSharingThroughReflection() {
    throw new ImplementMe();
  }
}
