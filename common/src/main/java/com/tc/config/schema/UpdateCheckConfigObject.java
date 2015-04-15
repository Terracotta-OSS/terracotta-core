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
package com.tc.config.schema;

import org.apache.xmlbeans.XmlBoolean;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.terracottatech.config.Servers;
import com.terracottatech.config.UpdateCheck;

public class UpdateCheckConfigObject extends BaseConfigObject implements UpdateCheckConfig {

  public UpdateCheckConfigObject(ConfigContext context) {
    super(context);
    context.ensureRepositoryProvides(UpdateCheck.class);
  }

  @Override
  public UpdateCheck getUpdateCheck() {
    return (UpdateCheck) this.context.bean();
  }

  public static void initializeUpdateCheck(Servers servers, DefaultValueProvider defaultValueProvider)
      throws ConfigurationSetupException {
    try {
      if (!servers.isSetUpdateCheck()) {
        servers.setUpdateCheck(getDefaultUpdateCheck(servers, defaultValueProvider));
      } else {
        UpdateCheck updateCheck = servers.getUpdateCheck();
        if (!updateCheck.isSetEnabled()) {
          updateCheck.setEnabled(getDefaultUpdateCheckEnabled(servers, defaultValueProvider));
        }

        if (!updateCheck.isSetPeriodDays()) {
          updateCheck.setPeriodDays(getDefaultPeriodDays(servers, defaultValueProvider));
        }
      }
    } catch (XmlException e) {
      throw new ConfigurationSetupException(e);
    }
  }

  private static UpdateCheck getDefaultUpdateCheck(Servers servers, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    final int defaultPeriodDays = getDefaultPeriodDays(servers, defaultValueProvider);
    final boolean defaultEnabled = getDefaultUpdateCheckEnabled(servers, defaultValueProvider);
    UpdateCheck uc = UpdateCheck.Factory.newInstance();
    uc.setEnabled(defaultEnabled);
    uc.setPeriodDays(defaultPeriodDays);
    return uc;
  }

  private static boolean getDefaultUpdateCheckEnabled(Servers servers, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(servers.schemaType(), "update-check/enabled"))
        .getBooleanValue();
  }

  private static int getDefaultPeriodDays(Servers servers, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlInteger) defaultValueProvider.defaultFor(servers.schemaType(), "update-check/period-days"))
        .getBigIntegerValue().intValue();
  }

}
