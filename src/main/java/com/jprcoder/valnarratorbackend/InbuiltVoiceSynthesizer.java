package com.jprcoder.valnarratorbackend;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class InbuiltVoiceSynthesizer {
    private Process powershellProcess;
    private PrintWriter powershellWriter;
    private BufferedReader powershellReader;

    public InbuiltVoiceSynthesizer() {
        try {
            // Initialize PowerShell process
            powershellProcess = new ProcessBuilder("powershell.exe", "-NoExit", "-Command", "-").start();
            powershellWriter = new PrintWriter(new OutputStreamWriter(powershellProcess.getOutputStream()), true);
            powershellReader = new BufferedReader(new InputStreamReader(powershellProcess.getInputStream()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getAvailableVoices() {
        List<String> voices = new ArrayList<>();
        try {
            // Send the command to get installed voices and add a marker at the end
            String command = "Add-Type -AssemblyName System.Speech;$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer;$speak.GetInstalledVoices() | Select-Object -ExpandProperty VoiceInfo | Select-Object -Property Name | ConvertTo-Csv -NoTypeInformation | Select-Object -Skip 1; echo 'END_OF_VOICES'";
            powershellWriter.println(command);

            // Read the output until the marker is found
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
        return voices;
    }

    public void speakInbuiltVoice(String voice, String text) {
        try {
            // Send the command to speak with the specified voice
            String command = String.format(
                    "Add-Type -AssemblyName System.Speech;$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer;$speak.SelectVoice('%s');$speak.Rate=0;$speak.Speak('%s');",
                    voice, text
            );
            powershellWriter.println(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            // Close PowerShell process
            if (powershellWriter != null) {
                powershellWriter.println("exit");
                powershellWriter.close();
            }
            if (powershellReader != null) {
                powershellReader.close();
            }
            if (powershellProcess != null) {
                powershellProcess.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
