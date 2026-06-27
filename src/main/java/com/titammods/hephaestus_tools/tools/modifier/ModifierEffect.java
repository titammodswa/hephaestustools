package com.titammods.hephaestus_tools.tools.modifier;

@FunctionalInterface
public interface ModifierEffect {

    void apply(ModifierStatContext stats, int level);
}