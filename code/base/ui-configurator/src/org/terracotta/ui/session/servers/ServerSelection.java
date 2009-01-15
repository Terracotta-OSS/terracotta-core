/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session.servers;

import org.terracotta.ui.session.SessionIntegratorContext;
import org.terracotta.ui.session.SessionIntegratorFrame;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.prefs.Preferences;

public class ServerSelection {
  private SessionIntegratorContext sessionIntegratorContext;
  private ServerInfo[]             serverInfos;
  private int                      selectedServerIndex;

  // CDV-300: Disable 'was6.1' for the time being; configurator support for WebSphere is untested in a non-trivial
  // environment
  private static final String[]    WEB_SERVERS               = { "tomcat5.5", "tomcat5.0", "tomcat6.0", "wls8.1",
      "wls9.2" /* , "was6.1" */                              };
  private static final String      SERVER_SELECTION_PREF_KEY = "SessionIntegrator.ServerSelection";
  private static final String      SELECTED_SERVER_PREF_KEY  = "SelectedServer";
  private static final String      SERVERS_PREF_KEY          = "Servers";
  private static final String      SERVER_PROPS_FILENAME     = "server.properties";
  private static final String      SERVER_ENV_FILENAME       = "server.environment";
  private static final String      WEBAPP_PROPS_FILENAME     = "webapps.properties";
  private static final String      DEFAULT_SERVER_NAME       = WEB_SERVERS[0];

  public ServerSelection(SessionIntegratorContext sessionIntegratorContext) {
    this.sessionIntegratorContext = sessionIntegratorContext;
    
    String sandBoxRoot = SessionIntegratorFrame.getSandBoxRoot();
    int serverCount = WEB_SERVERS.length;
    Preferences prefs = getPreferences();
    Preferences serversPrefs = prefs.node(SERVERS_PREF_KEY);
    Preferences serverPrefs;

    serverInfos = new ServerInfo[serverCount];

    for (int i = 0; i < serverCount; i++) {
      File serverDir = new File(sandBoxRoot, WEB_SERVERS[i]);
      File propsFile = new File(serverDir, SERVER_PROPS_FILENAME);
      File envFile = new File(serverDir, SERVER_ENV_FILENAME);
      Properties props = new Properties();
      Properties env = new Properties();

      try {
        props.load(new FileInputStream(propsFile));
        env.load(new FileInputStream(envFile));

        serverInfos[i] = new ServerInfo(props, env);
        serverPrefs = serversPrefs.node(serverInfos[i].getName());

        serverInfos[i].loadEnvironment(serverPrefs);
      } catch (IOException ioe) {/**/
      }
    }

    String selectedServerName = prefs.get(SELECTED_SERVER_PREF_KEY, DEFAULT_SERVER_NAME);
    if ((selectedServerIndex = Arrays.asList(WEB_SERVERS).indexOf(selectedServerName)) == -1) {
      selectedServerIndex = 0;
    }
  }

  public Properties getDefaultProperties(int i) {
    String sandBoxRoot = SessionIntegratorFrame.getSandBoxRoot();
    File serverDir = new File(sandBoxRoot, WEB_SERVERS[i]);
    File envFile = new File(serverDir, SERVER_ENV_FILENAME);
    Properties env = new Properties();

    try {
      env.load(new FileInputStream(envFile));
    } catch (IOException ioe) {/**/
    }

    return env;
  }

  public ServerInfo[] getServers() {
    return serverInfos;
  }

  public void setServers(ServerInfo[] servers) {
    serverInfos = servers;
  }

  public ServerInfo[] cloneServers() {
    ServerInfo[] servers = getServers();
    ServerInfo[] result = new ServerInfo[servers.length];

    for (int i = 0; i < servers.length; i++) {
      result[i] = new ServerInfo(servers[i]);
    }

    return result;
  }

  public int getSelectedServerIndex() {
    return selectedServerIndex;
  }

  public ServerInfo getSelectedServer() {
    return serverInfos[selectedServerIndex];
  }

  public File getSelectedServerWebAppProperties() {
    String sandBoxRoot = SessionIntegratorFrame.getSandBoxRoot();
    File serverDir = new File(sandBoxRoot, getSelectedServer().getName());
    File propsFile = new File(serverDir, WEBAPP_PROPS_FILENAME);

    return propsFile;
  }

  public void setSelectedServerIndex(int selectedServerIndex) {
    this.selectedServerIndex = selectedServerIndex;
    storeServerEnvironments();
  }

  public ServerInfo getServer(int index) {
    return serverInfos[index];
  }

  public void storeServerEnvironments() {
    int serverCount = WEB_SERVERS.length;
    Preferences prefs = getPreferences();
    Preferences serversPrefs = prefs.node(SERVERS_PREF_KEY);
    Preferences serverPrefs;

    for (int i = 0; i < serverCount; i++) {
      serverPrefs = serversPrefs.node(serverInfos[i].getName());
      serverInfos[i].storeEnvironment(serverPrefs);
    }

    prefs.put(SELECTED_SERVER_PREF_KEY, getSelectedServer().getName());
    storePreferences();
  }

  private Preferences getPreferences() {
    return sessionIntegratorContext.getPrefs().node(SERVER_SELECTION_PREF_KEY);
  }

  private void storePreferences() {
    sessionIntegratorContext.storePrefs();
  }
}
