/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.ConfigItemListener;
import com.tc.logging.TCLogging;

import java.io.File;

/**
 * Tells {@link TCLogging} to set its log file to the location specified. This must be attached to a {@link ConfigItem}
 * that returns {@link File} objects.
 */
public class LogSettingConfigItemListener implements ConfigItemListener {

  private final int processType;

  public LogSettingConfigItemListener(int processType) {
    this.processType = processType;
  }

  @Override
  public void valueChanged(Object oldValue, Object newValue) {
    if (newValue != null) {
      TCLogging.setLogDirectory((File) newValue, processType);
    }
  }

}
