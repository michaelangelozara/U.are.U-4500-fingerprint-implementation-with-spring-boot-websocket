package org.Fingerprint.web_socket;

import java.util.Map;

public interface MessageListener {
    void onMessageReceive(Map<String, Object> message) throws Exception;
}
