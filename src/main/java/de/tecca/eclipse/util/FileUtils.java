package de.tecca.eclipse.util;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;

public class FileUtils {

    public static boolean createDirectories(Path path) {
        try {
            Files.createDirectories(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean deleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean copyFile(Path source, Path target) {
        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean moveFile(Path source, Path target) {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean writeString(Path path, String content) {
        try {
            Files.writeString(path, content);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean writeBytes(Path path, byte[] bytes) {
        try {
            Files.write(path, bytes);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static List<Path> listFiles(Path directory, String extension) {
        List<Path> files = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*." + extension)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    files.add(file);
                }
            }
        } catch (IOException e) {
            // Return empty list
        }

        return files;
    }

    public static long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }

    public static boolean fileExists(Path path) {
        return Files.exists(path) && Files.isRegularFile(path);
    }

    public static boolean directoryExists(Path path) {
        return Files.exists(path) && Files.isDirectory(path);
    }

    public static String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    public static String getFileNameWithoutExtension(Path path) {
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
}