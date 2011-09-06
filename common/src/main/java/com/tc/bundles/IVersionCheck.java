/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

public interface IVersionCheck {

  static String OFF                             = "off";
  static String WARN                            = "warn";
  static String ENFORCE                         = "enforce";
  static String STRICT                          = "strict";

  static int    IGNORED                         = -1;
  static int    OK                              = 0;
  static int    WARN_REQUIRE_ATTRIBUTE_MISSING  = 1;
  static int    ERROR_REQUIRE_ATTRIBUTE_MISSING = 2;
  static int    WARN_INCORRECT_VERSION          = 3;
  static int    ERROR_INCORRECT_VERSION         = 4;
  static int    ERROR_BAD_REQUIRE_ATTRIBUTE     = 5;

}
