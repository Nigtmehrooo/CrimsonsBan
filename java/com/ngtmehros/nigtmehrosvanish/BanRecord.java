package com.ngtmehros.nigtmehrosvanish;

import java.time.Duration;
import java.util.UUID;

public record BanRecord(
        UUID uuid,
        String playerName,
        String bannedBy,
        String reason,
        long createdAtEpochMilli,
        long expiresAtEpochMilli,
        String ipAddress,
        boolean isIpBan
) {

    public BanRecord(UUID uuid, String playerName, String bannedBy, String reason, 
                     long createdAtEpochMilli, long expiresAtEpochMilli) {
        this(uuid, playerName, bannedBy, reason, createdAtEpochMilli, expiresAtEpochMilli, null, false);
    }

    public boolean isPermanent() {
        return expiresAtEpochMilli <= 0;
    }

    public boolean isExpired() {
        return !isPermanent() && System.currentTimeMillis() >= expiresAtEpochMilli;
    }

    public String formattedRemainingDuration() {
        if (isPermanent()) {
            return "Permanent";
        }

        long millis = Math.max(0L, expiresAtEpochMilli - System.currentTimeMillis());
        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0 || builder.isEmpty()) {
            builder.append(minutes).append("m");
        }
        return builder.toString().trim();
    }

    public String formattedBanDate() {
        java.time.Instant instant = java.time.Instant.ofEpochMilli(createdAtEpochMilli);
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    public String formattedExpiryDate() {
        if (isPermanent()) {
            return "Nie";
        }
        java.time.Instant instant = java.time.Instant.ofEpochMilli(expiresAtEpochMilli);
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}
