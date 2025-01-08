package org.Fingerprint.web_socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FingerprintUtil {

    public static List<Fingerprint> getFingerprints(Object body) throws Exception {
        List<Fingerprint> encodedFingerprints = new ArrayList<>();
        String myIp = UserUtil.getMyIp();

        // Handle as a nested Map
        Map<String, Object> bodyMap = (Map<String, Object>) body;
        Object data = bodyMap.get("data");
        if (data instanceof List) {
            List<Object> objects = (List<Object>) data;
            for (var object : objects) {
                Map<String, Object> tempObject = (Map<String, Object>) object;
                String webIp = (String) tempObject.get("ip");

                // check if the ip in the website and the desktop are the same
                // if they are the same means the user that uses the desktop and website is correct
                if (webIp.equals(myIp)) {
                    try{
                        List<Object> tempFingerprintList = (List<Object>) tempObject.get("fingerprints");
                        for (var tempFingerprint : tempFingerprintList) {
                            Map<String, String> actualFingerprint = (Map<String, String>) tempFingerprint;
                            encodedFingerprints.add(
                                    new Fingerprint(Integer.parseInt(actualFingerprint.get("id")),
                                            actualFingerprint.get("data"))
                            );
                        }
                    }catch (NumberFormatException e){
                        JOptionPane.showMessageDialog(null, "Something went wrong");
                    }

                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "No Fingerprint Found");
            return null;
        }

        return encodedFingerprints;
    }

    public static List<String> getAllStoredFingerprints(){
        String urlString = "http://localhost:8080/api/v1/auth/fingerprints";
        String jsonPayload = "{\"code\":\"3C1224C7DC1569381DE63C223113D9A524A1DE63C223113D9A524AF8E2A3113D9A524A1DE63C223113D9A524AF8E2A63C1224C7DC1569381DE63F8E2A63C1224C7DC1569381DE63C63C1224C7DC1569381DE63F8E2A63C1224C7DC1569381DE63CDE1D7C5F65177BF5\"}";
        StringBuilder response = new StringBuilder();

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Send the JSON payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read the response
            int status = connection.getResponseCode();
            if (status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line.trim());
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

        try {
            // Parse the JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            // Navigate to the "fingerprints" array
            JsonNode fingerprintsNode = rootNode.path("data").path("fingerprints");

            // Extract the fingerprints into a list
            List<String> fingerprints = new ArrayList<>();
            if (fingerprintsNode.isArray()) {
                for (JsonNode fingerprint : fingerprintsNode) {
                    fingerprints.add(fingerprint.asText());
                }
            }

            return fingerprints;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getWebSocketCode(){
        return "3C1224C7DC1569381DE63C223113D9A524A1DE63C223113D9A524AF8E2A3113D9A524A1DE63C223113D9A524AF8E2A63C1224C7DC1569381DE63F8E2A63C1224C7DC1569381DE63C63C1224C7DC1569381DE63F8E2A63C1224C7DC1569381DE63CDE1D7C5F65177BF5";
    }


}
