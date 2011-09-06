/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.context.StandardConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.MockConfigItemListener;
import com.tc.config.schema.repository.StandardBeanRepository;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.ByteArrayInputStream;

/**
 * A base class for all tests of real config objects. Tests derived from this aren't true unit tests, because they use a
 * bunch of the config infrastructure; they're more like subsystem tests. However, that's more like what we want to do
 * anyway &mdash; we want to make sure these objects hook up all the config stuff correctly so that they work right.
 */
public abstract class ConfigObjectTestBase extends TCTestCase {

  private StandardBeanRepository  repository;
  private TerracottaConfigBuilder builder;
  private ConfigContext           context;

  private MockConfigItemListener  listener1;
  private MockConfigItemListener  listener2;

  @Override
  public void setUp() throws Exception {
    throw Assert.failure("You must specify the repository bean class via a call to super.setUp(Class).");
  }

  public void setUp(Class repositoryBeanClass) throws Exception {
    this.repository = new StandardBeanRepository(repositoryBeanClass);

    DefaultValueProvider provider = new SchemaDefaultValueProvider();
    this.context = new StandardConfigContext(this.repository, provider, new MockIllegalConfigurationChangeHandler());

    this.builder = TerracottaConfigBuilder.newMinimalInstance();

    this.listener1 = new MockConfigItemListener();
    this.listener2 = new MockConfigItemListener();
  }

  protected final void addListeners(ConfigItem item) {
    item.addListener(this.listener1);
    item.addListener(this.listener2);
  }

  protected final void checkListener(Object oldObject, Object newObject) {
    assertEquals(1, this.listener1.getNumValueChangeds());
    assertEquals(oldObject, this.listener1.getLastOldValue());
    assertEquals(newObject, this.listener1.getLastNewValue());
    assertEquals(1, this.listener2.getNumValueChangeds());
    assertEquals(oldObject, this.listener2.getLastOldValue());
    assertEquals(newObject, this.listener2.getLastNewValue());
    this.listener1.reset();
    this.listener2.reset();
  }

  protected final void checkNoListener() {
    assertEquals(0, this.listener1.getNumValueChangeds());
    assertEquals(0, this.listener2.getNumValueChangeds());
    this.listener1.reset();
    this.listener2.reset();
  }

  public void setConfig() throws Exception {
    TcConfigDocument bean = (TcConfigDocument) new TerracottaDomainConfigurationDocumentBeanFactory()
        .createBean(new ByteArrayInputStream(this.builder.toString().getBytes()), "for test").bean();
    this.repository.setBean(getBeanFromTcConfig(bean.getTcConfig()), "from test config builder");
  }

  protected final void setBean(XmlObject bean) throws XmlException {
    this.repository.setBean(bean, "from test");
  }

  protected final ConfigContext context() throws Exception {
    return this.context;
  }

  protected final TerracottaConfigBuilder builder() throws Exception {
    return this.builder;
  }

  protected final void resetBuilder() throws Exception {
    this.builder = TerracottaConfigBuilder.newMinimalInstance();
  }

  protected abstract XmlObject getBeanFromTcConfig(TcConfig config) throws Exception;

}
