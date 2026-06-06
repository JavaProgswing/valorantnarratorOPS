package com.jprcoder.valnarratorbackend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class APIHandlerTest {

    @Test
    void extractRiotClientDetailsPrefersValorantSessionAndReadsLaunchArgs() {
        Map<String, JsonObject> sessions = new LinkedHashMap<>();
        sessions.put("host_app", JsonParser.parseString("{\"version\":\"ignored\"}").getAsJsonObject());
        sessions.put("league", JsonParser.parseString("""
                {
                  "productId": "league_of_legends",
                  "version": "bad-version",
                  "launchConfiguration": {"arguments": ["-subject=wrong", "-ares-deployment=na"]}
                }
                """).getAsJsonObject());
        sessions.put("valorant", JsonParser.parseString("""
                {
                  "productId": "valorant",
                  "version": "release-10.11",
                  "launchConfiguration": {"arguments": ["-subject=puuid", "-ares-deployment=ap"]}
                }
                """).getAsJsonObject());

        RiotClientDetails details = APIHandler.extractRiotClientDetails(sessions);

        assertNotNull(details);
        assertEquals("release-10.11", details.version());
        assertEquals("puuid", details.subject_id());
        assertEquals("ap", details.subject_deployment());
    }

    @Test
    void extractRiotClientDetailsReturnsNullWhenRequiredFieldsAreMissing() {
        Map<String, JsonObject> sessions = Map.of(
                "valorant", JsonParser.parseString("""
                        {
                          "productId": "valorant",
                          "version": "release-10.11",
                          "launchConfiguration": {"arguments": ["-subject=puuid"]}
                        }
                        """).getAsJsonObject());

        assertNull(APIHandler.extractRiotClientDetails(sessions));
        assertNull(APIHandler.extractRiotClientDetails(null));
    }
}
