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
import com.tc.config.schema.repository.MutableBeanRepository;
import com.terracottatech.config.Ha;
import com.terracottatech.config.HaMode;
import com.terracottatech.config.NetworkedActivePassive;
import com.terracottatech.config.HaMode.Enum;

public class NewHaConfigObject extends BaseNewConfigObject implements NewHaConfig {
  private final String haMode;
  private final int    electionTime;
  private final Ha     ha;

  public NewHaConfigObject(ConfigContext context) {
    super(context);
    context.ensureRepositoryProvides(Ha.class);
    ha = (Ha) context.bean();
    haMode = ha.getMode().toString();
    checkHaModeSet(haMode);
    electionTime = context.intItem("networked-active-passive/election-time").getInt();
  }

  private void checkHaModeSet(String mode) {
    if (mode == null) { throw new AssertionError("no default set for ha mode"); }
  }

  public String haMode() {
    return haMode;
  }

  public int electionTime() {
    return electionTime;
  }

  public boolean isNetworkedActivePassive() {
    return haMode.equals(HaMode.NETWORKED_ACTIVE_PASSIVE.toString());
  }

  public Ha getHa() {
    return ha;
  }

  public static Ha getDefaultCommonHa(DefaultValueProvider defaultValueProvider,
                                      MutableBeanRepository serversBeanRepository) throws XmlException {
    final int defaultElectionTime = ((XmlInteger) defaultValueProvider.defaultFor(serversBeanRepository
        .rootBeanSchemaType(), "ha/networked-active-passive/election-time")).getBigIntegerValue().intValue();
    final String defaultHaModeString = ((XmlString) defaultValueProvider.defaultFor(serversBeanRepository
        .rootBeanSchemaType(), "ha/mode")).getStringValue();
    final Enum defaultHaMode;
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
}
