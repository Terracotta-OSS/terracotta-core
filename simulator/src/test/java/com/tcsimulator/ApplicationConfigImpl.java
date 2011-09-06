/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcsimulator;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.net.proxy.TCPProxy;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfig;

import java.util.HashMap;
import java.util.Map;

public class ApplicationConfigImpl implements ApplicationConfig {

  private final String applicatonClassname;
  private final int    intensity;
  private final int    globalParticipantCount;
  private final Map    attributes;
  private int          hashCode;
  private final int    validatorCount;

  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    String classname = ApplicationConfigImpl.class.getName();
    config.addIncludePattern(classname);
    config.addWriteAutolock("* " + classname + ".*(..)");
  }

  public ApplicationConfigImpl(String applicatonClassname, int intensity, int globalParticipantCount) {
    this(applicatonClassname, intensity, globalParticipantCount, 0);
  }

  public ApplicationConfigImpl(String applicatonClassname, int intensity, int globalParticipantCount, int validatorCount) {
    this.applicatonClassname = applicatonClassname;
    this.intensity = intensity;
    this.globalParticipantCount = globalParticipantCount;
    this.validatorCount = validatorCount;
    attributes = new HashMap();
    hashCode = new HashCodeBuilder(17, 37).append(this.applicatonClassname).append(intensity)
        .append(globalParticipantCount).append(attributes).toHashCode();
  }

  public String getApplicationClassname() {
    return this.applicatonClassname;
  }

  public int getIntensity() {
    return intensity;
  }

  public int getGlobalParticipantCount() {
    return this.globalParticipantCount;
  }

  public int getValidatorCount() {
    return validatorCount;
  }

  public boolean equals(ApplicationConfig appconfig) {
    return applicatonClassname.equals(appconfig.getApplicationClassname()) && intensity == appconfig.getIntensity()
           && globalParticipantCount == appconfig.getGlobalParticipantCount();
  }

  public int hashCode() {
    return hashCode;
  }

  public void setAttribute(String key, String value) {
    attributes.put(key, value);
  }

  public String getAttribute(String key) {
    return (String) attributes.get(key);
  }

  public ApplicationConfig copy() {
    return new ApplicationConfigImpl(applicatonClassname, intensity, globalParticipantCount, validatorCount);
  }

  public ServerControl getServerControl() {
    throw new UnsupportedOperationException("not implemented, should not be used");
  }

  public int getGlobalValidatorCount() {
    throw new AssertionError("This method needs to be implemented");
  }

  public TCPProxy[] getProxies() {
    throw new UnsupportedOperationException("not implemented, should not be used");
  }

  public ServerControl[] getServerControls() {
    throw new UnsupportedOperationException("not implemented, should not be used");
  }

  public Object getAttributeObject(String key) {
    throw new UnsupportedOperationException("not implemented, should not be used");
  }
}
