package com.mawlee.cointcore.refinedstorage.batch;

import com.refinedmods.refinedstorage.api.autocrafting.*;
import com.refinedmods.refinedstorage.api.autocrafting.calculation.CancellationToken;
import com.refinedmods.refinedstorage.api.autocrafting.preview.PreviewType;
import com.refinedmods.refinedstorage.api.autocrafting.task.*;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.*;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StorageImpl;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BatchCraftingPlannerTest {
    private record Key(String name) implements ResourceKey { }
    private static final ResourceKey RAW = new Key("raw"), A = new Key("a"), B = new Key("b");

    private static Pattern pattern(ResourceKey output, long raw, boolean external) {
        var inputs = List.of(new Ingredient(raw, List.of(RAW)));
        var outputs = List.of(new ResourceAmount(output, 1));
        return new Pattern(UUID.randomUUID(), external ? PatternLayout.external(inputs, outputs) : PatternLayout.internal(inputs, outputs, List.of()));
    }
    private static RootStorageImpl storage(long raw) {
        var storage = new RootStorageImpl();
        var source = new StorageImpl();
        source.insert(RAW, raw, Action.EXECUTE, Actor.EMPTY);
        storage.addSource(source);
        return storage;
    }
    private static List<ResourceAmount> request(long a, long b) {
        return List.of(new ResourceAmount(A, a), new ResourceAmount(B, b));
    }

    @Test void previewAndPlanCannotSpendTheSameStockTwice() {
        var patterns = new PatternRepositoryImpl();
        patterns.add(pattern(A, 4, false), 0); patterns.add(pattern(B, 4, false), 0);
        var root = storage(7);
        var result = BatchCraftingPlanner.calculate(patterns, root, request(1, 1), 1, true, CancellationToken.NONE);
        assertEquals(PreviewType.MISSING_RESOURCES, result.preview().type());
        assertTrue(result.plan().isEmpty());
        var raw = result.preview().items().stream().filter(i -> i.resource().equals(RAW)).findFirst().orElseThrow();
        assertEquals(7, raw.available()); assertEquals(1, raw.missing());
        assertEquals(7, root.get(RAW), "calculation must not consume live storage");
    }

    @Test void oneTaskCraftsMultipleOutputsAndRestoresThroughNativeSnapshot() {
        var patterns = new PatternRepositoryImpl();
        patterns.add(pattern(A, 2, false), 0);
        patterns.add(new Pattern(UUID.randomUUID(), PatternLayout.internal(
                List.of(new Ingredient(1, List.of(A)), new Ingredient(1, List.of(RAW))),
                List.of(new ResourceAmount(B, 1)), List.of())), 0);
        var root = storage(10);
        var result = BatchCraftingPlanner.calculate(patterns, root, request(2, 1), 1, true, CancellationToken.NONE);
        assertEquals(PreviewType.SUCCESS, result.preview().type());
        var plan = result.plan().orElseThrow();
        assertEquals(2, plan.patterns().size(), "shared recipe is a single merged step");
        assertEquals(7, plan.initialRequirements().iterator().next().amount());
        var task = new TaskImpl(plan, Actor.EMPTY, true);
        task.step(root, ignored -> List.of(), StepBehavior.DEFAULT, TaskListener.EMPTY);
        var restored = new TaskImpl(task.createSnapshot());
        assertEquals(task.getId(), restored.getId());
        complete(restored, root, ignored -> List.of(), new ArrayList<>());
        assertEquals(2, root.get(A)); assertEquals(1, root.get(B)); assertEquals(3, root.get(RAW));
    }

    @Test void externalRecipesStillRunInTheirSinksAndAllOutputsReturnTogether() {
        var patterns = new PatternRepositoryImpl();
        patterns.add(pattern(A, 2, true), 0); patterns.add(pattern(B, 3, true), 0);
        var root = storage(5);
        var plan = BatchCraftingPlanner.calculate(patterns, root, request(1, 1), 1, true, CancellationToken.NONE).plan().orElseThrow();
        var incoming = new ArrayList<ResourceAmount>();
        var calls = new int[1];
        ExternalPatternSink sink = (pattern, resources, action) -> {
            if (action == Action.EXECUTE) { incoming.addAll(pattern.layout().outputs()); calls[0]++; }
            return ExternalPatternSink.Result.ACCEPTED;
        };
        var task = new TaskImpl(plan, Actor.EMPTY, true);
        root.addListener(task);
        complete(task, root, ignored -> List.of(sink), incoming);
        root.removeListener(task);
        assertEquals(2, calls[0]);
        assertEquals(1, root.get(A)); assertEquals(1, root.get(B)); assertEquals(0, root.get(RAW));
    }

    @Test void cancellingTheCombinedTaskReturnsReservedResources() {
        var patterns = new PatternRepositoryImpl();
        patterns.add(pattern(A, 2, false), 0); patterns.add(pattern(B, 3, false), 0);
        var root = storage(5);
        var plan = BatchCraftingPlanner.calculate(patterns, root, request(1, 1), 1, true, CancellationToken.NONE).plan().orElseThrow();
        var task = new TaskImpl(plan, Actor.EMPTY, true);
        task.step(root, ignored -> List.of(), StepBehavior.DEFAULT, TaskListener.EMPTY);
        assertEquals(0, root.get(RAW));
        task.cancel();
        complete(task, root, ignored -> List.of(), new ArrayList<>());
        assertEquals(5, root.get(RAW)); assertEquals(0, root.get(A)); assertEquals(0, root.get(B));
        assertFalse(task.shouldNotify());
    }

    @Test void multipliersAndDuplicateOutputsAreCheckedWithoutOverflow() {
        assertEquals(List.of(new ResourceAmount(A, 15)), BatchCraftingPlanner.normalize(
                List.of(new ResourceAmount(A, 2), new ResourceAmount(A, 3)), 3));
        assertThrows(ArithmeticException.class, () -> BatchCraftingPlanner.normalize(List.of(new ResourceAmount(A, Long.MAX_VALUE)), 2));
        assertThrows(IllegalArgumentException.class, () -> BatchCraftingPlanner.normalize(request(1, 1), 0));
        assertThrows(IllegalArgumentException.class, () -> BatchCraftingPlanner.normalize(List.of(), 1));
    }

    @Test void multiplierAppliesToEveryOutputAndMissingPatternRejectsTheWholeOrder() {
        var patterns = new PatternRepositoryImpl();
        patterns.add(pattern(A, 2, false), 0);
        var root = storage(10);
        assertEquals(PreviewType.NOT_AVAILABLE, BatchCraftingPlanner.calculate(patterns, root,
                request(1, 1), 2, true, CancellationToken.NONE).preview().type());
        assertEquals(10, root.get(RAW));
        patterns.add(pattern(B, 3, false), 0);
        var plan = BatchCraftingPlanner.calculate(patterns, root, request(1, 1), 2, true, CancellationToken.NONE).plan().orElseThrow();
        complete(new TaskImpl(plan, Actor.EMPTY, true), root, ignored -> List.of(), new ArrayList<>());
        assertEquals(2, root.get(A)); assertEquals(2, root.get(B)); assertEquals(0, root.get(RAW));
    }

    @Test void maximumIsForTheWholeOrderAndLeavesInventoryUntouched() {
        var patterns = new PatternRepositoryImpl();
        patterns.add(pattern(A, 2, false), 0); patterns.add(pattern(B, 3, false), 0);
        var root = storage(24);
        assertEquals(4, BatchCraftingPlanner.maxMultiplier(patterns, root, request(1, 1), CancellationToken.NONE));
        assertEquals(24, root.get(RAW));
        assertEquals(1, BatchCraftingPlanner.maxMultiplier(patterns, root, request(2, 3), CancellationToken.NONE));
        assertEquals(0, BatchCraftingPlanner.maxMultiplier(patterns, storage(1), request(1, 1), CancellationToken.NONE));
    }

    @Test void recipeOutputLabelsMonitorAndRestoredTaskWhileOnlyMissingIngredientsAreCrafted() {
        var patterns = new PatternRepositoryImpl();
        patterns.add(pattern(A, 2, false), 0); patterns.add(pattern(B, 3, false), 0);
        var root = storage(15);
        var plan = BatchCraftingPlanner.calculate(patterns, root, request(1, 1), 3, true, CancellationToken.NONE).plan().orElseThrow();
        ResourceKey recipeOutput = new Key("fluid_substitution_pattern");
        var labelled = BatchCraftingPlanner.withRecipeOutput(plan, new ResourceAmount(recipeOutput, 2), 3);
        assertSame(plan.rootPattern(), labelled.rootPattern());
        assertSame(plan.patterns(), labelled.patterns());
        assertSame(plan.initialRequirements(), labelled.initialRequirements());
        var task = new TaskImpl(labelled, Actor.EMPTY, true);
        assertEquals(recipeOutput, task.getStatus().info().resource());
        assertEquals(6, task.getStatus().info().amount());
        var restored = new TaskImpl(task.createSnapshot());
        assertEquals(recipeOutput, restored.getResource());
        assertEquals(6, restored.getAmount());
        complete(restored, root, ignored -> List.of(), new ArrayList<>());
        assertEquals(3, root.get(A)); assertEquals(3, root.get(B)); assertEquals(0, root.get(RAW));
        assertEquals(0, root.get(recipeOutput), "the recipe output identifies an ingredient order, not an extra crafting step");
    }

    @Test void missingRecipeContextKeepsExistingTaskLabelAndTargetQuantityCannotOverflow() {
        var patterns = new PatternRepositoryImpl();
        patterns.add(pattern(A, 2, false), 0); patterns.add(pattern(B, 3, false), 0);
        var plan = BatchCraftingPlanner.calculate(patterns, storage(5), request(1, 1), 1, true, CancellationToken.NONE).plan().orElseThrow();
        assertSame(plan, BatchCraftingPlanner.withRecipeOutput(plan, null, 1));
        assertThrows(ArithmeticException.class, () -> BatchCraftingPlanner.withRecipeOutput(plan, new ResourceAmount(A, Long.MAX_VALUE), 2));
        assertThrows(IllegalArgumentException.class, () -> BatchCraftingPlanner.withRecipeOutput(plan, new ResourceAmount(A, 1), 0));
    }

    private static void complete(TaskImpl task, RootStorageImpl root, ExternalPatternSinkProvider sinks, List<ResourceAmount> incoming) {
        for (int i = 0; i < 100 && task.getState() != TaskState.COMPLETED; i++) {
            task.step(root, sinks, StepBehavior.DEFAULT, TaskListener.EMPTY);
            for (var output : List.copyOf(incoming)) root.insert(output.resource(), output.amount(), Action.EXECUTE, Actor.EMPTY);
            incoming.clear();
        }
        assertEquals(TaskState.COMPLETED, task.getState());
    }
}
