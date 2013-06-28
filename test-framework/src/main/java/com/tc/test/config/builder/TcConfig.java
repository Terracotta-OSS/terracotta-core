package com.tc.test.config.builder;

import com.tc.util.PortChooser;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.File;
import java.util.ArrayList;
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
  private final List<TcConfigChild> children = new ArrayList<TcConfigChild>();

  public List<TcConfigChild> getChildren() {
    return children;
  }

  public TcConfig mirrorGroup(TcMirrorGroup mirrorGroup) {
    this.children.add(mirrorGroup);
    return this;
  }

  public TcConfig restartable(boolean restartable) {
    this.children.add(new Restartable().enabled(restartable));
    return this;
  }



  public void fillUpConfig(File workingDir) {
    int tempGroupNameIdx = 0;
    int tempServerNameIdx = 0;

    PortChooser portChooser = new PortChooser();

    for (TcConfigChild tcConfigChild : children) {
      if (tcConfigChild instanceof TcMirrorGroup) {
        TcMirrorGroup mirrorGroup = (TcMirrorGroup)tcConfigChild;
        tempGroupNameIdx = fillUpMirrorGroup(tempGroupNameIdx, mirrorGroup);

        for (TcMirrorGroupChild tcMirrorGroupChild : mirrorGroup.getChildren()) {
          if (tcMirrorGroupChild instanceof TcServer) {
            TcServer tcServer = (TcServer)tcMirrorGroupChild;
            tempServerNameIdx = fillUpTcServer(workingDir, tempServerNameIdx, portChooser, tcServer);
          }
        }
      }

      if (tcConfigChild instanceof TcServer) {
        TcServer tcServer = (TcServer)tcConfigChild;
        tempServerNameIdx = fillUpTcServer(workingDir, tempServerNameIdx, portChooser, tcServer);
      }
    }
  }

  private int fillUpMirrorGroup(int tempGroupNameIdx, final TcMirrorGroup mirrorGroup) {
    String groupName = mirrorGroup.getGroupName();
    if (groupName == null) {
      groupName = "testGroup" + (tempGroupNameIdx++);
      mirrorGroup.setGroupName(groupName);
    }
    return tempGroupNameIdx;
  }

  private int fillUpTcServer(final File workingDir, int tempServerNameIdx, final PortChooser portChooser, final TcServer tcServer) {
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

    File serverWorkingDir = new File(workingDir, tcServerName);


    if (tcServer.getData() == null) tcServer.setData(new File(serverWorkingDir, "data").getPath());
    if (tcServer.getIndex() == null) tcServer.setIndex(new File(serverWorkingDir, "index").getPath());
    if (tcServer.getLogs() == null) tcServer.setLogs(new File(serverWorkingDir, "logs").getPath());

    if (tcServer.getTsaPort() == 0) tcServer.setTsaPort(portChooser.chooseRandomPort());
    if (tcServer.getJmxPort() == 0) tcServer.setJmxPort(portChooser.chooseRandomPort());
    if (tcServer.getTsaGroupPort() == 0) tcServer.setTsaGroupPort(portChooser.chooseRandomPort());
    return tempServerNameIdx;
  }

  public TcServer server(int groupIdx, int serverIdx) {
    TcConfigChild tcConfigChild = children.get(groupIdx);
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
