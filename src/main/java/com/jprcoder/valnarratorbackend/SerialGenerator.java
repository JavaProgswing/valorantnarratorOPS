package com.jprcoder.valnarratorbackend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class SerialGenerator {

    private static ArrayList<String> getMBoardSerial() throws IOException {
        ArrayList<String> MBoardSerial = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(new String[]{"wmic", "baseboard", "get", "serialnumber"});
        } catch (IOException ignored) {
            return MBoardSerial;
        }

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        try (is) {
            try {
                os.close();
            } catch (IOException ignored) {
            }
            Scanner sc = new Scanner(is);
            sc.nextLine();
            while (sc.hasNext()) {
                String serial = sc.nextLine().trim().replace("\n", "");
                if (serial.isEmpty()) continue;
                MBoardSerial.add(serial);
            }
        }
        return MBoardSerial;
    }


    private static ArrayList<String> getDiskSerial() throws IOException {
        ArrayList<String> diskSerial = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(new String[]{"wmic", "diskdrive", "get", "serialnumber"});
        } catch (IOException ignored) {
            return diskSerial;
        }

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        try (is) {
            try {
                os.close();
            } catch (IOException ignored) {
            }
            Scanner sc = new Scanner(is);
            sc.nextLine();
            while (sc.hasNext()) {
                String serial = sc.nextLine().trim().replace("\n", "");
                if (serial.isEmpty()) continue;
                diskSerial.add(serial);
            }
        }
        return diskSerial;
    }


    private static ArrayList<String> getGPUSerial() throws IOException {
        ArrayList<String> gpuSerial = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(new String[]{"wmic", "PATH", "Win32_VideoController", "GET", "PNPDeviceID"});
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
            Scanner sc = new Scanner(is);
            sc.nextLine();
            while (sc.hasNext()) {
                String serial = sc.nextLine().trim().replace("\n", "");
                if (serial.isEmpty()) continue;
                gpuSerial.add(serial.substring(serial.indexOf('\\') + 1));
            }
        }
        return gpuSerial;
    }


    private static String getCPUSerial() throws IOException {
        String cpuSerial = null;
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(new String[]{"wmic", "cpu", "get", "ProcessorId"});
        } catch (IOException ignored) {
            return null;
        }
        InputStream is = process.getInputStream();

        try (is) {
            Scanner sc = new Scanner(is);
            while (sc.hasNext()) {
                String next = sc.next();
                if ("ProcessorId".equals(next)) {
                    cpuSerial = sc.next().trim();
                    break;
                }
            }
        }
        return cpuSerial;
    }

    public static String getSerialNumber() throws IOException {
        String cpuSerial = getCPUSerial();
        ArrayList<String> gpuSerial = getGPUSerial();
        ArrayList<String> hddSerial = getDiskSerial();
        ArrayList<String> mBoardSerial = getMBoardSerial();
        return String.valueOf(Objects.hash(cpuSerial, gpuSerial, hddSerial, mBoardSerial));
    }
}
