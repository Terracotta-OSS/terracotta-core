/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.repository;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.listen.ConfigurationChangeListener;
import com.tc.config.schema.listen.ConfigurationChangeListenerSet;
import com.tc.util.Assert;

/**
 * A {@link BeanRepository} that selects out a child of the parent bean.
 */
public class ChildBeanRepository implements BeanRepository, ConfigurationChangeListener {

  private final BeanRepository                 parent;
  private final Class                          requiredBeanClass;
  private final ChildBeanFetcher               childFetcher;
  private final ConfigurationChangeListenerSet listeners;

  public ChildBeanRepository(BeanRepository parent, Class requiredBeanClass, ChildBeanFetcher childFetcher) {
    Assert.assertNotNull(parent);
    Assert.assertNotNull(requiredBeanClass);
    Assert.assertNotNull(childFetcher);

    this.parent = parent;
    this.requiredBeanClass = requiredBeanClass;
    this.childFetcher = childFetcher;
    this.listeners = new ConfigurationChangeListenerSet();

    this.parent.addListener(this);
  }

  public SchemaType rootBeanSchemaType() {
    return StandardBeanRepository.getTypeFieldFrom(this.requiredBeanClass);
  }

  public void ensureBeanIsOfClass(Class theClass) {
    Assert.eval(theClass.isAssignableFrom(requiredBeanClass));
  }

  public XmlObject bean() {
    XmlObject parentBean = this.parent.bean();
    if (parentBean == null) return null;
    XmlObject out = this.childFetcher.getChild(parentBean);
    if (out != null) {
      Assert.eval("Child bean fetcher returned " + "a " + out.getClass() + ", not a " + this.requiredBeanClass,
                  this.requiredBeanClass.isInstance(out));
    }
    return out;
  }

  public void addListener(ConfigurationChangeListener listener) {
    Assert.assertNotNull(listener);
    this.listeners.addListener(listener);
  }

  public void configurationChanged(XmlObject oldConfig, XmlObject newConfig) {
    XmlObject oldChild = oldConfig == null ? null : this.childFetcher.getChild(oldConfig);
    XmlObject newChild = newConfig == null ? null : this.childFetcher.getChild(newConfig);

    Assert.eval(newChild == null || this.requiredBeanClass.isInstance(newChild));

    if (oldChild != newChild) this.listeners.configurationChanged(oldChild, newChild);
  }

}
