package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Server {
    private static ServerSocket listener;
    private static final String USER_DB_FILE = "users.txt";
    private static final Map<String, String> userDatabase = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            loadUserDatabase();
            String ip = saisirAdresseIP(scanner);
            int port = saisirPort(scanner);
            
            listener = new ServerSocket();
            listener.setReuseAddress(true);
            InetAddress serverIP = InetAddress.getByName(ip);
            listener.bind(new InetSocketAddress(serverIP, port));

            System.out.format("Le serveur est lancé sur %s:%d%n", ip, port);
            System.out.println("En attente de connexions clients...");

            // Boucle principale : chaque connexion entrante spawn un nouveau thread
            while (true) {
                Socket clientSocket = listener.accept();
                new ClientHandler(clientSocket, userDatabase, USER_DB_FILE).start();
            }

        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du serveur: " + e.getMessage());
        } finally {
            // Nettoyage des ressources
            fermerServeur();
            scanner.close();
        }
    }

    private static String saisirAdresseIP(Scanner scanner) {
        while (true) {
            System.out.print("Entrez l'adresse IP du serveur: ");
            String ip = scanner.nextLine().trim();
            if (isValidIP(ip)) {
                return ip;
            }
            System.out.println("Erreur: Adresse IP invalide. "
                    + "Format attendu: xxx.xxx.xxx.xxx (chaque octet entre 0 et 255).");
        }
    }



    private static int saisirPort(Scanner scanner) {
        while (true) {
            System.out.print("Entrez le port d'écoute (5000-5050): ");
            String portStr = scanner.nextLine().trim();
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



    private static void fermerServeur() {
        try {
            if (listener != null && !listener.isClosed()) {
                listener.close();
                System.out.println("Serveur fermé.");
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture du serveur: " + e.getMessage());
        }
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


    private static synchronized void loadUserDatabase() {
        File dbFile = new File(USER_DB_FILE);

        if (!dbFile.exists()) {
            System.out.println("Aucune base de données existante. "
                    + "Un nouveau fichier sera créé lors de la première inscription.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // Ignorer les lignes vides
                }
                String[] parts = line.split(",", 2);
                if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                    userDatabase.put(parts[0], parts[1]);
                    count++;
                } else {
                    System.err.println("Avertissement: Ligne mal formée ignorée dans "
                            + USER_DB_FILE + " => '" + line + "'");
                }
            }
            System.out.println(count + " utilisateur(s) chargé(s) depuis la base de données.");
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la base de données: " + e.getMessage());
        }
    }
}