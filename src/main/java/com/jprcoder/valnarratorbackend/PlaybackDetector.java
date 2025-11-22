package com.jprcoder.valnarratorbackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.util.Arrays;

class PlaybackDetector {
    private static final Logger logger = LoggerFactory.getLogger(PlaybackDetector.class);
    private static final float RATE = 48000f;
    private static final int BITS = 16;
    private static final int CHANNELS = 1;

    // Rolling average buffer
    // Avg of last 200ms
    // 16-bit = 2 bytes
    // Debounce
    private static final int DEBOUNCE_MS = 120;
    private final float[] rollingDb = new float[20];      // min 10 samples
    private final TargetDataLine line;
    private int rollPtr = 0;
    // Learned thresholds
    private float baselineDb = -60;
    private float noisePeakDb = -55;
    private float detectThresholdDb = -50;
    private long aboveThresholdSince = 0;
    private long belowThresholdSince = 0;
    private PlaybackDetector.State state = PlaybackDetector.State.IDLE;
    private PlaybackDetector.Listener listener;

    public PlaybackDetector() {
        try {
            this.line = openCableOutputLine();
        } catch (LineUnavailableException e) {
            logger.error("Failed to open CABLE Output line: ", e);
            throw new IllegalStateException("Failed to open CABLE Output line.", e);
        }
        this.line.start();
        Arrays.fill(rollingDb, -90);

        logger.info(String.format("BaselineDb: %f", baselineDb));
        logger.info(String.format("NoisePeakDb: %f", noisePeakDb));
        logger.info(String.format("Auto Threshold: %f dB", detectThresholdDb));
    }

    private static double calcRMS(byte[] audio) {
        long sum = 0;
        int samples = audio.length / 2;

        for (int i = 0; i < audio.length; i += 2) {
            int sample = (audio[i + 1] << 8) | (audio[i] & 0xff);
            sum += (long) sample * sample;
        }

        return Math.sqrt(sum / (double) samples) / 32768.0;
    }

    private static double rmsToDb(double rms) {
        if (rms <= 0) return -90;
        return 20.0 * Math.log10(rms);
    }

    private static TargetDataLine openCableOutputLine() throws LineUnavailableException {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixers) {
            if (info.getName().toLowerCase().contains("cable output")) {
                Mixer mixer = AudioSystem.getMixer(info);
                AudioFormat fmt = new AudioFormat(RATE, BITS, CHANNELS, true, false);
                DataLine.Info d = new DataLine.Info(TargetDataLine.class, fmt);
                if (mixer.isLineSupported(d)) {
                    TargetDataLine line = (TargetDataLine) mixer.getLine(d);
                    line.open(fmt);
                    return line;
                }
            }
        }
        throw new IllegalStateException("CABLE Output not found!");
    }

    public void setListener(PlaybackDetector.Listener l) {
        this.listener = l;
    }

    public void calibrateBaseline() {
        logger.debug("Calibrating (silent)...");

        long end = System.currentTimeMillis() + 1000;  // 1 sec silence sampling

        float min = 0, max = -90;
        boolean first = true;

        while (System.currentTimeMillis() < end) {
            float db = readOnce();
            if (first) {
                min = max = db;
                first = false;
            }
            min = Math.min(min, db);
            max = Math.max(max, db);
        }

        this.baselineDb = min;
        this.noisePeakDb = max;
        this.detectThresholdDb = max + 4;  // noise peak + margin

        logger.info(String.format("BaselineDb: %f", baselineDb));
        logger.info(String.format("NoisePeakDb: %f", noisePeakDb));
        logger.info(String.format("Auto Threshold: %f dB", detectThresholdDb));
    }

    /**
     * Read and process one audio chunk
     */
    private float readOnce() {
        byte[] buf = new byte[2048];
        int n = line.read(buf, 0, buf.length);

        if (n <= 0) return -90;

        double rms = calcRMS(buf);
        float db = (float) rmsToDb(rms);

        // Rolling avg insert
        rollingDb[rollPtr++] = db;
        if (rollPtr >= rollingDb.length) rollPtr = 0;

        return db;
    }

    /**
     * Returns smoothed audio level
     */
    public float getSmoothedDb() {
        float sum = 0;
        for (float d : rollingDb) sum += d;
        return sum / rollingDb.length;
    }

    /**
     * POLLING API — best for manual checks
     */
    public boolean isPlaying() {
        updateStateMachine();
        return state == PlaybackDetector.State.PLAYING;
    }

    // --------------------- UTILITIES ---------------------

    /**
     * Must be polled or run inside a worker thread
     */
    private void updateStateMachine() {
        float db = readOnce();
        float smooth = getSmoothedDb();

        long now = System.currentTimeMillis();

        boolean above = smooth > detectThresholdDb;

        if (state == PlaybackDetector.State.IDLE) {
            if (above) {
                if (aboveThresholdSince == 0) aboveThresholdSince = now;

                if (now - aboveThresholdSince >= DEBOUNCE_MS) {
                    state = PlaybackDetector.State.PLAYING;
                    if (listener != null) listener.onAudioStart();
                }
            } else {
                aboveThresholdSince = 0;
            }
        } else if (state == PlaybackDetector.State.PLAYING) {
            if (!above) {
                if (belowThresholdSince == 0) belowThresholdSince = now;

                if (now - belowThresholdSince >= DEBOUNCE_MS) {
                    state = PlaybackDetector.State.IDLE;
                    if (listener != null) listener.onAudioStop();
                }
            } else {
                belowThresholdSince = 0;
            }
        }
    }

    /**
     * For running continuously in background (auto FSM)
     */
    public void startBackgroundThread() {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    updateStateMachine();
                    Thread.sleep(10);
                }
            } catch (Exception ignored) {
            }
        }, "AudioMonitor");
        t.setDaemon(true);
        t.start();
    }

    // FSM States
    public enum State {IDLE, PLAYING}

    // Listener callback
    public interface Listener {
        void onAudioStart();

        void onAudioStop();
    }

}
