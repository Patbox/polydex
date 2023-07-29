package eu.pb4.polydex.impl;


import com.google.gson.*;
import eu.pb4.predicate.api.BuiltinPredicates;
import eu.pb4.predicate.api.GsonPredicateSerializer;
import eu.pb4.predicate.api.MinecraftPredicate;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import static eu.pb4.polydex.impl.PolydexImpl.id;

public class PolydexConfig {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping().setLenient().setPrettyPrinting()
            .registerTypeAdapter(Identifier.class, new IdentifierSerializer())
            .registerTypeHierarchyAdapter(MinecraftPredicate.class, GsonPredicateSerializer.INSTANCE)
            .create();

    public Identifier defaultDisplay = id(getDefaultDisplay());

    private static String getDefaultDisplay() {
        if (FabricLoader.getInstance().isModLoaded("wthit") || FabricLoader.getInstance().isModLoaded("jade")) {
            return "disabled";
        }

        return "bossbar_sneak";
    }

    public boolean displayEnabled = true;
    public int displayUpdateRate = 4;
    public boolean displayCantMine = true;
    public boolean displayModSource = true;
    public boolean displayAdditional = true;
    public boolean displayMiningProgress = true;
    public boolean displayEntity = true;
    public boolean displayEntityHealth = true;

    public MinecraftPredicate displayPredicate = BuiltinPredicates.hasPlayer();

    public static PolydexConfig loadOrCreateConfig() {
        try {
            PolydexConfig config;
            File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "polydex.json");

            if (configFile.exists()) {
                String json = IOUtils.toString(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8));

                config = GSON.fromJson(json, PolydexConfig.class);
            } else {
                config = new PolydexConfig();
            }

            saveConfig(config);
            return config;
        } catch (IOException exception) {
            PolydexImpl.LOGGER.error("Something went wrong while reading config!");
            exception.printStackTrace();
            return new PolydexConfig();
        }
    }

    public static void saveConfig(PolydexConfig config) {
        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "polydex.json");
        try {
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
