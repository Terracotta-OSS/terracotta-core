package com.tc.test.config.model;

import java.util.ArrayList;
import java.util.List;

import org.terracotta.tests.base.AbstractClientBase;

/**
 * Configuration for each client <br>
 * Default : <br>
 * run client parrallely : true <br>
 * 
 * @author rsingh
 * 
 */
public class ClientConfig {
	private boolean parallelClients = true;
	private final List<String> extraClientJvmArgs;
	private Class<? extends AbstractClientBase>[] classes;

	public ClientConfig() {
		extraClientJvmArgs = new ArrayList<String>();
	}

	/**
	 * @return list of extra jvm args for each clients
	 */
	public List<String> getExtraClientJvmArgs() {
		return extraClientJvmArgs;
	}

	/**
	 * Adds a jvm argument for each client 
	 * @param extraClientJvmArg : jvm arg to be added for each client
	 */
	public void addExtraClientJvmArg(String extraClientJvmArg) {
		extraClientJvmArgs.add(extraClientJvmArg);
	}

	/**
	 * Sets the client classes for the test
	 * @param classes an array of client classes to be run
	 */
	public void setClientClasses(Class<? extends AbstractClientBase>[] classes) {
		this.classes = classes;
	}

	/**
	 * Sets the classes for the test 
	 * @param clientClass the client class to be instantiated
	 * @param count number of client class to be instantiated
	 */
	public void setClientClasses(Class<? extends AbstractClientBase> clientClass, int count) {
		this.classes = new Class[count];
		for (int i = 0; i < count; i++) {
			classes[i] = clientClass;
		}
	}

	/**
	 * @return the classes to be instantiated for the test
	 */
	public Class<? extends AbstractClientBase>[] getClientClasses() {
		return classes;
	}

	/**
	 * Enable/Disable running of clients parallely 
	 * @param parallelClients 
	 */
	public void setParallelClients(boolean parallelClients) {
		this.parallelClients = parallelClients;
	}

	/**
	 * @return true if clients will run in parrallel, false otherwise
	 */
	public boolean isParallelClients() {
		return parallelClients;
	}

}
