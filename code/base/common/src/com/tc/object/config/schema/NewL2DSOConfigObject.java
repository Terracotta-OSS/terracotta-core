/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.BaseNewConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.dynamic.StringConfigItem;
import com.tc.config.schema.dynamic.XPathBasedConfigItem;
import com.tc.util.Assert;
import com.terracottatech.config.PersistenceMode;
import com.terracottatech.config.Server;

/**
 * The standard implementation of {@link NewL2DSOConfig}.
 */
public class NewL2DSOConfigObject extends BaseNewConfigObject implements NewL2DSOConfig {

  private final ConfigItem        persistenceMode;
  private final BooleanConfigItem garbageCollectionEnabled;
  private final BooleanConfigItem garbageCollectionVerbose;
  private final IntConfigItem     garbageCollectionInterval;
  private final IntConfigItem     listenPort;
  private final IntConfigItem     l2GroupPort;
  private final IntConfigItem     clientReconnectWindow;
  private final StringConfigItem  host;

  public NewL2DSOConfigObject(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(Server.class);

    this.persistenceMode = new XPathBasedConfigItem(this.context, "dso/persistence/mode") {
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
    this.listenPort = this.context.intItem("dso-port");
    this.l2GroupPort = this.context.intItem("l2-group-port");
    this.host = this.context.stringItem("@host");
  }

  public IntConfigItem listenPort() {
    return this.listenPort;
  }

  public IntConfigItem l2GroupPort() {
    return this.l2GroupPort;
  }

  public StringConfigItem host() {
    return host;
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

}
