package org.Fingerprint.web_socket;

import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import javax.swing.*;
import java.lang.reflect.Type;
import java.util.Map;

public class MyStompSessionHandler extends StompSessionHandlerAdapter {
    private String username;
    private MessageListener messageListener;

    public MyStompSessionHandler(MessageListener messageListener, String username) {
        this.username = username;
        this.messageListener = messageListener;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        System.out.println("Client Connected");

        session.subscribe("/topic/receive-fingerprints", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    if (payload instanceof Map) {
                        Map data = (Map) payload;
                        messageListener.onMessageReceive(data);
                    } else {
                        System.out.println("Received unexpected payload type: " + payload.getClass());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // error enrollment
        session.subscribe("/topic/enrollment.failed", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    if (payload instanceof Map) {
                        Map data = (Map) payload;
                        String errorMessage = (String) data.get("message");
                        JOptionPane.showMessageDialog(null, errorMessage, "Something went wrong", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(null, "Something went wrong", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // enrollment succeeded
        session.subscribe("/topic/enrollment.success", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    if (payload instanceof Map) {
                        Map data = (Map) payload;
                        String successMessage = (String) data.get("message");
                        JOptionPane.showMessageDialog(null, successMessage, "Fingerprint is Successfully Registered", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(null, "Something went wrong", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        exception.printStackTrace();
    }
}
