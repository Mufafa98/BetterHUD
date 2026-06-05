package com.mufafa98.mud.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mufafa98.mud.client.Config;
import com.mufafa98.mud.MUD;
import com.mufafa98.mud.client.ArmorConfig.LayoutOrientation;
import com.mufafa98.mud.client.ArmorConfig.TextPosition;

import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class Config {
    private static Config INSTANCE;
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(MUD.MOD_ID + ".json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_VERSION = 3;

    // top-level version for the whole config file
    public int configVersion = CURRENT_VERSION;

    // sub-configs
    public ArmorConfig armor = new ArmorConfig();

    private Config() {
    }

    public static Config getInstance() {
        if (INSTANCE == null)
            load();
        return INSTANCE;
    }

    public ArmorConfig getArmorConfig() {
        return armor;
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                String json = Files.readString(PATH);

                int fileVersion = -1;
                try {
                    JsonElement el = JsonParser.parseString(json);
                    if (el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        if (obj.has("configVersion")) {
                            fileVersion = obj.get("configVersion").getAsInt();
                        } else if (obj.has("version")) {
                            fileVersion = obj.get("version").getAsInt();
                        }
                    }
                } catch (Exception ignored) {
                }
                if (fileVersion == CURRENT_VERSION) {
                    INSTANCE = GSON.fromJson(json, Config.class);
                } else if (fileVersion == 2) {
                    INSTANCE = GSON.fromJson(json, Config.class);

                    INSTANCE.configVersion = CURRENT_VERSION;

                    INSTANCE.armor.textPosition = TextPosition.RIGHT;
                    INSTANCE.armor.layout = LayoutOrientation.VERTICAL;

                    INSTANCE.save();

                } else if (fileVersion == 1) {
                    INSTANCE = GSON.fromJson(json, Config.class);

                    INSTANCE.configVersion = CURRENT_VERSION;

                    INSTANCE.armor.lowDurabilityPercentage = 15;
                    INSTANCE.armor.lowDurabilityColor = 0xFFFF0000;

                    INSTANCE.armor.mediumDurabilityPercentage = 30;
                    INSTANCE.armor.mediumDurabilityColor = 0xFFFFFF00;

                    INSTANCE.save();

                } else if (fileVersion == -1) {
                    // legacy single ArmorConfig JSON: try to parse as ArmorConfig and wrap it
                    try {
                        ArmorConfig legacy = GSON.fromJson(json, ArmorConfig.class);
                        INSTANCE = new Config();
                        if (legacy != null)
                            INSTANCE.armor = legacy;
                        INSTANCE.configVersion = CURRENT_VERSION;
                        INSTANCE.save();
                    } catch (Exception ex) {
                        // fall back to defaults if parsing fails
                        INSTANCE = new Config();
                        INSTANCE.save();
                    }
                } else {
                    // explicit unsupported config version -> surface error to caller
                    throw new UnsupportedOperationException(
                            "Unsupported config version: " + fileVersion + ". Supported version: " + CURRENT_VERSION);
                }
            } else {
                INSTANCE = new Config();
                INSTANCE.save();
            }
        } catch (UnsupportedOperationException e) {
            throw e; // propagate unsupported-version errors
        } catch (Exception e) {
            // On any other error, fall back to defaults
            INSTANCE = new Config();
        }
    }

    public void save() {
        try {
            this.configVersion = CURRENT_VERSION;
            Files.writeString(PATH, GSON.toJson(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}