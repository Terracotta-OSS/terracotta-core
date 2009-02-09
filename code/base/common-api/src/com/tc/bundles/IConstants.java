/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bundles;

import java.util.regex.Pattern;

interface IConstants {

  // Cache pattern defining a valid Maven version: x[.y[.z]][-classifier][-i]
  // which is done with a regex like digits(.digits(.digits))-any
  // reminder: \\ is Java string escaping,
  // (?:) is a non-capture group,
  // ()? is an optional group
  // \d is a digit
  static final Pattern MAVEN_VERSION_PATTERN     = Pattern.compile("^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?(?:-(.*))?$");

  // Cache pattern defining a valid OSGi version: x[.y[.z[.qualifier]]]
  // which is done with a regex like digits(.digits(.digits))-any
  // reminder: \\ is Java string escaping,
  // (?:) is a non-capture group,
  // ()? is an optional group
  // \d is a digit
  static final Pattern OSGI_VERSION_PATTERN      = Pattern.compile("^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(\\.(.*))?)?)?$");

  // Cache pattern defining characters that aren't valid in OSGi symbolic name or version qualifier for speed
  static final Pattern INVALID_OSGI_CHAR_PATTERN = Pattern.compile("[^a-zA-Z0-9._\\-]");

}
