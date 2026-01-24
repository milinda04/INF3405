package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Client {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // adresse IP
        System.out.print("Entrez l'adresse IP du serveur: ");
        String ip = sc.nextLine().trim();
        
        if (!isValidIP(ip)) {
            System.out.println("Erreur: Adresse IP invalide. Format attendu: xxx.xxx.xxx.xxx");
            sc.close();
            return;
        }

        // port
        System.out.print("Entrez le port du serveur (5000-5050): ");
        String portStr = sc.nextLine().trim();
        int port;
        
        try {
            port = Integer.parseInt(portStr);
            if (port < 5000 || port > 5050) {
                System.out.println("Erreur: Le port doit être entre 5000 et 5050.");
                sc.close();
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Erreur: Le port doit être un nombre entier.");
            sc.close();
            return;
        }

        // identifiants
        System.out.print("Entrez votre nom d'utilisateur: ");
        String user = sc.nextLine().trim();
        System.out.print("Entrez votre mot de passe: ");
        String pass = sc.nextLine().trim();

        // Connexion au serveur
        try (Socket s = new Socket(ip, port);
             DataOutputStream out = new DataOutputStream(s.getOutputStream());
             DataInputStream in = new DataInputStream(s.getInputStream())) {

            // Envoi identifiants
            out.writeUTF(user);
            out.writeUTF(pass);

            // Réception de la réponse d'authentification
            String authResponse = in.readUTF();
            if (authResponse.equals("AUTH_FAILED")) {
                System.out.println("Erreur dans la saisie du mot de passe");
                sc.close();
                return;
            } else if (authResponse.equals("AUTH_SUCCESS")) {
                System.out.println("Connexion réussie!");
            } else if (authResponse.equals("ACCOUNT_CREATED")) {
                System.out.println("Nouveau compte créé avec succès!");
            }

            // image à traiter
            System.out.print("Entrez le nom de l'image à traiter: ");
            String inputImageName = sc.nextLine().trim();
            
            // existence du fichier
            File inputFile = new File(inputImageName);
            if (!inputFile.exists()) {
                System.out.println("Erreur: Le fichier " + inputImageName + " n'existe pas.");
                sc.close();
                return;
            }

            // image de sortie
            System.out.print("Entrez le nom pour l'image traitée: ");
            String outputImageName = sc.nextLine().trim();

            // Lecture et envoi de l'image
            byte[] imageBytes = Files.readAllBytes(Paths.get(inputImageName));
            out.writeUTF(inputImageName);
            out.writeInt(imageBytes.length);
            out.write(imageBytes);
            out.flush();
            
            System.out.println("Image envoyée au serveur pour traitement...");

            // Réception de l'image traitée
            int responseLength = in.readInt();
            byte[] processedImage = new byte[responseLength];
            in.readFully(processedImage);
            
            // Sauvegarde de l'image traitée
            Files.write(Paths.get(outputImageName), processedImage);
            
            System.out.println("Image traitée reçue du serveur.");
            System.out.println("Image sauvegardée dans: " + Paths.get(outputImageName).toAbsolutePath());

            sc.close();

        } catch (UnknownHostException e) {
            System.out.println("Erreur: Impossible de résoudre l'adresse IP du serveur.");
        } catch (ConnectException e) {
            System.out.println("Erreur: Impossible de se connecter au serveur. Vérifiez que le serveur est démarré.");
        } catch (IOException e) {
            System.out.println("Erreur de communication: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erreur inattendue: " + e.getMessage());
        }
    }


    private static boolean isValidIP(String ip) {
        // Vérification du format général
        if (!ip.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
            return false;
        }
        
        // Vérifier que chaque octet est entre 0 et 255
        String[] parts = ip.split("\\.");
        for (String part : parts) {
            try {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return true;
    }
}