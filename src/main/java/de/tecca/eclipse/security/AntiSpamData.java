package de.tecca.eclipse.security;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Anti-spam data for players
 */
class AntiSpamData {
    private final Queue<ChatMessage> recentMessages;
    private final long maxAge = 30000; // 30 seconds

    public AntiSpamData() {
        this.recentMessages = new LinkedList<>();
    }

    public synchronized boolean isSpam(String message, int threshold, long windowMs) {
        long now = System.currentTimeMillis();

        // Clean old messages
        recentMessages.removeIf(msg -> now - msg.timestamp > maxAge);

        // Count similar messages in the window
        long windowStart = now - windowMs;
        int similarCount = 0;

        for (ChatMessage msg : recentMessages) {
            if (msg.timestamp >= windowStart && isSimilar(message, msg.content)) {
                similarCount++;
            }
        }

        return similarCount >= threshold;
    }

    public synchronized void addMessage(String message) {
        long now = System.currentTimeMillis();
        recentMessages.offer(new ChatMessage(message, now));

        // Keep only recent messages
        recentMessages.removeIf(msg -> now - msg.timestamp > maxAge);
    }

    private boolean isSimilar(String msg1, String msg2) {
        if (msg1.equals(msg2)) return true;

        // Remove spaces and convert to lowercase for comparison
        String clean1 = msg1.replaceAll("\\s+", "").toLowerCase();
        String clean2 = msg2.replaceAll("\\s+", "").toLowerCase();

        if (clean1.equals(clean2)) return true;

        // Check if messages are very similar (80% similarity)
        return calculateSimilarity(clean1, clean2) > 0.8;
    }

    private double calculateSimilarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;

        return (maxLen - levenshteinDistance(s1, s2)) / (double) maxLen;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    public boolean isExpired(long currentTime) {
        return recentMessages.isEmpty() ||
                (currentTime - recentMessages.peek().timestamp) > maxAge * 2;
    }

    private static class ChatMessage {
        final String content;
        final long timestamp;

        ChatMessage(String content, long timestamp) {
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}