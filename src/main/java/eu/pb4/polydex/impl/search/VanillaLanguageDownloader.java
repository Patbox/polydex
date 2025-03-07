package eu.pb4.polydex.impl.search;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class VanillaLanguageDownloader {
    public static void checkAndDownload() throws Throwable {
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()) {
            var gson = new GsonBuilder().create();
            var manifest = gson.fromJson(client.send(
                    HttpRequest.newBuilder().uri(
                            URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
                    ).build(), HttpResponse.BodyHandlers.ofString()).body(), VersionManifest.class);

            var mcVer = SharedConstants.getGameVersion().getId();

            var version = manifest.versions.stream().filter(x -> x.id.equals(mcVer)).findFirst();
            if (version.isEmpty()) {
                return;
            }

            var versionData = gson.fromJson(client.send(
                    HttpRequest.newBuilder().uri(
                            URI.create(version.get().url)
                    ).build(), HttpResponse.BodyHandlers.ofString()).body(), VersionData.class);


            var assetIndex = gson.fromJson(client.send(
                    HttpRequest.newBuilder().uri(
                            URI.create(versionData.assetIndex.url)
                    ).build(), HttpResponse.BodyHandlers.ofString()).body(), AssetIndex.class);


            var packmcmeta = assetIndex.objects.get("pack.mcmeta");

            for (var x : assetIndex.objects.entrySet()) {
                if (x.getKey().startsWith("minecraft/lang/")) {
                    var lang = x.getKey().substring("minecraft/lang/".length());
                    var hash = x.getValue().hash;
                    var url = "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash;
                }
            }

        }
    }

    private static class Version {
        public String id = "";
        public String url = "";
        public String sha1 = "";
    }

    private static class VersionManifest {
        public List<Version> versions = List.of();
    }

    private static class AssetIndexPointer {
        public String url= "";
        public String sha1 = "";
    }

    private static class VersionData {
        public AssetIndexPointer assetIndex;
    }

    private static class AssetIndexEntry {
        public String hash = "";
    }

    private static class AssetIndex {
        public Map<String, AssetIndexEntry> objects = Map.of();
    }
}
