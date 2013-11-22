package com.tc.objectserver.persistence;

import org.terracotta.corestorage.StorageManager;

import com.tc.net.GroupID;
import com.tc.net.StripeID;
import com.tc.util.State;
import com.tc.util.version.Version;

import java.util.Map;

/**
 * @author tim
 */
public class ClusterStatePersistor {
  private static final String MAX_DATA_STORAGE_SIZE_KEY = "maxdatastoragesize";
  private static final String GROUP_ID_KEY = "groupid";
  private static final String DB_CLEAN_KEY = "dbclean";
  private static final String L2_STATE_KEY = "l2state";
  private static final String STRIPE_ID_KEY = "stripeid";
  private static final String GROUP_STRIPE_ID_PREFIX = "stripeid-for-";
  private static final String VERSION_KEY = "version";

  private final Map<String, String> map;
  private final State initialState;

  public ClusterStatePersistor(final StorageManager storageManager) {
    this.map = storageManager.getProperties();
    this.initialState = getCurrentL2State();
  }

  public void setMaxDataStorageSize(long size) {
    map.put(MAX_DATA_STORAGE_SIZE_KEY, String.valueOf(size));
  }

  public Long getMaxDataStorageSize() {
    String l = map.get(MAX_DATA_STORAGE_SIZE_KEY);
    return l == null ? null : Long.valueOf(l);
  }

  public void setGroupId(GroupID groupId) {
    map.put(GROUP_ID_KEY, String.valueOf(groupId.toInt()));
  }

  public GroupID getGroupId() {
    String g = map.get(GROUP_ID_KEY);
    return g == null ? GroupID.NULL_ID : new GroupID(Integer.valueOf(g));
  }

  public void setStripeID(GroupID groupID, StripeID stripeID) {
    map.put(groupStripeIdKey(groupID), stripeID.getName());
  }

  public StripeID getStripeID(GroupID groupID) {
    String s = map.get(groupStripeIdKey(groupID));
    return s == null ? StripeID.NULL_ID : new StripeID(s);
  }

  public void setThisStripeID(StripeID stripeID) {
    map.put(STRIPE_ID_KEY, stripeID.getName());
  }

  public StripeID getThisStripeID() {
    String s = map.get(STRIPE_ID_KEY);
    return s == null ? StripeID.NULL_ID : new StripeID(s);
  }

  public State getInitialState() {
    return initialState;
  }

  public void setCurrentL2State(State state) {
    map.put(L2_STATE_KEY, state.getName());
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
    map.put(VERSION_KEY, v.major() + "." + v.minor() + "." + v.micro());
  }

  public void setDBClean(boolean dbClean) {
    map.put(DB_CLEAN_KEY, String.valueOf(dbClean));
  }

  private static String groupStripeIdKey(GroupID groupID) {
    return GROUP_STRIPE_ID_PREFIX + groupID.toInt();
  }
}
