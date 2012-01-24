/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.terracottatech.config.UpdateCheck;

public interface UpdateCheckConfig extends Config {
  UpdateCheck getUpdateCheck();
}
