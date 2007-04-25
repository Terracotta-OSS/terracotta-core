/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.eclipse.core.resources.IProject;
import org.terracotta.dso.TcPlugin;

import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.Application;
import com.terracottatech.config.Client;
import com.terracottatech.config.DistributedMethods;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.InstrumentedClasses;
import com.terracottatech.config.Locks;
import com.terracottatech.config.Modules;
import com.terracottatech.config.Roots;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TransientFields;
import com.terracottatech.config.TcConfigDocument.TcConfig;

// NOTE: Elements referenced by hasX() should be treated as READ ONLY!
public final class XmlConfigParentElementProvider {

  private static final String      CLIENTS               = "clients";
  private static final String      MODULES              = "modules";
  private static final String      SERVERS              = "servers";
  private static final String      ROOTS                = "roots";
  private static final String      LOCKS                = "locks";
  private static final String      TRANSIENT_FIELDS     = "transient-fields";
  private static final String      INSTRUMENTED_CLASSES = "instrumented-classes";
  private static final String      DISTRIBUTED_METHODS  = "distributed-methods";
  private static final String      BOOT_CLASSES         = "additional-boot-jar-classes";
  private static final String      APPLICATION          = "application";
  private static final String      DSO_APPLICATION      = "dso-application";

  private final TcConfig           m_config;
  private Application              m_application;
  private DsoApplication           m_dsoApplication;
  private Client                   m_client;
  private Modules                  m_modules;
  private Servers                  m_servers;
  private Roots                    m_roots;
  private Locks                    m_locks;
  private TransientFields          m_transientFields;
  private InstrumentedClasses      m_instrumentedClasses;
  private DistributedMethods       m_distributedMethods;
  private AdditionalBootJarClasses m_additionalBootJarClasses;

  public XmlConfigParentElementProvider(IProject project) {
    this.m_config = TcPlugin.getDefault().getConfiguration(project);
  }

  Client ensureClient() {
    if (hasClient() != null) return m_client;
    return (Client) XmlConfigPersistenceManager.ensureXml(m_config, TcConfig.class, CLIENTS);
  }

  public Client hasClient() {
    if (m_client != null) return m_client;
    return (m_client = m_config.getClients());
  }

  Modules ensureModules() {
    if (hasModules() != null) return m_modules;
    return (Modules) XmlConfigPersistenceManager.ensureXml(ensureClient(), ensureClient().getClass(), MODULES);
  }

  public Modules hasModules() {
    if (m_modules != null) return m_modules;
    return (hasClient() != null) ? (m_modules = m_client.getModules()) : null;
  }

  Servers ensureServers() {
    if (hasServers() != null) return m_servers;
    return (Servers) XmlConfigPersistenceManager.ensureXml(m_config, TcConfig.class, SERVERS);
  }

  public Servers hasServers() {
    if (m_servers != null) return m_servers;
    return (m_servers = m_config.getServers());
  }

  Roots ensureRoots() {
    if (hasRoots() != null) return m_roots;
    return (Roots) XmlConfigPersistenceManager.ensureXml(ensureDsoApplication(), ensureDsoApplication().getClass(),
        ROOTS);
  }

  public Roots hasRoots() {
    if (m_roots != null) return m_roots;
    return (hasDsoApplication() != null) ? (m_roots = m_dsoApplication.getRoots()) : null;
  }

  Locks ensureLocks() {
    if (hasLocks() != null) return m_locks;
    return (Locks) XmlConfigPersistenceManager.ensureXml(ensureDsoApplication(), ensureDsoApplication().getClass(),
        LOCKS);
  }

  public Locks hasLocks() {
    if (m_locks != null) return m_locks;
    return (hasDsoApplication() != null) ? (m_locks = m_dsoApplication.getLocks()) : null;
  }

  TransientFields ensureTransientFields() {
    if (hasTransientFields() != null) return m_transientFields;
    return (TransientFields) XmlConfigPersistenceManager.ensureXml(ensureDsoApplication(), ensureDsoApplication()
        .getClass(), TRANSIENT_FIELDS);
  }

  public TransientFields hasTransientFields() {
    if (m_transientFields != null) return m_transientFields;
    return (hasDsoApplication() != null) ? (m_transientFields = m_dsoApplication.getTransientFields()) : null;
  }

  InstrumentedClasses ensureInstrumentedClasses() {
    if (hasInstrumentedClasses() != null) return m_instrumentedClasses;
    return (InstrumentedClasses) XmlConfigPersistenceManager.ensureXml(ensureDsoApplication(), ensureDsoApplication()
        .getClass(), INSTRUMENTED_CLASSES);
  }

  public InstrumentedClasses hasInstrumentedClasses() {
    if (m_instrumentedClasses != null) return m_instrumentedClasses;
    return (hasDsoApplication() != null) ? (m_instrumentedClasses = m_dsoApplication.getInstrumentedClasses()) : null;
  }

  DistributedMethods ensureDistributedMethods() {
    if (hasDistributedMethods() != null) return m_distributedMethods;
    return (DistributedMethods) XmlConfigPersistenceManager.ensureXml(ensureDsoApplication(), ensureDsoApplication()
        .getClass(), DISTRIBUTED_METHODS);
  }

  public DistributedMethods hasDistributedMethods() {
    if (m_distributedMethods != null) return m_distributedMethods;
    return (hasDsoApplication() != null) ? (m_distributedMethods = m_dsoApplication.getDistributedMethods()) : null;
  }

  AdditionalBootJarClasses ensureAdditionalBootJarClasses() {
    if (hasAdditionalBootJarClasses() != null) return m_additionalBootJarClasses;
    return (AdditionalBootJarClasses) XmlConfigPersistenceManager.ensureXml(ensureDsoApplication(),
        ensureDsoApplication().getClass(), BOOT_CLASSES);
  }

  public AdditionalBootJarClasses hasAdditionalBootJarClasses() {
    if (m_additionalBootJarClasses != null) return m_additionalBootJarClasses;
    return (hasDsoApplication() != null) ? (m_additionalBootJarClasses = m_dsoApplication.getAdditionalBootJarClasses())
        : null;
  }

  Application ensureApplication() {
    if (hasApplication() != null) return m_application;
    return (Application) XmlConfigPersistenceManager.ensureXml(m_config, TcConfig.class, APPLICATION);
  }

  public Application hasApplication() {
    if (m_application != null) return m_application;
    return (m_application = m_config.getApplication());
  }

  DsoApplication ensureDsoApplication() {
    if (hasDsoApplication() != null) return m_dsoApplication;
    return (DsoApplication) XmlConfigPersistenceManager.ensureXml(ensureApplication(), ensureApplication().getClass(),
        DSO_APPLICATION);
  }

  public DsoApplication hasDsoApplication() {
    if (m_dsoApplication != null) return m_dsoApplication;
    return (hasApplication() != null) ? (m_dsoApplication = m_application.getDso()) : null;
  }
}
