package com.tc.objectserver.entity;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.TestEntity;

import com.tc.object.EntityID;
import com.tc.objectserver.api.EntityManager;
import com.tc.services.TerracottaServiceProviderRegistry;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class EntityManagerImplTest {
  
  private EntityManager entityManager;
  private EntityID id;
  private long version;
  private long consumerID;

  @Before
  public void setUp() throws Exception {
    entityManager = new EntityManagerImpl(
        mock(TerracottaServiceProviderRegistry.class),
        mock(ClientEntityStateManager.class),
        mock(RequestProcessor.class)
    );
    id = new EntityID(TestEntity.class.getName(), "foo");
    version = 1;
  }

  @Test
  public void testGetNonExistent() throws Exception {
    assertThat(entityManager.getEntity(id, version), is(empty()));
  }

  @Test
  public void testCreateEntity() throws Exception {
    entityManager.createEntity(id, version, consumerID);
    assertThat(entityManager.getEntity(id, version).get().getID(), is(id));
  }

  @Test(expected = IllegalStateException.class)
  public void testCreateExistingEntity() throws Exception {
    entityManager.createEntity(id, version, consumerID);
    entityManager.createEntity(id, version, consumerID);
  }

  @Test
  public void testDestroyEntity() throws Exception {
    entityManager.createEntity(id, version, consumerID);
    entityManager.destroyEntity(id);
    assertThat(entityManager.getEntity(id, version), is(empty()));
  }

  @Test(expected = IllegalStateException.class)
  public void testDestroyNoExistent() throws Exception {
    entityManager.destroyEntity(id);
  }
}
