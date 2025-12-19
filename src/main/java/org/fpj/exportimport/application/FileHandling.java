package org.fpj.exportimport.application;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHandling {

    public static String openFileChooserAndGetPath(Window ownerWindow) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Datei auswählen");
        File selectedFile = fileChooser.showOpenDialog(ownerWindow);
        if (selectedFile != null) {
            return selectedFile.getAbsolutePath();
        } else {
            return null;
        }
    }

    public static String openDirectoryChooserAndGetPath(Window ownerWindow) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Ordner auswählen");
        File selectedDir = directoryChooser.showDialog(ownerWindow);
        return (selectedDir != null) ? selectedDir.getAbsolutePath() : null;
    }

    /** Legt die Datei im Zielordner an und hängt bei Namenskollisionen V1, V2, ... an (vor der Dateiendung). */
    public static File createFileVersioned(String folderPath, String fileName) throws IOException {
        if (folderPath == null || folderPath.isBlank()) {
            throw new IllegalArgumentException("folderPath darf nicht leer sein");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName darf nicht leer sein");
        }

        File dir = new File(folderPath);

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Ordner konnte nicht erstellt werden: " + dir.getAbsolutePath());
            }
        } else if (!dir.isDirectory()) {
            throw new IOException("Pfad ist kein Ordner: " + dir.getAbsolutePath());
        }

        String base = baseName(fileName);
        String ext  = extension(fileName);

        File candidate = new File(dir, base + ext);
        if (candidate.createNewFile()) {
            return candidate;
        }

        for (int n = 1; n < Integer.MAX_VALUE; n++) {
            candidate = new File(dir, base + "V" + n + ext);
            if (candidate.createNewFile()) {
                return candidate;
            }
        }

        throw new IOException("Keine freie Version gefunden (Zählerüberlauf): " + fileName);
    }

    /** Liefert den Dateinamen ohne Extension (z. B. "a.txt" => "a") für die Suffixbildung. */
    private static String baseName(String fileName) {
        int lastSep = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        String nameOnly = (lastSep >= 0) ? fileName.substring(lastSep + 1) : fileName;

        int dot = nameOnly.lastIndexOf('.');
        if (dot <= 0) return nameOnly;
        return nameOnly.substring(0, dot);
    }

    /** Liefert die Extension inklusive Punkt oder leer (z. B. "a.txt" 0> ".txt") für die Suffixbildung. */
    private static String extension(String fileName) {
        int lastSep = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        String nameOnly = (lastSep >= 0) ? fileName.substring(lastSep + 1) : fileName;

        int dot = nameOnly.lastIndexOf('.');
        if (dot <= 0) return "";
        return nameOnly.substring(dot);
    }

    public static OutputStream openFileAsOutStream(String filePath) throws IOException {
        return Files.newOutputStream(Path.of(filePath));
    }

    public static InputStream openFileAsStream(String filePath) throws IOException {
        return Files.newInputStream(Path.of(filePath));
    }
}
