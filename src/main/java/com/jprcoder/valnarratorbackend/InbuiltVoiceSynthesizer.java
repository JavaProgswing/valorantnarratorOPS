package com.jprcoder.valnarratorbackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class InbuiltVoiceSynthesizer {
    private static final Logger logger = LoggerFactory.getLogger(InbuiltVoiceSynthesizer.class);
    private Process powershellProcess;
    private PrintWriter powershellWriter;
    private BufferedReader powershellReader;
    private final List<String> voices = new ArrayList<>();

    public InbuiltVoiceSynthesizer() {
        try {
            powershellProcess = new ProcessBuilder("powershell.exe", "-NoExit", "-Command", "-").start();
            powershellWriter = new PrintWriter(new OutputStreamWriter(powershellProcess.getOutputStream()), true);
            powershellReader = new BufferedReader(new InputStreamReader(powershellProcess.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String command = "Add-Type -AssemblyName System.Speech;$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer;$speak.GetInstalledVoices() | Select-Object -ExpandProperty VoiceInfo | Select-Object -Property Name | ConvertTo-Csv -NoTypeInformation | Select-Object -Skip 1; echo 'END_OF_VOICES'";
            powershellWriter.println(command);

            String line;
            while ((line = powershellReader.readLine()) != null) {
                if (line.trim().equals("END_OF_VOICES")) {
                    break;
                }
                if (!line.trim().isEmpty()) {
                    voices.add(line.replace("\"", "").trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (voices.isEmpty()) {
            logger.warn("No inbuilt voices found.");
        } else {
            logger.info(String.format("Found %d inbuilt voices.", voices.size()));
            speakInbuiltVoice(voices.get(0), "Inbuilt voice synthesizer initialized.", (short) 100);
        }

        try {
            String fileLocation = String.format("%s/ValorantNarrator/SoundVolumeView.exe", System.getenv("ProgramFiles").replace("\\", "/"));
            long pid = powershellProcess.pid();
            String command = fileLocation + " /SetAppDefault \"CABLE Input\" all " + pid;
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            logger.error(String.format("SoundVolumeView.exe generated an error: %s", (Object) e.getStackTrace()));
        }
    }

    public List<String> getAvailableVoices() {
        return voices;
    }

    public void speakInbuiltVoice(String voice, String text, short rate) {
        rate = (short) (rate / 10.0 - 10);

        try {
            String command = String.format("Add-Type -AssemblyName System.Speech;$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer;$speak.SelectVoice('%s');$speak.Rate=%d;$speak.Speak('%s');", voice, rate, text);
            powershellWriter.println(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
