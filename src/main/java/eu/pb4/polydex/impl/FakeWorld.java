package eu.pb4.polydex.impl;


import it.unimi.dsi.fastutil.objects.ObjectIterators;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.FuelRegistry;
import net.minecraft.item.map.MapState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.ProfilerSystem;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.light.ChunkSkyLight;
import net.minecraft.world.chunk.light.LightSourceView;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.QueryableTickScheduler;
import net.minecraft.world.tick.TickManager;
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
public class FakeWorld extends World implements LightSourceView {
    static final Scoreboard SCOREBOARD = new Scoreboard();
    private static final EntityLookup<Entity> ENTITY_LOOKUP = new EntityLookup<>() {
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
        public Iterable<Entity> iterate() {
            return () -> ObjectIterators.emptyIterator();
        }

        @Override
        public <U extends Entity> void forEach(TypeFilter<Entity, U> filter, LazyIterationConsumer<U> consumer) {

        }

        @Override
        public void forEachIntersects(Box box, Consumer<Entity> action) {

        }

        @Override
        public <U extends Entity> void forEachIntersects(TypeFilter<Entity, U> filter, Box box, LazyIterationConsumer<U> consumer) {

        }

    };
    private static final QueryableTickScheduler<?> FAKE_SCHEDULER = new QueryableTickScheduler<Object>() {
        @Override
        public boolean isTicking(BlockPos pos, Object type) {
            return false;
        }

        @Override
        public void scheduleTick(OrderedTick<Object> orderedTick) {

        }

        @Override
        public boolean isQueued(BlockPos pos, Object type) {
            return false;
        }

        @Override
        public int getTickCount() {
            return 0;
        }
    };
    final ChunkManager chunkManager = new ChunkManager() {
        private LightingProvider lightingProvider = null;

        @Nullable
        @Override
        public Chunk getChunk(int x, int z, ChunkStatus leastStatus, boolean create) {
            return null;
        }

        @Override
        public void tick(BooleanSupplier shouldKeepTicking, boolean tickChunks) {

        }

        @Override
        public String getDebugString() {
            return "Potato";
        }

        @Override
        public int getLoadedChunkCount() {
            return 0;
        }

        @Override
        public LightingProvider getLightingProvider() {
            if (this.lightingProvider == null) {
                this.lightingProvider = new LightingProvider(new ChunkProvider() {
                    @Nullable
                    @Override
                    public LightSourceView getChunk(int chunkX, int chunkZ) {
                        return FakeWorld.this;
                    }

                    @Override
                    public BlockView getWorld() {
                        return FakeWorld.this;
                    }
                }, false, false);
            }

            return this.lightingProvider;
        }

        @Override
        public BlockView getWorld() {
            return FakeWorld.this;
        }
    };
    private final RecipeManager recipeManager;
    private final BrewingRecipeRegistry brewingRecipeRegistry;
    private final TickManager tickManager = new TickManager();
    private final FeatureSet featureSet;

    public FakeWorld(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager manager,
                      BrewingRecipeRegistry brewingRecipeRegistry, FeatureSet featureSet,
                      RecipeManager recipeManager, RegistryEntry<DimensionType> dimensionType,
                      boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, manager, dimensionType, isClient, debugWorld, seed, 0);
        this.brewingRecipeRegistry = brewingRecipeRegistry;
        this.recipeManager = recipeManager;
        this.featureSet = featureSet;
    }

    public FakeWorld(MinecraftServer server) {
        this(
                new FakeWorldProperties(),
                RegistryKey.of(RegistryKeys.WORLD, Identifier.of("polydex", "fake_world")),
                server.getRegistryManager(),
                server.getBrewingRecipeRegistry(),
                server.getOverworld().getEnabledFeatures(),
                server.getRecipeManager(),
                RegistryEntry.of(server.getOverworld().getDimension()),
                false,
                true,
                1
        );
    }

    @Override
    public void updateListeners(BlockPos pos, BlockState oldState, BlockState newState, int flags) {

    }

    @Override
    public void playSound(@Nullable PlayerEntity except, double x, double y, double z, RegistryEntry<SoundEvent> registryEntry, SoundCategory category, float volume, float pitch, long seed) {

    }

    @Override
    public void playSoundFromEntity(@Nullable PlayerEntity except, Entity entity, RegistryEntry<SoundEvent> registryEntry, SoundCategory category, float volume, float pitch, long seed) {

    }

    @Override
    public void playSound(@Nullable PlayerEntity player, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {

    }

    @Override
    public void playSoundFromEntity(@Nullable PlayerEntity player, Entity entity, SoundEvent sound, SoundCategory category, float volume, float pitch) {

    }

    @Override
    public void createExplosion(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionBehavior behavior, double x, double y, double z, float power, boolean createFire, ExplosionSourceType explosionSourceType, ParticleEffect smallParticle, ParticleEffect largeParticle, RegistryEntry<SoundEvent> soundEvent) {

    }

    @Override
    public String asString() {
        return "FakeWorld!";
    }

    @Nullable
    @Override
    public Entity getEntityById(int id) {
        return null;
    }

    @Override
    public Collection<EnderDragonPart> getEnderDragonParts() {
        return List.of();
    }

    @Override
    public TickManager getTickManager() {
        return this.tickManager;
    }

    @Nullable
    @Override
    public MapState getMapState(MapIdComponent id) {
        return null;
    }

    @Override
    public void putMapState(MapIdComponent id, MapState state) {

    }

    @Override
    public MapIdComponent increaseAndGetMapId() {
        return null;
    }

    @Override
    public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress) {

    }

    @Override
    public Scoreboard getScoreboard() {
        return SCOREBOARD;
    }

    @Override
    public RecipeManager getRecipeManager() {
        return this.recipeManager;
    }

    @Override
    protected EntityLookup<Entity> getEntityLookup() {
        return ENTITY_LOOKUP;
    }

    @Override
    public QueryableTickScheduler<Block> getBlockTickScheduler() {
        return (QueryableTickScheduler<Block>) FAKE_SCHEDULER;
    }

    @Override
    public QueryableTickScheduler<Fluid> getFluidTickScheduler() {
        return (QueryableTickScheduler<Fluid>) FAKE_SCHEDULER;
    }

    @Override
    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    @Override
    public void syncWorldEvent(@Nullable PlayerEntity player, int eventId, BlockPos pos, int data) {

    }

    @Override
    public void emitGameEvent(RegistryEntry<GameEvent> event, Vec3d emitterPos, GameEvent.Emitter emitter) {

    }

    @Override
    public BrewingRecipeRegistry getBrewingRecipeRegistry() {
        return null;
    }

    @Override
    public FuelRegistry getFuelRegistry() {
        return null;
    }

    @Override
    public FeatureSet getEnabledFeatures() {
        return this.featureSet;
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded) {
        return 0;
    }

    @Override
    public List<? extends PlayerEntity> getPlayers() {
        return Collections.emptyList();
    }

    @Override
    public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
        return this.getRegistryManager().getOrThrow(RegistryKeys.BIOME).getOrThrow(BiomeKeys.PLAINS);
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public void forEachLightSource(BiConsumer<BlockPos, BlockState> callback) {

    }

    @Override
    public ChunkSkyLight getChunkSkyLight() {
        return null;
    }


    static class FakeWorldProperties implements MutableWorldProperties {
        @Override
        public BlockPos getSpawnPos() {
            return BlockPos.ORIGIN;
        }

        @Override
        public float getSpawnAngle() {
            return 0;
        }

        @Override
        public long getTime() {
            return 0;
        }

        @Override
        public long getTimeOfDay() {
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
        public void setSpawnPos(BlockPos pos, float angle) {

        }
    }
}
