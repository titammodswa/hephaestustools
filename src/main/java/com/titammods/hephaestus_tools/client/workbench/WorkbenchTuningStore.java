package com.titammods.hephaestus_tools.client.workbench;

import com.titammods.hephaestus_tools.config.HephaestusConfig;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchTuning;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class WorkbenchTuningStore {
    private static final String FILE = "hephaestus_tools-workbench.properties";
    private static boolean loaded;

    private WorkbenchTuningStore() {}

    private static Path path() { return FMLPaths.CONFIGDIR.get().resolve(FILE); }

    public static boolean skipCinematics() {
        return HephaestusConfig.skipCinematics();
    }

    public static void setSkipCinematics(boolean value) {
        HephaestusConfig.setSkipCinematics(value);
    }

    public static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Path p = path();
        if (!Files.exists(p)) return;
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            props.load(r);
        } catch (IOException | IllegalArgumentException e) {
            return;
        }
        for (WorkbenchTuning t : WorkbenchTuning.values()) {
            String v = props.getProperty(t.name().toLowerCase(Locale.ROOT));
            if (v == null) continue;
            try {
                t.set(Double.parseDouble(v.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public static void save() {
        Properties props = new Properties();
        boolean any = false;
        for (WorkbenchTuning t : WorkbenchTuning.values())
            if (t.get() != t.defaultValue()) {
                props.setProperty(t.name().toLowerCase(Locale.ROOT), String.format(Locale.ROOT, "%.4f", t.get()));
                any = true;
            }
        try {
            if (!any) {
                Files.deleteIfExists(path());
                return;
            }
            Files.createDirectories(path().getParent());
            try (Writer w = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
                props.store(w, "Hephaestus Tools - workbench camera (/hcam)");
            }
        } catch (IOException ignored) {
        }
    }
}
