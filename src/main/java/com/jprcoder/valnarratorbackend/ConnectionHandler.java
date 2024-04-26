package com.jprcoder.valnarratorbackend;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class ConnectionHandler {
    private final HttpClient client;

    public ConnectionHandler() throws KeyManagementException, NoSuchAlgorithmException {
        this.client = createClient();
    }

    private HttpClient createClient() throws KeyManagementException, NoSuchAlgorithmException {
        TrustManager[] trustAllCertificates = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCertificates, null);
        return HttpClient.newBuilder().sslContext(sslContext).build();
    }

    public HttpClient getClient() {
        return client;
    }
}
