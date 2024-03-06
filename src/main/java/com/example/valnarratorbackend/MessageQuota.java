package com.example.valnarratorbackend;

public record MessageQuota(int remainingQuota, String premiumTill, boolean isPremium) {
}
