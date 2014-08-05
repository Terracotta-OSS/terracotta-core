/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.applicator.ChangeApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.platform.PlatformService;
import com.tc.util.ReflectionUtil;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ConcurrentModificationException;

/**
 * Peer of a Class under management.
 * <p>
 * This is used to cache the fields of each class by type.
 * 
 * @author orion
 */
public class TCClassImpl implements TCClass {
  private final static TCLogger     logger      = TCLogging.getLogger(TCClassImpl.class);

  /**
   * Peer java class that this TCClass represents.
   */
  private final Class               peer;
  private final TCClassFactory      clazzFactory;
  private final ChangeApplicator    applicator;
  private final boolean             useNonDefaultConstructor;
  private final ClientObjectManager objectManager;
  private Constructor               constructor = null;

  TCClassImpl(final TCClassFactory clazzFactory, final ClientObjectManager objectManager, final Class peer,
              final boolean useNonDefaultConstructor) {
    this.clazzFactory = clazzFactory;
    this.objectManager = objectManager;
    this.peer = peer;
    this.applicator = createApplicator();
    this.useNonDefaultConstructor = useNonDefaultConstructor;
  }

  @Override
  public Class getPeerClass() {
    return this.peer;
  }

  private ChangeApplicator createApplicator() {
    return this.clazzFactory.createApplicatorFor(this);
  }

  @Override
  public void hydrate(final TCObject tcObject, final DNA dna, final Object pojo, final boolean force)
      throws IOException, ClassNotFoundException {
    // Okay...long story here The application of the DNA used to be a synchronized(applicator) block. As best as Steve
    // and I could tell, the synchronization was solely a memory boundary and not a mutual exlusion mechanism. For the
    // time being, we have resolved that we need no synchronization here (either for memory, or exclusion). The memory
    // barrier aspect isn't known to be a problem and the concurrency is handled by the server (ie. we won't get
    // concurrent updates). At some point it would be a good idea to detect (and error out) when updates are received
    // from L2 but local read/writes have been made on the target TCObject

    final long localVersion = tcObject.getVersion();
    final long dnaVersion = dna.getVersion();

    if (force || (localVersion < dnaVersion)) {
      tcObject.setVersion(dnaVersion);
      this.applicator.hydrate(this.objectManager, tcObject, dna, pojo);
    } else if (logger.isDebugEnabled()) {
      logger
          .debug("IGNORING UPDATE, local object at version " + localVersion + ", dna update is version " + dnaVersion);
    }

  }

  @Override
  public void dehydrate(final TCObject tcObject, final DNAWriter writer, final Object pojo) {
    try {
      this.applicator.dehydrate(this.objectManager, tcObject, writer, pojo);
    } catch (final ConcurrentModificationException cme) {
      // try to log some useful stuff about the pojo in question here.
      // This indicates improper locking, but is certainly possible
      final String type = pojo == null ? "null" : pojo.getClass().getName();
      final String toString = String.valueOf(pojo);
      final int ihc = System.identityHashCode(pojo);
      logger.error("Shared object (presumably new) modified during dehydrate (type " + type + ", ihc " + ihc + "): "
                   + toString, cme);
      throw cme;
    }
  }

  @Override
  public String getName() {
    return this.peer.getName();
  }

  @Override
  public synchronized Constructor getConstructor() {
    if (this.constructor == null) {
      // As best as I can tell, the reason for the lazy initialization here is that we don't actually need the cstr
      // looked up for all of the TCClass instances we cook up. Additionally, the assertions in findConstructor will go
      // off for a fair number of abstract base classes (eg. java.util.AbstractMap, java.util.Dictionary, etc)
      this.constructor = findConstructor();
    }
    return this.constructor;
  }

  private Constructor findConstructor() {
    Constructor rv = null;

    final Constructor[] cons = this.peer.getDeclaredConstructors();
    for (final Constructor con : cons) {
      final Class[] types = con.getParameterTypes();
      if (types.length == 0) {
        rv = con;
        rv.setAccessible(true);
        return rv;
      }
    }

    rv = ReflectionUtil.newConstructor(this.peer);
    rv.setAccessible(true);
    return rv;
  }

  @Override
  public String toString() {
    return this.peer.getName();
  }

  @Override
  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
    return this.applicator.getPortableObjects(pojo, addTo);
  }

  @Override
  public ClientObjectManager getObjectManager() {
    return this.objectManager;
  }

  @Override
  public TCObject createTCObject(final ObjectID id, final Object pojo, final boolean isNew) {
    if (pojo instanceof TCObjectSelf) {
      TCObjectSelf self = (TCObjectSelf) pojo;
      self.initializeTCObject(id, this, isNew);
      return self;
    }

    return new TCObjectLogical(id, pojo, this, isNew);
  }

  @Override
  public boolean isUseNonDefaultConstructor() {
    return this.useNonDefaultConstructor;
  }

  @Override
  public Object getNewInstanceFromNonDefaultConstructor(final DNA dna, PlatformService platformService)
      throws IOException, ClassNotFoundException {
    final Object o = this.applicator.getNewInstance(this.objectManager, dna, platformService);

    if (o == null) { throw new AssertionError("Can't find suitable constructor for class: " + getName() + "."); }
    return o;
  }

}
