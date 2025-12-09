package eu.pb4.polydex.impl;


import it.unimi.dsi.fastutil.objects.ObjectIterators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.*;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
@ApiStatus.Internal
public class FakeWorld extends Level implements LightChunk {
    static final Scoreboard SCOREBOARD = new Scoreboard();
    private static final LevelEntityGetter<Entity> ENTITY_LOOKUP = new LevelEntityGetter<>() {
        @Nullable
        @Override
        public Entity get(int id) {
            return null;
        }

        @Nullable
        @Override
        public Entity get(UUID uuid) {
            return null;
        }

        @Override
        public Iterable<Entity> getAll() {
            return () -> ObjectIterators.emptyIterator();
        }

        @Override
        public <U extends Entity> void get(EntityTypeTest<Entity, U> filter, AbortableIterationConsumer<U> consumer) {

        }

        @Override
        public void get(AABB box, Consumer<Entity> action) {

        }

        @Override
        public <U extends Entity> void get(EntityTypeTest<Entity, U> filter, AABB box, AbortableIterationConsumer<U> consumer) {

        }

    };
    private static final LevelTickAccess<?> FAKE_SCHEDULER = new LevelTickAccess<Object>() {
        @Override
        public boolean willTickThisTick(BlockPos pos, Object type) {
            return false;
        }

        @Override
        public void schedule(ScheduledTick<Object> orderedTick) {

        }

        @Override
        public boolean hasScheduledTick(BlockPos pos, Object type) {
            return false;
        }

        @Override
        public int count() {
            return 0;
        }
    };
    final ChunkSource chunkManager = new ChunkSource() {
        private LevelLightEngine lightingProvider = null;

        @Nullable
        @Override
        public ChunkAccess getChunk(int x, int z, ChunkStatus leastStatus, boolean create) {
            return null;
        }

        @Override
        public void tick(BooleanSupplier shouldKeepTicking, boolean tickChunks) {

        }

        @Override
        public String gatherStats() {
            return "Potato";
        }

        @Override
        public int getLoadedChunksCount() {
            return 0;
        }

        @Override
        public LevelLightEngine getLightEngine() {
            if (this.lightingProvider == null) {
                this.lightingProvider = new LevelLightEngine(new LightChunkGetter() {
                    @Nullable
                    @Override
                    public LightChunk getChunkForLighting(int chunkX, int chunkZ) {
                        return FakeWorld.this;
                    }

                    @Override
                    public BlockGetter getLevel() {
                        return FakeWorld.this;
                    }
                }, false, false);
            }

            return this.lightingProvider;
        }

        @Override
        public BlockGetter getLevel() {
            return FakeWorld.this;
        }
    };
    private final RecipeAccess recipeManager;
    private final PotionBrewing brewingRecipeRegistry;
    private final TickRateManager tickManager = new TickRateManager();
    private final FeatureFlagSet featureSet;

    public FakeWorld(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess manager,
                      PotionBrewing brewingRecipeRegistry, FeatureFlagSet featureSet,
                      RecipeAccess recipeManager, Holder<DimensionType> dimensionType,
                      boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, manager, dimensionType, isClient, debugWorld, seed, 0);
        this.brewingRecipeRegistry = brewingRecipeRegistry;
        this.recipeManager = recipeManager;
        this.featureSet = featureSet;
    }

    public FakeWorld(MinecraftServer server) {
        this(
                new FakeWorldProperties(),
                ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("polydex", "fake_world")),
                server.registryAccess(),
                server.potionBrewing(),
                server.overworld().enabledFeatures(),
                server.getRecipeManager(),
                Holder.direct(server.overworld().dimensionType()),
                false,
                true,
                1
        );
    }

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {

    }

    @Override
    public void playSeededSound(@Nullable Entity source, double x, double y, double z, Holder<SoundEvent> sound, SoundSource category, float volume, float pitch, long seed) {

    }

    @Override
    public void playSeededSound(@Nullable Entity source, Entity entity, Holder<SoundEvent> sound, SoundSource category, float volume, float pitch, long seed) {

    }

    @Override
    public void explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator behavior, double x, double y, double z, float power, boolean createFire, ExplosionInteraction explosionSourceType, ParticleOptions smallParticle, ParticleOptions largeParticle, WeightedList<ExplosionParticleInfo> blockParticles, Holder<SoundEvent> soundEvent) {

    }

    @Override
    public String gatherChunkSourceStats() {
        return "FakeWorld!";
    }

    @Override
    public void setRespawnData(LevelData.RespawnData spawnPoint) {

    }

    @Override
    public LevelData.RespawnData getRespawnData() {
        return new LevelData.RespawnData(GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO), 0, 0);
    }

    @Nullable
    @Override
    public Entity getEntity(int id) {
        return null;
    }

    @Override
    public Collection<EnderDragonPart> dragonParts() {
        return List.of();
    }

    @Override
    public TickRateManager tickRateManager() {
        return this.tickManager;
    }

    @Nullable
    @Override
    public MapItemSavedData getMapData(MapId id) {
        return null;
    }

    @Override
    public void destroyBlockProgress(int entityId, BlockPos pos, int progress) {

    }

    @Override
    public Scoreboard getScoreboard() {
        return SCOREBOARD;
    }

    @Override
    public RecipeAccess recipeAccess() {
        return this.recipeManager;
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return ENTITY_LOOKUP;
    }

    @Override
    public EnvironmentAttributeSystem environmentAttributes() {
        return EnvironmentAttributeSystem.builder().build();
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return (LevelTickAccess<Block>) FAKE_SCHEDULER;
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return (LevelTickAccess<Fluid>) FAKE_SCHEDULER;
    }

    @Override
    public ChunkSource getChunkSource() {
        return chunkManager;
    }

    @Override
    public void levelEvent(@Nullable Entity source, int eventId, BlockPos pos, int data) {

    }

    @Override
    public void gameEvent(Holder<GameEvent> event, Vec3 emitterPos, GameEvent.Context emitter) {

    }

    @Override
    public PotionBrewing potionBrewing() {
        return null;
    }

    @Override
    public FuelValues fuelValues() {
        return null;
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.featureSet;
    }

    @Override
    public float getShade(Direction direction, boolean shaded) {
        return 0;
    }

    @Override
    public List<? extends Player> players() {
        return Collections.emptyList();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        return this.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public void findBlockLightSources(BiConsumer<BlockPos, BlockState> callback) {

    }

    @Override
    public ChunkSkyLightSources getSkyLightSources() {
        return null;
    }

    @Override
    public WorldBorder getWorldBorder() {
        return null;
    }


    static class FakeWorldProperties implements WritableLevelData {
        public float getSpawnAngle() {
            return 0;
        }

        @Override
        public RespawnData getRespawnData() {
            return null;
        }

        @Override
        public long getGameTime() {
            return 0;
        }

        @Override
        public long getDayTime() {
            return 0;
        }

        @Override
        public boolean isThundering() {
            return false;
        }

        @Override
        public boolean isRaining() {
            return false;
        }

        @Override
        public void setRaining(boolean raining) {

        }

        @Override
        public boolean isHardcore() {
            return false;
        }

        @Override
        public Difficulty getDifficulty() {
            return Difficulty.NORMAL;
        }

        @Override
        public boolean isDifficultyLocked() {
            return false;
        }

        @Override
        public void setSpawn(RespawnData spawnPoint) {

        }
    }
}
