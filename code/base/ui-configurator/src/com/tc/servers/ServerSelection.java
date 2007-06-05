/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.servers;

import com.tc.SessionIntegrator;
import com.tc.SessionIntegratorContext;
import com.tc.SessionIntegratorFrame;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.prefs.Preferences;

public class ServerSelection {
  private ServerInfo[] m_serverInfos;
  private int          m_selectedServerIndex;
  
  private static final String[] WEB_SERVERS               = {"tomcat5.5", "tomcat5.0", "tomcat6.0", "wls8.1", "was6.1"};
  private static final String   SERVER_SELECTION_PREF_KEY = "SessionIntegrator.ServerSelection";
  private static final String   SELECTED_SERVER_PREF_KEY  = "SelectedServer";
  private static final String   SERVERS_PREF_KEY          = "Servers";
  private static final String   SERVER_PROPS_FILENAME     = "server.properties";
  private static final String   SERVER_ENV_FILENAME       = "server.environment";
  private static final String   WEBAPP_PROPS_FILENAME     = "webapps.properties";
  private static final String   DEFAULT_SERVER_NAME       = WEB_SERVERS[0];
  
  private static ServerSelection m_instance;
  
  private ServerSelection() {
    String      sandBoxRoot  = SessionIntegratorFrame.getSandBoxRoot();
    int         serverCount  = WEB_SERVERS.length;
    Preferences prefs        = getPreferences();
    Preferences serversPrefs = prefs.node(SERVERS_PREF_KEY);
    Preferences serverPrefs;
    
    m_serverInfos = new ServerInfo[serverCount];
    
    for(int i = 0; i < serverCount; i++) {
      File       serverDir = new File(sandBoxRoot, WEB_SERVERS[i]);
      File       propsFile = new File(serverDir, SERVER_PROPS_FILENAME);
      File       envFile   = new File(serverDir, SERVER_ENV_FILENAME);
      Properties props     = new Properties();
      Properties env       = new Properties();
      
      try {
        props.load(new FileInputStream(propsFile));
        env.load(new FileInputStream(envFile));
        
        m_serverInfos[i] = new ServerInfo(props, env);
        serverPrefs      = serversPrefs.node(m_serverInfos[i].getName());
        
        m_serverInfos[i].loadEnvironment(serverPrefs);
      } catch(IOException ioe) {/**/}
    }
    
    String selectedServerName = prefs.get(SELECTED_SERVER_PREF_KEY, DEFAULT_SERVER_NAME);
    if((m_selectedServerIndex = Arrays.asList(WEB_SERVERS).indexOf(selectedServerName)) == -1) {
      m_selectedServerIndex = 0;
    }
  }
  
  public Properties getDefaultProperties(int i) {
    String     sandBoxRoot = SessionIntegratorFrame.getSandBoxRoot();
    File       serverDir   = new File(sandBoxRoot, WEB_SERVERS[i]);
    File       envFile     = new File(serverDir, SERVER_ENV_FILENAME);
    Properties env         = new Properties();
    
    try {
      env.load(new FileInputStream(envFile));
    } catch(IOException ioe) {/**/}
    
    return env;
  }
  
  public static ServerSelection getInstance() {
    if(m_instance == null) {
      m_instance = new ServerSelection();
    }
    return m_instance;
  }
  
  public ServerInfo[] getServers() {
    return m_serverInfos;
  }
  
  public void setServers(ServerInfo[] servers) {
    m_serverInfos = servers;
  }
  
  public ServerInfo[] cloneServers() {
    ServerInfo[] servers = getServers();
    ServerInfo[] result  = new ServerInfo[servers.length];
    
    for(int i = 0; i < servers.length; i++) {
      result[i] = new ServerInfo(servers[i]);
    }
    
    return result;
  }
  
  public int getSelectedServerIndex() {
    return m_selectedServerIndex;
  }
  
  public ServerInfo getSelectedServer() {
    return m_serverInfos[m_selectedServerIndex];
  }
  
  public File getSelectedServerWebAppProperties() {
    String sandBoxRoot = SessionIntegratorFrame.getSandBoxRoot();
    File   serverDir   = new File(sandBoxRoot, getSelectedServer().getName());
    File   propsFile   = new File(serverDir, WEBAPP_PROPS_FILENAME);
    
    return propsFile;
  }
  
  public void setSelectedServerIndex(int selectedServerIndex) {
    m_selectedServerIndex = selectedServerIndex;
    storeServerEnvironments();
  }
  
  public ServerInfo getServer(int index) {
    return m_serverInfos[index];
  }
  
  public void storeServerEnvironments() {
    int         serverCount  = WEB_SERVERS.length;
    Preferences prefs        = getPreferences();
    Preferences serversPrefs = prefs.node(SERVERS_PREF_KEY);
    Preferences serverPrefs;
    
    for(int i = 0; i < serverCount; i++) {
      serverPrefs = serversPrefs.node(m_serverInfos[i].getName());
      m_serverInfos[i].storeEnvironment(serverPrefs);
    }

    prefs.put(SELECTED_SERVER_PREF_KEY, getSelectedServer().getName());
    storePreferences();
  }
  
  private Preferences getPreferences() {
    SessionIntegratorContext cntx = SessionIntegrator.getContext();
    return cntx.prefs.node(SERVER_SELECTION_PREF_KEY);
  }
  
  private void storePreferences() {
    SessionIntegratorContext cntx = SessionIntegrator.getContext();
    cntx.client.storePrefs();
  }
}
