package eu.pb4.polydex.impl;


import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.hover.HoverSettings;
import eu.pb4.predicate.api.BuiltinPredicates;
import eu.pb4.predicate.api.GsonPredicateSerializer;
import eu.pb4.predicate.api.MinecraftPredicate;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static eu.pb4.polydex.impl.PolydexImpl.id;

public class PolydexConfigImpl {
    public static final Identifier DEFAULT_DISPLAY = id(getDefaultDisplay());

    private static String getDefaultDisplay() {
        if (FabricLoader.getInstance().isModLoaded("wthit") || FabricLoader.getInstance().isModLoaded("jade")) {
            return "disabled";
        }

        return "bossbar";
    }

    @SerializedName("enable_search")
    public boolean enableSearch = true;
    @SerializedName("enable_language_support_in_search")
    public boolean enableLanguageSearch = true;

    @SerializedName(value = "enable_hover_display", alternate = "displayEnabled")
    public boolean displayEnabled = true;
    @SerializedName(value = "hover_display_update_rate", alternate = "displayUpdateRate")
    public int displayUpdateRate = 4;
    @SerializedName("display_entity_absorption")
    public boolean displayEntityAbsorption = true;
    @SerializedName("default_hover_settings")
    public GlobalSettings defaultHoverSettings = new GlobalSettings();
    @SerializedName("disabled_hover_information")
    public Set<Identifier> disabledHoverInformation = new HashSet<>();

    @SerializedName(value = "display_can_show_requirement", alternate = "displayPredicate")
    public MinecraftPredicate displayPredicate = BuiltinPredicates.hasPlayer();

    public boolean displayCantMine = true;
    public boolean displayModSource = true;
    public boolean displayAdditional = true;
    public boolean displayMiningProgress = true;
    public boolean displayEntity = true;
    public boolean displayEntityHealth = true;

    public static class GlobalSettings implements HoverSettings {
        @SerializedName("display_type")
        public Identifier defaultDisplay = DEFAULT_DISPLAY;

        @SerializedName("display_mode")
        public DisplayMode displayMode = DisplayMode.TARGET;
        @SerializedName("visible_components")
        public HashMap<Identifier, HoverDisplayBuilder.ComponentType.Visibility> visibilityMap = new HashMap<>();

        @Override
        public Identifier currentType() {
            return defaultDisplay;
        }

        @Override
        public DisplayMode displayMode() {
            return displayMode;
        }

        @Override
        public boolean isComponentVisible(ServerPlayerEntity player, HoverDisplayBuilder.ComponentType type) {
            var value =  visibilityMap.getOrDefault(type.identifier(), HoverDisplayBuilder.ComponentType.Visibility.DEFAULT);
            if (value == HoverDisplayBuilder.ComponentType.Visibility.DEFAULT) {
                value = type.defaultVisibility();
            }

            return switch (value) {
                case ALWAYS -> true;
                case NEVER, DEFAULT -> false;
                case SNEAKING -> player.isSneaking();
            };
        }
    }

    private void fillDefaults() {
        for (var c : HoverDisplayBuilder.ComponentType.getAll()) {
            if (!this.defaultHoverSettings.visibilityMap.containsKey(c.identifier())) {
                this.defaultHoverSettings.visibilityMap.put(c.identifier(), c.defaultVisibility());
            }
        }
    }


    public static PolydexConfigImpl loadOrCreateConfig(RegistryWrapper.WrapperLookup lookup) {
        try {
            Gson GSON = new GsonBuilder()
                    .disableHtmlEscaping().setLenient().setPrettyPrinting()
                    .registerTypeAdapter(Identifier.class, new IdentifierSerializer())
                    .registerTypeHierarchyAdapter(MinecraftPredicate.class, GsonPredicateSerializer.create(lookup))
                    .create();

            PolydexConfigImpl config;
            File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "polydex.json");

            if (configFile.exists()) {
                String json = IOUtils.toString(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8));

                config = GSON.fromJson(json, PolydexConfigImpl.class);
            } else {
                config = new PolydexConfigImpl();
            }

            config.fillDefaults();

            saveConfig(config, lookup);
            return config;
        } catch (IOException exception) {
            PolydexImpl.LOGGER.error("Something went wrong while reading config!");
            exception.printStackTrace();
            return new PolydexConfigImpl();
        }
    }

    public static void saveConfig(PolydexConfigImpl config, RegistryWrapper.WrapperLookup lookup) {
        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "polydex.json");
        try {
            Gson GSON = new GsonBuilder()
                    .disableHtmlEscaping().setLenient().setPrettyPrinting()
                    .registerTypeAdapter(Identifier.class, new IdentifierSerializer())
                    .registerTypeHierarchyAdapter(MinecraftPredicate.class, GsonPredicateSerializer.create(lookup))
                    .create();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8));
            writer.write(GSON.toJson(config));
            writer.close();
        } catch (Exception e) {
            PolydexImpl.LOGGER.error("Something went wrong while saving config!");
            e.printStackTrace();
        }
    }

    private static final class IdentifierSerializer implements JsonSerializer<Identifier>, JsonDeserializer<Identifier> {

        @Override
        public Identifier deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return Identifier.tryParse(json.getAsString());
            }
            return null;
        }

        @Override
        public JsonElement serialize(Identifier src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }
}
