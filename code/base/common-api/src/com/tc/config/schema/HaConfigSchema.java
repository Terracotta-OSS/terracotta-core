/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.terracottatech.config.Ha;

public interface HaConfigSchema extends Config {
  boolean isNetworkedActivePassive();

  Ha getHa();
}
