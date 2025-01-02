package org.Fingerprint.web_socket;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UserUtil {
    public static String username = "";

    public static String getMyIp() throws Exception{
        // Create a URL object
        URL obj = new URL("https://api.ipify.org?format=json");

        // Open a connection to the URL
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // Set the request method (GET, POST, etc.)
        con.setRequestMethod("GET");

        // Set request headers (if needed)
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        // Get the response code
        int responseCode = con.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        // Read the response from the server
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Parse the JSON response to get the IP address
        JSONObject myResponse = new JSONObject(response.toString());

        // Extract and return the IP address
        return myResponse.getString("ip");
    }
}
