package com.mawlee.cointcore.refinedstorage.multicrafter;

import com.mawlee.cointcore.config.MultiCrafterConfig;
import com.refinedmods.refinedstorage.api.autocrafting.Pattern;
import com.refinedmods.refinedstorage.api.autocrafting.PatternType;
import com.refinedmods.refinedstorage.api.autocrafting.task.StepBehavior;
import com.refinedmods.refinedstorage.api.autocrafting.task.Task;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskImpl;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskSnapshot;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.autocrafting.AutocraftingNetworkComponent;
import com.refinedmods.refinedstorage.api.network.autocrafting.ParentContainer;
import com.refinedmods.refinedstorage.api.network.autocrafting.PatternProvider;
import com.refinedmods.refinedstorage.api.network.impl.node.patternprovider.PatternProviderNetworkNode;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.autocrafting.Autocrafter;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.autocrafting.PatternInventory;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.support.network.ColoredConnectionStrategy;
import com.refinedmods.refinedstorage.common.support.network.InWorldNetworkNodeContainerImpl;
import com.refinedmods.refinedstorage.common.util.ContainerUtil;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Pattern storage of a multiblock crafter: a pattern provider for crafting (processing-free) patterns that steps its
 * tasks every tick, as many steps as the structure's CPUs allow. Inactive, and its patterns hidden from the network,
 * while the structure around it is not valid.
 */
public final class MultiCrafterStorageBlockEntity extends AbstractBaseNetworkNodeContainerBlockEntity<PatternProviderNetworkNode>
        implements StepBehavior {
    public static final int PATTERNS = 78;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_PATTERNS = "patterns";
    private static final String TAG_TASKS = "tasks";
    private static final String TAG_VISIBLE_IN_MANAGER = "vaum";
    private static final int INVALID_RECHECK_TICKS = 40;

    private final PatternInventory patternContainer = new StoragePatternInventory(this::getLevel);
    private MultiCrafterShape.Result structure;
    private int structureVersion = -1;
    private int ticks;
    // Hidden by default: big crafters with many storages make the Autocrafter Manager slow to open.
    private boolean visibleInManager;

    public MultiCrafterStorageBlockEntity(BlockPos pos, BlockState state) {
        super(MultiCrafters.STORAGE_BLOCK_ENTITY.get(), pos, state,
                new PatternProviderNetworkNode(MultiCrafterConfig.getStorageEnergy(), PATTERNS));
        patternContainer.setListener(this::patternChanged);
        patternContainer.addListener(container -> {
            updateEnergyUsage();
            setChanged();
        });
        mainNetworkNode.setStepBehavior(this);
        mainNetworkNode.onAddedIntoContainer(new TaskChangeListener());
    }

    public static int stepsFor(MultiCrafterShape.Result result) {
        if (!result.valid()) return 0;
        long steps = (long) result.cpus() * MultiCrafterConfig.getStepsPerCpu();
        return (int) Math.max(1, Math.min(steps, MultiCrafterConfig.getMaxStepsPerTick()));
    }

    @Override
    protected InWorldNetworkNodeContainer createMainContainer(PatternProviderNetworkNode networkNode) {
        return new AutocrafterContainer(this, networkNode);
    }

    public Container getPatternContainer() {
        return patternContainer;
    }

    boolean isVisibleInManager() {
        return visibleInManager;
    }

    void setVisibleInManager(boolean visible) {
        if (visibleInManager == visible) return;
        visibleInManager = visible;
        setChanged();
    }

    void serverTick() {
        refreshStructure();
        updateActiveness(getBlockState(), null);
        doWork();
    }

    private void refreshStructure() {
        if (level == null) return;
        int version = MultiCrafterStructures.version(level);
        boolean stale = version != structureVersion
                || (structure != null && !structure.valid() && ++ticks % INVALID_RECHECK_TICKS == 0);
        if (structure != null && !stale) return;
        structureVersion = version;
        structure = MultiCrafterStructures.get(level, worldPosition);
        updateEnergyUsage();
    }

    private boolean isStructureValid() {
        return structure != null && structure.valid();
    }

    private void updateEnergyUsage() {
        mainNetworkNode.setEnergyUsage(MultiCrafterConfig.getStorageEnergy() + patternContainer.getEnergyUsage());
    }

    @Override
    protected boolean calculateActive() {
        return isStructureValid() && super.calculateActive();
    }

    @Override
    protected boolean hasRedstoneMode() {
        return false;
    }

    @Override
    public Component getName() {
        return overrideName(Component.translatable("block.cointcore.multicrafter"));
    }

    private void patternChanged(int slot) {
        if (level == null) return;
        Pattern pattern = RefinedStorageApi.INSTANCE.getPattern(patternContainer.getItem(slot), level)
                .filter(MultiCrafterStorageBlockEntity::isCraftingPattern)
                .orElse(null);
        mainNetworkNode.setPattern(slot, pattern);
    }

    private static boolean isCraftingPattern(Pattern pattern) {
        return pattern.layout().type() == PatternType.INTERNAL;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide()) {
            for (int i = 0; i < patternContainer.getContainerSize(); i++) patternChanged(i);
        }
    }

    @Override
    public boolean canStep(Pattern pattern) {
        PatternProvider provider = lookupProvider(pattern);
        if (provider == null) return false;
        return provider == mainNetworkNode ? mainNetworkNode.isActive() && isStructureValid() : provider.canStep(pattern);
    }

    @Override
    public int getSteps(Pattern pattern) {
        PatternProvider provider = lookupProvider(pattern);
        if (provider == null) return 0;
        return provider == mainNetworkNode ? (structure == null ? 0 : stepsFor(structure)) : provider.getSteps(pattern);
    }

    @Nullable
    private PatternProvider lookupProvider(Pattern pattern) {
        Network network = mainNetworkNode.getNetwork();
        return network == null ? null : network.getComponent(AutocraftingNetworkComponent.class).getProviderByPattern(pattern);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put(TAG_PATTERNS, ContainerUtil.write(patternContainer, provider));
        ListTag tasks = new ListTag();
        for (Task task : mainNetworkNode.getTasks()) {
            if (!(task instanceof TaskImpl taskImpl)) continue;
            try {
                tasks.add(TaskPersistence.encode(taskImpl.createSnapshot()));
            } catch (Exception exception) {
                LOGGER.error("Error while saving multicrafter task {} x{}", task.getResource(), task.getAmount(), exception);
            }
        }
        tag.put(TAG_TASKS, tasks);
        tag.putBoolean(TAG_VISIBLE_IN_MANAGER, visibleInManager);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains(TAG_PATTERNS)) {
            ContainerUtil.read(tag.getCompound(TAG_PATTERNS), patternContainer, provider);
        }
        if (tag.contains(TAG_TASKS)) {
            ListTag tasks = tag.getList(TAG_TASKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < tasks.size(); i++) {
                try {
                    mainNetworkNode.addTask(new TaskImpl(TaskPersistence.decode(tasks.getCompound(i))));
                } catch (Exception exception) {
                    LOGGER.error("Error while loading multicrafter task, skipping", exception);
                }
            }
        }
        visibleInManager = tag.getBoolean(TAG_VISIBLE_IN_MANAGER);
        super.loadAdditional(tag, provider);
    }

    /** Only crafting patterns: the multiblock has no machine to push processing patterns into. */
    static final class StoragePatternInventory extends PatternInventory {
        private final java.util.function.Supplier<Level> level;

        StoragePatternInventory(java.util.function.Supplier<Level> level) {
            super(PATTERNS, level);
            this.level = level;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (!super.canPlaceItem(slot, stack)) return false;
            Level current = level.get();
            return current == null || RefinedStorageApi.INSTANCE.getPattern(stack, current)
                    .map(MultiCrafterStorageBlockEntity::isCraftingPattern).orElse(false);
        }
    }

    /** Lets the Autocrafter Manager list and edit the storage's patterns. */
    private static final class AutocrafterContainer extends InWorldNetworkNodeContainerImpl implements Autocrafter {
        // The manager builds (and syncs) slots even for hidden autocrafters; an empty container keeps a hidden
        // crafter from costing anything there.
        private static final Container HIDDEN = new net.minecraft.world.SimpleContainer(0);
        private final MultiCrafterStorageBlockEntity blockEntity;

        AutocrafterContainer(MultiCrafterStorageBlockEntity blockEntity, PatternProviderNetworkNode node) {
            super(blockEntity, node, "main", 0,
                    new ColoredConnectionStrategy(blockEntity::getBlockState, blockEntity.getBlockPos()), null);
            this.blockEntity = blockEntity;
        }

        @Override
        public Component getAutocrafterName() {
            return blockEntity.getName();
        }

        @Override
        public Container getPatternContainer() {
            return blockEntity.visibleInManager ? blockEntity.patternContainer : HIDDEN;
        }

        @Override
        public boolean isVisibleToTheAutocrafterManager() {
            return blockEntity.visibleInManager;
        }
    }

    private final class TaskChangeListener implements ParentContainer {
        @Override public void add(PatternProvider provider, Pattern pattern, int priority) { }
        @Override public void remove(PatternProvider provider, Pattern pattern) { }
        @Override public void update(Pattern pattern, int priority) { }
        @Override public void taskAdded(PatternProvider provider, Task task) { setChanged(); }
        @Override public void taskRemoved(Task task) { setChanged(); }
        @Override public void taskCompleted(Task task) { }
        @Override public void taskChanged(Task task) { setChanged(); }
    }

    /** RS keeps its task codec package-private; reuse it so saved tasks match the stock autocrafter's format. */
    private static final class TaskPersistence {
        private static final String CLASS =
                "com.refinedmods.refinedstorage.common.autocrafting.autocrafter.TaskSnapshotPersistence";
        @Nullable private static final Method ENCODE;
        @Nullable private static final Method DECODE;

        static {
            Method encode = null;
            Method decode = null;
            try {
                Class<?> type = Class.forName(CLASS);
                encode = type.getDeclaredMethod("encodeSnapshot", TaskSnapshot.class);
                decode = type.getDeclaredMethod("decodeSnapshot", CompoundTag.class);
                encode.setAccessible(true);
                decode.setAccessible(true);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                LOGGER.error("Refined Storage task codec not found; multicrafter tasks will not survive restarts", exception);
                encode = null;
                decode = null;
            }
            ENCODE = encode;
            DECODE = decode;
        }

        static CompoundTag encode(TaskSnapshot snapshot) throws ReflectiveOperationException {
            if (ENCODE == null) throw new IllegalStateException("task codec unavailable");
            return (CompoundTag) ENCODE.invoke(null, snapshot);
        }

        static TaskSnapshot decode(CompoundTag tag) throws ReflectiveOperationException {
            if (DECODE == null) throw new IllegalStateException("task codec unavailable");
            return (TaskSnapshot) DECODE.invoke(null, tag);
        }
    }
}
