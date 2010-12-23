/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.context.IndexSyncContext;
import com.tc.l2.context.SyncIndexesRequest;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.IndexSyncMessage;
import com.tc.l2.msg.IndexSyncMessageFactory;
import com.tc.l2.objectserver.L2IndexStateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class L2IndexSyncSendHandler extends AbstractEventHandler {

  private static final TCLogger     logger     = TCLogging.getLogger(L2IndexSyncSendHandler.class);
  private final SyncLogger          syncLogger = new SyncLogger();

  private final L2IndexStateManager l2IndexStateManager;

  private GroupManager              groupManager;
  private Sink                      syncRequestSink;

  public L2IndexSyncSendHandler(final L2IndexStateManager l2IndexStateManager) {
    this.l2IndexStateManager = l2IndexStateManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof IndexSyncContext) {
      final IndexSyncContext isc = (IndexSyncContext) context;
      if (sendFiles(isc)) {
        if (isc.hasMore()) {
          this.syncRequestSink.add(new SyncIndexesRequest(isc.getNodeID()));
        }
      }
    } else {
      throw new AssertionError("Unknown context type : " + context.getClass().getName() + " : " + context);
    }
  }

  private boolean sendFiles(final IndexSyncContext context) {

    try {
      File syncFile = context.syncFile();
      byte[] fileBytes = getBytesFromFile(syncFile);
      final IndexSyncMessage msg = IndexSyncMessageFactory.createIndexSyncMessage(context.getCacheName(), syncFile
          .getName(), fileBytes.length, fileBytes, context.getSequenceID());
      this.groupManager.sendTo(context.getNodeID(), msg);
      this.syncLogger.logSynced(context);
      this.l2IndexStateManager.close(context);
      return true;
    } catch (final GroupException e) {
      // this.serverTxnMgr.acknowledgement(sid.getSourceID(), sid.getClientTransactionID(), mosc.getNodeID());
      logger.error("Removing " + context.getNodeID() + " from group because of Exception :", e);
      this.groupManager.zapNode(context.getNodeID(), L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                                "Error sending objects." + L2HAZapNodeRequestProcessor.getErrorString(e));
      return false;
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    final L2Coordinator l2Coordinator = oscc.getL2Coordinator();
    this.groupManager = l2Coordinator.getGroupManager();
    this.syncRequestSink = oscc.getStage(ServerConfigurationContext.INDEXES_SYNC_REQUEST_STAGE).getSink();
  }

  private byte[] getBytesFromFile(File file) throws IOException {

    InputStream is = new FileInputStream(file);
    byte[] bytes = null;
    try {

      // Get the size of the file
      long length = file.length();

      // Create the byte array to hold the data
      bytes = new byte[(int) length];

      // Read in the bytes
      int offset = 0;
      int numRead = 0;
      while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
        offset += numRead;
      }

      // Ensure all the bytes have been read in
      if (offset < bytes.length) { throw new IOException("Could not completely read file " + file.getName()); }
    } finally {
      // Close the input stream and return bytes
      is.close();
    }
    return bytes;
  }

  private static class SyncLogger {

    public void logSynced(final IndexSyncContext mosc) {
      // final int current = mosc.getTotalObjectsSynced();
      // final int last = current - mosc.getLookupIDs().size();
      // final int totalObjectsToSync = mosc.getTotalObjectsToSync();
      // final int lastPercent = (last * 100) / totalObjectsToSync;
      // final int currentPercent = (current * 100) / totalObjectsToSync;
      //
      // if (currentPercent > lastPercent) {
      // logger.info("Sent " + current + " (" + currentPercent + "%) objects out of " + mosc.getTotalObjectsToSync()
      // + " to " + mosc.getNodeID()
      // + (mosc.getRootsMap().size() == 0 ? "" : " roots = " + mosc.getRootsMap().size()));
      // }
      // }
    }
  }

}