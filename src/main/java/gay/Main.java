package gay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;
import gay.JsonResult;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a line of text:");
        String input = scanner.nextLine();
        URL url = URI.create("https://api.modrinth.com/v2/search?query=" + input).toURL();
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        InputStream meowmeowsillykittycatmrrrp =  connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(meowmeowsillykittycatmrrrp));
        String line = reader.readLine();
        reader.close();
        connection.disconnect();
        System.out.println(line);
        
        
        
    }
}