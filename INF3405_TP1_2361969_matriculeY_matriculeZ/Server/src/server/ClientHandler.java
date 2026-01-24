package server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;


public class ClientHandler extends Thread {
    private Socket socket;
    private Map<String, String> userDatabase;
    private String dbPath;


    public ClientHandler(Socket socket, Map<String, String> userDatabase, String dbPath) {
        this.socket = socket;
        this.userDatabase = userDatabase;
        this.dbPath = dbPath;
    }

    public void run() {
        String clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        System.out.println("Nouvelle connexion établie avec: " + clientInfo);
        
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            // identifiants
            String username = in.readUTF();
            String password = in.readUTF();

            // Authentification
            String authResult = authenticate(username, password);
            out.writeUTF(authResult);
            out.flush();

            if (authResult.equals("AUTH_FAILED")) {
                System.out.println("Échec d'authentification pour: " + username + " depuis " + clientInfo);
                return;
            }
            
            System.out.println("Utilisateur authentifié: " + username + " depuis " + clientInfo);

            //  nom de l'image
            String imageName = in.readUTF();
            
            //  données de l'image
            int imageLength = in.readInt();
            byte[] imageData = new byte[imageLength];
            in.readFully(imageData);

            // Affichage des informations de traitement
            String timestamp = new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss").format(new Date());
            System.out.format("[%s - %s:%d - %s] : Image %s reçue pour traitement.%n",
                              username, 
                              socket.getInetAddress().getHostAddress(), 
                              socket.getPort(), 
                              timestamp, 
                              imageName);

            // Traitement de l'image avec le filtre de Sobel
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
            
            if (originalImage == null) {
                System.err.println("Erreur: Impossible de lire l'image reçue.");
                return;
            }
            
            BufferedImage processedImage = Sobel.process(originalImage);

            // Conversion de l'image traitée en bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(processedImage, "jpg", baos);
            byte[] processedImageData = baos.toByteArray();

            // Envoi de l'image traitée au client
            out.writeInt(processedImageData.length);
            out.write(processedImageData);
            out.flush();

            System.out.format("[%s - %s:%d - %s] : Image %s traitée et envoyée.%n",
                              username, 
                              socket.getInetAddress().getHostAddress(), 
                              socket.getPort(), 
                              new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss").format(new Date()), 
                              imageName);

        } catch (EOFException e) {
            System.err.println("Connexion interrompue avec " + clientInfo);
        } catch (IOException e) {
            System.err.println("Erreur de communication avec " + clientInfo + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement pour " + clientInfo + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                System.out.println("Connexion fermée avec: " + clientInfo);
            } catch (IOException e) {
                System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
            }
        }
    }


    private synchronized String authenticate(String username, String password) {
        if (userDatabase.containsKey(username)) {
            // Utilisateur existant - vérification du mot de passe
            if (userDatabase.get(username).equals(password)) {
                return "AUTH_SUCCESS";
            } else {
                return "AUTH_FAILED";
            }
        } else {
            // Nouvel utilisateur - création du compte
            userDatabase.put(username, password);
            saveUserToDatabase(username, password);
            System.out.println("Nouveau compte créé pour: " + username);
            return "ACCOUNT_CREATED";
        }
    }


    private void saveUserToDatabase(String username, String password) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(dbPath, true))) {
            writer.println(username + "," + password);
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde dans la base de données: " + e.getMessage());
        }
    }
}