package com.mufafa98.better_hud.client;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class ArmorConfig {
    static public boolean renderVertically = true;
    static public int margin = 4;
    static public int gapBetweenItems = 4;

    public static void save() {
        try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("better-hud.json");
            Files.writeString(path, new Gson().toJson(new ArmorConfig()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
