/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public final class CreateWLSConfig {

  private final StringBuffer  data;
  private static final int    L2         = 1;
  private static final int    L1         = 2;
  private static final String WLS_CONFIG = "config_clustered.xml";

  private CreateWLSConfig() {
    data = new StringBuffer();
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Please supply the file path to the file named hosts");
      return;
    }
    String[][] hosts = new String[4][];
    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
    for (int i = 0; i < 3; i++) {
      String line = in.readLine();
      String[] labelPart = line.split("\\s");
      if (labelPart.length == 2) hosts[i] = labelPart[1].split(":");
    }
    in.close();
    CreateWLSConfig config = new CreateWLSConfig();
    config.buildConfig(hosts);
    
    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(WLS_CONFIG));
    out.write(config.toString().getBytes());
    out.flush();
    out.close();
  }

  private void buildConfig(String[][] hosts) {
    ad("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    ad("<Domain Name=\"tcdomain\" ConfigurationVersion=\"8.1.6.0\">");
    ad("    <EmbeddedLDAP CredentialEncrypted=\"{3DES}NSVQgtn7xdojOWXyb7G46ESr89OP3EubgrJNnmMOBLQ=\" Name=\"mydomain\"/>");
    ad("    <SecurityConfiguration");
    ad("        CredentialEncrypted=\"{3DES}ehgR79weUhgb3Uauun9I821FdlseLqNSuAZFU6LPcCoem4SER8l3n3nFL/RuiNRWQXkreaQaqmg6aIkXeI7NRqb29Aq+YDHf\"");
    ad("        Name=\"mydomain\" RealmBootStrapVersion=\"1\"/>");
    ad("    <Security Name=\"tcdomain\" PasswordPolicy=\"wl_default_password_policy\" Realm=\"wl_default_realm\" RealmSetup=\"true\"/>");
    ad("    <Realm FileRealm=\"wl_default_file_realm\" Name=\"wl_default_realm\"/>");
    ad("    <FileRealm Name=\"wl_default_file_realm\"/>");
    ad("    <PasswordPolicy Name=\"wl_default_password_policy\"/>");
    ad("");
    ad("    <Application Name=\"_appsdir_ROOT_dir\" Path=\"applications\"");
    ad("        StagingMode=\"nostage\" TwoPhase=\"true\">");
    ad("        <WebAppComponent Name=\"ROOT\" Targets=\"tc-cluster\" URI=\"ROOT\"/>");
    ad("    </Application>");
    ad("    <Application Name=\"_appsdir_perftest_war\"");
    ad("            Path=\"applications\"");
    ad("        StagingMode=\"nostage\" TwoPhase=\"true\">");
    ad("        <WebAppComponent Name=\"perftest\" Targets=\"tc-cluster\" URI=\"perftest.war\"/>");
    ad("    </Application>");
    ad("");
    ad("    <Cluster Name=\"tc-cluster\" MulticastAddress=\"224.0.0.8\"/>");
    ad("    <Server Name=\"tc-wls-admin-server\" ListenAddress=\"" + hosts[L2][0] + "\"");
    ad("        ListenPort=\"7001\" Machine=\"" + hosts[L2][0] + "-machine\"");
    ad("        ReliableDeliveryPolicy=\"RMDefaultPolicy\" NativeIOEnabled=\"true\">");
    ad("        <SSL Name=\"tc-wls-admin-server\" Enabled=\"false\" HostnameVerificationIgnored=\"false\"/>");
    ad("    </Server>");
    ad("");
    for (int i = 0; i < hosts[L1].length; i++) {
      ad("    <Server Name=\"" + hosts[L1][i] + "-managed\" Cluster=\"tc-cluster\"");
      ad("        ListenAddress=\"" + hosts[L1][i] + "\" ListenPort=\"8080\" Machine=\"" + hosts[L1][i]
         + "-machine\" NativeIOEnabled=\"true\">");
      ad("        <NetworkAccessPoint Name=\"sipchannel\" ListenPort=\"5060\" Protocol=\"t3\"/>");
      ad("        <SSL Name=\"" + hosts[L1][i] + "-managed\" Enabled=\"false\"/>");
      ad("        <ExecuteQueue Name=\"weblogic.kernel.Default\" ThreadCount=\"15\"/>");
      ad("        <ExecuteQueue Name=\"sip.transport.Default\"/>");
      ad("        <ExecuteQueue Name=\"sip.tracing.local\" QueueLength=\"1024\"");
      ad("            ThreadCount=\"1\" ThreadsMaximum=\"1\" ThreadsMinimum=\"1\"/>");
      ad("        <ExecuteQueue Name=\"sip.tracing.domain\" QueueLength=\"1024\"");
      ad("            ThreadCount=\"1\" ThreadsMaximum=\"1\" ThreadsMinimum=\"1\"/>");
      ad("        <ExecuteQueue Name=\"sip.timer.Default\" QueueLength=\"1024\" ThreadCount=\"3\"/>");
      ad("    </Server>");
    }
    ad("");
    for (int i = 0; i < hosts[L1].length; i++) {
      ad("    <MigratableTarget Name=\"" + hosts[L1][i] + "-managed (migratable)\"");
      ad("        Cluster=\"tc-cluster\" UserPreferredServer=\"" + hosts[L1][i] + "-managed\"/>");
    }
    ad("");
    for (int i = 0; i < hosts[L1].length; i++) {
      ad("    <Machine Name=\"" + hosts[L1][i] + "-machine\">");
      ad("        <NodeManager Name=\"" + hosts[L1][i] + "-machine\" ListenAddress=\"" + hosts[L1][i] + "\"/>");
      ad("    </Machine>");
    }
    ad("    <!-- L2 MACHINE -->");
    ad("    <Machine Name=\"" + hosts[L2][0] + "-machine\">");
    ad("        <NodeManager Name=\"" + hosts[L2][0] + "-machine\" ListenAddress=\"" + hosts[L2][0] + "\"/>");
    ad("    </Machine>");
    ad("");
    ad("    <JMSFileStore Name=\"FileStore\" Directory=\"rmfilestore\"/>");
    ad("    <WSReliableDeliveryPolicy Name=\"RMDefaultPolicy\" Store=\"FileStore\"");
    ad("        DefaultRetryCount=\"10\" DefaultTimeToLive=\"60000\"/>");
    ad("</Domain>");
  }

  private void ad(String appender) {
    data.append(appender + "\n");
  }

  public String toString() {
    return data.toString();
  }
}
