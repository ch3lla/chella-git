package com.explore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Chella {
    private static final String REPO_DIR = "src/main/java/com/explore/.chella/";
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

    /**
     * hashes content of current commit of type Commit Data by obtaining the string format first
     * then converting it into bytes using the UTF_8 format and hashing it using SHA-1
     * @param content typeof CommitData
     * @return hashed content
     */
    private static String hashContent(CommitData content){
        try {
            String commitDataString = content.toString();

            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(commitDataString.getBytes(StandardCharsets.UTF_8));
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

    /**
     * This method is responsible for adding files to staging area, it first reads the
     * content of the file and returns a hash of the content, the first two letters of
     * the hash is used to create a sub folder, and the remaining 38 letters are used
     * to name the file in the objects folder
     *
     * @param fileName name of file to be added
     */
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

    private static String getCurrentHead() {
        try {
            return Files.readString(Path.of(REPO_DIR + "HEAD"));
//            Path headPath = Path.of(REPO_DIR, "HEAD");
//            System.out.println("Trying to read: " + headPath.toAbsolutePath());
//
//            if (!Files.exists(headPath)) {
//                System.out.println("❌ HEAD file does not exist at: " + headPath.toAbsolutePath());
//                return null;
//            }
//            if (!Files.isRegularFile(headPath)) {
//                System.out.println("❌ HEAD is not a regular file.");
//                return null;
//            }
//            if (!Files.isReadable(headPath)) {
//                System.out.println("❌ HEAD file is not readable.");
//                return null;
//            }
//
//            return Files.readString(headPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Error reading HEAD file.");
            return null;
        }
    }

    private static void commit(String commitMessage) {
        try {
            Path path = Paths.get(REPO_DIR + "index");
            byte[] index = Files.readAllBytes(path); // stores all the content from the "staging area"
            String parentCommit = getCurrentHead(); // retrieves the current head

            CommitData commitData = new CommitData(new Date().toString(), commitMessage, index, parentCommit); // stores commit data that will be later hashed

            String commitHash = hashContent(commitData);

            if (commitHash != null) {
                Path commitPath = Paths.get(REPO_DIR + OBJECTS_DIR + commitHash);
                Files.write(commitPath, commitData.toJson().getBytes()); // writes commit data to objects database
                Files.writeString(Paths.get(REPO_DIR + "HEAD"), commitHash);
                Files.writeString(path, "", StandardOpenOption.TRUNCATE_EXISTING); // clears the staging area

                System.out.println("Commit successfully created: " + commitHash);
            } else {
                System.out.println("Unable to process commit (No such algorithm)");
            }
        } catch (IOException e) {
            System.out.println("Error serializing commit data");
        }
    }

    private static void log(){
        try {
            String currentCommitHash = getCurrentHead();
            while (currentCommitHash != null && !currentCommitHash.isEmpty()) {
                String commitJson  = Files.readString(Path.of(REPO_DIR + OBJECTS_DIR + currentCommitHash));
                CommitData commitData = CommitData.fromJson(commitJson);
                System.out.println("commit " + currentCommitHash
                        + "\nDate: " + commitData.timeStamp
                        + "\n\n" + commitData.message + "\n\n");

                currentCommitHash = (commitData.parent == null || commitData.parent.isEmpty()) ? null : commitData.parent;
            }
            
        } catch (IOException e) {
            // throw new RuntimeException(e);
            System.out.println("Error reading commit data: " + e.getMessage());
        }

    }

    public static void main(String[] args) throws IOException {
        log();
        System.out.println(Arrays.toString(args));
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
                case "commit":
                    if (args.length < 2) {
                        System.out.println("Usage: java Chella commit <commit message>");
                        return;
                    }
                    commit(args[1]);
                    break;
                case "log":
                    if (args.length < 1) {
                        System.out.println("Usage: java Chella log");
                        return;
                    }
                    log();
                    break;
            }
        }  catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}


// todo: move to its own class
class CommitData {
    @JsonProperty("timeStamp")
    String timeStamp;
    @JsonProperty("message")
    String message;
    @JsonProperty("files")
    byte[] files;
    @JsonProperty("parent")
    String parent;

    CommitData(){}

    CommitData(String timeStamp, String message, byte[] files, String parent) {
        this.timeStamp = timeStamp;
        this.message = message;
        this.files = files;
        this.parent = parent;
    }

    public String getTimeStamp() {
        return timeStamp;
    }
    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getParent() {
        return parent;
    }
    public void setParent(String parent) {
        this.parent = parent;
    }

    public byte[] getFiles() {
        return files;
    }

    public void setFiles(byte[] files) {
        this.files = files;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString(){
        return "{" +
                "timeStamp: " + timeStamp + "\n" +
                "message: " + message + "\n" +
                "files: " + Arrays.toString(files) + "\n" +
                "parent: " + parent + "\n"
                +"}";
    }

    public String toJson() throws IOException {
        ObjectMapper om = new ObjectMapper();
        return om.writeValueAsString(this);
    }

    public static CommitData fromJson(String json) throws IOException {
        ObjectMapper om = new ObjectMapper();
        return om.readValue(json, CommitData.class);
    }
}