package org.terracotta.testing.rules;

import org.terracotta.testing.master.ConfigBuilder;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class BasicExternalClusterBuilder {
  private final int stripeSize;

  private File clusterDirectory = new File("target" + File.separator + "cluster");
  private List<File> serverJars = Collections.emptyList();
  private String namespaceFragment = "";
  private String serviceFragment = "";
  private int clientReconnectWindowTime = ConfigBuilder.DEFAULT_CLIENT_RECONNECT_WINDOW_TIME;
  private int failoverPriorityVoterCount = ConfigBuilder.FAILOVER_PRIORITY_AVAILABILITY;
  private Properties tcProperties = new Properties();
  private Properties systemProperties = new Properties();
  private String logConfigExt = "logback-ext.xml";


  private BasicExternalClusterBuilder(final int stripeSize) {
    this.stripeSize = stripeSize;
  }

  public static BasicExternalClusterBuilder newCluster() {
    return new BasicExternalClusterBuilder(1);
  }

  public static BasicExternalClusterBuilder newCluster(int stripeSize) {
    if(stripeSize < 1) {
      throw new IllegalArgumentException("Must be at least one server in the cluster");
    }
    return new BasicExternalClusterBuilder(stripeSize);
  }

  public BasicExternalClusterBuilder in(File clusterDirectory) {
    if (clusterDirectory == null) {
      throw new NullPointerException("Cluster directory must be non-null");
    }
    this.clusterDirectory = clusterDirectory;
    return this;
  }

  public BasicExternalClusterBuilder withServerJars(final List<File> serverJars) {
    if (serverJars == null) {
      throw new NullPointerException("Server JARs list must be non-null");
    }
    this.serverJars = serverJars;
    return this;
  }

  public BasicExternalClusterBuilder withNamespaceFragment(final String namespaceFragment) {
    if (namespaceFragment == null) {
      throw new NullPointerException("Namespace fragment must be non-null");
    }
    this.namespaceFragment = namespaceFragment;
    return this;
  }

  public BasicExternalClusterBuilder withServiceFragment(final String serviceFragment) {
    if (serviceFragment == null) {
      throw new NullPointerException("Service fragment must be non-null");
    }
    this.serviceFragment = serviceFragment;
    return this;
  }

  public BasicExternalClusterBuilder withClientReconnectWindowTime(final int clientReconnectWindowTime) {
    this.clientReconnectWindowTime = clientReconnectWindowTime;
    return this;
  }

  /**
   * Zero or any positive value will tune the cluster for consistency and set the respective voter count as provided.
   * A value of -1 will tune the cluster for availability. This is the default.
   */
  public BasicExternalClusterBuilder withFailoverPriorityVoterCount(final int failoverPriorityVoterCount) {
    this.failoverPriorityVoterCount = failoverPriorityVoterCount;
    return this;
  }

  public BasicExternalClusterBuilder withTcProperties(Properties tcProperties) {
    this.tcProperties.putAll(tcProperties);
    return this;
  }

  public BasicExternalClusterBuilder withTcProperty(String key, String value) {
    this.tcProperties.put(key, value);
    return this;
  }

  public BasicExternalClusterBuilder withSystemProperties(Properties props) {
    this.systemProperties.putAll(props);
    return this;
  }

  public BasicExternalClusterBuilder withSystemProperty(String key, String value) {
    this.systemProperties.put(key, value);
    return this;
  }
  
  public BasicExternalClusterBuilder logConfigExtensionResourceName(String logConfigExt) {
    this.logConfigExt = logConfigExt;
    return this;
  }

  public BasicExternalCluster build() {
    return new BasicExternalCluster(clusterDirectory, stripeSize, serverJars, namespaceFragment, serviceFragment,
        clientReconnectWindowTime, failoverPriorityVoterCount, tcProperties, systemProperties, logConfigExt);
  }
}
