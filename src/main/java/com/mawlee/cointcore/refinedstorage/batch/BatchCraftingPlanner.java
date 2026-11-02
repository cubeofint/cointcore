package com.mawlee.cointcore.refinedstorage.batch;

import com.refinedmods.refinedstorage.api.autocrafting.Pattern;
import com.refinedmods.refinedstorage.api.autocrafting.PatternRepository;
import com.refinedmods.refinedstorage.api.autocrafting.calculation.CancellationToken;
import com.refinedmods.refinedstorage.api.autocrafting.calculation.CraftingCalculatorImpl;
import com.refinedmods.refinedstorage.api.autocrafting.preview.*;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskPlan;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskPlanCraftingCalculatorListener;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceListImpl;
import com.refinedmods.refinedstorage.api.storage.root.RootStorage;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl;

import java.util.*;

/** Plans all requested outputs against one inventory snapshot and creates one native, persistent RS task. */
public final class BatchCraftingPlanner {
    public static final int MAX_REQUESTS = 64;
    public static final long MAX_MULTIPLIER = 1_000_000;
    private BatchCraftingPlanner() { }

    public record Result(Preview preview, Optional<TaskPlan> plan) { }

    /** The owner pattern and material plan stay unchanged; RS persists this label for monitor and completion. */
    public static TaskPlan withRecipeOutput(TaskPlan plan, ResourceAmount recipeOutput, long multiplier) {
        if (recipeOutput == null) return plan;
        if (recipeOutput.amount() <= 0 || multiplier < 1) throw new IllegalArgumentException("Invalid recipe output amount");
        return new TaskPlan(recipeOutput.resource(), Math.multiplyExact(recipeOutput.amount(), multiplier),
                plan.rootPattern(), plan.patterns(), plan.initialRequirements());
    }

    public static long maxMultiplier(PatternRepository patterns, RootStorage storage,
                                     List<ResourceAmount> outputs, CancellationToken token) {
        long low = 0, high = MAX_MULTIPLIER;
        while (low < high && !token.isCancelled()) {
            long middle = low + (high - low + 1) / 2;
            boolean fits;
            try { fits = calculate(patterns, storage, outputs, middle, false, token).preview().type() == PreviewType.SUCCESS; }
            catch (ArithmeticException overflow) { fits = false; }
            if (fits) low = middle; else high = middle - 1;
        }
        return low;
    }

    public static List<ResourceAmount> normalize(List<ResourceAmount> requests, long multiplier) {
        if (requests.isEmpty() || requests.size() > MAX_REQUESTS || multiplier < 1 || multiplier > MAX_MULTIPLIER)
            throw new IllegalArgumentException("Invalid batch size");
        var amounts = new LinkedHashMap<ResourceKey, Long>();
        for (var request : requests) {
            if (request.amount() <= 0) throw new IllegalArgumentException("Invalid requested amount");
            amounts.merge(request.resource(), Math.multiplyExact(request.amount(), multiplier), Math::addExact);
        }
        return amounts.entrySet().stream().map(e -> new ResourceAmount(e.getKey(), e.getValue())).toList();
    }

    public static Result calculate(PatternRepository patterns, RootStorage storage, List<ResourceAmount> requests,
                                   long multiplier, boolean includePlan, CancellationToken token) {
        var wanted = normalize(requests, multiplier);
        var remaining = MutableResourceListImpl.create();
        storage.getAll().forEach(remaining::add);
        var snapshot = new RootStorageImpl(remaining);
        var previewItems = new LinkedHashMap<ResourceKey, PreviewItem>();
        var plans = new ArrayList<TaskPlan>();
        boolean missing = false;
        for (var request : wanted) {
            var calculator = new CraftingCalculatorImpl(patterns, snapshot);
            if (patterns.getByOutput(request.resource()).isEmpty()) return unavailable();
            var preview = PreviewCraftingCalculatorListener.calculatePreview(calculator, request.resource(), request.amount(), token);
            if (preview.type() != PreviewType.SUCCESS && preview.type() != PreviewType.MISSING_RESOURCES)
                return new Result(preview, Optional.empty());
            if (includePlan && preview.type() == PreviewType.SUCCESS) {
                var plan = TaskPlanCraftingCalculatorListener.calculatePlan(calculator, request.resource(), request.amount(), token);
                if (plan.isEmpty()) return unavailable();
                plans.add(plan.get());
            }
            missing |= preview.type() == PreviewType.MISSING_RESOURCES;
            for (var item : preview.items()) {
                // Later outputs cannot spend inventory that an earlier output already requires.
                if (item.available() > 0) remaining.remove(item.resource(), item.available());
                previewItems.merge(item.resource(), item, (a, b) -> new PreviewItem(a.resource(),
                        Math.addExact(a.available(), b.available()), Math.addExact(a.missing(), b.missing()),
                        Math.addExact(a.toCraft(), b.toCraft())));
            }
        }
        var preview = new Preview(missing ? PreviewType.MISSING_RESOURCES : PreviewType.SUCCESS,
                List.copyOf(previewItems.values()), List.of());
        return new Result(preview, !includePlan || missing ? Optional.empty() : Optional.of(merge(plans)));
    }

    private static Result unavailable() {
        return new Result(new Preview(PreviewType.NOT_AVAILABLE, List.of(), List.of()), Optional.empty());
    }

    private static TaskPlan merge(List<TaskPlan> plans) {
        var patterns = new LinkedHashMap<Pattern, TaskPlan.PatternPlan>();
        var requirements = new LinkedHashMap<ResourceKey, Long>();
        for (var plan : plans) {
            plan.initialRequirements().forEach(r -> requirements.merge(r.resource(), r.amount(), Math::addExact));
            plan.patterns().forEach((pattern, part) -> patterns.merge(pattern,
                    new TaskPlan.PatternPlan(false, part.iterations(), part.ingredients()), BatchCraftingPlanner::mergePattern));
        }
        var first = plans.getFirst();
        // All outputs stay in native task storage until the combined task returns them. Marking the old
        // roots as roots would send an intermediate to the network before another merged pattern uses it.
        return new TaskPlan(first.resource(), first.amount(), first.rootPattern(), Collections.unmodifiableMap(patterns),
                requirements.entrySet().stream().map(e -> new ResourceAmount(e.getKey(), e.getValue())).toList());
    }

    private static TaskPlan.PatternPlan mergePattern(TaskPlan.PatternPlan a, TaskPlan.PatternPlan b) {
        var ingredients = new LinkedHashMap<Integer, Map<ResourceKey, Long>>();
        a.ingredients().forEach((slot, values) -> ingredients.put(slot, new LinkedHashMap<>(values)));
        b.ingredients().forEach((slot, values) -> {
            var target = ingredients.computeIfAbsent(slot, ignored -> new LinkedHashMap<>());
            values.forEach((key, amount) -> target.merge(key, amount, Math::addExact));
        });
        return new TaskPlan.PatternPlan(false, Math.addExact(a.iterations(), b.iterations()), ingredients);
    }
}
