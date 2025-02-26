package com.explore;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    private static void add(String fileName){
        try {
            byte[] fileContent = Files.readAllBytes(Path.of(fileName));
            String hash = hashContent(fileContent);
            if (hash != null) {
                String stagingFolder = hash.substring(0,2) + "/";
                Path subFolderPath = Paths.get(OBJECTS_DIR, stagingFolder);
                Files.createDirectories(subFolderPath);

                String stagingFileName = hash.substring(2);
                Path filePath = Paths.get(subFolderPath.toString(), stagingFileName);
                Files.write(filePath, fileContent);
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
