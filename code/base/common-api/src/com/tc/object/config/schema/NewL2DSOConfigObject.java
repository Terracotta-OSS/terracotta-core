/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.BaseNewConfigObject;
import com.tc.config.schema.NewCommonL2Config;
import com.tc.config.schema.OffHeapConfigItem;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.BindPortConfigItem;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.dynamic.StringConfigItem;
import com.tc.config.schema.dynamic.XPathBasedConfigItem;
import com.tc.util.Assert;
import com.terracottatech.config.BindPort;
import com.terracottatech.config.PersistenceMode;
import com.terracottatech.config.Server;

/**
 * The standard implementation of {@link NewL2DSOConfig}.
 */
public class NewL2DSOConfigObject extends BaseNewConfigObject implements NewL2DSOConfig {

  private final ConfigItem         persistenceMode;
  private final OffHeapConfigItem  offHeapConfig;
  private final BooleanConfigItem  garbageCollectionEnabled;
  private final BooleanConfigItem  garbageCollectionVerbose;
  private final IntConfigItem      garbageCollectionInterval;
  private final BindPortConfigItem dsoPort;
  private final BindPortConfigItem l2GroupPort;
  private final IntConfigItem      clientReconnectWindow;
  private final StringConfigItem   host;
  private final StringConfigItem   serverName;
  private final StringConfigItem   bind;

  public NewL2DSOConfigObject(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(Server.class);

    this.persistenceMode = new XPathBasedConfigItem(this.context, "dso/persistence/mode") {
      @Override
      protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
        if (xmlObject == null) return null;
        if (((PersistenceMode) xmlObject).enumValue() == PersistenceMode.TEMPORARY_SWAP_ONLY) return com.tc.object.config.schema.PersistenceMode.TEMPORARY_SWAP_ONLY;
        if (((PersistenceMode) xmlObject).enumValue() == PersistenceMode.PERMANENT_STORE) return com.tc.object.config.schema.PersistenceMode.PERMANENT_STORE;
        throw Assert.failure("Persistence mode " + xmlObject + " is not anything in the enum?");
      }
    };

    this.garbageCollectionEnabled = this.context.booleanItem("dso/garbage-collection/enabled");
    this.garbageCollectionVerbose = this.context.booleanItem("dso/garbage-collection/verbose");
    this.garbageCollectionInterval = this.context.intItem("dso/garbage-collection/interval");
    this.clientReconnectWindow = this.context.intItem("dso/client-reconnect-window");

    this.bind = this.context.stringItem("@bind");
    this.host = this.context.stringItem("@host");
    this.serverName = this.context.stringItem("@name");

    int listenPort = this.context.intItem("dso-port").getInt();
    int tempGroupPort = this.context.intItem("dso-port").getInt()
                        + NewL2DSOConfig.DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT;
    int defaultGroupPort = ((tempGroupPort <= NewCommonL2Config.MAX_PORTNUMBER) ? (tempGroupPort)
        : (tempGroupPort % NewCommonL2Config.MAX_PORTNUMBER) + NewCommonL2Config.MIN_PORTNUMBER);

    BindPort defaultDsoPort = BindPort.Factory.newInstance();
    defaultDsoPort.setIntValue(listenPort);
    defaultDsoPort.setBind(this.bind.getString());
    this.dsoPort = this.context.bindPortItem("dso-port", defaultDsoPort);

    BindPort defaultL2GroupPort = BindPort.Factory.newInstance();
    defaultL2GroupPort.setIntValue(defaultGroupPort);
    defaultL2GroupPort.setBind(this.bind.getString());
    this.l2GroupPort = this.context.bindPortItem("l2-group-port", defaultL2GroupPort);
    this.offHeapConfig = new OffHeapConfigItem(this.context, "dso/persistence/offheap");
  }

  public OffHeapConfigItem offHeapConfig() {
    return this.offHeapConfig;
  }

  public BindPortConfigItem dsoPort() {
    return this.dsoPort;
  }

  public BindPortConfigItem l2GroupPort() {
    return this.l2GroupPort;
  }

  public StringConfigItem host() {
    return host;
  }

  public StringConfigItem serverName() {
    return this.serverName;
  }

  public ConfigItem persistenceMode() {
    return this.persistenceMode;
  }

  public BooleanConfigItem garbageCollectionEnabled() {
    return this.garbageCollectionEnabled;
  }

  public BooleanConfigItem garbageCollectionVerbose() {
    return this.garbageCollectionVerbose;
  }

  public IntConfigItem garbageCollectionInterval() {
    return this.garbageCollectionInterval;
  }

  public IntConfigItem clientReconnectWindow() {
    return this.clientReconnectWindow;
  }

  public StringConfigItem bind() {
    return this.bind;
  }

}
