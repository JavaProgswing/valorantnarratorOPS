package com.jprcoder.valnarratorbackend;

public record MessageQuota(int remainingQuota, String premiumTill, boolean isPremium) {
}
