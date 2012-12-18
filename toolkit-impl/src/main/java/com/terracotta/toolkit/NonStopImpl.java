/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.nonstop.NonStop;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;

public class NonStopImpl implements NonStop {
  private final NonStopToolkitImpl nonStopToolkitImpl;

  public NonStopImpl(NonStopToolkitImpl nonStopToolkitImpl) {
    this.nonStopToolkitImpl = nonStopToolkitImpl;
  }

  @Override
  public void start(NonStopConfiguration nonStopConfig) {

    if (nonStopConfig.getWriteOpNonStopTimeoutBehavior() != NonStopConfigurationFields.NonStopWriteTimeoutBehavior.EXCEPTION) { throw new UnsupportedOperationException(
                                                                                                                                                                        "unsupported write behavior: "
                                                                                                                                                                            + nonStopConfig
                                                                                                                                                                                .getWriteOpNonStopTimeoutBehavior()); }
    if (nonStopConfig.getReadOpNonStopTimeoutBehavior() != NonStopConfigurationFields.NonStopReadTimeoutBehavior.EXCEPTION) { throw new UnsupportedOperationException(
                                                                                                                                                                      "unsupported read behavior: "
                                                                                                                                                                          + nonStopConfig
                                                                                                                                                                              .getReadOpNonStopTimeoutBehavior()); }
    nonStopToolkitImpl.start(nonStopConfig);
  }

  @Override
  public void finish() {
    nonStopToolkitImpl.stop();
  }

  @Override
  public NonStopConfigurationRegistry getNonStopConfigurationRegistry() {
    return nonStopToolkitImpl.getNonStopConfigurationToolkitRegistry();
  }

}
