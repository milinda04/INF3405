package server;

import java.io.*;
import java.net.*;
import java.util.*;


public class Server {
    private static ServerSocket listener;
    private static final String USER_DB_FILE = "users.txt";
    private static Map<String, String> userDatabase = new HashMap<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Charger la base de données des users
        loadUserDatabase();

        //Adresse IP
        System.out.print("Entrez l'adresse IP du serveur: ");
        String ip = scanner.nextLine().trim();
        
        if (!isValidIP(ip)) {
            System.out.println("Erreur: Adresse IP invalide. Format attendu: xxx.xxx.xxx.xxx");
            scanner.close();
            return;
        }

        // Port
        System.out.print("Entrez le port d'écoute (5000-5050): ");
        String portStr = scanner.nextLine().trim();
        int port;
        
        try {
            port = Integer.parseInt(portStr);
            if (port < 5000 || port > 5050) {
                System.out.println("Erreur: Le port doit être entre 5000 et 5050.");
                scanner.close();
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Erreur: Le port doit être un nombre entier.");
            scanner.close();
            return;
        }

        // Démarrer serveur
        try {
            listener = new ServerSocket();
            listener.setReuseAddress(true);
            InetAddress serverIP = InetAddress.getByName(ip);
            listener.bind(new InetSocketAddress(serverIP, port));
            
            System.out.format("Le serveur est lancé sur %s:%d%n", ip, port);
            System.out.println("En attente de connexions clients...");

            while (true) {
                Socket clientSocket = listener.accept();
                new ClientHandler(clientSocket, userDatabase, USER_DB_FILE).start();
            }
            
        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du serveur: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (listener != null && !listener.isClosed()) {
                    listener.close();
                }
            } catch (IOException e) {
                System.err.println("Erreur lors de la fermeture du serveur: " + e.getMessage());
            }
            scanner.close();
        }
    }


    private static boolean isValidIP(String ip) {
        // Vérification du format général
        if (!ip.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
            return false;
        }
        
        // Vérifier que chaque octet est entre 0 et 255
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
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


    private static synchronized void loadUserDatabase() {
        File dbFile = new File(USER_DB_FILE);
        
        if (!dbFile.exists()) {
            System.out.println("Aucune base de données existante. Un nouveau fichier sera créé.");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    userDatabase.put(parts[0], parts[1]);
                    count++;
                }
            }
            System.out.println(count + " utilisateur(s) chargé(s) depuis la base de données.");
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la base de données: " + e.getMessage());
        }
    }
}