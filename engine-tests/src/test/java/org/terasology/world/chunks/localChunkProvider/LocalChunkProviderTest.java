/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.world.chunks.localChunkProvider;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TShortObjectHashMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.EntityStore;
import org.terasology.entitySystem.event.Event;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.math.geom.Vector3i;
import org.terasology.persistence.ChunkStore;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.OnActivatedBlocks;
import org.terasology.world.block.OnAddedBlocks;
import org.terasology.world.chunks.Chunk;
import org.terasology.world.chunks.event.OnChunkGenerated;
import org.terasology.world.chunks.event.OnChunkLoaded;
import org.terasology.world.chunks.internal.ReadyChunkInfo;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalChunkProviderTest {

    private LocalChunkProvider chunkProvider;
    private ChunkFinalizer chunkFinalizer;
    private EntityManager entityManager;
    private BlockManager blockManager;
    private BlockEntityRegistry blockEntityRegistry;
    private EntityRef worldEntity;

    @Before
    public void setUp() throws Exception {
        entityManager = mock(EntityManager.class);
        chunkFinalizer = mock(ChunkFinalizer.class);
        blockManager = mock(BlockManager.class);
        blockEntityRegistry = mock(BlockEntityRegistry.class);
        worldEntity = mock(EntityRef.class);
        chunkProvider = new LocalChunkProvider(null,
                entityManager, null, blockManager, null, chunkFinalizer, null);
        chunkProvider.setBlockEntityRegistry(blockEntityRegistry);
        chunkProvider.setWorldEntity(worldEntity);
    }

    @Test
    public void testCompleteUpdateHandlesFinalizedChunkIfReady() throws Exception {
        final Chunk chunk = mockChunkAt(0, 0, 0);
        final ReadyChunkInfo readyChunkInfo = ReadyChunkInfo.createForNewChunk(chunk, new TShortObjectHashMap<>(), Collections.emptyList());
        when(chunkFinalizer.completeFinalization()).thenReturn(readyChunkInfo);

        chunkProvider.completeUpdate();

        final InOrder inOrder = inOrder(worldEntity);
        inOrder.verify(worldEntity).send(any(OnChunkGenerated.class));
        inOrder.verify(worldEntity).send(any(OnChunkLoaded.class));
    }

    @Test
    public void testCompleteUpdateGeneratesStoredEntities() throws Exception {
        final Chunk chunk = mockChunkAt(0, 0, 0);
        final EntityStore entityStore = new EntityStore();
        final ChunkProviderTestComponent testComponent = new ChunkProviderTestComponent();
        entityStore.addComponent(testComponent);
        final List<EntityStore> entityStores = Collections.singletonList(entityStore);
        final ReadyChunkInfo readyChunkInfo = ReadyChunkInfo.createForNewChunk(chunk, new TShortObjectHashMap<>(), entityStores);
        when(chunkFinalizer.completeFinalization()).thenReturn(readyChunkInfo);
        final EntityRef mockEntity = mock(EntityRef.class);
        when(entityManager.create()).thenReturn(mockEntity);

        chunkProvider.completeUpdate();

        verify(entityManager).create();
        verify(mockEntity).addComponent(eq(testComponent));
    }

    @Test
    public void testCompleteUpdateGeneratesStoredEntitiesFromPrefab() throws Exception {
        final Chunk chunk = mockChunkAt(0, 0, 0);
        final Prefab prefab = mock(Prefab.class);
        final EntityStore entityStore = new EntityStore(prefab);
        final ChunkProviderTestComponent testComponent = new ChunkProviderTestComponent();
        entityStore.addComponent(testComponent);
        final List<EntityStore> entityStores = Collections.singletonList(entityStore);
        final ReadyChunkInfo readyChunkInfo = ReadyChunkInfo.createForNewChunk(chunk, new TShortObjectHashMap<>(), entityStores);
        when(chunkFinalizer.completeFinalization()).thenReturn(readyChunkInfo);
        final EntityRef mockEntity = mock(EntityRef.class);
        when(entityManager.create(any(Prefab.class))).thenReturn(mockEntity);

        chunkProvider.completeUpdate();

        verify(entityManager).create(eq(prefab));
        verify(mockEntity).addComponent(eq(testComponent));
    }


    @Test
    public void testCompleteUpdateRestoresEntitiesForRestoredChunks() throws Exception {
        final Chunk chunk = mockChunkAt(0, 0, 0);
        final ChunkStore chunkStore = mock(ChunkStore.class);
        final ReadyChunkInfo readyChunkInfo = ReadyChunkInfo.createForRestoredChunk(chunk, new TShortObjectHashMap<>(), chunkStore, Collections.emptyList());
        when(chunkFinalizer.completeFinalization()).thenReturn(readyChunkInfo);

        chunkProvider.completeUpdate();

        verify(chunkStore).restoreEntities();
    }

    @Test
    public void testCompleteUpdateSendsBlockAddedEvents() throws Exception {
        final Chunk chunk = mockChunkAt(0, 0, 0);
        final TShortObjectHashMap<TIntList> blockPositionMapppings = new TShortObjectHashMap<>();
        final short blockId = 42;
        final EntityRef blockEntity = mock(EntityRef.class);
        final Block block = new Block();
        block.setEntity(blockEntity);
        when(blockManager.getBlock(eq(blockId))).thenReturn(block);
        final TIntArrayList positions = new TIntArrayList();
        final Vector3i position = new Vector3i(1, 2, 3);
        positions.add(position.x);
        positions.add(position.y);
        positions.add(position.z);
        blockPositionMapppings.put(blockId, positions);
        final ReadyChunkInfo readyChunkInfo = ReadyChunkInfo.createForRestoredChunk(chunk, blockPositionMapppings, mock(ChunkStore.class), Collections.emptyList());
        when(chunkFinalizer.completeFinalization()).thenReturn(readyChunkInfo);

        chunkProvider.completeUpdate();

        final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(blockEntity, atLeastOnce()).send(eventArgumentCaptor.capture());
        final Event event = eventArgumentCaptor.getAllValues().get(0);
        assertThat(event, instanceOf(OnAddedBlocks.class));
        assertThat(((OnAddedBlocks) event).getBlockPositions(), hasItem(position));
    }

    @Test
    public void testCompleteUpdateSendsBlockActivatedEvents() throws Exception {
        final Chunk chunk = mockChunkAt(0, 0, 0);
        final TShortObjectHashMap<TIntList> blockPositionMapppings = new TShortObjectHashMap<>();
        final short blockId = 42;
        final EntityRef blockEntity = mock(EntityRef.class);
        final Block block = new Block();
        block.setEntity(blockEntity);
        when(blockManager.getBlock(eq(blockId))).thenReturn(block);
        final TIntArrayList positions = new TIntArrayList();
        final Vector3i position = new Vector3i(1, 2, 3);
        positions.add(position.x);
        positions.add(position.y);
        positions.add(position.z);
        blockPositionMapppings.put(blockId, positions);
        final ReadyChunkInfo readyChunkInfo = ReadyChunkInfo.createForRestoredChunk(chunk, blockPositionMapppings, mock(ChunkStore.class), Collections.emptyList());
        when(chunkFinalizer.completeFinalization()).thenReturn(readyChunkInfo);

        chunkProvider.completeUpdate();

        final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(blockEntity, atLeastOnce()).send(eventArgumentCaptor.capture());
        final Event event = eventArgumentCaptor.getAllValues().get(1);
        assertThat(event, instanceOf(OnActivatedBlocks.class));
        assertThat(((OnActivatedBlocks) event).getBlockPositions(), hasItem(position));
    }

    private static Chunk mockChunkAt(final int x, final int y, final int z) {
        final Chunk chunk = mock(Chunk.class);
        when(chunk.getPosition()).thenReturn(new Vector3i(x, y, z));
        return chunk;
    }

    private static class ChunkProviderTestComponent implements Component {

    }
}
