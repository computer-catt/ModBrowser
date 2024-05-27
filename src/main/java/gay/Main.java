package gay;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.google.gson.Gson;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;
import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;

public class Main implements NativeKeyListener {
    static String beginText;
    static String endText;

    static int currentIndex = 0;
    static List<String> items;

    public Main() {
        AnsiConsole.systemInstall();
        // Disable logging for JNativeHook
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(e.getMessage());
            System.exit(1);
        }

        GlobalScreen.addNativeKeyListener(this);

        // Initial display
        displayItems();
    }

    private void displayItems() {
        System.out.println(beginText);
        for (int i = 0; i < items.toArray().length; i++) {
            if (i == currentIndex) {
                System.out.println(ansi().fgBlack().bg(Color.WHITE).a(items.toArray()[i]).reset());
            } else {
                System.out.println(items.toArray()[i]);
            }
        }
        System.out.println(endText);
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException nativeHookException) {
                nativeHookException.printStackTrace();
            }
        }
        else if (e.getKeyCode() == NativeKeyEvent.VC_UP) {
            if (currentIndex > 0) {
                currentIndex--;
                clearScreen();
                displayItems();
            }
        } else if (e.getKeyCode() == NativeKeyEvent.VC_DOWN) {
            if (currentIndex < items.toArray().length - 1) {
                currentIndex++;
                clearScreen();
                displayItems();
            }
        } else if (e.getKeyCode() == NativeKeyEvent.VC_ENTER) {
            System.out.println("ENTERPRESSED");
        }
    }
    
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
        beginText = modLoader + "  :  " + version + "\n" + "Search query: " + searchquery + "\n";
        System.out.println(beginText);
        int ModWidth = 35;
        
        
        currentIndex = 0;
        items = new ArrayList<>();
        items.add("Found " + result.total_hits + " results");
        for (Hit hit : result.hits)
            items.add(padString(hit.title, ModWidth) + "by " + hit.author);
        
        endText = """
                                
                                Press:
                                Up/Down to move selection
                                Enter to select
                                ESC to quit
                        """;

        new Main();
        System.out.println("meow!");
        AnsiConsole.systemUninstall();
    }
}