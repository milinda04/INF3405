package server;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;


public class ClientHandler extends Thread {

    private final Socket socket;
    private final Map<String, String> userDatabase;
    private final String dbPath;
    
    public ClientHandler(Socket socket, Map<String, String> userDatabase, String dbPath) {
        this.socket = socket;
        this.userDatabase = userDatabase;
        this.dbPath = dbPath;
    }


    @Override
    public void run() {
        String clientIP   = socket.getInetAddress().getHostAddress();
        int    clientPort = socket.getPort();
        String clientInfo = clientIP + ":" + clientPort;

        System.out.println("Nouvelle connexion établie avec: " + clientInfo);

        try (DataInputStream  in  = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

        	String username = in.readUTF();
            String password = in.readUTF();

            String authResult = authenticate(username, password);
            out.writeUTF(authResult);
            out.flush();

            if ("AUTH_FAILED".equals(authResult)) {
                System.out.println("Échec d'authentification pour: " + username
                        + " depuis " + clientInfo);
                return;
            }

            System.out.println("Utilisateur authentifié: " + username + " depuis " + clientInfo);
            String imageName  = in.readUTF();
            int    imageLength = in.readInt();
            byte[] imageData  = new byte[imageLength];
            in.readFully(imageData);

            logTraitement(username, clientIP, clientPort, imageName, "reçue pour traitement");

            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (originalImage == null) {
                System.err.println("Erreur: Impossible de décoder l'image reçue de " + clientInfo);
                return;
            }

            BufferedImage processedImage = Sobel.process(originalImage);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean written = ImageIO.write(processedImage, "png", baos);
            if (!written) {
                System.err.println("Erreur: Échec de l'encodage PNG pour " + clientInfo);
                return;
            }
            byte[] processedImageData = baos.toByteArray();
            baos.close();

            out.writeInt(processedImageData.length);
            out.write(processedImageData);
            out.flush();

            // Log de fin de traitement
            logTraitement(username, clientIP, clientPort, imageName, "traitée et envoyée");

        } catch (EOFException e) {
            System.err.println("Connexion interrompue prématurément avec " + clientInfo);
        } catch (IOException e) {
            System.err.println("Erreur de communication avec " + clientInfo + ": " + e.getMessage());
        } finally {
            // Fermeture du socket dans tous les cas
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
                System.out.println("Connexion fermée avec: " + clientInfo);
            } catch (IOException e) {
                System.err.println("Erreur lors de la fermeture avec " + clientInfo
                        + ": " + e.getMessage());
            }
        }
    }


    private synchronized String authenticate(String username, String password) {
        String hashedPassword = hashPassword(password);

        if (userDatabase.containsKey(username)) {
            // Utilisateur existant — comparaison du hash
            if (userDatabase.get(username).equals(hashedPassword)) {
                return "AUTH_SUCCESS";
            } else {
                return "AUTH_FAILED";
            }
        } else {
            // Nouvel utilisateur — création automatique
            userDatabase.put(username, hashedPassword);
            saveUserToDatabase(username, hashedPassword);
            System.out.println("Nouveau compte créé pour: " + username);
            return "ACCOUNT_CREATED";
        }
    }

    private synchronized void saveUserToDatabase(String username, String hashedPassword) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(dbPath, true))) {
            writer.println(username + "," + hashedPassword);
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde dans la base de données: "
                    + e.getMessage());
        }
    }


    private static String hashPassword(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Avertissement: SHA-256 non disponible. Stockage en clair.");
            return input;
        }
    }


    private static void logTraitement(String username, String clientIP,
                                      int clientPort, String imageName, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss").format(new Date());
        System.out.format("[%s - %s:%d - %s] : Image %s %s.%n",
                username, clientIP, clientPort, timestamp, imageName, message);
    }
}