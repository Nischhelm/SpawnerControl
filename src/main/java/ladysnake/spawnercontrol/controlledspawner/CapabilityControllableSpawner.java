package ladysnake.spawnercontrol.controlledspawner;

import ladysnake.spawnercontrol.SpawnerControl;
import ladysnake.spawnercontrol.config.MSCConfig;
import ladysnake.spawnercontrol.config.SpawnerConfig;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class CapabilityControllableSpawner {
    @CapabilityInject(IControllableSpawner.class)
    public static Capability<IControllableSpawner> CAPABILITY_SPAWNER;

    public static final ResourceLocation CAPABILITY_KEY = new ResourceLocation(SpawnerControl.MOD_ID, "controllable_spawner_cap");

    public static IControllableSpawner getHandler(TileEntityMobSpawner entity) {
        return Objects.requireNonNull(entity.getCapability(CAPABILITY_SPAWNER, null));
    }

    public static class DefaultControllableSpawner implements IControllableSpawner {
        private final TileEntityMobSpawner spawner;
        private int spawnedMobsCount;
        private int aliveMobsCount;

        public DefaultControllableSpawner() {
            this(null);
        }

        DefaultControllableSpawner(TileEntityMobSpawner spawner) {
            this.spawner = spawner;
        }

        @Override
        public void setSpawnedMobsCount(int mobCount) {
            this.spawnedMobsCount = mobCount;
        }

        @Override
        public void setAliveMobsCount(int mobCount) {
            this.aliveMobsCount = mobCount;
        }

        @Override
        public boolean incrementSpawnedMobsCount() {
            SpawnerConfig cfg = getConfig();
            if(++this.spawnedMobsCount >= cfg.mobThreshold) {
                if (cfg.breakSpawner)
                    spawner.getWorld().setBlockToAir(spawner.getPos());
                return true;
            }
            this.adjustDelayAfterSpawn(spawner.getSpawnerBaseLogic(), cfg.spawnRateModifier);
            return false;
        }

        @Override
        public void incrementAliveMobsCount() {
            ++aliveMobsCount;
        }

        @Override
        public void decrementAliveMobsCount() {
            --aliveMobsCount;
        }

        /**
         * Should be called after every spawn to tweak the cooldown according to the config
         */
        protected void adjustDelayAfterSpawn(MobSpawnerBaseLogic spawnerBaseLogic, double spawnRateModifier) {
            NBTTagCompound tags = new NBTTagCompound();
            spawnerBaseLogic.writeToNBT(tags);
            int minSpawnDelay = tags.getInteger("MinSpawnDelay");
            int maxSpawnDelay = tags.getInteger("MaxSpawnDelay");

            minSpawnDelay = (int) (minSpawnDelay*spawnRateModifier);
            maxSpawnDelay = (int) (maxSpawnDelay*spawnRateModifier);

            tags.setInteger("MinSpawnDelay",minSpawnDelay);
            tags.setInteger("MaxSpawnDelay",maxSpawnDelay);

            spawnerBaseLogic.readFromNBT(tags);

        }

        @Override
        public int getSpawnedMobsCount() {
            return spawnedMobsCount;
        }

        @Override
        public int getAliveMobsCount() {
            return aliveMobsCount;
        }

        @Override
        public boolean canSpawn() {
            return this.spawnedMobsCount < getConfig().mobThreshold;
        }

        @Override
        public boolean tooManyAlive() {
            if(getConfig().pauseIfTooManyAlive)
                return this.aliveMobsCount >= getConfig().aliveMobThreshold;
            else
                return false;
        }

        @Nonnull
        @Override
        public SpawnerConfig getConfig() {
            return  MSCConfig.vanillaSpawnerConfig;
        }
    }

    public static class Provider implements ICapabilitySerializable<NBTTagCompound> {
        final IControllableSpawner instance;

        public Provider(TileEntityMobSpawner spawner) {
            this.instance = new DefaultControllableSpawner(spawner);
        }

        @Override
        public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CAPABILITY_SPAWNER;
        }

        @Nullable
        @Override
        public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
            return capability == CAPABILITY_SPAWNER ? CAPABILITY_SPAWNER.cast(instance) : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return (NBTTagCompound) CAPABILITY_SPAWNER.getStorage().writeNBT(CAPABILITY_SPAWNER, instance, null);
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            CAPABILITY_SPAWNER.getStorage().readNBT(CAPABILITY_SPAWNER, instance, null, nbt);
        }
    }

    public static class Storage implements Capability.IStorage<IControllableSpawner> {

        @Nullable
        @Override
        public NBTBase writeNBT(Capability<IControllableSpawner> capability, IControllableSpawner instance, EnumFacing side) {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setInteger("SpawnedMobsCount", instance.getSpawnedMobsCount());
            nbt.setInteger("AliveMobsCount", instance.getAliveMobsCount());
            return nbt;
        }

        @Override
        public void readNBT(Capability<IControllableSpawner> capability, IControllableSpawner instance, EnumFacing side, NBTBase nbt) {
            if (nbt instanceof NBTTagCompound) {
                instance.setSpawnedMobsCount(((NBTTagCompound) nbt).getInteger("SpawnedMobsCount"));
                instance.setAliveMobsCount(((NBTTagCompound) nbt).getInteger("AliveMobsCount"));
            }
        }
    }
}
