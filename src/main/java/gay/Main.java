package gay;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
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
            case "net.fabricmc.installer.ServerLauncher" -> "Fabric";
            case "io.papermc.paperclip.Main" -> "Paper";
            case "org.bukkit.craftbukkit.bootstrap.Main" -> "Bukkit";
            case "net.minecraftforge.bootstrap.shim.Main" -> "Forge";
            default -> "Could Not Find Mod Loader";
        };
        
        
        System.out.println("ModLoader: " + modLoader);

        /*        InputStream stream = ServerJarArchive.getInputStream(ServerJarArchive.getEntry("version.json"));
        System.out.println(new String(stream.readAllBytes(), StandardCharsets.UTF_8));*/

        //noinspection InfiniteLoopStatement
        do {
            System.out.println("What mod are you looking for?:");
            URL url = URI.create("https://api.modrinth.com/v2/search?query=" + scanner.nextLine()).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            InputStream meowmeowsillykittycatmrrrp = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(meowmeowsillykittycatmrrrp));
            String line = reader.readLine();
            reader.close();
            connection.disconnect();

            SearchResult result = new Gson().fromJson(line, SearchResult.class);
            for (Hit hit : result.hits)
                System.out.println(hit.title + "   " + hit.author);
        } while (true);
    }
}