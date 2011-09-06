/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.PhysicalManagedObjectFacade;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * ManagedObjectState for URLs.
 */
public class URLManagedObjectState extends LogicalManagedObjectState {

  private String protocol  = null;
  private String host      = null;
  private int    port      = -1;
  private String authority = null;
  private String userInfo  = null;
  private String path      = null;
  private String query     = null;
  private String ref       = null;

  public URLManagedObjectState(long classID) {
    super(classID);
  }

  protected URLManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo includeIDs) throws IOException {
    while (cursor.next()) {
      LogicalAction action = cursor.getLogicalAction();
      int method = action.getMethod();
      Object[] params = action.getParameters();

      switch (method) {
        case SerializationUtil.URL_SET:
          Assert.assertNotNull(params[0]);
          Assert.assertNotNull(params[1]);
          Assert.assertNotNull(params[2]);
          Assert.assertNotNull(params[3]);
          Assert.assertNotNull(params[4]);
          Assert.assertNotNull(params[5]);
          Assert.assertNotNull(params[6]);
          Assert.assertNotNull(params[7]);
          if (!ObjectID.NULL_ID.equals(params[0])) {
            protocol = params[0].toString();
          }
          if (!ObjectID.NULL_ID.equals(params[1])) {
            host = params[1].toString();
          }
          if (!ObjectID.NULL_ID.equals(params[2])) {
            port = ((Integer) params[2]).intValue();
          }
          if (!ObjectID.NULL_ID.equals(params[3])) {
            authority = params[3].toString();
          }
          if (!ObjectID.NULL_ID.equals(params[4])) {
            userInfo = params[4].toString();
          }
          if (!ObjectID.NULL_ID.equals(params[5])) {
            path = params[5].toString();
          }
          if (!ObjectID.NULL_ID.equals(params[6])) {
            query = params[6].toString();
          }
          if (!ObjectID.NULL_ID.equals(params[7])) {
            ref = params[7].toString();
          }
          break;
        default:
          throw new AssertionError("Invalid action:" + method);
      }
    }
  }

  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    writer.addLogicalAction(SerializationUtil.URL_SET, new Object[] { protocol, host, Integer.valueOf(port), authority,
        userInfo, path, query, ref });
  }

  /**
   * This method returns whether this ManagedObjectState can have references or not. @ return true : The Managed object
   * represented by this state object will never have any reference to other objects. false : The Managed object
   * represented by this state object can have references to other objects.
   */
  @Override
  public boolean hasNoReferences() {
    return true;
  }

  @Override
  protected void addAllObjectReferencesTo(Set refs) {
    return;
  }

  private URL createURLFromState() {
    String file = path;
    if (query != null && query.length() > 0) {
      file = file + "?" + query;
    }
    if (ref != null && ref.length() > 0) {
      file = file + "#" + ref;
    }

    try {
      return new URL(protocol, host, port, file);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    Map dataCopy = new HashMap();
    dataCopy.put("url", createURLFromState());
    return new PhysicalManagedObjectFacade(objectID, ObjectID.NULL_ID, className, dataCopy, false, DNA.NULL_ARRAY_SIZE,
                                           false);
  }

  @Override
  protected void basicWriteTo(ObjectOutput o) throws IOException {
    o.writeUTF(protocol);
    o.writeUTF(host);
    o.writeInt(port);
    o.writeUTF(authority);
    o.writeUTF(userInfo);
    o.writeUTF(path);
    o.writeUTF(query);
    o.writeUTF(ref);
  }

  static URLManagedObjectState readFrom(ObjectInput in) throws IOException {
    URLManagedObjectState state = new URLManagedObjectState(in);
    state.protocol = in.readUTF();
    state.host = in.readUTF();
    state.port = in.readInt();
    state.authority = in.readUTF();
    state.userInfo = in.readUTF();
    state.path = in.readUTF();
    state.query = in.readUTF();
    state.ref = in.readUTF();
    return state;
  }

  @Override
  protected boolean basicEquals(LogicalManagedObjectState o) {
    URLManagedObjectState dms = (URLManagedObjectState) o;
    return dms.protocol.equals(protocol) && dms.host.equals(host) && dms.port == port
           && dms.authority.equals(authority) && dms.userInfo.equals(userInfo) && dms.path.equals(path)
           && dms.query.equals(query) && dms.ref.equals(ref);
  }

  public byte getType() {
    return URL_TYPE;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((authority == null) ? 0 : authority.hashCode());
    result = prime * result + ((host == null) ? 0 : host.hashCode());
    result = prime * result + ((path == null) ? 0 : path.hashCode());
    result = prime * result + port;
    result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
    result = prime * result + ((query == null) ? 0 : query.hashCode());
    result = prime * result + ((ref == null) ? 0 : ref.hashCode());
    result = prime * result + ((userInfo == null) ? 0 : userInfo.hashCode());
    return result;
  }
}