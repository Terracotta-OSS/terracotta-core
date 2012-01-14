package com.tc.test.config.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class L2Config {

  private boolean           dgcEnabled            = false;
  private int               dgcIntervalInSec      = 3600;
  private boolean           offHeapEnabled        = false;
  private PersistenceMode   persistenceMode       = PersistenceMode.TEMPORARY_SWAP_ONLY;
  private int               clientReconnectWindow = 120;
  private int               maxOffHeapDataSize    = 128;
  private ArrayList<String> extraServerJvmArgs;
  private boolean           isProxyL2groupPorts   = false;
  private boolean           isProxyDsoPorts       = false;
  private final Map<String, String> configBuilderPropertyMap;

  public L2Config() {
    extraServerJvmArgs = new ArrayList<String>();
    configBuilderPropertyMap = new HashMap<String, String>();
  }

  public boolean isDgcEnabled() {
    return dgcEnabled;
  }

  public void setDgcEnabled(boolean dgcEnabled) {
    this.dgcEnabled = dgcEnabled;
  }

  public int getDgcIntervalInSec() {
    return dgcIntervalInSec;
  }

  public void setDgcIntervalInSec(int dgcIntervalInSec) {
    this.dgcIntervalInSec = dgcIntervalInSec;
  }

  public boolean isOffHeapEnabled() {
    return offHeapEnabled;
  }

  public void setOffHeapEnabled(boolean offHeapEnabled) {
    this.offHeapEnabled = offHeapEnabled;
  }

  public PersistenceMode getPersistenceMode() {
    return persistenceMode;
  }

  public void setPersistenceMode(PersistenceMode persistenceMode) {
    this.persistenceMode = persistenceMode;
  }

  public int getClientReconnectWindow() {
    return clientReconnectWindow;
  }

  public void setClientReconnectWindow(int clientReconnectWindow) {
    this.clientReconnectWindow = clientReconnectWindow;
  }

  public int getMaxOffHeapDataSize() {
    return maxOffHeapDataSize;
  }

  public void setMaxOffHeapDataSize(int maxOffHeapDataSize) {
    this.maxOffHeapDataSize = maxOffHeapDataSize;
  }

  public ArrayList<String> getExtraServerJvmArgs() {
    return extraServerJvmArgs;
  }

  public void addExtraServerJvmArg(String arg) {
    extraServerJvmArgs.add(arg);
  }

  public boolean isProxyL2groupPorts() {
    return isProxyL2groupPorts;
  }

  public void setProxyL2groupPorts(boolean isProxyL2groupPorts) {
    this.isProxyL2groupPorts = isProxyL2groupPorts;
  }

  public boolean isProxyDsoPorts() {
    return isProxyDsoPorts;
  }

  public void setProxyDsoPorts(boolean isProxyDsoPorts) {
    this.isProxyDsoPorts = isProxyDsoPorts;
  }

  public Map<String, String> getConfigBuilderPropertyMap() {
    return configBuilderPropertyMap;
  }

  public void addConfigBuilderProperty(String key, String value) {
    this.configBuilderPropertyMap.put(key, value);
  }

}
