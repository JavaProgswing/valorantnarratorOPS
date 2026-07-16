package com.jprcoder.valnarratorbackend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public final class SerialGenerator {

    private SerialGenerator() {
        throw new AssertionError("Utility class");
    }

    private static ArrayList<String> getMBoardSerial() throws IOException {
        ArrayList<String> mBoardSerial = new ArrayList<>();

        Process process;
        try {
            process = Runtime.getRuntime().exec(
                    new String[]{"wmic", "baseboard", "get", "serialnumber"}
            );
        } catch (IOException ignored) {
            return mBoardSerial;
        }

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        try (is) {
            try {
                os.close();
            } catch (IOException ignored) {
            }

            try (Scanner sc = new Scanner(is)) {
                if (sc.hasNextLine()) {
                    sc.nextLine();
                }

                while (sc.hasNextLine()) {
                    String serial = sc.nextLine().trim();

                    if (!serial.isEmpty()) {
                        mBoardSerial.add(serial);
                    }
                }
            }
        }

        return mBoardSerial;
    }

    private static String getDiskSerial() throws IOException {
        Process process;
        try {
            process = Runtime.getRuntime().exec(
                    new String[]{"wmic", "diskdrive", "get", "serialnumber"}
            );
        } catch (IOException ignored) {
            return null;
        }

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        try (is) {
            try {
                os.close();
            } catch (IOException ignored) {
            }

            try (Scanner sc = new Scanner(is)) {
                if (sc.hasNextLine()) {
                    sc.nextLine();
                }

                while (sc.hasNextLine()) {
                    String serial = sc.nextLine().trim();

                    if (!serial.isEmpty()) {
                        return serial;
                    }
                }
            }
        }

        return null;
    }

    private static ArrayList<String> getGPUSerial() throws IOException {
        ArrayList<String> gpuSerial = new ArrayList<>();

        Process process;
        try {
            process = Runtime.getRuntime().exec(
                    new String[]{
                            "wmic",
                            "PATH",
                            "Win32_VideoController",
                            "GET",
                            "PNPDeviceID"
                    }
            );
        } catch (IOException ignored) {
            return gpuSerial;
        }

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        try (is) {
            try {
                os.close();
            } catch (IOException ignored) {
            }

            try (Scanner sc = new Scanner(is)) {
                if (sc.hasNextLine()) {
                    sc.nextLine();
                }

                while (sc.hasNextLine()) {
                    String serial = sc.nextLine().trim();

                    if (serial.isEmpty()) {
                        continue;
                    }

                    int separatorIndex = serial.indexOf('\\');

                    gpuSerial.add(
                            separatorIndex >= 0
                                    ? serial.substring(separatorIndex + 1)
                                    : serial
                    );
                }
            }
        }

        return gpuSerial;
    }

    private static String getCPUSerial() throws IOException {
        Process process;
        try {
            process = Runtime.getRuntime().exec(
                    new String[]{"wmic", "cpu", "get", "ProcessorId"}
            );
        } catch (IOException ignored) {
            return null;
        }

        try (InputStream is = process.getInputStream();
             Scanner sc = new Scanner(is)) {

            while (sc.hasNext()) {
                String next = sc.next();

                if ("ProcessorId".equals(next) && sc.hasNext()) {
                    return sc.next().trim();
                }
            }
        }

        return null;
    }

    public static String getSerialNumber() throws IOException {
        String cpuSerial = getCPUSerial();
        ArrayList<String> gpuSerial = getGPUSerial();
        String diskSerial = getDiskSerial();
        ArrayList<String> mBoardSerial = getMBoardSerial();

        return String.valueOf(
                Objects.hash(
                        cpuSerial,
                        gpuSerial,
                        diskSerial,
                        mBoardSerial
                )
        );
    }
}