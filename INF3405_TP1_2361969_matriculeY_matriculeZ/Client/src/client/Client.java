package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Paths;


public class Client {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try {
            String ip = saisirAdresseIP(sc);
            int port = saisirPort(sc);
            System.out.print("Entrez votre nom d'utilisateur: ");
            String user = sc.nextLine().trim();
            System.out.print("Entrez votre mot de passe: ");
            String pass = sc.nextLine().trim();

         try (Socket socket = new Socket(ip, port);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in  = new DataInputStream(socket.getInputStream())) {

                // Authentification
                if (!authentifier(out, in, user, pass)) {
                    return; 
                }


                File inputFile = saisirFichierImage(sc);
                String inputImageName = inputFile.getName();


                System.out.print("Entrez le nom pour l'image traitée: ");
                String outputImageName = sc.nextLine().trim();

                //image => serveur 
                envoyerImage(out, inputImageName, inputFile);
                System.out.println("Image envoyée au serveur pour traitement...");

                // --- Réception de l'image traitée ---
                receiverImage(in, outputImageName);

            } catch (UnknownHostException e) {
                System.out.println("Erreur: Impossible de résoudre l'adresse IP du serveur.");
            } catch (ConnectException e) {
                System.out.println("Erreur: Impossible de se connecter au serveur. "
                        + "Vérifiez que le serveur est démarré.");
            } catch (IOException e) {
                System.out.println("Erreur de communication: " + e.getMessage());
            }

        } finally {
            sc.close();
        }
    }


    private static String saisirAdresseIP(Scanner sc) {
        while (true) {
            System.out.print("Entrez l'adresse IP du serveur: ");
            String ip = sc.nextLine().trim();
            if (isValidIP(ip)) {
                return ip;
            }
            System.out.println("Erreur: Adresse IP invalide. "
                    + "Format attendu: xxx.xxx.xxx.xxx (chaque octet entre 0 et 255).");
        }
    }


    private static int saisirPort(Scanner sc) {
        while (true) {
            System.out.print("Entrez le port du serveur (5000-5050): ");
            String portStr = sc.nextLine().trim();
            try {
                int port = Integer.parseInt(portStr);
                if (port >= 5000 && port <= 5050) {
                    return port;
                }
                System.out.println("Erreur: Le port doit être entre 5000 et 5050.");
            } catch (NumberFormatException e) {
                System.out.println("Erreur: Le port doit être un nombre entier.");
            }
        }
    }


    private static File saisirFichierImage(Scanner sc) {
        while (true) {
            System.out.print("Entrez le nom de l'image à traiter: ");
            String imageName = sc.nextLine().trim();
            File file = new File(imageName);
            if (file.exists() && file.isFile()) {
                return file;
            }
            System.out.println("Erreur: Le fichier '" + imageName
                    + "' n'existe pas ou n'est pas un fichier valide. Réessayez.");
        }
    }


    private static boolean authentifier(DataOutputStream out, DataInputStream in,
                                        String user, String pass) throws IOException {
        out.writeUTF(user);
        out.writeUTF(pass);
        out.flush();

        String authResponse = in.readUTF();

        if ("AUTH_FAILED".equals(authResponse)) {
            System.out.println("Erreur dans la saisie du mot de passe");
            return false;
        } else if ("AUTH_SUCCESS".equals(authResponse)) {
            System.out.println("Connexion réussie!");
        } else if ("ACCOUNT_CREATED".equals(authResponse)) {
            System.out.println("Nouveau compte créé avec succès!");
        }
        return true;
    }


    private static void envoyerImage(DataOutputStream out, String inputImageName,
                                     File inputFile) throws IOException {
        byte[] imageBytes = Files.readAllBytes(inputFile.toPath());
        out.writeUTF(inputImageName);
        out.writeInt(imageBytes.length);
        out.write(imageBytes);
        out.flush();
    }


    private static void receiverImage(DataInputStream in, String outputImageName) throws IOException {
        int responseLength = in.readInt();
        byte[] processedImage = new byte[responseLength];
        in.readFully(processedImage);

        Files.write(Paths.get(outputImageName), processedImage);

        System.out.println("Image traitée reçue du serveur.");
        System.out.println("Image sauvegardée dans: " + Paths.get(outputImageName).toAbsolutePath());
    }

    
    private static boolean isValidIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // Format général : exactement 4 groupes de 1 à 3 chiffres séparés par des points
        if (!ip.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
            return false;
        }

        // Vérifier que chaque octet est entre 0 et 255
        String[] parts = ip.split("\\.");
        for (String part : parts) {
            int value = Integer.parseInt(part); 
            if (value < 0 || value > 255) {
                return false;
            }
        }

        return true;
    }
}