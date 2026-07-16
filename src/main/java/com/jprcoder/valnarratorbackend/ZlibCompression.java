package com.jprcoder.valnarratorbackend;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class ZlibCompression {

    private ZlibCompression() {
        throw new AssertionError("Utility class");
    }

    public static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(
                Deflater.DEFAULT_COMPRESSION,
                true
        );

        try {
            deflater.setInput(data);
            deflater.finish();

            byte[] buffer = new byte[1024];
            ByteArrayOutputStream result =
                    new ByteArrayOutputStream(data.length);

            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                result.write(buffer, 0, count);
            }

            return result.toByteArray();
        } finally {
            deflater.end();
        }
    }

    public static String deflateAndBase64Encode(String data) {
        byte[] compressedData = deflate(
                data.getBytes(StandardCharsets.UTF_8)
        );

        return Base64.getEncoder()
                .encodeToString(compressedData);
    }

    public static byte[] inflate(byte[] compressedData)
            throws DataFormatException {

        Inflater inflater = new Inflater(true);

        try {
            inflater.setInput(compressedData);

            byte[] buffer = new byte[1024];
            ByteArrayOutputStream result =
                    new ByteArrayOutputStream(compressedData.length * 2);

            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);

                if (count > 0) {
                    result.write(buffer, 0, count);
                    continue;
                }

                if (inflater.needsInput()) {
                    throw new DataFormatException(
                            "Incomplete deflate stream"
                    );
                }

                if (inflater.needsDictionary()) {
                    throw new DataFormatException(
                            "Deflate stream requires a dictionary"
                    );
                }

                throw new DataFormatException(
                        "Unable to make progress while inflating data"
                );
            }

            return result.toByteArray();
        } finally {
            inflater.end();
        }
    }

    public static String decodeBase64AndInflate(String data)
            throws DataFormatException {

        byte[] compressedData = Base64.getDecoder().decode(data);
        byte[] inflatedData = inflate(compressedData);

        return new String(
                inflatedData,
                StandardCharsets.UTF_8
        );
    }
}