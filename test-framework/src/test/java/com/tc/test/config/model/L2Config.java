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
 * client reconnect window : 120 secs
 * 
 * @author rsingh
 * 
 */
public class L2Config {

	private boolean dgcEnabled = false;
	private int dgcIntervalInSec = 3600;
	private boolean offHeapEnabled = false;
	private PersistenceMode persistenceMode = PersistenceMode.TEMPORARY_SWAP_ONLY;
	private int clientReconnectWindow = 120;
	private int maxOffHeapDataSize = 128;
	private ArrayList<String> extraServerJvmArgs;
	private boolean isProxyL2groupPorts = false;
	private boolean isProxyDsoPorts = false;

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
	 * Is DGC enabled
	 * @return true if dgc is enabled
	 */
	public boolean isDgcEnabled() {
		return dgcEnabled;
	}

	/**
	 * enable/disable dgc
	 * @param dgcEnabled true if dgc to be enabled. false otherwise
	 */
	public void setDgcEnabled(boolean dgcEnabled) {
		this.dgcEnabled = dgcEnabled;
	}

	/**
	 * @return dgc interveal in seconds
	 */
	public int getDgcIntervalInSec() {
		return dgcIntervalInSec;
	}

	/**
	 * sets the dgc interval in seconds
	 */
	public void setDgcIntervalInSec(int dgcIntervalInSec) {
		this.dgcIntervalInSec = dgcIntervalInSec;
	}

	/**
	 * Is off heap enabled
	 * @return : true if off heap is enabled
	 */
	public boolean isOffHeapEnabled() {
		return offHeapEnabled;
	}

	/**
	 * Enabled/Disable off heap
	 * @param offHeapEnabled : true if the off heap is to be enabled, false otherwise
	 */
	public void setOffHeapEnabled(boolean offHeapEnabled) {
		this.offHeapEnabled = offHeapEnabled;
	}

	/**
	 * Persistence mode for the L2
	 */
	public PersistenceMode getPersistenceMode() {
		return persistenceMode;
	}

	/**
	 * Sets the persistence mode for each L2
	 * @param persistenceMode persistence Mode 
	 */
	public void setPersistenceMode(PersistenceMode persistenceMode) {
		this.persistenceMode = persistenceMode;
	}

	/**
	 * client reconnect window in secs
	 */
	public int getClientReconnectWindow() {
		return clientReconnectWindow;
	}

	/**
	 * sets client reconnect window in seconds
	 */
	public void setClientReconnectWindow(int clientReconnectWindow) {
		this.clientReconnectWindow = clientReconnectWindow;
	}

	/**
	 * max off heap data size in MBs
	 * @return
	 */
	public int getMaxOffHeapDataSize() {
		return maxOffHeapDataSize;
	}

	/**
	 * Sets max off heap data size
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
	 * @param arg jvm argument
	 */
	public void addExtraServerJvmArg(String arg) {
		extraServerJvmArgs.add(arg);
	}

	/**
	 * @return true if proxy is enabled between two mirror groups communication
	 */
	public boolean isProxyL2groupPorts() {
		return isProxyL2groupPorts;
	}
	
	/**
	 * Enable/Disable l2 group proxy between two mirror groups
	 * @param isProxyL2groupPorts
	 */
	public void setProxyL2groupPorts(boolean isProxyL2groupPorts) {
		this.isProxyL2groupPorts = isProxyL2groupPorts;
	}

	/**
	 * is L2 started with a proxy port in bertween the server and client
	 * @return
	 */
	public boolean isProxyDsoPorts() {
		return isProxyDsoPorts;
	}

	/**
	 * Enable/Disable l2 proxy for dso port
	 * @param isProxyDsoPorts
	 */
	public void setProxyDsoPorts(boolean isProxyDsoPorts) {
		this.isProxyDsoPorts = isProxyDsoPorts;
	}
}
