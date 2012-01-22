/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.terracottatech.config.Ha;

public interface HaConfigSchema extends Config {
  boolean isNetworkedActivePassive();

  boolean isDiskBasedActivePassive();

  Ha getHa();
}
