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

import com.tc.net.core.security.TCSecurityManager;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.sun.jmx.remote.generic.DefaultConfig;
import com.sun.jmx.remote.generic.ServerSynchroMessageConnection;
import com.sun.jmx.remote.generic.SynchroCallback;
import com.sun.jmx.remote.generic.SynchroMessageConnectionServer;
import com.sun.jmx.remote.generic.SynchroMessageConnectionServerImpl;
import com.sun.jmx.remote.socket.SocketConnectionServer;
import com.tc.async.api.Sink;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.CustomerLogging;
import com.tc.logging.JMXLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2Dumper;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCDumper;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.util.concurrent.TCExceptionResultException;
import com.tc.util.concurrent.TCFuture;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.MBeanServerForwarder;
import javax.management.remote.generic.GenericConnectorServer;
import javax.management.remote.generic.MessageConnectionServer;
import javax.management.remote.message.MBeanServerRequestMessage;
import javax.management.remote.message.Message;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import javax.security.auth.Subject;

public class L2Management extends TerracottaManagement {
  private static final TCLogger               logger = TCLogging.getLogger(L2Management.class);

  protected MBeanServer                       mBeanServer;
  protected JMXConnectorServer                jmxConnectorServer;
  protected final L2ConfigurationSetupManager configurationSetupManager;
  private final TCServerInfoMBean             tcServerInfo;
  private final TCDumper                      tcDumper;
  protected final int                         jmxPort;
  protected final InetAddress                 bindAddress;
  private final Sink                          remoteEventsSink;
  protected final boolean                     listenerEnabled;
  private final TCSecurityManager             securityManager;

  static {
    // LKC-2990 and LKC-3171: Remove the JMX generic optional logging
    Logger.getLogger("javax.management.remote.generic").setLevel(Level.OFF);
    // DEV-1304: ClientCommunicatorAdmin uses a different logger
    Logger.getLogger("javax.management.remote.misc").setLevel(Level.OFF);
  }

  public L2Management(TCServerInfoMBean tcServerInfo, L2ConfigurationSetupManager configurationSetupManager,
                      TCDumper tcDumper, boolean listenerEnabled, InetAddress bindAddr,
                      int port, Sink remoteEventsSink, TCSecurityManager securityManager) throws MBeanRegistrationException, NotCompliantMBeanException,
      InstanceAlreadyExistsException {
    this.tcServerInfo = tcServerInfo;
    this.configurationSetupManager = configurationSetupManager;
    this.tcDumper = tcDumper;
    this.listenerEnabled = listenerEnabled;
    this.bindAddress = bindAddr;
    this.jmxPort = port;
    this.remoteEventsSink = remoteEventsSink;
    this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
    this.securityManager = securityManager;

    registerMBeans();
  }

  protected void validateAuthenticationElement() {
  }

  public synchronized void start() throws Exception {
    if (!listenerEnabled) { return; }
    if(!this.tcServerInfo.isSecure()) {
      unsecureStartup();
    } else {
      secureStartup();
    }

  }

  private void secureStartup() throws Exception {
    if (!listenerEnabled) { return; }

    final boolean legacyAuthEnabled = configurationSetupManager.commonl2Config().authentication();
    Map<String, Object> env = new HashMap<>();
    env.put("jmx.remote.x.server.connection.timeout", Long.valueOf(Long.MAX_VALUE));
    env.put("jmx.remote.server.address.wildcard", "false");
    final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();
    if (configurationSetupManager.isSecure()) {
      Assert.assertNotNull(securityManager);
      if (legacyAuthEnabled) {
        consoleLogger
            .warn("Legacy authentication configured, while security being enabled! Only security level config will be used");
      }
      env.put(JMXConnectorServer.AUTHENTICATOR, new JMXAuthenticator() {

        @Override
        public Subject authenticate(Object credentials) {
          if (credentials == null) {
            throw new SecurityException("You must provide a valid username and password!");
          }
          String username = null;
          char[] password = null;
          if (credentials instanceof Object[]) {
            final Object[] creds = (Object[]) credentials;
            if(creds.length == 2
                && creds[0] instanceof String) {
              username = (String) creds[0];

              if (creds[1] instanceof char[]) {
                password = (char[]) creds[1];
              } else if (creds[1] instanceof String) {
                password = ((String) creds[1]).toCharArray();
              }
            }
          }
          final Principal principal = securityManager.authenticate(username, password);
          if (principal != null) {
            // if the admin role is not found, the subject is created read-only
            return new Subject(!securityManager.isUserInRole(principal, "admin"),
                Collections.singleton(principal),
                Collections.EMPTY_SET,
                Collections.EMPTY_SET);
          } else {
            throw new SecurityException("Username and/or password is not valid!");
          }
        }
      });

      JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
      final SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
      final SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
      RMIJRMPServerImpl server = new RMIJRMPServerImpl(jmxPort, csf, ssf, env);
      jmxConnectorServer = new RMIConnectorServer(url, env, server, mBeanServer);
      jmxConnectorServer.setMBeanServerForwarder(MBSFInvocationHandler.newProxyInstance());
      jmxConnectorServer.start();
      getRMIRegistry(jmxPort, csf, ssf).bind("jmxrmi", server);
      consoleLogger.info("Secured RMI JMX port " + jmxPort);
    } else if (legacyAuthEnabled) {
      String authMsg = "Authentication OFF";
      String credentialsMsg = "";
      String pwd = configurationSetupManager.commonl2Config().authenticationPasswordFile();
      String loginConfig = configurationSetupManager.commonl2Config().authenticationLoginConfigName();
      String access = configurationSetupManager.commonl2Config().authenticationAccessFile();
      if (pwd != null && !new File(pwd).exists()) {
        consoleLogger.error("Password file does not exist: " + pwd);
      }
      if (!new File(access).exists()) {
        consoleLogger.error("Access file does not exist: " + access);
      }
      if (pwd != null) {
        env.put("jmx.remote.x.password.file", pwd);
        credentialsMsg = "Credentials: pwd[" + pwd + "] access[" + access + "]";
      } else if (loginConfig != null) {
        if (!Vm.isJDK16Compliant()) {
          consoleLogger
              .error("JAAS LoginModule support requires version 1.6 or greater of the Java Runtime; all credentials will be accepted");
        } else {
          env.put("jmx.remote.x.login.config", loginConfig);
          credentialsMsg = "Credentials: loginConfig[" + loginConfig + "] access[" + access + "]";
        }
      }
      env.put("jmx.remote.x.access.file", access);
      authMsg = "Authentication ON";
      JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
      RMISocketFactory socketFactory = new BindAddrSocketFactory(bindAddress);
      RMIClientSocketFactory csf = bindAddress.isAnyLocalAddress() ? null : socketFactory;
      RMIJRMPServerImpl server = new RMIJRMPServerImpl(jmxPort, csf, socketFactory, env);
      jmxConnectorServer = new RMIConnectorServer(url, env, server, mBeanServer);
      jmxConnectorServer.start();
      getRMIRegistry(jmxPort, csf, socketFactory).bind("jmxrmi", server);
      String urlHost = bindAddress.getHostAddress();
      consoleLogger.info("JMX Server started. " + authMsg + " - Available at URL["
          + "Service:jmx:rmi:///jndi/rmi://" + urlHost + ":" + jmxPort
          + "/jmxrmi" + "]");
      if (!credentialsMsg.equals("")) consoleLogger.info(credentialsMsg);
    }
  }

  private void unsecureStartup() throws IOException {
    JMXServiceURL url;
    Map<String, Object> env = new HashMap<>();
    env.put("jmx.remote.x.server.connection.timeout", Long.valueOf(Long.MAX_VALUE));
    env.put("jmx.remote.server.address.wildcard", "false");

    validateAuthenticationElement();

    // DEV-1060
    url = new JMXServiceURL("jmxmp", bindAddress.getHostAddress(), jmxPort);

    MessageConnectionServer msgConnectionServer = new SocketConnectionServer(url, env);

    // We use our own connection server classes here so that we can intervene when remote jmx requests come in.
    // Specifically we take every request and instead of processing it directly in the JMX thread, we pass the request
    // off to a thread pool. The whole point of doing is that the JMX runtime likes to use Thread.interrupt() which we
    // don't want happening in our code. Pushing the requests into our own threads provides isolation from these
    // interrupts (see DEV-1955)
    SynchroMessageConnectionServer synchroMessageConnectionServer = new TCSynchroMessageConnectionServer(
                                                                                                         remoteEventsSink,
                                                                                                         msgConnectionServer,
                                                                                                         env);
    env.put(DefaultConfig.SYNCHRO_MESSAGE_CONNECTION_SERVER, synchroMessageConnectionServer);

    jmxConnectorServer = new GenericConnectorServer(env, mBeanServer);
    jmxConnectorServer.start();
    CustomerLogging.getConsoleLogger().info("JMX Server started. Available at URL[" + url + "]");
  }

  public synchronized void stop() throws IOException, InstanceNotFoundException, MBeanRegistrationException {
    unregisterMBeans();

    if (listenerEnabled) {
      jmxConnectorServer.stop();
    }
  }

  @Override
  public Object findMBean(ObjectName objectName, Class<?> mBeanInterface) throws IOException {
    return findMBean(objectName, mBeanInterface, mBeanServer);
  }

  public MBeanServer getMBeanServer() {
    return mBeanServer;
  }

  public JMXConnectorServer getJMXConnServer() {
    if (!listenerEnabled) { throw new IllegalStateException("jmx listener is not enabled"); }
    return jmxConnectorServer;
  }

  protected void registerMBeans() throws MBeanRegistrationException, NotCompliantMBeanException,
      InstanceAlreadyExistsException {
    mBeanServer.registerMBean(tcServerInfo, L2MBeanNames.TC_SERVER_INFO);
    mBeanServer.registerMBean(JMXLogging.getJMXAppender().getMBean(), L2MBeanNames.LOGGER);
    mBeanServer.registerMBean(new L2Dumper(tcDumper, mBeanServer), L2MBeanNames.DUMPER);
  }

  protected void unregisterMBeans() throws InstanceNotFoundException, MBeanRegistrationException {
    mBeanServer.unregisterMBean(L2MBeanNames.TC_SERVER_INFO);
    mBeanServer.unregisterMBean(L2MBeanNames.LOGGER);
    mBeanServer.unregisterMBean(L2MBeanNames.OBJECT_MANAGEMENT);
    mBeanServer.unregisterMBean(L2MBeanNames.DUMPER);
  }

  public static class TCSynchroMessageConnectionServer extends SynchroMessageConnectionServerImpl {

    private final Sink remoteEventsSink;

    public TCSynchroMessageConnectionServer(Sink remoteEventsSink, MessageConnectionServer msServer, Map<String, Object> env) {
      super(msServer, env);
      this.remoteEventsSink = remoteEventsSink;
    }

    @Override
    public ServerSynchroMessageConnection accept() throws IOException {
      return new ServerSynchroMessageConnectionWrapper(remoteEventsSink, super.accept());
    }
  }

  private static class ServerSynchroMessageConnectionWrapper implements ServerSynchroMessageConnection {

    private final ServerSynchroMessageConnection conn;
    private final Sink                           queue;

    public ServerSynchroMessageConnectionWrapper(Sink queue, ServerSynchroMessageConnection conn) {
      this.queue = queue;
      this.conn = conn;
    }

    @Override
    public void close() throws IOException {
      conn.close();
    }

    @Override
    public void connect(@SuppressWarnings("rawtypes") Map env) throws IOException {
      conn.connect(env);
    }

    @Override
    public String getConnectionId() {
      return conn.getConnectionId();
    }

    @Override
    public Subject getSubject() {
      return conn.getSubject();
    }

    @Override
    public void sendOneWay(Message msg) throws IOException, UnsupportedOperationException {
      conn.sendOneWay(msg);
    }

    @Override
    public void setCallback(SynchroCallback cb) {
      conn.setCallback(new SynchroCallbackWrapper(queue, cb, getConnectionId()));
    }
  }

  private static class SynchroCallbackWrapper implements SynchroCallback {

    private final SynchroCallback callback;
    private final Sink<CallbackExecuteContext> remoteEventsSink;
    private final String          connectionId;

    public SynchroCallbackWrapper(Sink<CallbackExecuteContext> remoteEventsSink, SynchroCallback cb, String connectionId) {
      this.remoteEventsSink = remoteEventsSink;
      this.callback = cb;
      this.connectionId = connectionId;
    }

    @Override
    public void connectionException(Exception ie) {
      callback.connectionException(ie);
    }

    @Override
    public Message execute(Message request) {
      if (request instanceof MBeanServerRequestMessage) {
        // log shutdown call
        MBeanServerRequestMessage msrm = (MBeanServerRequestMessage) request;
        Object params[] = msrm.getParams();
        if (params != null && params.length > 1 && "shutdown".equals(params[1])) {
          StringBuilder buf = new StringBuilder();
          buf.append("JMX shutdown request connectionId:" + connectionId + " execute methodId: " + msrm.getMethodId()
                     + " params:");
          for (Object o : params) {
            buf.append(" " + o);
          }
          logger.info(buf);
        }
        TCFuture future = new TCFuture();
        remoteEventsSink.addSingleThreaded(new CallbackExecuteContext(Thread.currentThread().getContextClassLoader(), callback, request, future));

        try {
          return (Message) future.get();
        } catch (InterruptedException e) {
          logger.debug("remote JMX call interrupted");
          Thread.currentThread().interrupt();
          return null;
        } catch (TCExceptionResultException e) {
          throw new RuntimeException(e.getCause());
        }
      }

      return callback.execute(request);
    }
  }

  private static class MBSFInvocationHandler implements InvocationHandler {

    private MBeanServer mbs;

    public static MBeanServerForwarder newProxyInstance() {

      final InvocationHandler handler = new MBSFInvocationHandler();

      final Class<?>[] interfaces =
          new Class[] { MBeanServerForwarder.class };

      Object proxy = Proxy.newProxyInstance(
          MBeanServerForwarder.class.getClassLoader(),
          interfaces,
          handler);

      return MBeanServerForwarder.class.cast(proxy);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {

      final String methodName = method.getName();

      if (methodName.equals("getMBeanServer")) {
        return mbs;
      }

      if (methodName.equals("setMBeanServer")) {
        if (args[0] == null)
          throw new IllegalArgumentException("Null MBeanServer");
        if (mbs != null)
          throw new IllegalArgumentException("MBeanServer object already initialized");
        mbs = (MBeanServer)args[0];
        return null;
      }

      // Retrieve Subject from current AccessControlContext
      AccessControlContext acc = AccessController.getContext();
      Subject subject = Subject.getSubject(acc);

      // Allow operations performed locally on behalf of the connector server itself
      if (subject == null) {
        return method.invoke(mbs, args);
      }

      // Restrict access to "createMBean" and "unregisterMBean" to any user
      if (methodName.equals("createMBean") || methodName.equals("unregisterMBean")) {
        throw new SecurityException("Access denied");
      }

      if (methodRequiresAdminRole(args, methodName) && subject.isReadOnly()) {
        throw new SecurityException("Access denied");
      }

      // Retrieve Principal from Subject
      Set<Principal> principals = subject.getPrincipals(Principal.class);
      if (principals == null || principals.isEmpty()) {
        throw new SecurityException("Access denied");
      }

      // shouldn't this be a list of roles ?
      Principal principal = principals.iterator().next();

      principal.getName();

      // Wanna check for some roles here maybe ?
      return method.invoke(mbs, args);
    }

    // reloadConfiguration and shutdown require the "admin" role
    private boolean methodRequiresAdminRole(Object[] args, String methodName) {
      if ("invoke".equals(methodName)
          && args != null
          && args.length >= 2
          && L2MBeanNames.TC_SERVER_INFO.equals(args[0])
          && "shutdown".equals(args[1])
          ) {
        return true;
      }
      if ("invoke".equals(methodName)
          && args != null
          && args.length >= 2
          && L2MBeanNames.ENTERPRISE_TC_SERVER.equals(args[0])
          && "reloadConfiguration".equals(args[1])
          ) {
        return true;
      }

      return false;
    }
  }
  private static final Map<Integer, Registry> rmiRegistryMap = new HashMap<>();

  /**
   * Keep track of RMI Registries by jmxPort. In 1.5 and forward you can create multiple RMI Registries in a single VM.
   */
  protected static Registry getRMIRegistry(int jmxPort, RMIClientSocketFactory csf, RMIServerSocketFactory ssf)
      throws RemoteException {
    Integer key = Integer.valueOf(jmxPort);
    Registry registry = rmiRegistryMap.get(key);
    if (registry == null) {
      rmiRegistryMap.put(key, registry = LocateRegistry.createRegistry(jmxPort, csf, ssf));
    }
    return registry;
  }

  private static class BindAddrSocketFactory extends RMISocketFactory implements Serializable {
    private final InetAddress bindAddr;

    public BindAddrSocketFactory(InetAddress bindAddress) {
      this.bindAddr = bindAddress;
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
      return new ServerSocket(port, 0, this.bindAddr);
    }

    @Override
    public Socket createSocket(String dummy, int port) throws IOException {
      return new Socket(bindAddr, port);
    }

    @Override
    public int hashCode() {
      return bindAddr.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj == null || getClass() != obj.getClass()) { return false; }

      BindAddrSocketFactory other = (BindAddrSocketFactory)obj;
      return bindAddr.equals(other.bindAddr);
    }
  }

}
