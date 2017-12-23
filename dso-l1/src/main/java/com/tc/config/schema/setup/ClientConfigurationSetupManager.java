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
import java.net.InetSocketAddress;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * @author vmad
 */
public class ClientConfigurationSetupManager implements L1ConfigurationSetupManager {
  private final String[] args;
  private L2ConfigForL1.L2Data[] l2Data;
  // For historical reasons, we need to serialize the list of member URIs.
  private final String legacyStripeConfigText;
  
  public ClientConfigurationSetupManager(List<InetSocketAddress> stripeMemberUris, String[] args) {
    this.args = args;
    l2Data = new L2ConfigForL1.L2Data[stripeMemberUris.size()];
    for(int i = 0; i < l2Data.length; i++) {
      l2Data[i] = new L2ConfigForL1.L2Data(stripeMemberUris.get(i), false);
    }
    
    // Build the legacyStripeConfigText.
    String stripeText = null;
    for (InetSocketAddress member : stripeMemberUris) {
      if (null == stripeText) {
        stripeText = member.toString();
      } else {
        stripeText = stripeText + "," + member;
      }
    }
    this.legacyStripeConfigText = stripeText;
  }

  public void addServer(String host, int port) {
    l2Data = Arrays.copyOf(l2Data, l2Data.length + 1);
    l2Data[l2Data.length - 1] = new L2ConfigForL1.L2Data(InetSocketAddress.createUnresolved(host, port), false);
  }

  @Override
  public String[] processArguments() {
    return args;
  }

  @Override
  public boolean loadedFromTrustedSource() {
    return false;
  }

  @Override
  public String rawConfigText() {
    return this.legacyStripeConfigText;
  }

  @Override
  public String source() {
    return this.legacyStripeConfigText;
  }

  @Override
  public CommonL1Config commonL1Config() {
    return null;
  }

  @Override
  public L2ConfigForL1 l2Config() {
    return new L2ConfigForL1() {
      @Override
      public L2Data[] l2Data() {
        return l2Data;
      }
    };
  }

  @Override
  public Map<String, String> getOverrideTCProperties() {
    return Collections.<String, String>emptyMap();
  }
  
  
}
