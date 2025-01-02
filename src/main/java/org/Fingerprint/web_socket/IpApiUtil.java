package org.Fingerprint.web_socket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IpApiUtil {
    public static String getMyIp(){
        String urlString = "https://api.ipify.org?format=json"; // Example URL
        StringBuilder response = new StringBuilder();

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // Check the response code
            int status = connection.getResponseCode();
            if (status == 200) {
                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
            } else {
                response.append("Error: ").append(status);
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        String jsonResponse = response.toString();
        return jsonResponse.replaceAll(".*\"ip\":\"([^\"]+)\".*", "$1");
    }
}
