/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

import java.util.regex.Pattern;

public class ToolkitConstants {

  public static final String  API_VERSION_REGEX             = "(\\d+)\\.(\\d+)";

  public static final String  GROUP_ID                      = "org.terracotta.toolkit";

  public static final String  ARTIFACT_ID_PREFIX            = "terracotta-toolkit-";

  public static final String  TOOLKIT_SYMBOLIC_NAME_PREFIX  = GROUP_ID + "." + ARTIFACT_ID_PREFIX;

  public static final Pattern TOOLKIT_ARTIFACT_ID_PATTERN   = Pattern.compile("^" + ARTIFACT_ID_PREFIX
                                                                              + API_VERSION_REGEX + "(-ee)?$");
  public static final Pattern TOOLKIT_SYMBOLIC_NAME_PATTERN = Pattern.compile("^" + TOOLKIT_SYMBOLIC_NAME_PREFIX
                                                                              + API_VERSION_REGEX + "(-ee)?$");

}
