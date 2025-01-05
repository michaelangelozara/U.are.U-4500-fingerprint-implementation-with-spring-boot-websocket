package org.Fingerprint.ui;

import com.digitalpersona.uareu.*;
import org.Fingerprint.web_socket.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Verification
        extends JPanel
        implements ActionListener {
    private static final long serialVersionUID = 6;
    private static final String ACT_BACK = "back";

    private CaptureThread m_capture;
    private Reader m_reader;
    private Fmd[] m_fmds;
    private JDialog m_dlgParent;
    private JTextArea m_text;
    private static List<Fingerprint> encodedFingerprints;

    private static MyStompClient myStompClient;

    private final String m_strPrompt1 = "Verification started\n    put any finger on the reader\n\n";

    private Verification(Reader reader) {
        m_reader = reader;
        m_fmds = new Fmd[1];
        encodedFingerprints = new ArrayList<>();

        final int vgap = 5;
        final int width = 380;

        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(layout);

        m_text = new JTextArea(22, 1);
        m_text.setEditable(false);
        JScrollPane paneReader = new JScrollPane(m_text);
        add(paneReader);
        Dimension dm = paneReader.getPreferredSize();
        dm.width = width;
        paneReader.setPreferredSize(dm);

        add(Box.createVerticalStrut(vgap));

        JButton btnBack = new JButton("Back");
        btnBack.setActionCommand(ACT_BACK);
        btnBack.addActionListener(this);
        add(btnBack);
        add(Box.createVerticalStrut(vgap));

        setOpaque(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(ACT_BACK)) {
            StopCaptureThread();
        } else if (e.getActionCommand().equals(CaptureThread.ACT_CAPTURE)) {
            CaptureThread.CaptureEvent evt = (CaptureThread.CaptureEvent) e;
            if (ProcessCaptureResult(evt)) {
                WaitForCaptureThread();
                StartCaptureThread();
            } else {
                m_dlgParent.setVisible(false);
            }
        }
    }

    private void StartCaptureThread() {
        m_capture = new CaptureThread(m_reader, false, Fid.Format.ANSI_381_2004, Reader.ImageProcessing.IMG_PROC_DEFAULT);
        m_capture.start(this);
    }

    private void StopCaptureThread() {
        if (null != m_capture) m_capture.cancel();
    }

    private void WaitForCaptureThread() {
        if (null != m_capture) m_capture.join(1000);
    }

    private boolean ProcessCaptureResult(CaptureThread.CaptureEvent evt) {
        boolean bCanceled = false;

        if (null != evt.capture_result) {
            if (null != evt.capture_result.image && Reader.CaptureQuality.GOOD == evt.capture_result.quality) {
                Engine engine = UareUGlobal.GetEngine();

                try {
                    // Create FMD from captured image
                    Fmd capturedFmd = engine.CreateFmd(evt.capture_result.image, Fmd.Format.ANSI_378_2004);
                    m_fmds[0] = capturedFmd;

                    if (null != m_fmds[0]) {
                        boolean matchFound = false;
                        Map<String, String> tempMap = new HashMap<>();
                        for (var encodedFingerprint : encodedFingerprints) {
                            try {
                                // Decode stored fingerprint
                                byte[] fingerprintBytes = Base64.getDecoder().decode(encodedFingerprint.getFingerprint());

                                // Create FMD directly from stored data
                                Fmd storedFmd = UareUGlobal.GetImporter().ImportFmd(fingerprintBytes, Fmd.Format.ANSI_378_2004, Fmd.Format.ANSI_378_2004);

                                // Compare fingerprints
                                int falsematch_rate = engine.Compare(m_fmds[0], 0, storedFmd, 0);
                                int target_falsematch_rate = Engine.PROBABILITY_ONE / 100000;

                                if (falsematch_rate < target_falsematch_rate) {
                                    String ip = IpApiUtil.getMyIp();
                                    if (ip == null) {
                                        JOptionPane.showMessageDialog(null, "Failed to send fingerprint print");
                                        break;
                                    }

                                    tempMap.put("ip", ip);
                                    tempMap.put("fingerprint_matched_id", String.valueOf(encodedFingerprint.getId()));
                                    matchFound = true;
                                    m_text.append("Fingerprint matched!\n");
                                    String str = String.format("Dissimilarity score: 0x%x\n", falsematch_rate);
                                    m_text.append(str);
                                    str = String.format("False match rate: %e\n\n", (double) (falsematch_rate) / Engine.PROBABILITY_ONE);
                                    m_text.append(str);
                                    myStompClient.sendValidatedFingerprint(tempMap);

                                    // re-fetch the fingerprints of the current user
                                    fetchFingerprint();
                                    break;
                                }
                            } catch (UareUException e) {
                                m_text.append("Error comparing fingerprints: " + e.getMessage() + "\n");
                            }
                        }

                        if (!matchFound) {
                            m_text.append("No matching fingerprint found.\n\n");
                        }

                        // Reset for next capture
                        m_fmds[0] = null;
                        m_text.append(m_strPrompt1);
                    }
                } catch (UareUException e) {
                    MessageBox.DpError("Error processing fingerprint", e);
                    bCanceled = true;
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Error processing fingerprint");
                }
            } else if (Reader.CaptureQuality.CANCELED == evt.capture_result.quality) {
                bCanceled = true;
            } else {
                MessageBox.BadQuality(evt.capture_result.quality);
            }
        } else if (null != evt.exception) {
            MessageBox.DpError("Capture", evt.exception);
            bCanceled = true;
        } else if (null != evt.reader_status) {
            MessageBox.BadStatus(evt.reader_status);
            bCanceled = true;
        }

        return !bCanceled;
    }

    private void doModal(JDialog dlgParent) {
        try {
            m_reader.Open(Reader.Priority.COOPERATIVE);
            StartCaptureThread();
            m_text.append(m_strPrompt1);

            m_dlgParent = dlgParent;
            m_dlgParent.setContentPane(this);
            m_dlgParent.pack();
            m_dlgParent.setLocationRelativeTo(null);
            m_dlgParent.toFront();
            m_dlgParent.setVisible(true);
            m_dlgParent.dispose();

            StopCaptureThread();
            WaitForCaptureThread();
            m_reader.Close();
        } catch (UareUException e) {
            MessageBox.DpError("Reader operation failed", e);
        }
    }

    private static void fetchFingerprint() throws ExecutionException, InterruptedException {
        if (encodedFingerprints != null)
            encodedFingerprints.clear();

        myStompClient = new MyStompClient(new MessageListener() {
            @Override
            public void onMessageReceive(Map<String, Object> message) throws Exception {
                Object body = message.get("body");
                if (body instanceof Map) {
                    encodedFingerprints.addAll(Objects.requireNonNull(FingerprintUtil.getFingerprints(body)));
                } else {
                    System.out.println("Unexpected body type: " + body.getClass());
                }
            }
        }, "");
    }

    public static void Run(Reader reader) throws ExecutionException, InterruptedException {
        fetchFingerprint();
        JDialog dlg = new JDialog((JDialog) null, "Verification", true);
        Verification verification = new Verification(reader);
        verification.doModal(dlg);
    }
}