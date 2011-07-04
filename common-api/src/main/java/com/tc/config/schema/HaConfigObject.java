/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlString;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.util.Assert;
import com.terracottatech.config.Ha;
import com.terracottatech.config.HaMode;
import com.terracottatech.config.NetworkedActivePassive;
import com.terracottatech.config.Servers;

public class HaConfigObject extends BaseConfigObject implements HaConfigSchema {
  private final Ha ha;

  public HaConfigObject(ConfigContext context) {
    super(context);
    context.ensureRepositoryProvides(Ha.class);
    ha = (Ha) context.bean();
  }

  public boolean isNetworkedActivePassive() {
    return this.ha.getMode().equals(HaMode.NETWORKED_ACTIVE_PASSIVE);
  }

  public Ha getHa() {
    return ha;
  }

  public static Ha getDefaultCommonHa(Servers servers, DefaultValueProvider defaultValueProvider) throws XmlException {
    final int defaultElectionTime = ((XmlInteger) defaultValueProvider
        .defaultFor(servers.schemaType(), "ha/networked-active-passive/election-time")).getBigIntegerValue().intValue();
    Assert.assertTrue(defaultElectionTime > 0);
    final String defaultHaModeString = ((XmlString) defaultValueProvider.defaultFor(servers.schemaType(), "ha/mode"))
        .getStringValue();
    final HaMode.Enum defaultHaMode;
    if (HaMode.DISK_BASED_ACTIVE_PASSIVE.toString().equals(defaultHaModeString)) {
      defaultHaMode = HaMode.DISK_BASED_ACTIVE_PASSIVE;
    } else {
      defaultHaMode = HaMode.NETWORKED_ACTIVE_PASSIVE;
    }

    Ha ha = Ha.Factory.newInstance();
    ha.setMode(defaultHaMode);
    NetworkedActivePassive nap = NetworkedActivePassive.Factory.newInstance();
    nap.setElectionTime(defaultElectionTime);
    ha.setNetworkedActivePassive(nap);
    return ha;
  }

  public static void initializeHa(Servers servers, DefaultValueProvider defaultValueProvider)
      throws ConfigurationSetupException, XmlException {
    Ha defaultHa = HaConfigObject.getDefaultCommonHa(servers, defaultValueProvider);
    if (servers.isSetHa()) {
      checkAndInitializeHa(servers.getHa(), defaultHa);
    } else {
      servers.setHa(defaultHa);
    }
  }

  public static void checkAndInitializeHa(Ha definedHa, Ha defaultHa) throws ConfigurationSetupException {
    if (!definedHa.isSetMode()) {
      definedHa.setMode(defaultHa.getMode());
    }

    if (definedHa.getMode().equals(HaMode.DISK_BASED_ACTIVE_PASSIVE) && definedHa.isSetNetworkedActivePassive()) {
      throw new ConfigurationSetupException(HaMode.NETWORKED_ACTIVE_PASSIVE
                                            + " can not be provided if ha mode is set to "
                                            + HaMode.DISK_BASED_ACTIVE_PASSIVE);
    } else if (!definedHa.isSetNetworkedActivePassive()) {
      definedHa.addNewNetworkedActivePassive().setElectionTime(defaultHa.getNetworkedActivePassive().getElectionTime());
    } else if (!definedHa.getNetworkedActivePassive().isSetElectionTime()) {
      definedHa.getNetworkedActivePassive().setElectionTime(defaultHa.getNetworkedActivePassive().getElectionTime());
    }

  }
}
