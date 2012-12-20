/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.web.shiro;

import org.apache.shiro.web.env.IniWebEnvironment;

import com.terracotta.management.web.utils.TSAConfig;

/**
 * @author Ludovic Orban
 */
public class TSAIniWebEnvironment extends IniWebEnvironment {

  private static final String NOIA_SECURE_INI_RESOURCE_PATH = "classpath:shiro-ssl-noIA.ini";
  private final static String SECURE_INI_RESOURCE_PATH = "classpath:shiro-ssl.ini";
  private final static String UNSECURE_INI_RESOURCE_PATH = "classpath:shiro.ini";

  @Override
  protected String[] getDefaultConfigLocations() {
    if (Boolean.getBoolean("com.terracotta.management.debug.noIA") && TSAConfig.isSslEnabled()) {
      return new String[] { NOIA_SECURE_INI_RESOURCE_PATH };
    } else if (TSAConfig.isSslEnabled()) {
      return new String[] { SECURE_INI_RESOURCE_PATH };
    } else {
      return new String[] { UNSECURE_INI_RESOURCE_PATH, };
    }
  }

}
