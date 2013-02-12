package com.tc.test.config.model;

import java.util.ArrayList;

/**
 * The config for each L2 <br>
 * Default: <br>
 * dgc enabled: false <br>
 * dgc interval : 3600 sec <br>
 * off heap enabled : false <br>
 * max off heap data size: 128M <br>
 * persistence : temporary swap <br>
 * client reconnect window : 25 secs
 * 
 * @author rsingh
 */
public class L2Config {

  private boolean                 offHeapEnabled       = false;
  private int                     maxOffHeapDataSize   = 512;
  private final ArrayList<String> extraServerJvmArgs;
  private boolean                 isProxyTsaGroupPorts = false;
  private boolean                 isProxyTsaPorts      = false;
  private int                     minHeap              = 256;
  private int                     maxHeap              = 256;
  private int                     directMemorySize     = -1;
  private int                     proxyWaitTime        = 20 * 1000;
  private int                     proxyDownTime        = 100;
  private boolean                 manualProxycontrol   = false;

  private final BytemanConfig     bytemanConfig        = new BytemanConfig();
  private boolean                 autoOffHeapEnable    = true;

  /**
   * Creates a l2 config with these defaults <br>
   * dgc enabled: false <br>
   * dgc interval : 3600 sec <br>
   * off heap enabled : false <br>
   * max off heap data size: 128M <br>
   * persistence : temporary swap <br>
   * client reconnect window : 120 secs
   */
  public L2Config() {
    extraServerJvmArgs = new ArrayList<String>();
  }

  /**
   * Is off heap enabled
   * 
   * @return : true if off heap is enabled
   */
  public boolean isOffHeapEnabled() {
    return offHeapEnabled;
  }

  /**
   * Enabled/Disable off heap
   * 
   * @param offHeapEnabled : true if the off heap is to be enabled, false otherwise
   */
  public void setOffHeapEnabled(boolean offHeapEnabled) {
    this.offHeapEnabled = offHeapEnabled;
  }

  /**
   * max off heap data size in MBs
   * 
   * @return
   */
  public int getMaxOffHeapDataSize() {
    return maxOffHeapDataSize;
  }

  /**
   * Sets max off heap data size
   * 
   * @param maxOffHeapDataSize offheap data size in MB
   */
  public void setMaxOffHeapDataSize(int maxOffHeapDataSize) {
    this.maxOffHeapDataSize = maxOffHeapDataSize;
  }

  /**
   * @return List of jvm arguments for each server
   */
  public ArrayList<String> getExtraServerJvmArgs() {
    return extraServerJvmArgs;
  }

  /**
   * Adds a jvm argumnet for each server
   * 
   * @param arg jvm argument
   */
  public void addExtraServerJvmArg(String arg) {
    extraServerJvmArgs.add(arg);
  }

  /**
   * @return true if proxy is enabled between two mirror groups communication
   */
  public boolean isProxyTsaGroupPorts() {
    return isProxyTsaGroupPorts;
  }

  /**
   * Enable/Disable tsa group proxy between two mirror groups
   * 
   * @param isProxyTsaGroupPorts
   */
  public void setProxyTsaGroupPorts(boolean isProxyTsaGroupPorts) {
    this.isProxyTsaGroupPorts = isProxyTsaGroupPorts;
  }

  /**
   * is L2 started with a proxy port in between the server and client
   * 
   * @return
   */
  public boolean isProxyTsaPorts() {
    return isProxyTsaPorts;
  }

  /**
   * Enable/Disable l2 proxy for tsa port
   * 
   * @param isProxyTsaPorts
   */
  public void setProxyTsaPorts(boolean isProxyTsaPorts) {
    this.isProxyTsaPorts = isProxyTsaPorts;
  }

  /**
   * Get the -Xms size to pass to L2s
   * 
   * @return Minimum heap size
   */
  public int getMinHeap() {
    return minHeap;
  }

  /**
   * Set the min heap size
   * 
   * @param minHeap minimum heap size
   */
  public void setMinHeap(int minHeap) {
    this.minHeap = minHeap;
    if (maxHeap < minHeap) {
      maxHeap = minHeap;
    }
  }

  /**
   * Get the -Xmx size to pass to L2s
   * 
   * @return Maximum heap size
   */
  public int getMaxHeap() {
    return maxHeap;
  }

  /**
   * Set the max heap size
   * 
   * @param maxHeap maximum heap size
   */
  public void setMaxHeap(int maxHeap) {
    this.maxHeap = maxHeap;
    if (minHeap > maxHeap) {
      minHeap = maxHeap;
    }
  }

  /**
   * Gets the "-XX:MaxDirectMemorySize" to pass to the server
   * 
   * @return -XX:MaxDirectMemorySize
   */
  public int getDirectMemorySize() {
    return directMemorySize;
  }

  /**
   * Sets "-XX:MaxDirectMemorySize"
   * 
   * @param -XX:MaxDirectMemorySize in MB
   */
  public void setDirectMemorySize(int directMemorySize) {
    this.directMemorySize = directMemorySize;
  }

  public boolean isAutoOffHeapEnable() {
    return autoOffHeapEnable;
  }

  public void setAutoOffHeapEnable(boolean autoOffHeapEnable) {
    this.autoOffHeapEnable = autoOffHeapEnable;
  }

  public int getProxyWaitTime() {
    return proxyWaitTime;
  }

  public int getProxyDownTime() {
    return proxyDownTime;
  }

  public void setProxyWaitTime(int proxyWaitTime) {
    this.proxyWaitTime = proxyWaitTime;
  }

  public void setProxyDownTime(int proxyDownTime) {
    this.proxyDownTime = proxyDownTime;
  }

  public BytemanConfig getBytemanConfig() {
    return bytemanConfig;
  }

  public boolean isManualProxycontrol() {
    return manualProxycontrol;
  }

  public void setManualProxycontrol(boolean manualProxycontrol) {
    this.manualProxycontrol = manualProxycontrol;
  }
}
