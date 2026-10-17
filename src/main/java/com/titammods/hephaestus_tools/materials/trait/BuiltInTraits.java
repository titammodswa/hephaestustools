package com.titammods.hephaestus_tools.materials.trait;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.effect.EffectOperation;
import com.titammods.hephaestus_tools.effect.ScaledAmount;
import com.titammods.hephaestus_tools.effect.StatType;
import com.titammods.hephaestus_tools.effect.ToolEffect;
import com.titammods.hephaestus_tools.effect.conditions.TargetInTagCondition;
import com.titammods.hephaestus_tools.effect.conditions.TargetWetCondition;
import com.titammods.hephaestus_tools.effect.scales.ColdScale;
import com.titammods.hephaestus_tools.effect.scales.DepthScale;
import com.titammods.hephaestus_tools.effect.scales.DurabilityLeftScale;
import com.titammods.hephaestus_tools.effect.scales.TargetArmorScale;
import com.titammods.hephaestus_tools.effect.scales.WearScale;
import com.titammods.hephaestus_tools.effect.types.BonusDamageEffect;
import com.titammods.hephaestus_tools.effect.types.BreakSpeedEffect;
import com.titammods.hephaestus_tools.effect.types.PullItemsEffect;
import com.titammods.hephaestus_tools.effect.types.RepairEffect;
import com.titammods.hephaestus_tools.effect.types.StatAddEffect;
import com.titammods.hephaestus_tools.effect.types.StatMultiplyEffect;
import com.titammods.hephaestus_tools.effect.LevelValue;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuiltInTraits {

    private BuiltInTraits() {}

    public static Map<Identifier, TraitDefinition> definitions() {
        Map<Identifier, TraitDefinition> out = new LinkedHashMap<>();
        add(out, "lightweight",
                new StatMultiplyEffect(StatType.MINING_SPEED, 0.05f),
                new StatAddEffect(StatType.ATTACK_SPEED, 0.5f));
        add(out, "featherweight",
                new StatMultiplyEffect(StatType.MINING_SPEED, 0.07f),
                new StatAddEffect(StatType.ATTACK_SPEED, 0.3f));
        add(out, "solid", new StatMultiplyEffect(StatType.DURABILITY, 0.25f));
        add(out, "dense", new StatMultiplyEffect(StatType.DURABILITY, 0.40f));
        add(out, "heavy",
                new StatMultiplyEffect(StatType.ATTACK_DAMAGE, 0.20f),
                new StatAddEffect(StatType.ATTACK_SPEED, -0.4f));
        add(out, "dwarven", new BreakSpeedEffect(EffectOperation.MULTIPLY,
                ScaledAmount.scaled(0.004f, new DepthScale(0), 0.5f)));
        add(out, "stonebound", new BreakSpeedEffect(EffectOperation.MULTIPLY,
                ScaledAmount.scaled(0.5f, WearScale.INSTANCE)));
        add(out, "maintained", new BreakSpeedEffect(EffectOperation.MULTIPLY,
                ScaledAmount.scaled(0.25f, DurabilityLeftScale.INSTANCE)));
        add(out, "smite", new BonusDamageEffect(EffectOperation.ADD, ScaledAmount.of(2.5f),
                new TargetInTagCondition(EntityTypeTags.SENSITIVE_TO_SMITE)));
        add(out, "jagged", new BonusDamageEffect(EffectOperation.ADD,
                ScaledAmount.scaled(3.0f, WearScale.INSTANCE)));
        add(out, "pierce", new BonusDamageEffect(EffectOperation.ADD,
                ScaledAmount.scaled(0.2f, TargetArmorScale.INSTANCE, 4.0f)));
        add(out, "cultivated", new RepairEffect(40, LevelValue.of(1f), true));
        add(out, "magnetic", new PullItemsEffect(5, 5.0, 0.12));
        add(out, "shock", new BonusDamageEffect(EffectOperation.ADD, ScaledAmount.of(2.5f),
                TargetWetCondition.INSTANCE));
        add(out, "temperate",
                new BreakSpeedEffect(EffectOperation.ADD, ScaledAmount.scaled(4.0f, new ColdScale(0.75f))),
                new BonusDamageEffect(EffectOperation.ADD, ScaledAmount.scaled(1.5f, new ColdScale(0.75f))));
        return out;
    }

    private static void add(Map<Identifier, TraitDefinition> out, String path, ToolEffect... effects) {
        out.put(Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, path), new TraitDefinition(1, List.of(effects)));
    }
}
