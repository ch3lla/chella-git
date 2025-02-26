package com.explore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class Chella {
    private static final String REPO_DIR = ".chella/";
    private static final String OBJECTS_DIR = "objects/";

    private static void init() throws IOException {
        Path repoPath = Paths.get(REPO_DIR);
        if (Files.exists(repoPath)) {
            System.out.println("Repository already initialized.");
            return;
        }
        Files.createDirectories(repoPath);
        Files.createDirectories(Paths.get(REPO_DIR + OBJECTS_DIR));
        Files.createFile(Paths.get(REPO_DIR + "HEAD"));
        Files.createFile(Paths.get(REPO_DIR + "index"));
        System.out.println("Initializing empty chella repository.");
    }

    private static String hashContent(byte[] content){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02X", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("No such algorithm.");
            return null;
        }
    }

    private static byte[] compressContent(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)){
            gos.write(data);
        }
        return baos.toByteArray();
    }

    private static void addFileToStagingArea(String hash, byte[] content) {
        try {
            byte[] compressedFileContent = compressContent(content);
            StringBuilder stagingContent = new StringBuilder();
            Path path = Paths.get(REPO_DIR + "index");
            if (Files.exists(path)) {
                stagingContent.append(Files.readString(path));
            }

            String newStagingContent = hash + " " + Arrays.toString(compressedFileContent) + "\n";
            stagingContent.append(newStagingContent);
            Files.writeString(path, stagingContent.toString(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.out.println("file not found");
        }
    }
    private static void add(String fileName){
        try {
            byte[] fileContent = Files.readAllBytes(Path.of(fileName));
            String hash = hashContent(fileContent);
            if (hash != null) {
                String stagingFolder = hash.substring(0,2) + "/";
                Path subFolderPath = Paths.get(REPO_DIR + OBJECTS_DIR, stagingFolder);
                Files.createDirectories(subFolderPath);

                String stagingFileName = hash.substring(2);
                Path filePath = Paths.get(subFolderPath.toString(), stagingFileName);
                Files.write(filePath, fileContent);

                // add file to staging area
                addFileToStagingArea(hash, fileContent);

                System.out.println("Added " + fileName);
            } else {
                System.out.println("No such algorithm.");
            }
        } catch (IOException e) {
            System.out.println("Error reading file.");
        }
    }

    public static void main(String[] args) throws IOException {

        String command = args[0];

        try {
            switch (command) {
                case "init":
                    init();
                    break;
                case "add":
                    if (args.length < 2) {
                        System.out.println("Usage: java Chella add <file>");
                        return;
                    }
                    add(args[1]);
                    break;
            }
        }  catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
