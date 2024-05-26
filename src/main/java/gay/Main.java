package gay;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import com.google.gson.Gson;
import java.util.zip.ZipFile;
import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;

public class Main {
    public static void clearScreen() {
        System.out.print(ansi().eraseScreen().cursor(0, 0));
        System.out.flush();
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
        AnsiConsole.systemInstall();
        Scanner scanner = new Scanner(System.in);

        String jarLocation;
        while (true) {
            //clearScreen();
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
            default -> "Could Not Find Mod Loader\nPlease use purpur/Fabric/Forge";
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
        System.out.println("What mod are you looking for?:");
        String searchquery = scanner.nextLine().trim();
        URLConnection connection = new URL("https://api.modrinth.com/v2/search?limit=20&query=" +  searchquery+
                "&facets=" + URLEncoder.encode("[[\"categories:" + modLoader + "\"],[\"versions:" + version + "\"]]", StandardCharsets.UTF_8))
                .openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        InputStream response = connection.getInputStream();
        String responseText = new String(response.readAllBytes(), StandardCharsets.UTF_8);
        response.close();
        SearchResult result = new Gson().fromJson(responseText, SearchResult.class);
        
        clearScreen();
        String beginText = modLoader + "  :  " + version + "\n" + "Search query: " + searchquery;
        System.out.println(beginText);
        int ModWidth = 35;
        System.out.println();
        System.out.println(ansi().fgBlack().bg(Color.WHITE).a("Found " + result.total_hits + " results").reset());
        for (Hit hit : result.hits) {
            System.out.println(padString(hit.title, ModWidth) + "by " + hit.author);
        }
        
        System.out.println();
        System.out.println("        Press:");
        System.out.println("        Up/Down to move selection");
        System.out.println("        Enter to select");
        System.out.println("        ESC to quit");

        AnsiConsole.systemUninstall();
    }
}