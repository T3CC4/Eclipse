package de.tecca.eclipse.util;

import java.util.UUID;
import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private static final Pattern PERMISSION_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._-]+$"
    );

    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_-]+$"
    );

    public static boolean isValidUUID(String uuid) {
        return uuid != null && UUID_PATTERN.matcher(uuid).matches();
    }

    public static boolean isValidPermission(String permission) {
        return permission != null &&
                permission.length() > 0 &&
                permission.length() <= 100 &&
                PERMISSION_PATTERN.matcher(permission).matches();
    }

    public static boolean isValidCommand(String command) {
        return command != null &&
                command.length() > 0 &&
                command.length() <= 32 &&
                COMMAND_PATTERN.matcher(command).matches();
    }

    public static boolean isValidGroupName(String groupName) {
        return groupName != null &&
                groupName.length() > 0 &&
                groupName.length() <= 32 &&
                groupName.matches("^[a-zA-Z0-9_-]+$");
    }

    public static boolean isValidEventName(String eventName) {
        return eventName != null &&
                eventName.length() > 0 &&
                eventName.length() <= 64 &&
                eventName.matches("^[a-zA-Z0-9._-]+$");
    }

    public static boolean isValidFileName(String fileName) {
        return fileName != null &&
                fileName.length() > 0 &&
                fileName.length() <= 255 &&
                !fileName.contains("..") &&
                !fileName.contains("/") &&
                !fileName.contains("\\");
    }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isPositive(Number value) {
        return value != null && value.doubleValue() > 0;
    }

    public static boolean isNonNegative(Number value) {
        return value != null && value.doubleValue() >= 0;
    }

    public static boolean isInRange(Number value, Number min, Number max) {
        if (value == null || min == null || max == null) {
            return false;
        }

        double val = value.doubleValue();
        double minVal = min.doubleValue();
        double maxVal = max.doubleValue();

        return val >= minVal && val <= maxVal;
    }

    public static String sanitizeString(String input) {
        if (input == null) {
            return null;
        }

        return input.trim().replaceAll("[\\r\\n\\t]", "");
    }

    public static UUID parseUUID(String uuid) {
        if (!isValidUUID(uuid)) {
            return null;
        }

        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}