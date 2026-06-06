package com.jprcoder.valnarratorbackend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiotUtilityHandlerTest {

    @Test
    void firstCsvFieldExtractsTheImageNameColumn() {
        assertEquals("VALORANT-Win64-Shipping.exe",
                RiotUtilityHandler.firstCsvField("\"VALORANT-Win64-Shipping.exe\",\"1234\",\"Console\""));
    }

    @Test
    void firstCsvFieldHandlesUnquotedAndEscapedQuotedFields() {
        assertEquals("VALORANT-Win64-Shipping.exe",
                RiotUtilityHandler.firstCsvField("VALORANT-Win64-Shipping.exe,1234,Console"));
        assertEquals("VALORANT\"-Win64-Shipping.exe",
                RiotUtilityHandler.firstCsvField("\"VALORANT\"\"-Win64-Shipping.exe\",\"1234\""));
        assertEquals("", RiotUtilityHandler.firstCsvField(null));
        assertEquals("", RiotUtilityHandler.firstCsvField(""));
    }

    @Test
    void isValorantTasklistRowMatchesOnlyTheImageNameColumn() {
        assertTrue(RiotUtilityHandler.isValorantTasklistRow(
                "\"VALORANT-Win64-Shipping.exe\",\"1234\",\"Console\""));
        assertFalse(RiotUtilityHandler.isValorantTasklistRow(
                "\"Discord.exe\",\"1234\",\"VALORANT-Win64-Shipping.exe\""));
    }

    @Test
    void isValorantTasklistRowIsCaseInsensitive() {
        assertTrue(RiotUtilityHandler.isValorantTasklistRow(
                "\"valorant-win64-shipping.exe\",\"1234\",\"Console\""));
    }
}

