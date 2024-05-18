package com.jprcoder.valnarratorbackend;

public record EntitlementsTokenResponse(String accessToken, String[] entitlements, String issuer, String subject,
                                        String token) {
}
