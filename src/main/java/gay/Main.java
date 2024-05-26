package gay;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import com.google.gson.Gson;
import java.util.zip.ZipFile;

public class Main {
    public static void clearScreen() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    public static String padString(String str, int length) {
        if (str.length() >= length)
            return str.substring(0, length);

        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length)
            sb.append(' ');

        return sb.toString();
    }



    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        String jarLocation;
        while (true) {
            clearScreen();
            System.out.println("Server jar location:");
            jarLocation = scanner.nextLine().trim();

            if (jarLocation.startsWith("\"")) {
                jarLocation = jarLocation.substring(1, jarLocation.length() - 1);
            }
            if (!new File(jarLocation).exists()) {
                System.out.println("Jar not found, try again");
                scanner.nextLine();
            } else break;
        }

        var ServerJarArchive = new ZipFile(jarLocation);

        //get modloader
        var modLoaderInputStream = ServerJarArchive.getInputStream(ServerJarArchive.getEntry("META-INF/MANIFEST.MF"));
        String modLoader = new String(modLoaderInputStream.readAllBytes(), StandardCharsets.UTF_8);

        for (String line : modLoader.split("\n"))
            if (line.startsWith("Main-Class: "))
                modLoader = line.replace("Main-Class: ", "").trim();

        modLoader = switch (modLoader) {
            case "net.fabricmc.installer.ServerLauncher" -> "fabric";
            case "io.papermc.paperclip.Main" -> "paper";
            case "net.minecraftforge.bootstrap.shim.Main" -> "forge";
            default -> "Could Not Find Mod Loader\nPlease use Purpur/Fabric/Forge";
        };
        System.out.println("ModLoader: " + modLoader);

        //Version finder\
        String version = "";
        switch (modLoader) {
            case "paper" -> {
                InputStream stream = ServerJarArchive.getInputStream(ServerJarArchive.getEntry("version.json"));
                version = new Gson().fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), Version.class).id;

            }
            case "fabric" -> {
                InputStream stream = ServerJarArchive.getInputStream(ServerJarArchive.getEntry("install.properties"));
                String fabricFileText = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                for (String line : fabricFileText.split("\n"))
                    if (line.startsWith("game-version="))
                        version = line.replace("game-version=", "").trim();

            }
            case "forge" -> System.out.println("You are using Forge Mod Loader");
            default -> {
                System.out.println("Minecraft version cannot be automatically determined\nPlease enter the Minecraft version:");
                version = scanner.nextLine();
            }
        }
        System.out.println("version: " + version);


        //Mod Downloading portion
        //noinspection InfiniteLoopStatement
        while (true) {
            System.out.println("What mod are you looking for?:");

            URLConnection connection = new URL("https://api.modrinth.com/v2/search?limit=20&query=" + scanner.nextLine() +
                    "&facets=" + URLEncoder.encode("[[\"categories:" + modLoader + "\"],[\"versions:" + version + "\"]]", StandardCharsets.UTF_8))
                    .openConnection();
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            InputStream response = connection.getInputStream();
            String responsetext = new String(response.readAllBytes(), StandardCharsets.UTF_8);
            response.close();
            SearchResult result = new Gson().fromJson(responsetext, SearchResult.class);

            int ModWidth = 25;
            System.out.println();
            System.out.println("Found " + result.total_hits + " results");
            for (Hit hit : result.hits) {
                System.out.println(padString(hit.title, ModWidth) + "by " + hit.author);
            }
        }
    }
}