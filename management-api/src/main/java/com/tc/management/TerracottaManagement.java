/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management;

import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.MBeanNames;
import com.tc.net.TCSocketAddress;
import com.tc.util.UUID;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;

public abstract class TerracottaManagement {

  private static final ManagementResources MANAGEMENT_RESOURCES = new ManagementResources();

  public static class Type {

    private static final Map typesByName      = Collections.synchronizedMap(new HashMap());
    public static final Type DsoClient        = new Type(MANAGEMENT_RESOURCES.getDsoClientType());
    public static final Type Sessions         = new Type(MANAGEMENT_RESOURCES.getSessionsType());
    public static final Type Server           = new Type(MANAGEMENT_RESOURCES.getTerracottaServerType());
    public static final Type Cluster          = new Type(MANAGEMENT_RESOURCES.getTerracottaClusterType());
    public static final Type Agent            = new Type(MANAGEMENT_RESOURCES.getTerracottaAgentType());
    public static final Type Tim              = new Type(MANAGEMENT_RESOURCES.getTerracottaTimType());
    public static final Type TcOperatorEvents = new Type(MANAGEMENT_RESOURCES.getTerracottaOperatorEventType());

    private final String     type;

    private Type(final String type) {
      this.type = type;
      typesByName.put(type, this);
    }

    @Override
    public String toString() {
      return type;
    }

    static Type getType(final String name) {
      return (Type) typesByName.get(name);
    }
  }

  public static class Subsystem {

    private static final Map      subsystemByName  = Collections.synchronizedMap(new HashMap());
    public static final Subsystem Tx               = new Subsystem(MANAGEMENT_RESOURCES.getTransactionSubsystem());
    public static final Subsystem Locking          = new Subsystem(MANAGEMENT_RESOURCES.getLockingSubsystem());
    public static final Subsystem ObjectManagement = new Subsystem(MANAGEMENT_RESOURCES.getObjectManagementSubsystem());
    public static final Subsystem Logging          = new Subsystem(MANAGEMENT_RESOURCES.getLoggingSubsystem());
    public static final Subsystem Statistics       = new Subsystem(MANAGEMENT_RESOURCES.getStatisticsSubsystem());
    public static final Subsystem None             = new Subsystem(MANAGEMENT_RESOURCES.getNoneSubsystem());

    private final String          subsystem;

    private Subsystem(final String subsystem) {
      this.subsystem = subsystem;
      subsystemByName.put(subsystem, this);
    }

    @Override
    public String toString() {
      return subsystem;
    }

    static Subsystem getSubsystem(final String name) {
      return (Subsystem) subsystemByName.get(name);
    }
  }

  public static interface MBeanKeys {
    public static final String TYPE            = "type";
    public static final String MBEAN_NODE      = "node";
    public static final String MBEAN_NODE_NAME = "node-name";
    public static final String SUBSYSTEM       = "subsystem";
    public static final String NAME            = "name";
  }

  public enum MBeanDomain {
    PUBLIC(MANAGEMENT_RESOURCES.getPublicMBeanDomain()), INTERNAL(MANAGEMENT_RESOURCES.getInternalMBeanDomain()), TIM(
        MANAGEMENT_RESOURCES.getTimMBeanDomain());

    private final String value;

    private MBeanDomain(final String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  private static final String COMMA           = ",";
  private static final String COLON           = ":";
  private static final String EQUALS          = "=";
  private static final String SLASH           = "/";

  private static final String NODE_PREFIX_KEY = "clients";
  private static final String NODE_PREFIX     = NODE_PREFIX_KEY + EQUALS + "Clients";

  private static final String NODE_NAME       = System.getProperty(MANAGEMENT_RESOURCES.getNodeNameSystemProperty());

  public static ObjectName createObjectName(final Type type, final Subsystem subsystem,
                                            final TCSocketAddress remoteBeanHome, final String uiFriendlyName,
                                            final MBeanDomain domain) throws MalformedObjectNameException {
    final StringBuffer objName = new StringBuffer(null == domain ? MBeanDomain.INTERNAL.toString() : domain.toString());
    objName.append(COLON);
    if (NODE_NAME != null || remoteBeanHome != null) {
      objName.append(NODE_PREFIX);
      if (NODE_NAME != null) {
        objName.append(COMMA).append(MBeanKeys.MBEAN_NODE_NAME).append(EQUALS).append(NODE_NAME);
      }
      if (remoteBeanHome != null) {
        addNodeInfo(objName, remoteBeanHome);
      }
      objName.append(COMMA);
    }
    objName.append(MBeanKeys.TYPE).append(EQUALS).append(type);
    if (subsystem != Subsystem.None) {
      objName.append(COMMA).append(MBeanKeys.SUBSYSTEM).append(EQUALS).append(subsystem);
    }
    objName.append(COMMA).append(MBeanKeys.NAME).append(EQUALS).append(uiFriendlyName);
    return new ObjectName(objName.toString());
  }

  private static void addNodeInfo(final StringBuffer objName, final TCSocketAddress addr) {
    String remoteHost = addr.getAddress().getCanonicalHostName();
    int remotePort = addr.getPort();
    objName.append(COMMA).append(MBeanKeys.MBEAN_NODE).append(EQUALS).append(remoteHost).append(SLASH)
        .append(remotePort);
  }

  public static ObjectName addNodeInfo(ObjectName objName, final TCSocketAddress addr)
      throws MalformedObjectNameException {
    if (objName.getKeyProperty(MBeanKeys.MBEAN_NODE) != null) {
      Hashtable kpl = objName.getKeyPropertyList();
      kpl.remove(MBeanKeys.MBEAN_NODE);
      objName = ObjectName.getInstance(objName.getDomain(), kpl);
    }
    StringBuffer sb = new StringBuffer(objName.getCanonicalName());
    if (objName.getKeyProperty(NODE_PREFIX_KEY) == null) {
      sb.append(COMMA).append(NODE_PREFIX);
    }
    addNodeInfo(sb, addr);
    return new ObjectName(sb.toString());
  }

  private static void addNodeInfo(final StringBuffer objName, final UUID id) {
    objName.append(COMMA).append(MBeanKeys.MBEAN_NODE).append(EQUALS).append(id);
  }

  public static ObjectName addNodeInfo(ObjectName objName, final UUID id) throws MalformedObjectNameException {
    if (objName.getKeyProperty(MBeanKeys.MBEAN_NODE) != null) {
      Hashtable kpl = objName.getKeyPropertyList();
      kpl.remove(MBeanKeys.MBEAN_NODE);
      objName = ObjectName.getInstance(objName.getDomain(), kpl);
    }
    StringBuffer sb = new StringBuffer(objName.getCanonicalName());
    if (objName.getKeyProperty(NODE_PREFIX_KEY) == null) {
      sb.append(COMMA).append(NODE_PREFIX);
    }
    addNodeInfo(sb, id);
    return new ObjectName(sb.toString());
  }

  public abstract Object findMBean(final ObjectName objectName, final Class mBeanInterface) throws Exception;

  public static final Object findMBean(final ObjectName objectName, final Class mBeanInterface,
                                       final MBeanServerConnection mBeanServer) throws IOException {
    final Set matchingBeans = mBeanServer.queryMBeans(objectName, null);
    final Iterator beanPos = matchingBeans.iterator();
    if (beanPos.hasNext()) { return MBeanServerInvocationHandler.newProxyInstance(mBeanServer, objectName,
                                                                                  mBeanInterface, false); }
    return null;
  }

  public static final QueryExp matchAllTerracottaMBeans(UUID id, String[] tunneledDomains) {
    try {
      QueryExp query = Query.or(new ObjectName(MBeanDomain.PUBLIC + ":*,node=" + id),
                                new ObjectName(MBeanDomain.INTERNAL + ":*,node=" + id));
      if (tunneledDomains != null) {
        for (String tunneledDomain : tunneledDomains) {
          query = Query.or(query, new ObjectName(tunneledDomain + ":*,node=" + id));
        }
      }
      return query;
    } catch (MalformedObjectNameException e) {
      throw new RuntimeException(e);
    }
  }

  public static final QueryExp matchAllTimMBeans() {
    try {
      return new ObjectName(MBeanDomain.TIM + ":*");
    } catch (MalformedObjectNameException e) {
      throw new RuntimeException(e);
    }
  }

  public static final String quoteIfNecessary(final String objectNamePart) {
    if (objectNamePart.matches("[,=:*?\"']")) { return ObjectName.quote(objectNamePart); }
    return objectNamePart;
  }

  public static final Set getAllL1DumperMBeans(final MBeanServerConnection mbs) throws MalformedObjectNameException,
      NullPointerException, IOException {
    return mbs.queryNames(new ObjectName(MBeanNames.L1DUMPER_INTERNAL.getCanonicalName() + ",*"), null);
  }

  public static final Set getAllL2DumperMBeans(final MBeanServerConnection mbs) throws MalformedObjectNameException,
      NullPointerException, IOException {
    return mbs.queryNames(new ObjectName(L2MBeanNames.DUMPER.getCanonicalName() + ",*"), null);
  }
}
