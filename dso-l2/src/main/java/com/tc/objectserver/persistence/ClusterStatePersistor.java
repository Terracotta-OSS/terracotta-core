/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.persistence;

import com.tc.net.StripeID;
import com.tc.text.PrettyPrinter;
import com.tc.util.State;
import com.tc.util.version.Version;
import java.io.IOException;
import java.util.HashMap;

import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.persistence.IPlatformPersistence;


public class ClusterStatePersistor implements StateDumpable {
  private static final String MAP_FILE_NAME = "ClusterStatePersistor.map";
  private static final String DB_CLEAN_KEY = "dbclean";
  private static final String L2_STATE_KEY = "l2state";
  private static final String STRIPE_ID_KEY = "stripeid";
  private static final String VERSION_KEY = "version";

  private final IPlatformPersistence storageManager;
  private final HashMap<String, String> map;
  private State initialState;

  @SuppressWarnings("unchecked")
  public ClusterStatePersistor(IPlatformPersistence storageManager) {
    this.storageManager = storageManager;
    HashMap<String, String> map = null;
    try {
      map = (HashMap<String, String>) this.storageManager.loadDataElement(MAP_FILE_NAME);
    } catch (IOException e) {
      // We don't expect this during startup so just throw it as runtime.
      throw new RuntimeException("Failure reading ClusterStatePersistor map file", e);
    }
    this.map = (null != map) ? map : new HashMap<String, String>();
    this.initialState = getCurrentL2State();
  }

  public void setStripeID(StripeID stripeID) {
    putAndStore(STRIPE_ID_KEY, stripeID.getName());
  }

  public StripeID getStripeID() {
    String s = map.get(STRIPE_ID_KEY);
    return s == null ? StripeID.NULL_ID : new StripeID(s);
  }

  public void setThisStripeID(StripeID stripeID) {
    putAndStore(STRIPE_ID_KEY, stripeID.getName());
  }

  public StripeID getThisStripeID() {
    String s = map.get(STRIPE_ID_KEY);
    return s == null ? StripeID.NULL_ID : new StripeID(s);
  }

  public State getInitialState() {
    return initialState;
  }

  public void setCurrentL2State(State state) {
    putAndStore(L2_STATE_KEY, state.getName());
  }

  public State getCurrentL2State() {
    String s = map.get(L2_STATE_KEY);
    return s == null ? null : new State(s);
  }

  public boolean isDBClean() {
    String s = map.get(DB_CLEAN_KEY);
    return s == null || Boolean.valueOf(s);
  }

  public Version getVersion() {
    String v = map.get(VERSION_KEY);
    return v == null ? null : new Version(v);
  }

  public void setVersion(Version v) {
    putAndStore(VERSION_KEY, v.major() + "." + v.minor() + "." + v.micro());
  }

  public void setDBClean(boolean dbClean) {
    putAndStore(DB_CLEAN_KEY, String.valueOf(dbClean));
  }

  public void clear() {
    map.clear();
    initialState = null;
  }

  // This isn't called from different threads but we can easily synchronize around the putAndStore.
  private synchronized void putAndStore(String key, String value) {
    this.map.put(key, value);
    try {
      this.storageManager.storeDataElement(MAP_FILE_NAME, this.map);
    } catch (IOException e) {
      // In general, we have no way of solving this problem so throw it.
      throw new RuntimeException("Failure storing ClusterStatePersistor map file", e);
    }
  }

  @Override
  public void addStateTo(final StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("StripeID", String.valueOf(getStripeID()));
    stateDumpCollector.addState("Version", String.valueOf(getVersion()));
    stateDumpCollector.addState("Initial State", String.valueOf(getInitialState()));
    stateDumpCollector.addState("Current State", String.valueOf(getCurrentL2State()));
    stateDumpCollector.addState("isDBClean", String.valueOf(isDBClean()));
  }
}
