/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.L2ConfigForL1;

import java.util.Map;

/**
 * Knows how to set up configuration for L1.
 */
public interface L1ConfigurationSetupManager {
  String[] processArguments();

  boolean loadedFromTrustedSource();

  String rawConfigText();
  
  String source();

  CommonL1Config commonL1Config();

  L2ConfigForL1 l2Config();

  Map<String, String> getOverrideTCProperties();
}
