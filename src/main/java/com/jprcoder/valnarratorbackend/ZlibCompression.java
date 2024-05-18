package com.jprcoder.valnarratorbackend;

import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ZlibCompression {
    public static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true); // Pass true for nowrap option
        deflater.setInput(data);
        deflater.finish();

        byte[] buffer = new byte[1024];
        byte[] result = new byte[0];

        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            byte[] temp = new byte[result.length + count];
            System.arraycopy(result, 0, temp, 0, result.length);
            System.arraycopy(buffer, 0, temp, result.length, count);
            result = temp;
        }

        deflater.end();

        return result;
    }

    public static String deflateAndBase64Encode(String data) {
        byte[] compressedData = deflate(data.getBytes());
        return Base64.getEncoder().encodeToString(compressedData);
    }

    public static byte[] inflate(byte[] compressedData) throws DataFormatException {
        Inflater inflater = new Inflater(true);
        inflater.setInput(compressedData);

        byte[] buffer = new byte[1024];
        byte[] result = new byte[0];

        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            byte[] temp = new byte[result.length + count];
            System.arraycopy(result, 0, temp, 0, result.length);
            System.arraycopy(buffer, 0, temp, result.length, count);
            result = temp;
        }

        inflater.end();

        return result;
    }

    public static String decodeBase64AndInflate(String data) throws DataFormatException {
        byte[] compressedData = Base64.getDecoder().decode(data);
        byte[] inflatedData = inflate(compressedData);
        return new String(inflatedData);
    }
}
