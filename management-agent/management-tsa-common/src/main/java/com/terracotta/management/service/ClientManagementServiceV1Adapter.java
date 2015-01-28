package com.terracotta.management.service;

import com.tc.license.ProductID;
import com.terracotta.management.resource.services.utils.ClientEntityV1Bridge;
import org.terracotta.management.ServiceExecutionException;

import java.util.Collection;
import java.util.Set;

/**
 * Created by SUND on 16-01-2015.
 */
public interface ClientManagementServiceV1Adapter {

    public Collection<ClientEntityV1Bridge> getClientsV1(Set<String> clientIds, Set<ProductID> clientProductIds) throws ServiceExecutionException;

}
