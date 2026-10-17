package com.titammods.hephaestus_tools.materials.trait;

import com.titammods.hephaestus_tools.effect.ActiveEffect;
import com.titammods.hephaestus_tools.effect.ToolEffect;
import com.titammods.hephaestus_tools.materials.DefinitionId;
import com.titammods.hephaestus_tools.materials.Material;
import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.materials.MaterialStats;
import com.titammods.hephaestus_tools.tools.helper.ToolBuildHandler;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ToolTraits {

    public record TraitInstance(Identifier id, TraitDefinition definition, int level) {}

    private record CacheKey(Item tool, List<MaterialId> materials) {}

    private record Collected(int generation, List<TraitInstance> traits, List<ActiveEffect> effects, int fingerprint) {}

    private static final int MAX_CACHED = 4096;
    private static final Map<CacheKey, Collected> CACHE = new ConcurrentHashMap<>();
    private static final AtomicInteger GENERATION = new AtomicInteger();

    private ToolTraits() {}

    public static void invalidate() {
        GENERATION.incrementAndGet();
        CACHE.clear();
    }

    public static List<TraitInstance> collect(ItemStack tool) {
        return collect(tool.getItem(), ToolStack.getMaterials(tool));
    }

    public static List<TraitInstance> collect(Item tool, List<MaterialId> materials) {
        if (materials == null) return List.of();
        return cached(tool, materials).traits();
    }

    public static List<ActiveEffect> effects(Item tool, List<MaterialId> materials) {
        if (materials == null) return List.of();
        return cached(tool, materials).effects();
    }

    public static int fingerprint(Item tool, List<MaterialId> materials) {
        if (materials == null) return 1;
        return cached(tool, materials).fingerprint();
    }

    private static Collected cached(Item tool, List<MaterialId> materials) {
        int generation = GENERATION.get();
        Collected found = CACHE.get(new CacheKey(tool, materials));
        if (found != null && found.generation() == generation) return found;
        List<TraitInstance> traits = compute(tool, materials);
        List<ActiveEffect> effects = new ArrayList<>();
        for (TraitInstance trait : traits) {
            for (ToolEffect effect : trait.definition().effects()) effects.add(new ActiveEffect(effect, trait.level()));
        }
        Collected fresh = new Collected(generation, traits, List.copyOf(effects), hash(traits));
        if (CACHE.size() >= MAX_CACHED) CACHE.clear();
        CACHE.put(new CacheKey(tool, Collections.unmodifiableList(new ArrayList<>(materials))), fresh);
        return fresh;
    }

    private static List<TraitInstance> compute(Item tool, List<MaterialId> materials) {
        List<TraitInstance> out = new ArrayList<>();
        List<Item> layoutParts = ToolBuildHandler.getToolParts(tool);
        Map<Identifier, Integer> levels = new LinkedHashMap<>();
        for (int i = 0; i < materials.size(); i++) {
            for (TraitEntry trait : ofSlot(materials.get(i), ToolBuildHandler.partRole(layoutParts, i))) {
                levels.merge(trait.id(), trait.level(), Integer::sum);
            }
        }
        TraitManager traits = TraitManager.getInstance();
        levels.forEach((id, level) -> traits.get(id).ifPresent(definition ->
                out.add(new TraitInstance(id, definition, Math.min(level, definition.maxLevel())))));
        return List.copyOf(out);
    }

    private static int hash(List<TraitInstance> collected) {
        int hash = 1;
        TraitManager traits = TraitManager.getInstance();
        for (TraitInstance trait : collected) {
            hash = 31 * hash + trait.id().hashCode();
            hash = 31 * hash + trait.level();
            hash = 31 * hash + traits.fingerprint(trait.id());
        }
        return hash;
    }

    public static List<TraitEntry> ofSlot(MaterialId material, int role) {
        Material mat = MaterialManager.getInstance().getMaterial(material);
        if (mat == null) return List.of();
        List<TraitEntry> head = declared(mat.headStats()).orElse(List.of());
        return switch (role) {
            case 0 -> head;
            case 2 -> declared(mat.bindingStats()).orElse(head);
            default -> declared(mat.handleStats()).orElse(head);
        };
    }

    private static Optional<List<TraitEntry>> declared(MaterialStats stats) {
        if (stats.traits().isPresent()) return stats.traits();
        if (!stats.hasTrait()) return Optional.empty();
        return Optional.of(DefinitionId.resolve(stats.traitId())
                .map(id -> List.of(new TraitEntry(id, 1)))
                .orElse(List.of()));
    }

    public static List<TraitInstance> ofPart(MaterialId material, int role) {
        Map<Identifier, Integer> levels = new LinkedHashMap<>();
        for (TraitEntry trait : ofSlot(material, role)) levels.merge(trait.id(), trait.level(), Integer::sum);
        TraitManager traits = TraitManager.getInstance();
        List<TraitInstance> out = new ArrayList<>();
        levels.forEach((id, level) -> traits.get(id).ifPresent(definition ->
                out.add(new TraitInstance(id, definition, Math.min(level, definition.maxLevel())))));
        return out;
    }

    public static String nameKey(Identifier trait) {
        return trait.toLanguageKey("trait");
    }

    public static String descriptionKey(Identifier trait) {
        return trait.toLanguageKey("trait", "desc");
    }
}
