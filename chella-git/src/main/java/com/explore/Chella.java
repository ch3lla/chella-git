package com.explore;

import java.io.IOException;
import java.nio.file.*;

public class Chella {
    private static final String REPO_DIR = ".chella/";

    private static void init() throws IOException {
        Path repoPath = Paths.get(REPO_DIR);
        if (Files.exists(repoPath)) {
            System.out.println("Repository already initialized.");
            return;
        }
        Files.createDirectories(repoPath);
        Files.createDirectories(Paths.get(REPO_DIR + "objects"));
        Files.createFile(Paths.get(REPO_DIR + "HEAD"));
        Files.createFile(Paths.get(REPO_DIR + "index"));
        System.out.println("Initializing empty chella repository.");
    }

    private static void add(String fileName){

    }

    public static void main(String[] args) throws IOException {

        String command = args[0];

        try {
            switch (command) {
                case "init":
                    init();
                    break;
            }
        }  catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
