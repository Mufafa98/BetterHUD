package com.mufafa98.better_hud.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class ArmorConfig {
    private static ArmorConfig INSTANCE;
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("better-hud.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean renderVertically = true;
    public int margin = 4;
    public int gapBetweenItems = 4;

    private ArmorConfig() {
    }

    public static ArmorConfig getInstance() {
        if (INSTANCE == null)
            load();
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                INSTANCE = GSON.fromJson(Files.readString(PATH), ArmorConfig.class);
            } else {
                INSTANCE = new ArmorConfig();
                INSTANCE.save();
            }
        } catch (Exception e) {
            INSTANCE = new ArmorConfig();
        }
    }

    public void save() {
        try {
            Files.writeString(PATH, GSON.toJson(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}