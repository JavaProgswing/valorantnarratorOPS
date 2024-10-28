package com.jprcoder.valnarratorbackend;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Sign AWS Requests with Signature Version 4 Signing Process.
 */
public class AWSSignatureV4Generator {
    private final String accessKeyID;
    private final String secretAccessKey;
    private final String regionName;
    private final String serviceName;
    private final String httpMethodName;
    private final TreeMap<String, String> queryParameters;
    private final TreeMap<String, String> awsHeaders;
    private final String xAmzDate;
    private final String currentDate;
    private String canonicalURI;
    private String payload;
    // Other variables
    private String signedHeaderString;

    private AWSSignatureV4Generator(Builder builder) {

        accessKeyID = builder.accessKeyID;
        secretAccessKey = builder.secretAccessKey;
        regionName = builder.regionName;
        serviceName = builder.serviceName;
        httpMethodName = builder.httpMethodName;
        canonicalURI = builder.canonicalURI;
        queryParameters = builder.queryParameters;
        awsHeaders = builder.awsHeaders;
        payload = builder.payload;

        // Get current timestamp value.(GTM)
        xAmzDate = getTimeStamp();
        currentDate = getDate();
    }

    /**
     * Hashes the string contents (assumed to be UTF-8) using the SHA-256 algorithm.
     *
     * @param data text to be hashed
     * @return SHA-256 hashed text
     */
    public static String hash(String data) throws NoSuchAlgorithmException {
        MessageDigest messageDigest;
        messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] digest = messageDigest.digest();
        return String.format("%064x", new java.math.BigInteger(1, digest));
    }

    /**
     * Task 1: Create a Canonical Request for Signature Version 4.
     *
     * @return Canonical Request.
     */
    private String prepareCanonicalRequest() throws NoSuchAlgorithmException {

        StringBuilder canonicalURL = new StringBuilder();

        // Step 1.1 Start with the HTTP request method (GET, PUT, POST, etc.), followed by a newline character.
        canonicalURL.append(httpMethodName).append("\n");

        // Step 1.2 Add the canonical URI parameter, followed by a newline character.
        canonicalURI = canonicalURI == null || canonicalURI.trim().isEmpty() ? "/" : canonicalURI;
        canonicalURL.append(canonicalURI).append("\n");

        // Step 1.3 Add the canonical query string, followed by a newline character.
        addCanonicalQueryString(canonicalURL);

        // Step 1.4 Add the canonical headers, followed by a newline character.
        StringBuilder signedHeaders = new StringBuilder();
        if (awsHeaders != null && !awsHeaders.isEmpty()) {
            for (Map.Entry<String, String> entrySet : awsHeaders.entrySet()) {
                String key = entrySet.getKey().toLowerCase();
                signedHeaders.append(key).append(";");
                canonicalURL.append(key).append(":").append(entrySet.getValue().trim()).append("\n");
            }
            /* Note: Each header is followed by a newline character, meaning the complete list ends with
             a newline character. */
            canonicalURL.append("\n");
        } else {
            canonicalURL.append("\n");
        }

        // Step 1.5 Add the signed headers, followed by a newline character.
        signedHeaderString = signedHeaders.substring(0, signedHeaders.length() - 1); // Remove last ";"
        canonicalURL.append(signedHeaderString).append("\n");

        /* Step 1.6 Use a hash (digest) function like SHA256 to create a hashed value from the payload in the body of
        the HTTP or HTTPS. */
        if (payload == null) {
            payload = "";
        }
        canonicalURL.append(hash(payload));
        return canonicalURL.toString();
    }

    /**
     * Add the canonical query string, followed by a newline character.
     *
     * @param canonicalURL Canonical URL
     */
    private void addCanonicalQueryString(StringBuilder canonicalURL) {

        StringBuilder queryString = new StringBuilder();
        if (queryParameters != null && !queryParameters.isEmpty()) {
            for (Map.Entry<String, String> entrySet : queryParameters.entrySet()) {
                String key = entrySet.getKey();
                String value = entrySet.getValue();
                queryString.append(key).append("=").append(encodeParameter(value)).append("&");
            }

            queryString.deleteCharAt(queryString.lastIndexOf("&"));
            queryString.append("\n");
        } else {
            queryString.append("\n");
        }

        canonicalURL.append(queryString);
    }

    /**
     * Task 2: Create a String to Sign for Signature Version 4.
     */
    private String prepareStringToSign(String canonicalURL) throws NoSuchAlgorithmException {

        String stringToSign;

        // Step 2.1 Start with the algorithm designation, followed by a newline character.
        stringToSign = "AWS4-HMAC-SHA256" + "\n";

        // Step 2.2 Append the request date value, followed by a newline character.
        stringToSign += xAmzDate + "\n";

        // Step 2.3 Append the credential scope value, followed by a newline character.
        stringToSign += currentDate + "/" + regionName + "/" + serviceName + "/" + "aws4_request" + "\n";

        /* Step 2.4 Append the hash of the canonical request that you created in Task 1: Create a Canonical Request
        for Signature Version 4. */
        stringToSign += hash(canonicalURL);
        return stringToSign;
    }

    /**
     * Task 3: Calculate the AWS Signature Version 4.
     */
    private String calculateSignature(String stringToSign) throws NoSuchAlgorithmException, InvalidKeyException {
        // Step 3.1 Derive your signing key.
        byte[] signatureKey = getSignatureKey(secretAccessKey, currentDate, regionName, serviceName);
        // Step 3.2 Calculate the signature.
        byte[] signature = hmacSHA256(signatureKey, stringToSign);
        // Step 3.2.1 Encode signature (byte[]) to Hex.
        return bytesToHex(signature);
    }

    /**
     * Task 4: Add the Signing Information to the Request. We'll return a Map of
     * all headers put these headers in your request.
     *
     * @return Headers.
     */
    public Map<String, String> getHeaders() throws NoSuchAlgorithmException, InvalidKeyException {
        awsHeaders.put("x-amz-date", xAmzDate);

        // Execute Task 1: Create a Canonical Request for Signature Version 4.
        String canonicalURL = prepareCanonicalRequest();

        // Execute Task 2: Create a String to Sign for Signature Version 4.
        String stringToSign = prepareStringToSign(canonicalURL);

        // Execute Task 3: Calculate the AWS Signature Version 4.
        String signature = calculateSignature(stringToSign);

        Map<String, String> header = new HashMap<>(0);
        header.put("x-amz-date", xAmzDate);
        header.put("Authorization", buildAuthorizationString(signature));
        return header;
    }

    /**
     * Build string for Authorization header.
     *
     * @param strSignature Signature value.
     * @return Authorization String.
     */
    private String buildAuthorizationString(String strSignature) {

        return "AWS4-HMAC-SHA256" + " "
                + "Credential=" + accessKeyID + "/" + getDate() + "/" + regionName + "/" + serviceName + "/"
                + "aws4_request" + ","
                + "SignedHeaders=" + signedHeaderString + ","
                + "Signature=" + strSignature;
    }

    /**
     * Provides the HMAC SHA 256 encoded value (using the provided key) of the given data.
     *
     * @param data to be encoded.
     * @param key  to use for encoding.
     * @return HMAC SHA 256 encoded byte array.
     * @throws InvalidKeyException      This is the exception for invalid Keys (invalid encoding, wrong length,
     *                                  uninitialized, etc.).
     * @throws NoSuchAlgorithmException When a particular cryptographic algorithm that is requested is not available.
     */
    private byte[] hmacSHA256(byte[] key, String data) throws InvalidKeyException,
            NoSuchAlgorithmException {

        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate AWS signature key.
     *
     * @param key         Key to be used for signing.
     * @param date        Current date stamp.
     * @param regionName  Region name of AWS cloud directory.
     * @param serviceName The Name of the service being addressed.
     * @return Signature key.
     * @throws InvalidKeyException      This is the exception for invalid Keys (invalid encoding, wrong length,
     *                                  uninitialized, etc.).
     * @throws NoSuchAlgorithmException When a particular cryptographic algorithm that is requested is not available.
     */
    private byte[] getSignatureKey(String key, String date, String regionName, String serviceName)
            throws InvalidKeyException, NoSuchAlgorithmException {

        byte[] kSecret = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSHA256(kSecret, date);
        byte[] kRegion = hmacSHA256(kDate, regionName);
        byte[] kService = hmacSHA256(kRegion, serviceName);
        return hmacSHA256(kService, "aws4_request");
    }

    /**
     * Convert a byte array to Hex.
     *
     * @param bytes bytes to be hex encoded.
     * @return hex encoded String of the given byte array.
     */
    private String bytesToHex(byte[] bytes) {

        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars).toLowerCase();
    }

    /**
     * Get timestamp with yyyyMMdd'T'HHmmss'Z' format.
     *
     * @return Time stamp value.
     */
    private String getTimeStamp() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GTM"));// Set the server time as GTM
        return dateFormat.format(new Date());
    }

    /**
     * Get date with yyyyMMdd format.
     *
     * @return Date.
     */
    private String getDate() {

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GTM"));// Set the server time as GTM
        return dateFormat.format(new Date());
    }

    /**
     * Encode string value.
     *
     * @param param String value that needs to be encoded.
     * @return encoded string.
     */
    private String encodeParameter(String param) {
        return URLEncoder.encode(param, StandardCharsets.UTF_8);
    }

    public static class Builder {

        private final String accessKeyID;
        private final String secretAccessKey;
        private String regionName;
        private String serviceName;
        private String httpMethodName;
        private String canonicalURI;
        private TreeMap<String, String> queryParameters;
        private TreeMap<String, String> awsHeaders;
        private String payload;

        public Builder(String accessKeyID, String secretAccessKey) {
            this.accessKeyID = accessKeyID;
            this.secretAccessKey = secretAccessKey;
        }

        public Builder regionName(String regionName) {

            this.regionName = regionName;
            return this;
        }

        public Builder serviceName(String serviceName) {

            this.serviceName = serviceName;
            return this;
        }

        public Builder httpMethodName(String httpMethodName) {

            this.httpMethodName = httpMethodName;
            return this;
        }

        public Builder canonicalURI(String canonicalURI) {

            this.canonicalURI = canonicalURI;
            return this;
        }

        public Builder queryParameters(TreeMap<String, String> queryParameters) {

            this.queryParameters = queryParameters;
            return this;
        }

        public Builder awsHeaders(TreeMap<String, String> awsHeaders) {
            this.awsHeaders = awsHeaders;
            return this;
        }

        public Builder payload(String payload) {

            this.payload = payload;
            return this;
        }

        public AWSSignatureV4Generator build() {

            return new AWSSignatureV4Generator(this);
        }
    }
}