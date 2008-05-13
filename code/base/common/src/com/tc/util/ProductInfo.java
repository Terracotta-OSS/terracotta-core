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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
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
  private static final String               UNKNOWN_VALUE                = "[unknown]";

  private final String                      moniker;
  private final String                      version;
  private final String                      maven_version;
  private final Date                        timestamp;
  private final String                      host;
  private final String                      user;
  private final String                      branch;
  private final String                      edition;
  private final String                      revision;
  private final String                      ee_revision;
  private final String                      kitID;

  private ProductInfo(InputStream in, String fromWhere) {
    Properties properties = new Properties();

    moniker = bundleHelper.getString("moniker");

    if (in != null) {
      try {
        properties.load(in);
      } catch (IOException ioe) {
        ioe.printStackTrace();
        System.exit(1);
      }
    }

    this.version = getProperty(properties, BUILD_DATA_VERSION_KEY, UNKNOWN_VALUE);
    this.maven_version = getProperty(properties, BUILD_DATA_MAVEN_VERSION_KEY, UNKNOWN_VALUE);
    this.edition = getProperty(properties, BUILD_DATA_EDITION_KEY, "opensource");

    String timestampString = getProperty(properties, BUILD_DATA_TIMESTAMP_KEY, null);
    this.host = getProperty(properties, BUILD_DATA_HOST_KEY, UNKNOWN_VALUE);
    this.user = getProperty(properties, BUILD_DATA_USER_KEY, UNKNOWN_VALUE);
    this.branch = getProperty(properties, BUILD_DATA_BRANCH_KEY, UNKNOWN_VALUE);
    this.revision = getProperty(properties, BUILD_DATA_REVISION_KEY, UNKNOWN_VALUE);
    this.ee_revision = getProperty(properties, BUILD_DATA_EE_REVISION_KEY, UNKNOWN_VALUE);

    Date realTimestamp = null;
    if (timestampString != null) {
      try {
        realTimestamp = new SimpleDateFormat(DATE_FORMAT).parse(timestampString);
      } catch (ParseException pe) {
        pe.printStackTrace();
        System.exit(1);
      }
    }

    this.timestamp = realTimestamp;

    Matcher matcher = KITIDPATTERN.matcher(maven_version);
    if (matcher.matches()) {
      kitID = matcher.group(1);
    } else {
      kitID = UNKNOWN_VALUE;
    }
  }

  private String getProperty(Properties properties, String name, String defaultValue) {
    String out = properties.getProperty(BUILD_DATA_ROOT_KEY + name);
    if (StringUtils.isBlank(out)) out = defaultValue;
    return out;
  }

  private static ProductInfo PRODUCTINFO = null;

  public static synchronized ProductInfo getInstance() {
    if (PRODUCTINFO == null) {
      InputStream in = ProductInfo.class.getResourceAsStream(BUILD_DATA_RESOURCE_NAME);
      PRODUCTINFO = new ProductInfo(in, "resource '" + BUILD_DATA_RESOURCE_NAME + "'");
    }

    return PRODUCTINFO;
  }

  public static void printRawData() throws IOException {
    InputStream in = ProductInfo.class.getResourceAsStream(BUILD_DATA_RESOURCE_NAME);
    IOUtils.copy(in, System.out);
  }

  public boolean isDevMode() {
    return this.version.endsWith(UNKNOWN_VALUE);
  }

  public String moniker() {
    return this.moniker;
  }

  public String edition() {
    return this.edition;
  }

  public String rawVersion() {
    return this.version;
  }

  public String buildVersion() {
    return this.version;
  }

  public String mavenArtifactsVersion() {
    return this.maven_version;
  }

  public String kitID() {
    return this.kitID;
  }

  public Date buildTimestamp() {
    return this.timestamp;
  }

  public String buildTimestampAsString() {
    if (this.timestamp == null) return UNKNOWN_VALUE;
    else return new SimpleDateFormat(DATE_FORMAT).format(this.timestamp);
  }

  public String buildHost() {
    return this.host;
  }

  public String buildUser() {
    return this.user;
  }

  public String buildBranch() {
    return this.branch;
  }

  public String copyright() {
    return bundleHelper.getString("copyright");
  }

  public String buildRevision() {
    return this.revision;
  }

  public String buildRevisionFromEE() {
    return this.ee_revision;
  }

  public String toShortString() {
    return this.moniker + " " + ("opensource".equals(edition) ? "" : (edition + " ")) + buildVersion();
  }

  public String toLongString() {
    return toShortString() + ", as of " + buildID();
  }

  public String buildID() {
    String rev = this.revision;
    if (edition.indexOf("Enterprise") >= 0) {
      rev = this.ee_revision + "-" + this.revision;
    }
    return buildTimestampAsString() + " (Revision " + rev + " by " + buildUser() + "@" + buildHost() + " from "
           + buildBranch() + ")";
  }

  public String toString() {
    return toShortString();
  }

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("v", "verbose", false, bundleHelper.getString("option.verbose"));
    options.addOption("r", "raw", false, bundleHelper.getString("option.raw"));
    options.addOption("h", "help", false, bundleHelper.getString("option.help"));

    CommandLineParser parser = new GnuParser();
    CommandLine cli = parser.parse(options, args);

    if (cli.hasOption("h")) {
      new HelpFormatter().printHelp("java " + ProductInfo.class.getName(), options);
    }

    if (cli.hasOption("v")) {
      System.out.println(getInstance().toLongString());
    } else if (cli.hasOption("r")) {
      printRawData();
    } else {
      System.out.println(getInstance().toShortString());
    }
  }
}
