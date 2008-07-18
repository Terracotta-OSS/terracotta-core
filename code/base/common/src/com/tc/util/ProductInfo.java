/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to retrieve the build information for the product.
 */
public final class ProductInfo {
  private static final ResourceBundleHelper bundleHelper                 = new ResourceBundleHelper(ProductInfo.class);

  private static final String               DATE_FORMAT                  = "yyyyMMdd-HHmmss";
  private static final Pattern              KITIDPATTERN                 = Pattern.compile("(\\d+\\.\\d+).*");
  private static final String               BUILD_DATA_RESOURCE_NAME     = "/build-data.txt";
  private static final String               PATCH_DATA_RESOURCE_NAME     = "/patch-data.txt";

  private static final String               BUILD_DATA_ROOT_KEY          = "terracotta.build.";
  private static final String               BUILD_DATA_VERSION_KEY       = "version";
  private static final String               BUILD_DATA_MAVEN_VERSION_KEY = "maven.artifacts.version";
  private static final String               BUILD_DATA_EDITION_KEY       = "edition";
  private static final String               BUILD_DATA_TIMESTAMP_KEY     = "timestamp";
  private static final String               BUILD_DATA_HOST_KEY          = "host";
  private static final String               BUILD_DATA_USER_KEY          = "user";
  private static final String               BUILD_DATA_REVISION_KEY      = "revision";
  private static final String               BUILD_DATA_EE_REVISION_KEY   = "ee.revision";
  private static final String               BUILD_DATA_BRANCH_KEY        = "branch";
  private static final String               PATCH_DATA_ROOT_KEY          = "terracotta.patch.";
  private static final String               PATCH_DATA_LEVEL_KEY         = "level";
  public static final String                UNKNOWN_VALUE                = "[unknown]";
  public static final String                DEFAULT_LICENSE              = "Unlimited development";

  private static ProductInfo                INSTANCE                     = null;

  private final String                      moniker;
  private final String                      maven_version;
  private final Date                        timestamp;
  private final String                      host;
  private final String                      user;
  private final String                      branch;
  private final String                      edition;
  private final String                      revision;
  private final String                      ee_revision;
  private final String                      kitID;

  private final String                      patchLevel;
  private final String                      patchHost;
  private final String                      patchUser;
  private final Date                        patchTimestamp;
  private final String                      patchRevision;
  private final String                      patchBranch;

  private String                            version;
  private String                            buildID;
  private String                            copyright;
  private String                            license                      = DEFAULT_LICENSE;

  // XXX: Can't have a logger in this class...
  // private static final TCLogger logger = TCLogging.getLogger(ProductInfo.class);
  // private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  /**
   * Construct a ProductInfo by reading properties from streams (most commonly by loading properties files as resources
   * from the classpath). If an IOException occurs while loading the build or patch streams, the System will exit. These
   * resources are always expected to be in the path and are necessary to do version compatibility checks.
   * 
   * @param buildData Build properties in stream conforming to Java Properties file format, must not be null
   * @param patchData Patch properties in stream conforming to Java Properties file format, null if none
   * @throws NullPointerException If buildData is null and assertions are enabled
   * @throws IOException If there is an error reading the build or patch data streams
   * @throws ParseException If there is an error reading the timestamp format in build or patch data streams
   */
  ProductInfo(InputStream buildData, InputStream patchData) throws IOException, java.text.ParseException {
    Assert.assertNotNull("buildData", buildData);

    Properties properties = new Properties();
    moniker = bundleHelper.getString("moniker");
    properties.load(buildData);
    if (patchData != null) properties.load(patchData);

    // Get all release build properties
    this.version = getBuildProperty(properties, BUILD_DATA_VERSION_KEY, UNKNOWN_VALUE);
    this.maven_version = getBuildProperty(properties, BUILD_DATA_MAVEN_VERSION_KEY, UNKNOWN_VALUE);
    this.edition = getBuildProperty(properties, BUILD_DATA_EDITION_KEY, "opensource");

    this.timestamp = parseTimestamp(getBuildProperty(properties, BUILD_DATA_TIMESTAMP_KEY, null));
    this.host = getBuildProperty(properties, BUILD_DATA_HOST_KEY, UNKNOWN_VALUE);
    this.user = getBuildProperty(properties, BUILD_DATA_USER_KEY, UNKNOWN_VALUE);
    this.branch = getBuildProperty(properties, BUILD_DATA_BRANCH_KEY, UNKNOWN_VALUE);
    this.revision = getBuildProperty(properties, BUILD_DATA_REVISION_KEY, UNKNOWN_VALUE);
    this.ee_revision = getBuildProperty(properties, BUILD_DATA_EE_REVISION_KEY, UNKNOWN_VALUE);

    // Get all patch build properties
    this.patchLevel = getPatchProperty(properties, PATCH_DATA_LEVEL_KEY, UNKNOWN_VALUE);
    this.patchHost = getPatchProperty(properties, BUILD_DATA_HOST_KEY, UNKNOWN_VALUE);
    this.patchUser = getPatchProperty(properties, BUILD_DATA_USER_KEY, UNKNOWN_VALUE);
    this.patchTimestamp = parseTimestamp(getPatchProperty(properties, BUILD_DATA_TIMESTAMP_KEY, null));
    this.patchRevision = getPatchProperty(properties, BUILD_DATA_REVISION_KEY, UNKNOWN_VALUE);
    this.patchBranch = getPatchProperty(properties, BUILD_DATA_BRANCH_KEY, UNKNOWN_VALUE);

    Matcher matcher = KITIDPATTERN.matcher(maven_version);
    kitID = matcher.matches() ? matcher.group(1) : UNKNOWN_VALUE;
  }

  static Date parseTimestamp(String timestampString) throws java.text.ParseException {
    return (timestampString == null) ? null : new SimpleDateFormat(DATE_FORMAT).parse(timestampString);
  }

  public static synchronized ProductInfo getInstance() {
    if (INSTANCE == null) {
      try {
        InputStream buildData = getData(BUILD_DATA_RESOURCE_NAME);
        InputStream patchData = getData(PATCH_DATA_RESOURCE_NAME);
        INSTANCE = new ProductInfo(buildData, patchData);
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e.getMessage());
      }
    }
    return INSTANCE;
  }

  static InputStream getData(String name) {
    return ProductInfo.class.getResourceAsStream(name);
  }

  static InputStream getBuildData() {
    return getData(BUILD_DATA_RESOURCE_NAME);
  }

  static InputStream getPatchData() {
    return getData(PATCH_DATA_RESOURCE_NAME);
  }

  private String getBuildProperty(Properties properties, String name, String defaultValue) {
    return getProperty(properties, BUILD_DATA_ROOT_KEY, name, defaultValue);
  }

  private String getPatchProperty(Properties properties, String name, String defaultValue) {
    return getProperty(properties, PATCH_DATA_ROOT_KEY, name, defaultValue);
  }

  private String getProperty(Properties properties, String root, String name, String defaultValue) {
    String out = properties.getProperty(root + name);
    if (StringUtils.isBlank(out)) out = defaultValue;
    return out;
  }

  public static void printRawData() {
    try {
      InputStream buildData = getBuildData();
      if (buildData != null) IOUtils.copy(buildData, System.out);

      InputStream patchData = getPatchData();
      if (patchData != null) IOUtils.copy(patchData, System.out);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public boolean isDevMode() {
    return this.version.endsWith(UNKNOWN_VALUE);
  }

  public String moniker() {
    return moniker;
  }

  public String edition() {
    return edition;
  }

  public String version() {
    return version;
  }

  public String mavenArtifactsVersion() {
    return maven_version;
  }

  public String kitID() {
    return kitID;
  }

  public Date buildTimestamp() {
    return (timestamp == null) ? null : (Date) timestamp.clone();
  }

  public String buildTimestampAsString() {
    return (timestamp == null) ? UNKNOWN_VALUE : new SimpleDateFormat(DATE_FORMAT).format(timestamp);
  }

  public String buildHost() {
    return host;
  }

  public String buildUser() {
    return user;
  }

  public String buildBranch() {
    return branch;
  }

  public String copyright() {
    if (copyright == null) {
      copyright = bundleHelper.getString("copyright");
    }
    return copyright;
  }

  public String license() {
    return license;
  }

  public String buildRevision() {
    return revision;
  }

  public String buildRevisionFromEE() {
    return ee_revision;
  }

  public boolean isPatched() {
    return !UNKNOWN_VALUE.equals(patchLevel);
  }

  public String patchLevel() {
    return patchLevel;
  }

  public String patchHost() {
    return patchHost;
  }

  public String patchUser() {
    return patchUser;
  }

  public Date patchTimestamp() {
    return (patchTimestamp == null) ? null : (Date) patchTimestamp.clone();
  }

  public String patchTimestampAsString() {
    return (patchTimestamp == null) ? UNKNOWN_VALUE : new SimpleDateFormat(DATE_FORMAT).format(patchTimestamp);
  }

  public String patchRevision() {
    return patchRevision;
  }

  public String patchBranch() {
    return patchBranch;
  }

  public String toShortString() {
    return moniker + " " + ("opensource".equals(edition) ? "" : (edition + " ")) + version;
  }

  public String toLongString() {
    return toShortString() + ", as of " + buildID();
  }

  public String buildID() {
    if (buildID == null) {
      String rev = revision;
      if (edition.indexOf("Enterprise") >= 0) {
        rev = ee_revision + "-" + revision;
      }
      buildID = buildTimestampAsString() + " (Revision " + rev + " by " + user + "@" + host + " from " + branch + ")";
    }
    return buildID;
  }

  public String toLongPatchString() {
    return toShortPatchString() + ", as of " + patchBuildID();
  }

  public String toShortPatchString() {
    return "Patch Level " + patchLevel;
  }

  public String patchBuildID() {
    return patchTimestampAsString() + " (Revision " + patchRevision + " by " + patchUser + "@" + patchHost + " from "
           + patchBranch + ")";
  }

  public String toString() {
    return toShortString();
  }

  private static void printHelp(Options options) {
    new HelpFormatter().printHelp("java " + ProductInfo.class.getName(), options);
  }

  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("v", "verbose", false, bundleHelper.getString("option.verbose"));
    options.addOption("r", "raw", false, bundleHelper.getString("option.raw"));
    options.addOption("h", "help", false, bundleHelper.getString("option.help"));

    CommandLineParser parser = new GnuParser();
    try {
      CommandLine cli = parser.parse(options, args);

      if (cli.hasOption("h")) {
        printHelp(options);
        System.exit(0);
      }

      if (cli.hasOption("v")) {
        System.out.println(getInstance().toLongString());
        if (getInstance().isPatched()) System.out.println(getInstance().toLongPatchString());
        System.exit(0);
      }

      if (cli.hasOption("r")) {
        printRawData();
        System.exit(0);
      }

      System.out.println(getInstance().toShortString());
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      System.out.println();
      printHelp(options);
      System.exit(1);
    }
  }
}
