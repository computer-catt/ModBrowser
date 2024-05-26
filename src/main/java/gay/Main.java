package gay;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import com.google.gson.Gson;

import java.util.zip.ZipFile;

public class Main {
    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Server jar location:");
        var ServerJarArchive = new ZipFile(scanner.nextLine());
        
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
            case "forge" -> {
                System.out.println("You are using Forge Mod Loader, Minecraft version cannot be automatically determined\nPlease enter the Minecraft version:");
                version = scanner.nextLine();
            }
        }
        System.out.println("version: " + version);
        
        
        //
        //noinspection InfiniteLoopStatement
        while (true) {
            System.out.println("What mod are you looking for?:");
            
            URLConnection connection = new URL("https://api.modrinth.com/v2/search?query=" + scanner.nextLine() + 
                    "&facets=" + URLEncoder.encode("[[\"categories:" + modLoader + "\"],[\"versions:" + version + "\"]]", StandardCharsets.UTF_8))
                    .openConnection();
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            InputStream response = connection.getInputStream();
            String responsetext = new String(response.readAllBytes(), StandardCharsets.UTF_8);
            response.close();

            SearchResult result = new Gson().fromJson(responsetext, SearchResult.class);
            for (Hit hit : result.hits)
                System.out.println(hit.title + "   " + hit.author);
        }
    }
}