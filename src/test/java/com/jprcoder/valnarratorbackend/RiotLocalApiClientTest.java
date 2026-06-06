package com.jprcoder.valnarratorbackend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RiotLocalApiClientTest {

    @Test
    void classLoadDisablesJavaHttpClientHostnameVerificationForLocalRiotApi() {
        RiotLocalApiClient.launchArgument(new JsonObject(), "-noop=");

        assertEquals("true", System.getProperty("jdk.internal.httpclient.disableHostnameVerification"));
    }

    @Test
    void launchArgumentExtractsSubjectAndDeploymentFromValorantSession() {
        JsonObject session = JsonParser.parseString("""
                {
                  "launchConfiguration": {
                    "arguments": [
                      "-remoting-auth-token=secret",
                      "-subject=player-puuid",
                      "-ares-deployment=ap"
                    ]
                  }
                }
                """).getAsJsonObject();

        assertEquals("player-puuid", RiotLocalApiClient.launchArgument(session, "-subject="));
        assertEquals("ap", RiotLocalApiClient.launchArgument(session, "-ares-deployment="));
    }

    @Test
    void launchArgumentReturnsNullForMissingOrMalformedLaunchConfig() {
        assertNull(RiotLocalApiClient.launchArgument(new JsonObject(), "-subject="));
        assertNull(RiotLocalApiClient.launchArgument(JsonParser.parseString("""
                {"launchConfiguration":{"arguments":[]}}
                """).getAsJsonObject(), "-subject="));
        assertNull(RiotLocalApiClient.launchArgument(JsonParser.parseString("""
                {"launchConfiguration":{"arguments":["-subject"]}}
                """).getAsJsonObject(), "-subject="));
    }
}
