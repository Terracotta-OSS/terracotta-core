/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;


public class ClusterModelTest /*implements Runnable, ClientConnectionListener, ServerStateListener, DBBackupListener */{
//  IClusterModel fModel;
//  Object        fLock;
//  boolean       stop;
//
//  ClusterModelTest(IClusterModel model, boolean interactive) {
//    fModel = model;
//    fLock = new Object();
//    if (!interactive) {
//      fModel.addClientConnectionListener(this);
//      fModel.addServerStateListener(this);
//      fModel.addDBBackupListener(this);
//      model.addPropertyChangeListener(new PropertyChangeListener() {
//        public void propertyChange(PropertyChangeEvent evt) {
//          String prop = evt.getPropertyName();
//          try {
//            if (IServer.PROP_CONNECTED.equals(prop)) {
//              if (fModel.isConnected()) {
//                handleConnected();
//              } else {
//                handleDisconnect();
//              }
//            } else if (IClusterNode.PROP_READY.equals(prop)) {
//              handleReady();
//            } else if (IServer.PROP_CONNECT_ERROR.equals(prop)) {
//              if(evt.getNewValue() != null) {
//                handleConnectError();
//              }
//            } else if (IClusterModel.PROP_ACTIVE_SERVER.equals(prop)) {
//              handleActiveServer();
//            }
//          } catch (Exception e) {
//            e.printStackTrace();
//          }
//        }
//      });
//    }
//  }
//
//  private void handleConnected() {
//    if (fModel.isActive()) {
//      handleActivation();
//    } else if (fModel.isPassiveStandby()) {
//      handlePassiveStandby();
//    } else if (fModel.isPassiveUninitialized()) {
//      handlePassiveUninitialized();
//    } else if (fModel.isStarted()) {
//      handleStarting();
//    }
//  }
//
//  private void handleActivation() {
//    println("activated");
//    dumpClusterModel();
//  }
//
//  private void handlePassiveStandby() {
//    println("passive-standby");
//  }
//
//  private void handlePassiveUninitialized() {
//    println("passive-uninitialized");
//  }
//
//  private void handleStarting() {
//    println("starting");
//  }
//
//  private void handleDisconnect() {
//    println("disconnected");
//  }
//
//  private void handleReady() {
//    println("ready=" + fModel.isReady());
//  }
//
//  private void handleConnectError() {
//    println("connectError: " + fModel.getConnectErrorMessage());
//  }
//
//  private void handleActiveServer() {
//    println("activeServer: " + fModel.getActiveServer().getConnectionStatusString());
//  }
//
//  public void stop() {
//    stop = true;
//    fLock.notifyAll();
//  }
//
//  public void run() {
//    Thread t = new Thread() {
//      public void run() {
//        synchronized (fLock) {
//          while (!stop) {
//            try {
//              fLock.wait(1000);
//            } catch (InterruptedException ie) {/**/
//            }
//          }
//        }
//      }
//    };
//    t.start();
//    fModel.setAutoConnect(true);
//    try {
//      t.join();
//    } catch (Exception e) {/**/
//    }
//    println("exiting...");
//  }
//
//  public void dumpClusterModel() {
//    dumpServers();
//    dumpClients();
//    dumpRoots();
//    dumpDBBackupStatus();
//  }
//
//  private void dumpServers() {
//    println("Cluster servers:");
//    for (IServer server : fModel.getClusterServers()) {
//      dumpServer(server);
//    }
//  }
//
//  private void dumpClients() {
//    println("Cluster clients:");
//    for (IClient client : fModel.getClients()) {
//      dumpClient(client);
//    }
//  }
//
//  private void dumpServer(IServer server) {
//    println(" " + server.getConnectionStatusString());
//  }
//
//  private void dumpClient(IClient client) {
//    println(" " + client.toString());
//  }
//
//  private void listRoots() {
//    for (IBasicObject root : fModel.getRoots()) {
//      println(root.toString());
//    }
//  }
//
//  private void dumpRoots() {
//    Set<ObjectID> seenSet = new HashSet<ObjectID>();
//    for (IBasicObject root : fModel.getRoots()) {
//      dumpBasicObject(0, root, seenSet);
//    }
//  }
//
//  private static interface ObjectTester {
//    boolean matches(IObject o);
//  }
//
//  private static class LiteralStringTester implements ObjectTester {
//    private String fLiteral;
//
//    LiteralStringTester(String literal) {
//      fLiteral = literal;
//    }
//
//    public boolean matches(IObject o) {
//      return o.toString().contains(fLiteral);
//    }
//  }
//
//  private static class PatternStringTester implements ObjectTester {
//    private String fPattern;
//
//    PatternStringTester(String pattern) {
//      fPattern = pattern;
//    }
//
//    public boolean matches(IObject o) {
//      return o.toString().matches(fPattern);
//    }
//  }
//
//  private static class ObjectIDTester implements ObjectTester {
//    private long fObjectId;
//
//    ObjectIDTester(long oid) {
//      fObjectId = oid;
//    }
//
//    public boolean matches(IObject o) {
//      if (o instanceof IBasicObject) {
//        ObjectID oid = ((IBasicObject) o).getObjectID();
//        return oid != null && oid.toLong() == fObjectId;
//      }
//      return false;
//    }
//  }
//
//  private void findObject(ObjectTester tester) {
//    Set<ObjectID> seenSet = new HashSet<ObjectID>();
//    for (IBasicObject root : fModel.getRoots()) {
//      IObject o = findObject(root, tester, seenSet);
//      if (o != null) {
//        List<IObject> list = new ArrayList<IObject>();
//        IObject parent = o;
//        while (parent != null) {
//          list.add(parent);
//          parent = parent.getParent();
//        }
//        for (int i = list.size() - 1, j = 0; i >= 0; i--, j++) {
//          printWhitespace(j);
//          println(list.get(i).toString());
//        }
//      }
//    }
//  }
//
//  private IObject findObject(IObject o, ObjectTester tester, Set<ObjectID> seenSet) {
//    ObjectID oid = o.getObjectID();
//    if (oid != null && seenSet.contains(oid)) return null;
//    seenSet.add(oid);
//    if (tester.matches(o)) { return o; }
//    if (o instanceof IBasicObject) {
//      IBasicObject basicObject = (IBasicObject) o;
//      basicObject.refresh();
//      int fieldCount = basicObject.getFieldCount();
//      for (int i = 0; i < fieldCount; i++) {
//        if ((o = findObject(basicObject.getField(i), tester, seenSet)) != null) { return o; }
//      }
//    }
//    return null;
//  }
//
//  private void dumpDBBackupStatus() {
//    boolean backupEnabled = fModel.isDBBackupEnabled();
//    println("DBBackup enabled=" + backupEnabled);
//    if (backupEnabled) {
//      boolean backupRunning = fModel.isDBBackupRunning();
//      if (backupRunning) {
//        println("Backup is running");
//      }
//    }
//  }
//
//  private void printWhitespace(int count) {
//    for (int i = 0; i < count; i++)
//      print(' ');
//  }
//
//  private void dumpBasicObject(int offset, IBasicObject basicObject, Set<ObjectID> seenSet) {
//    printWhitespace(offset);
//    print(basicObject.toString());
//    ObjectID oid = basicObject.getObjectID();
//    if (oid != null && seenSet.contains(oid)) {
//      println(" ^^^");
//      return;
//    }
//    seenSet.add(oid);
//    println();
//    basicObject.refresh();
//    int fieldCount = basicObject.getFieldCount();
//    for (int i = 0; i < fieldCount; i++) {
//      dumpObject(offset + 1, basicObject.getField(i), seenSet);
//    }
//  }
//
//  private void dumpObject(int offset, IObject object, Set<ObjectID> seenSet) {
//    if (object instanceof IBasicObject) {
//      dumpBasicObject(offset, (IBasicObject) object, seenSet);
//    } else if (object instanceof IMapEntry) {
//      dumpMapEntry(offset, (IMapEntry) object, seenSet);
//    }
//  }
//
//  private void dumpMapEntry(int offset, IMapEntry mapEntry, Set<ObjectID> seenSet) {
//    printWhitespace(offset);
//    println(mapEntry.toString());
//    dumpObject(offset + 1, mapEntry.getKey(), seenSet);
//    IObject value = mapEntry.getValue();
//    if (value != null) dumpObject(offset + 1, value, seenSet);
//  }
//
//  public void clientConnected(IClient client) {
//    println("client connected: " + client);
//  }
//
//  public void clientDisconnected(IClient client) {
//    println("client disconnected: " + client);
//  }
//
//  public void serverStateChanged(IServer server, PropertyChangeEvent pce) {
//    String prop = pce.getPropertyName();
//
//    if (!IServer.PROP_CONNECT_ERROR.equals(prop)) {
//      println(" " + server.getConnectionStatusString() + " [" + prop + "=" + pce.getNewValue() + "]");
//    }
//  }
//
//  public void backupCompleted() {
//    println("backup completed");
//  }
//
//  public void backupFailed(String message) {
//    println("backup failed: " + message);
//  }
//
//  public void backupProgress(int percentCopied) {
//    println(MessageFormat.format("backup {0}% completed", percentCopied));
//  }
//
//  public void backupStarted() {
//    println("backup started");
//  }
//
//  public static final void main(String[] args) throws Exception {
//    String host;
//    int port;
//    ArrayList<String> argList = new ArrayList(Arrays.asList(args));
//    boolean interactive = argList.remove("-i");
//    if (argList.size() > 1) {
//      host = argList.remove(0);
//      port = Integer.parseInt(argList.remove(0));
//    } else {
//      host = "localhost";
//      port = 9520;
//    }
//    ClusterModel clusterModel = new ClusterModel(host, port, false);
//    ClusterModelTest test = new ClusterModelTest(clusterModel, interactive);
//    if (interactive) {
//      clusterModel.setAutoConnect(true);
//      print("Waiting for cluster to be ready.");
//      while (!clusterModel.isReady()) {
//        Thread.sleep(1000);
//        println(".");
//      }
//      println("ready.");
//      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//      while (true) {
//        print("'?' for help > ");
//        String line = br.readLine();
//        String[] tokens = StringUtils.split(line);
//        if (tokens == null || tokens.length == 0) continue;
//        List<String> list = new ArrayList(Arrays.asList(tokens));
//        String cmd = list.remove(0);
//        if ("da".equals(cmd)) {
//          test.dumpClusterModel();
//        } else if ("dr".equals(cmd)) {
//          test.dumpRoots();
//        } else if ("lr".equals(cmd)) {
//          test.listRoots();
//        } else if ("dc".equals(cmd)) {
//          test.dumpClients();
//        } else if ("ds".equals(cmd)) {
//          test.dumpServers();
//        } else if ("fo".equals(cmd)) {
//          long oid = Long.parseLong(list.remove(0));
//          test.findObject(new ObjectIDTester(oid));
//        } else if ("fp".equals(cmd)) {
//          test.findObject(new PatternStringTester(StringUtils.join(list.iterator(), ' ')));
//        } else if ("fl".equals(cmd)) {
//          test.findObject(new LiteralStringTester(StringUtils.join(list.iterator(), ' ')));
//        } else if ("q".equals(cmd)) {
//          break;
//        } else if ("?".equals(cmd)) {
//          println("da=dump all");
//          println("lr=list roots");
//          println("dr=dump roots");
//          println("fo=find object <objectID>");
//          println("fl=find object <literal>");
//          println("fp=find object <regex-pattern>");
//          println("dc=dump clients");
//          println("ds=dump servers");
//          println("q=quit");
//        } else {
//          println("unknown command: " + line);
//        }
//      }
//    } else {
//      Thread t = new Thread(test);
//      t.start();
//    }
//  }
//
//  private static void print(Object o) {
//    System.out.print(o);
//  }
//
//  private static void println(Object o) {
//    System.out.println(o);
//  }
//
//  private static void println() {
//    System.out.println();
//  }
//
}
