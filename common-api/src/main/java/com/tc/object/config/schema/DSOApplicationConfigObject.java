/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import org.apache.xmlbeans.XmlBoolean;
import org.apache.xmlbeans.XmlException;

import com.tc.config.schema.BaseConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.license.LicenseManager;
import com.tc.util.Assert;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.Application;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Root;
import com.terracottatech.config.Roots;
import com.terracottatech.config.TcConfigDocument.TcConfig;
import com.terracottatech.config.TransientFields;
import com.terracottatech.config.WebApplications;

public class DSOApplicationConfigObject extends BaseConfigObject implements DSOApplicationConfig {
  private final InstrumentedClass[]                instrumentedClasses;
  private final TransientFields                    transientFields;
  private final com.tc.object.config.schema.Root[] roots;
  private final AdditionalBootJarClasses           additionalBootJarClasses;
  private final boolean                            supportSharingThroughReflection;
  private final WebApplications                    webApplications;

  public DSOApplicationConfigObject(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(DsoApplication.class);
    DsoApplication dsoApplication = (DsoApplication) this.context.bean();

    if (!dsoApplication.isSetInstrumentedClasses()) {
      dsoApplication.addNewInstrumentedClasses();
    }
    this.instrumentedClasses = ConfigTranslationHelper.translateIncludes(dsoApplication.getInstrumentedClasses());

    if (!dsoApplication.isSetLocks()) {
      dsoApplication.addNewLocks();
    }

    if (!dsoApplication.isSetRoots()) {
      dsoApplication.addNewRoots();
    }
    this.roots = translateRoots(dsoApplication.getRoots());
    if (LicenseManager.enterpriseEdition() && roots.length > 0) {
      LicenseManager.verifyRootCapability();
    }

    if (!dsoApplication.isSetTransientFields()) {
      dsoApplication.addNewTransientFields();
    }
    this.transientFields = dsoApplication.getTransientFields();

    if (!dsoApplication.isSetAdditionalBootJarClasses()) {
      dsoApplication.addNewAdditionalBootJarClasses();
    }
    this.additionalBootJarClasses = dsoApplication.getAdditionalBootJarClasses();

    if (!dsoApplication.isSetWebApplications()) {
      dsoApplication.addNewWebApplications();
    }
    this.webApplications = dsoApplication.getWebApplications();

    this.supportSharingThroughReflection = dsoApplication.getDsoReflectionEnabled();
  }

  public WebApplications webApplications() {
    return this.webApplications;
  }

  public InstrumentedClass[] instrumentedClasses() {
    return this.instrumentedClasses;
  }

  public TransientFields transientFields() {
    return this.transientFields;
  }

  public Lock[] locks() {
    DsoApplication dsoApplication = (DsoApplication) this.context.bean();
    return ConfigTranslationHelper.translateLocks(dsoApplication.getLocks());
  }

  public com.tc.object.config.schema.Root[] roots() {
    return this.roots;
  }

  public AdditionalBootJarClasses additionalBootJarClasses() {
    return this.additionalBootJarClasses;
  }

  public boolean supportSharingThroughReflection() {
    return this.supportSharingThroughReflection;
  }

  private static com.tc.object.config.schema.Root[] translateRoots(Roots roots) {

    com.tc.object.config.schema.Root[] out;
    Root[] theRoots = roots.getRootArray();
    out = new com.tc.object.config.schema.Root[theRoots == null ? 0 : theRoots.length];
    for (int i = 0; i < out.length; ++i) {
      out[i] = new com.tc.object.config.schema.Root(theRoots[i].getRootName(), theRoots[i].getFieldName());
    }
    return out;
  }

  public static void initializeApplication(TcConfig config, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    Application application;
    if (!config.isSetApplication()) {
      application = config.addNewApplication();
    } else {
      application = config.getApplication();
    }
    initializeApplicationDso(application, defaultValueProvider);
  }

  private static void initializeApplicationDso(Application application, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    if (!application.isSetDso()) {
      application.addNewDso();
    }

    initializeDsoReflectionEnabled(application, defaultValueProvider);
  }

  private static void initializeDsoReflectionEnabled(Application application, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    Assert.assertTrue(application.isSetDso());

    DsoApplication dsoApplication = application.getDso();
    if (!dsoApplication.isSetDsoReflectionEnabled()) {
      dsoApplication.setDsoReflectionEnabled(getDefaultDsoReflectionEnabled(application, defaultValueProvider));
    }
  }

  private static boolean getDefaultDsoReflectionEnabled(Application application,
                                                        DefaultValueProvider defaultValueProvider) throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(application.schemaType(), "dso/dso-reflection-enabled"))
        .getBooleanValue();
  }

}
