/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import com.terracotta.management.resource.BackupEntity;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for performing TSA backups.
 *
 * @author Ludovic Orban
 */
public interface BackupResourceService {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  Collection<BackupEntity> getBackupStatus(@Context UriInfo info);

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  Collection<BackupEntity> backup(@Context UriInfo info);

}
