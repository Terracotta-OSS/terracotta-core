/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.BaseNewConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.ObjectArrayConfigItem;
import com.tc.config.schema.dynamic.ObjectArrayXPathBasedConfigItem;
import com.terracottatech.config.NonDistributedFields;
import com.terracottatech.config.SpringAppContext;
import com.terracottatech.config.SpringAppContexts;
import com.terracottatech.config.SpringApplication;
import com.terracottatech.config.SpringApps;
import com.terracottatech.config.SpringBean;
import com.terracottatech.config.SpringDistributedEvent;

public class NewSpringApplicationConfigObject extends BaseNewConfigObject implements NewSpringApplicationConfig {

  private final ObjectArrayConfigItem springApps;

  public NewSpringApplicationConfigObject(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(SpringApplication.class);

    this.springApps = new ObjectArrayXPathBasedConfigItem(this.context, ".") {
      protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
        return translateSpringApps(xmlObject);
      }
    };

  }

  public ObjectArrayConfigItem springApps() {
    return this.springApps;
  }

  private static Object translateSpringBeans(XmlObject xmlObject) {
    if (xmlObject == null) return null;

    NonDistributedFields[] springBeans = ((SpringBean) xmlObject).getBeanArray();
    SpringContextBean[] beans = new SpringContextBean[springBeans.length];

    for (int i = 0; i < springBeans.length; i++) {
      String name = springBeans[i].getName();
      String[] fields = springBeans[i].getNonDistributedFieldArray();
      beans[i] = new SpringContextBean(name, fields);
    }

    return beans;
  }

  private static Object translateAppContexts(XmlObject xmlObject) {
    if (xmlObject == null) return null;

    SpringAppContext[] springAppContexts = ((SpringAppContexts) xmlObject).getApplicationContextArray();
    AppContext[] appContexts = new AppContext[springAppContexts.length];

    for (int i = 0; i < springAppContexts.length; i++) {
      SpringAppContext ctx = springAppContexts[i];
      String[] paths = ctx.getPaths().getPathArray();
      SpringDistributedEvent distributedEvent = ctx.getDistributedEvents();
      String[] distributedEvents = null;
      if (distributedEvent != null) {
        distributedEvents = distributedEvent.getDistributedEventArray();
      }
      SpringContextBean[] beans = (SpringContextBean[]) NewSpringApplicationConfigObject.translateSpringBeans(ctx
          .getBeans());
      
      String rootName = ctx.getRootName();
      
      boolean locationInfoEnabled = ctx.getEnableLocationInfo();
      
      appContexts[i] = new AppContext(paths, distributedEvents, beans, rootName, locationInfoEnabled);
    }

    return appContexts;
  }

  private static Object translateSpringApps(XmlObject xmlObject) {
    if (xmlObject == null) return null;

    SpringApps[] springApps = ((SpringApplication) xmlObject).getJeeApplicationArray();
    SpringApp[] springApp = new SpringApp[springApps.length];

    for (int i = 0; i < springApps.length; i++) {
      SpringApps app = springApps[i];
      String name = app.getName();
      boolean sessionSupport = app.getSessionSupport();
      Lock[] locks = (Lock[]) ConfigTranslationHelper.translateLocks(app.getLocks());
      InstrumentedClass[] includes = (InstrumentedClass[]) ConfigTranslationHelper.translateIncludes(app
          .getInstrumentedClasses());

      String[] transientFields = null;
      if (app.getTransientFields() != null) {
        transientFields = app.getTransientFields().getFieldNameArray();
      }
      if (transientFields == null) transientFields = new String[0];

      AppContext[] appContexts = (AppContext[]) NewSpringApplicationConfigObject.translateAppContexts(app
          .getApplicationContexts());

      boolean fastProxy = app.getFastProxy();

      springApp[i] = new SpringApp(sessionSupport, locks, includes, appContexts, name, fastProxy, transientFields);
    }

    return springApp;
  }

}
