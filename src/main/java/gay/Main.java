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
    
    enum Page{
        Main,
        ModDownloader,
        ExpandedModTab
    }
    static Page page;
    
    static int currentIndex = 0;
    
    static String beginText;
    static List<String> items;
    static String endText;
    
    static String modLoader;
    static String version;
    
    static boolean inputting;
    static String inputstring;
    
    public Main() {
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
        displayItems();
    }

    static void displayItems() {
        StringBuilder Buffer;
        Buffer = new StringBuilder(beginText + "\n");
        for (int i = 0; i < items.toArray().length; i++) {
            if (i == currentIndex) {
                Buffer.append(ansi().fgBlack().bg(Color.WHITE).a(items.toArray()[i]).reset().toString()).append("\n");
            } else {
                Buffer.append(items.toArray()[i]).append("\n");
            }
        }
        Buffer.append(endText).append("\n");
        clearScreen();
        System.out.println(Buffer);
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (inputting) {
            if (e.getKeyCode() == NativeKeyEvent.VC_ENTER) {
                inputting = false;
                if (moddownloaderrequestingstring) {
                    try {
                        ModDownloader();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return;
            }
            clearScreen();
            System.out.println(beginText);
            if (e.getKeyCode() == NativeKeyEvent.VC_BACKSPACE) {
                if (!inputstring.isEmpty()) {
                    inputstring = inputstring.substring(0, inputstring.length() - 1);
                }
            } else
                inputstring += NativeKeyEvent.getKeyText(e.getKeyCode()).toLowerCase();

            System.out.println(inputstring);
            return;
        }

        if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
            if (page.equals(Page.ModDownloader)) {
                MainPage();
            } else if (page.equals(Page.ExpandedModTab)) {
                try {
                    ModDownloader();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } else if (e.getKeyCode() == NativeKeyEvent.VC_UP) {
            if (currentIndex > 0) {
                currentIndex--;
                displayItems();
            }
        } else if (e.getKeyCode() == NativeKeyEvent.VC_DOWN) {
            if (currentIndex < items.toArray().length - 1) {
                currentIndex++;
                displayItems();
            }
        } else if (e.getKeyCode() == NativeKeyEvent.VC_ENTER) {
            String currentitem = items.get(currentIndex).trim();
            System.out.println(currentitem);
            if (page.equals(Page.Main)) {
                if (currentitem.equals("Download mods")) {
                    try {
                        ModDownloader();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                } else if (currentitem.equals("Quit program")) {
                    try {
                        GlobalScreen.unregisterNativeHook();
                    } catch (NativeHookException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } else if (page.equals(Page.ModDownloader)) {
                String modid = (String)ProjectIDS.toArray()[currentIndex - 1];
                System.out.println(modid);
                try {
                    ExpandModTab(modid);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else if (page.equals(Page.ExpandedModTab)) {
                switch (currentitem) {
                    case "Download mod" -> System.out.println("download");
                    case "Visit mod page" -> System.out.println("mod page visit");
                    case "Return" -> MainPage();
                }
            }
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
    
    static void ExpandModTab(String ProjectID) throws IOException {
        page = Page.ExpandedModTab;
        clearScreen();
        URLConnection connection = new URL("https://api.modrinth.com/v2/project/" + ProjectID).openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        InputStream response = connection.getInputStream();
        String responseText = new String(response.readAllBytes(), StandardCharsets.UTF_8);
        response.close();
        ProjectIdResult result = new Gson().fromJson(responseText, ProjectIdResult.class);

        beginText =
                    result.title + "\n\n" +
                    "support:\nServer:" + result.server_side + "\n" +
                        "Client : " + result.client_side + "\n" +
                        result.description + "\n" +
                        "Published date: " + result.published + "\n" +
                        "Downloads: " + result.downloads + "\n" +
                        "Followers: " + result.followers + "\n" +
                        "categories: " + String.join(", ", result.categories) + "\n" +
                        "Loaders: " + String.join(", ", result.loaders) + "\n";

        items = new ArrayList<>();
        items.add("Download mod");
        items.add("Visit mod page");
        items.add("Return");
        currentIndex = 0;
        endText = "";
        displayItems();
    }
    
    static void FetchInfo() throws IOException {
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

        try (var ServerJarArchive = new ZipFile(jarLocation)) {
            //get modloader
            var modLoaderInputStream = ServerJarArchive.getInputStream(ServerJarArchive.getEntry("META-INF/MANIFEST.MF"));
            modLoader = new String(modLoaderInputStream.readAllBytes(), StandardCharsets.UTF_8);

            for (String line : modLoader.split("\n"))
                if (line.startsWith("Main-Class: "))
                    modLoader = line.replace("Main-Class: ", "").trim();

            System.out.println("Main class: " + modLoader);

            modLoader = switch (modLoader) {
                case "net.fabricmc.installer.ServerLauncher" -> "fabric";
                case "io.papermc.paperclip.Main" -> "paper";
                case "net.minecraftforge.bootstrap.shim.Main" -> "forge";
                default -> "Could Not Find Mod Loader\nPlease use purpur/Fabric/Forge";
            };
            System.out.println("ModLoader: " + modLoader);

            //Version finder\
            version = "";
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
        }
        System.out.println("version: " + version);
    }
    
    static void MainPage(){
        beginText = "Welcome to ModBrowser!\nYou are using " + modLoader + " (Mod)Loader On " + version + "\n";
        items = new ArrayList<>();
        items.add("Start server");
        items.add("Download mods");
        items.add("Quit program");
        endText = "";
        page = Page.Main;
        clearScreen();
        currentIndex = 0;
        displayItems();
    }
    
    static boolean moddownloaderrequestingstring;
    static List<String> ProjectIDS;
    static void ModDownloader() throws IOException {
        beginText = "What mod are you looking for?:";
        if (!moddownloaderrequestingstring) {
            inputstring = "";
            moddownloaderrequestingstring = true;
            inputting = true;
            currentIndex = 0;
            clearScreen();
            System.out.println(beginText);
            return;
        }
        moddownloaderrequestingstring = false;
        
        //Mod Downloading portion
        String searchquery = inputstring.trim();
        URLConnection connection = new URL("https://api.modrinth.com/v2/search?limit=20&query=" +  searchquery +
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
        ProjectIDS = new ArrayList<>();
        items.add("Found " + result.total_hits + " results");
        for (Hit hit : result.hits) {
            ProjectIDS.add(hit.project_id);
            items.add(padString(hit.title, ModWidth) + "by " + hit.author);
        }
        endText = """
                        
                                Press:
                                Up/Down to move selection
                                Enter to select
                                ESC to quit
                        """;
        
        page = Page.ModDownloader;
        displayItems();
    }
    
    public static void main(String[] args) throws IOException {
        AnsiConsole.systemInstall();
        FetchInfo();
        
        MainPage();
        new Main();
    }
}