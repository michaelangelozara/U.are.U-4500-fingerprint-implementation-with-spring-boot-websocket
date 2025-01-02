package org.Fingerprint.web_socket;

import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.dpfpdd.FidImpl;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Base64;
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

    public static byte[] decodeFingerprint(String encodedFingerprint) {
        return Base64.getDecoder().decode(encodedFingerprint);
    }

    public static Fid decodeFingerprintToFid(String encodedFingerprint) {
        // Step 1: Decode the Base64 encoded fingerprint string
        byte[] fingerprintData = Base64.getDecoder().decode(encodedFingerprint);
        // Step 2: Initialize FidImpl (assuming the format is ANSI_381_2004 and a single view for simplicity)
        return new FidImpl(Fid.Format.ANSI_381_2004, 1);
    }
}
