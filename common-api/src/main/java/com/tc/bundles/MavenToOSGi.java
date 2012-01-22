/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

import com.tc.util.Assert;

import java.util.regex.Matcher;

/**
 * This class provides static helper methods to convert Maven identifiers to valid OSGi bundle identifiers. For example,
 * converting Maven artifactId to OSGi bundle symbolic names.
 */
public class MavenToOSGi implements IConstants {
  private MavenToOSGi() {
    //
  }

  private static String replaceInvalidChars(String value) {
    return INVALID_OSGI_CHAR_PATTERN.matcher(value).replaceAll("_");
  }

  /**
   * Valid OSGi symbolic name format from OSGi v4 spec:
   * 
   * <pre>
   *   token ::= ( alphanum | '_' | '-' )+
   *   symbolic-name :: = token('.'token)*
   * </pre>
   * 
   * However, I've seen errors from Knoplerfish using - in symbolic names, so we are currently replacing anything other
   * than alphanumeric, dash, or underscore with an underscore. If groupId and artifact are both null or empty string,
   * then you'll see some
   * 
   * @param groupId Maven groupId, like "org.terracotta.modules", might be null
   * @param artifactId Maven artifactId, like "tim-terracotta-cache", might be null
   * @return Valid OSGi symbolic name format based on the groupId and artifactId
   * @throws IllegalArgumentException If groupId AND artifactId are null or empty string
   */
  public static String artifactIdToSymbolicName(String groupId, String artifactId) {
    String name = groupId;
    if (name == null) {
      name = "";
    }

    if (artifactId != null && artifactId.length() > 0) {
      if (name.length() > 0) {
        name = name + ".";
      }
      name = name + artifactId;
    }

    if (name.length() == 0) { throw new IllegalArgumentException(
                                                                 "Maven groupId and artifactId are both null or empty, at least one must be defined."); }

    return replaceInvalidChars(name);
  }

  /**
   * Maven versions are defined as:
   * 
   * <pre>
   *   x[.y[.z]][-classifier][-i]
   * </pre>
   * 
   * see: http://docs.codehaus.org/display/MAVENUSER/Dependency+Mechanism The OSGi spec defines the OSGi bundle version
   * as:
   * 
   * <pre>
   *   version ::= major( '.' minor ( '.' micro ( '.' qualifier )? )? )? 
   *   major ::= number                    
   *   minor ::= number 
   *   micro ::= number 
   *   qualifier ::= ( alphanum | '_' | '-' )+
   * </pre>
   * 
   * The -classifer-i part (such as -ALPHA-1 or -SNAPSHOT) will be parsed upstream, specifically the leading -, which
   * should not be passed. So, a Maven version 1.0-SNAPSHOT should be passed as {1, 0, 0, SNAPSHOT} to this method. This
   * method will rebuild it and return it as "1.0.0.SNAPSHOT".
   * 
   * @param majorVersion Major version from Maven, must be >= 0 (0 if not specified)
   * @param minorVersion Minor version from Maven, must be >= 0 (0 if not specified)
   * @param incrementalVersion Incremental version from Maven, must be >= 0 (0 if not specified)
   * @param classifier Classifier string from Maven, this is expected to be parsed and not contain the leading dash, may
   *        be null
   */
  public static String projectVersionToBundleVersion(int majorVersion, int minorVersion, int incrementalVersion,
                                                     String classifier) {
    checkNonNegative(majorVersion, "major version");
    checkNonNegative(minorVersion, "minor version");
    checkNonNegative(minorVersion, "micro version");

    String projectVersion = majorVersion + "." + minorVersion + "." + incrementalVersion;

    if (classifier != null && classifier.length() > 0) {
      return projectVersion + "." + replaceInvalidChars(classifier);
    } else {
      return projectVersion;
    }
  }

  /**
   * Parse a Maven version string of the form "digits(.digits(.digits))-classifier". Maven implements this parsing in a
   * class called org.apache.maven.artifact.versioning.DefaultArtifactVersion, which I theoretically could have used
   * except I would have added a Maven dependency to common which I didn't want to do.
   * 
   * @param mavenVersion Maven version string, such as 1.2.3-RC-1
   * @return OSGi bundle version to use, such as 1.2.3.RC_1
   */
  public static String projectVersionToBundleVersion(String mavenVersion) {
    if (mavenVersion == null) {
      mavenVersion = "";
    }

    int major = 0;
    int minor = 0;
    int micro = 0;
    String classifier = null;

    Matcher matcher = MAVEN_VERSION_PATTERN.matcher(mavenVersion);
    if (matcher.matches()) {
      // Grab the capturing groups from the pattern, only first is required, others may be null, 0=whole match, so
      // ignored
      String majorStr = matcher.group(1);
      String minorStr = matcher.group(2);
      String microStr = matcher.group(3);
      classifier = matcher.group(4);

      major = Integer.parseInt(majorStr);
      if (minorStr != null) {
        minor = Integer.parseInt(minorStr);
      }
      if (microStr != null) {
        micro = Integer.parseInt(microStr);
      }

    } else if (OSGI_VERSION_PATTERN.matcher(mavenVersion).matches()) {
      // if doesn't match pattern, check if this is already a valid osgi format
      return mavenVersion;

    } else {
      // if doesn't match pattern, use whole thing as classifier
      // some Maven examples show something like "RELEASE" as a version == 0.0.0-RELEASE
      classifier = mavenVersion;
    }

    return projectVersionToBundleVersion(major, minor, micro, classifier);
  }

  private static void checkNonNegative(int value, String valueDescription) {
    if (value < 0) {
      Assert.fail("Invalid " + valueDescription + ": " + value + ", must be >= 0");
    }
  }
}
