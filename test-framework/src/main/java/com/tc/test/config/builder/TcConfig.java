package com.tc.test.config.builder;

import com.tc.util.PortChooser;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.util.List;

/**
 * @author Ludovic Orban
 */
@XStreamAlias("tc:tc-config")
public class TcConfig {

  @XStreamAlias("xmlns:tc")
  @XStreamAsAttribute
  private String xmlnsTc = "http://www.terracotta.org/config";

  @XStreamAlias("xmlns:xsi")
  @XStreamAsAttribute
  private String xmlnsXsi = "http://www.w3.org/2001/XMLSchema-instance";

  @XStreamAlias("xsi:schemaLocation")
  @XStreamAsAttribute
  private String xsiSchemaLocation = "http://www.terracotta.org/schema/terracotta-8.xsd";

  @XStreamAlias("servers")
  private TcServers servers = new TcServers();

  public List<TcConfigChild> getChildren() {
    return servers.getChildren();
  }

  public TcConfig mirrorGroup(TcMirrorGroup mirrorGroup) {
    this.servers.getChildren().add(mirrorGroup);
    return this;
  }

  public TcConfig mirrorGroup(TcServer... tcServers) {
    TcMirrorGroup tcMirrorGroup = new TcMirrorGroup();
    for (TcServer tcServer : tcServers) {
      tcMirrorGroup.server(tcServer);
    }
    this.servers.getChildren().add(tcMirrorGroup);
    return this;
  }

  public TcConfig restartable(boolean restartable) {
    this.servers.getChildren().add(new Restartable().enabled(restartable));
    return this;
  }

  public boolean isSecure() {
    return this.servers.isSecure();
  }

  public TcConfig secure(boolean secure) {
    this.servers.setSecure(secure);
    return this;
  }


  /**
   * Fills up the config object with missing bits:
   *  <ul>Mirror groups name</ul>
   *  <ul>TC servers name</ul>
   *  <ul>TC servers host</ul>
   *  <ul>TC servers index folder</ul>
   *  <ul>TC servers log folder</ul>
   *  <ul>TC servers data folder</ul>
   *  <ul>TC servers TSA, JMX and TSA group ports</ul>
   *  <ul>TC servers offheap if restartable is set to true</ul>
   */
  public void fillUpConfig() {
    int tempGroupNameIdx = 0;
    int tempServerNameIdx = 0;

    PortChooser portChooser = new PortChooser();

    boolean restartable = false;
    for (TcConfigChild tcConfigChild : servers.getChildren()) {
      if (tcConfigChild instanceof Restartable) {
        restartable = true;
        break;
      }
    }

    for (TcConfigChild tcConfigChild : servers.getChildren()) {
      if (tcConfigChild instanceof TcMirrorGroup) {
        TcMirrorGroup mirrorGroup = (TcMirrorGroup)tcConfigChild;
        tempGroupNameIdx = fillUpMirrorGroup(tempGroupNameIdx, mirrorGroup);

        for (TcMirrorGroupChild tcMirrorGroupChild : mirrorGroup.getChildren()) {
          if (tcMirrorGroupChild instanceof TcServer) {
            TcServer tcServer = (TcServer)tcMirrorGroupChild;
            tempServerNameIdx = fillUpTcServer(tempServerNameIdx, portChooser, tcServer, restartable);
          }
        }
      }

      if (tcConfigChild instanceof TcServer) {
        TcServer tcServer = (TcServer)tcConfigChild;
        tempServerNameIdx = fillUpTcServer(tempServerNameIdx, portChooser, tcServer, restartable);
      }
    }
  }

  private int fillUpMirrorGroup(int tempGroupNameIdx, TcMirrorGroup mirrorGroup) {
    String groupName = mirrorGroup.getGroupName();
    if (groupName == null) {
      groupName = "testGroup" + (tempGroupNameIdx++);
      mirrorGroup.setGroupName(groupName);
    }
    return tempGroupNameIdx;
  }

  private int fillUpTcServer(int tempServerNameIdx, PortChooser portChooser, TcServer tcServer, boolean restartable) {
    String tcServerName = tcServer.getName();
    if (tcServerName == null) {
      tcServerName = "testServer" + (tempServerNameIdx++);
      tcServer.setName(tcServerName);
    }
    String host = tcServer.getHost();
    if (host == null) {
      host = "localhost";
      tcServer.setHost(host);
    }

    if (tcServer.getData() == null) tcServer.setData("data");
    if (tcServer.getIndex() == null) tcServer.setIndex("index");
    if (tcServer.getLogs() == null) tcServer.setLogs("logs");

    if (tcServer.getTsaPort() == 0) tcServer.setTsaPort(portChooser.chooseRandomPort());
    if (tcServer.getJmxPort() == 0) tcServer.setJmxPort(portChooser.chooseRandomPort());
    if (tcServer.getTsaGroupPort() == 0) tcServer.setTsaGroupPort(portChooser.chooseRandomPort());

    if (restartable && tcServer.getOffHeap() == null)
      tcServer.offHeap(new OffHeap().enabled(true).maxDataSize(ClusterManager.DEFAULT_MAX_DATA_SIZE));

    return tempServerNameIdx;
  }

  public TcServer serverAt(int groupIdx, int serverIdx) {
    TcConfigChild tcConfigChild = servers.getChildren().get(groupIdx);
    if (tcConfigChild instanceof TcMirrorGroup) {
      TcMirrorGroup mirrorGroup = (TcMirrorGroup)tcConfigChild;
      TcMirrorGroupChild tcMirrorGroupChild = mirrorGroup.getChildren().get(serverIdx);
      if (tcMirrorGroupChild instanceof TcServer) {
        return (TcServer)tcMirrorGroupChild;
      }
    }
    throw new IllegalArgumentException("No server at " + groupIdx + ":" + serverIdx);
  }
}
