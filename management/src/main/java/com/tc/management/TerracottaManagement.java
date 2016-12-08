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
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;

public abstract class TerracottaManagement {

  private static final ManagementResources MANAGEMENT_RESOURCES = new ManagementResources();

  public static class Type {

    private static final Map<String, Type> typesByName      = Collections.synchronizedMap(new HashMap<String, Type>());
    public static final Type Client        = new Type(MANAGEMENT_RESOURCES.getDsoClientType());
    public static final Type Sessions         = new Type(MANAGEMENT_RESOURCES.getSessionsType());
    public static final Type Server           = new Type(MANAGEMENT_RESOURCES.getTerracottaServerType());
    public static final Type Cluster          = new Type(MANAGEMENT_RESOURCES.getTerracottaClusterType());
    public static final Type Agent            = new Type(MANAGEMENT_RESOURCES.getTerracottaAgentType());
    public static final Type Tim              = new Type(MANAGEMENT_RESOURCES.getTerracottaTimType());
    public static final Type TcOperatorEvents = new Type(MANAGEMENT_RESOURCES.getTerracottaOperatorEventType());

    private final String     type;

    private Type(String type) {
      this.type = type;
      typesByName.put(type, this);
    }

    @Override
    public String toString() {
      return type;
    }

    static Type getType(String name) {
      return typesByName.get(name);
    }
  }

  public static class Subsystem {

    private static final Map<String, Subsystem>      subsystemByName  = Collections.synchronizedMap(new HashMap<String, Subsystem>());
    public static final Subsystem Tx               = new Subsystem(MANAGEMENT_RESOURCES.getTransactionSubsystem());
    public static final Subsystem Locking          = new Subsystem(MANAGEMENT_RESOURCES.getLockingSubsystem());
    public static final Subsystem ObjectManagement = new Subsystem(MANAGEMENT_RESOURCES.getObjectManagementSubsystem());
    public static final Subsystem Logging          = new Subsystem(MANAGEMENT_RESOURCES.getLoggingSubsystem());
    public static final Subsystem Statistics       = new Subsystem(MANAGEMENT_RESOURCES.getStatisticsSubsystem());
    public static final Subsystem None             = new Subsystem(MANAGEMENT_RESOURCES.getNoneSubsystem());

    private final String          subsystem;

    private Subsystem(String subsystem) {
      this.subsystem = subsystem;
      subsystemByName.put(subsystem, this);
    }

    @Override
    public String toString() {
      return subsystem;
    }

    static Subsystem getSubsystem(String name) {
      return subsystemByName.get(name);
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

    private MBeanDomain(String value) {
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
  private static final String UNDERSCORE      = "_";

  private static final String NODE_PREFIX_KEY = "clients";
  private static final String NODE_PREFIX     = NODE_PREFIX_KEY + EQUALS + "Clients";

  private static final String NODE_NAME       = System.getProperty(MANAGEMENT_RESOURCES.getNodeNameSystemProperty());

  public static ObjectName createObjectName(Type type, Subsystem subsystem,
                                            TCSocketAddress remoteBeanHome, String uiFriendlyName,
                                            MBeanDomain domain) throws MalformedObjectNameException {
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
    objName.append(MBeanKeys.NAME).append(EQUALS).append(uiFriendlyName);
    if (type != null) {
      objName.append(COMMA).append(MBeanKeys.TYPE).append(EQUALS).append(type);
    }
    if (subsystem != Subsystem.None) {
      objName.append(COMMA).append(MBeanKeys.SUBSYSTEM).append(EQUALS).append(subsystem);
    }
    try {
      return new ObjectName(objName.toString());
    } catch (MalformedObjectNameException mal) {
      throw new MalformedObjectNameException(objName.toString() + " " + mal.getMessage());
    }
  }

  private static void addNodeInfo(StringBuffer objName, TCSocketAddress addr) {
    objName.append(COMMA).append(MBeanKeys.MBEAN_NODE).append(EQUALS).append(buildNodeId(addr));
  }

  public static String buildNodeId(TCSocketAddress addr) {
    String remoteHost = addr.getAddress().getCanonicalHostName();
    int remotePort = addr.getPort();
    return remoteHost + UNDERSCORE + remotePort;
  }

  public static ObjectName addNodeInfo(ObjectName objName, TCSocketAddress addr)
      throws MalformedObjectNameException {
    if (objName.getKeyProperty(MBeanKeys.MBEAN_NODE) != null) {
      Hashtable<String, String> kpl = objName.getKeyPropertyList();
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

  private static void addNodeInfo(StringBuffer objName, UUID id) {
    objName.append(COMMA).append(MBeanKeys.MBEAN_NODE).append(EQUALS).append(id);
  }

  public static ObjectName addNodeInfo(ObjectName objName, UUID id) throws MalformedObjectNameException {
    if (objName.getKeyProperty(MBeanKeys.MBEAN_NODE) != null) {
      Hashtable<String, String> kpl = objName.getKeyPropertyList();
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

  public abstract Object findMBean(ObjectName objectName, Class<?> mBeanInterface) throws Exception;

  public static final Object findMBean(ObjectName objectName, Class<?> mBeanInterface,
                                       MBeanServerConnection mBeanServer) throws IOException {
    final Set<ObjectInstance> matchingBeans = mBeanServer.queryMBeans(objectName, null);
    final Iterator<ObjectInstance> beanPos = matchingBeans.iterator();
    if (beanPos.hasNext()) { return MBeanServerInvocationHandler.newProxyInstance(mBeanServer, objectName,
                                                                                  mBeanInterface, false); }
    return null;
  }

  public static final QueryExp matchAllTerracottaMBeans(UUID id) {
    try {
      QueryExp query = Query.or(new ObjectName(MBeanDomain.PUBLIC + ":*,node=" + id),
                                new ObjectName(MBeanDomain.INTERNAL + ":*,node=" + id));
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

  public static final String quoteIfNecessary(String objectNamePart) {
    if (objectNamePart.matches("[,=:*?\"']")) { return ObjectName.quote(objectNamePart); }
    return objectNamePart;
  }

  public static final Set<ObjectName> getAllL1DumperMBeans(MBeanServerConnection mbs) throws MalformedObjectNameException,
      NullPointerException, IOException {
    return mbs.queryNames(new ObjectName(MBeanNames.L1DUMPER_INTERNAL.getCanonicalName() + ",*"), null);
  }

  public static final Set<ObjectName> getAllL2DumperMBeans(MBeanServerConnection mbs) throws MalformedObjectNameException,
      NullPointerException, IOException {
    return mbs.queryNames(new ObjectName(L2MBeanNames.DUMPER.getCanonicalName() + ",*"), null);
  }
}
