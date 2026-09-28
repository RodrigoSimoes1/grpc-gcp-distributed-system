package pt.isel.client.tui;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.isel.client.LabelsGrpcClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static pt.isel.util.Config.HTTP_IP_LOOKUP_CLOUD_FUNCTION;

public class TuiApp {
    private final Scanner scanner = new Scanner(System.in);
    private LabelsGrpcClient grpcClient;
    private ManagedChannel currentChannel;
    private String selectedIp = null;

    private enum Options {
        EXIT,
        SELECT_SERVER,
        CONNECT_LOCALHOST,
        SUBMIT_IMAGE,
        IMAGE_CHARACTERISTICS_BY_ID,
        STORED_FILE_GIVEN_TWO_DATES,
        DOWNLOAD_IMAGE_BY_ID,
        SCALE_GRPC_SERVERS,
        SCALE_WORKER_NODES,
        UNKNOWN
    }

    public void run() {

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    System.out.println("Shutdown initiated, closing...");
                    safeExit();
                })
        );

        Options userInput;

        do {
            userInput = displayMenu();

            try {

                switch (userInput) {

                    case EXIT -> System.out.println("User requested exit, shutting down...");

                    case SELECT_SERVER -> fetchAndSelectIp();

                    case CONNECT_LOCALHOST -> {
                        clearConsole();
                        System.out.println("Connecting to local server...");
                        selectedIp = "localhost";
                        connectToServer(selectedIp);
                        System.out.println("Connected successfully to localhost!");
                        pause();
                    }

                    case SUBMIT_IMAGE -> {
                        if (isServerSelected()) break;

                        clearConsole();
                        System.out.print("Enter the full path to the image: ");
                        String path = scanner.nextLine();

                        long startTime = System.currentTimeMillis();

                        grpcClient.submitImage(path);

                        long elapsedTime = System.currentTimeMillis() - startTime;
                        System.out.println("Total time: " + (elapsedTime / 1000) + " seconds");
                        pause();
                    }

                    case IMAGE_CHARACTERISTICS_BY_ID -> {
                        if (isServerSelected()) break;
                        String id = getId();
                        System.out.printf("Fetching characteristics for ID: %s%n", id);

                        grpcClient.getImageCharacteristicsById(id);

                        pause();
                    }

                    case STORED_FILE_GIVEN_TWO_DATES -> {
                        if (isServerSelected()) break;
                        String startDate = getDate("Enter the start date (dd-MM-yyyy): ");
                        String endDate = getDate("Enter the end date (dd-MM-yyyy): ");
                        String keyWord = getKeyword();

                        grpcClient.getStoredFilesGivenTwoDates(startDate, endDate, keyWord);
                        System.out.printf("Fetching files between %s and %s%n", startDate, endDate);

                        pause();
                    }
                    case DOWNLOAD_IMAGE_BY_ID -> {
                        if (isServerSelected()) break;
                        String id = getId();

                        clearConsole();
                        System.out.print("Enter the destination folder path: ");
                        String destFolder = scanner.nextLine().trim();

                        System.out.println("Downloading...");
                        grpcClient.downloadImage(id, destFolder);

                        pause();
                    }

                    case SCALE_GRPC_SERVERS -> {
                        if (isServerSelected()) break;
                        clearConsole();
                        System.out.print("Number of gRPC servers: ");
                        int count = Integer.parseInt(scanner.nextLine());

                        grpcClient.scaleGrpcServers(count);
                        pause();
                    }

                    case SCALE_WORKER_NODES -> {
                        if (isServerSelected()) break;
                        clearConsole();
                        System.out.print("Number of workers: ");
                        int count = Integer.parseInt(scanner.nextLine());

                        grpcClient.scaleWorkerNodes(count);
                        pause();
                    }

                    case UNKNOWN -> {
                        System.out.println("Invalid option");
                        pause();
                    }
                }

            } catch (Exception e) {

                clearConsole();

                System.out.println("Contact the dev team");
                System.out.println(e.getMessage());

                System.exit(2);
            }

        } while (userInput != Options.EXIT);
    }

    private String getId() {

        String id = "";

        while (id.isBlank()) {
            clearConsole();

            System.out.println("Enter image Request ID:");
            id = scanner.nextLine().trim();

            if (id.isBlank()) {
                System.out.println("Invalid ID. Cannot be empty.");
                pause();
            }
        }

        return id;
    }

    private String getDate(String message) {

        String date;

        do {
            clearConsole();

            System.out.println(message);

            date = scanner.nextLine();

        } while (date.isBlank());

        return date;
    }

    private String getKeyword() {

        String keyWord;

        do {
            clearConsole();

            System.out.println("Enter the keyword: ");

            keyWord = scanner.nextLine();

        } while (keyWord.isBlank());

        return keyWord;
    }

    private Options displayMenu() {

        try {

            clearConsole();
            System.out.println();

            System.out.println("0. Exit");
            System.out.println("1. Fetch and Select Server IP");
            System.out.println("2. Connect to Localhost ");
            System.out.println("3. Submit Image");
            System.out.println("4. Image Characteristics By ID");
            System.out.println("5. Stored Files Between Two Dates");
            System.out.println("6. Download Image By ID");
            System.out.println("7. Scale gRPC Servers ");
            System.out.println("8. Scale Worker Nodes ");

            System.out.println();
            System.out.print("> ");

            int option = Integer.parseInt(scanner.nextLine());

            if (option < 0 || option >= Options.values().length) {
                return Options.UNKNOWN;
            }

            return Options.values()[option];

        } catch (Exception e) {

            return Options.UNKNOWN;
        }
    }

    private void clearConsole() {

        for (int i = 0; i < 25; i++) {
            System.out.println();
        }
    }

    private void pause() {
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private void safeExit() {

        try {
            System.out.println("Cleaning resources before shutdown...");
        } catch (Exception e) {
            System.out.println("Error while shutting down: " + e.getMessage());
        } finally {
            System.out.println("Shutdown hook finished");
        }
    }

    private boolean isServerSelected() {
        if (selectedIp == null || grpcClient == null) {
            clearConsole();
            System.out.println("Warning: The server was not selected!");
            System.out.println("Choose option 1 to selected a server.");
            pause();
            return true;
        }
        return false;
    }

    private void fetchAndSelectIp() {
        clearConsole();
        System.out.println("Running the Cloud Function to fetch IPs...\n");
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HTTP_IP_LOOKUP_CLOUD_FUNCTION))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Error fetching the ips. HTTP Status Code: " + response.statusCode());
                pause();
                return;
            }

            String body = response.body().replace("[", "").replace("]", "").replace("\"", "").trim();
            if (body.isEmpty()) {
                System.out.println("There are no servers available");
                pause();
                return;
            }

            String[] ipArray = body.split(",");
            List<String> availableIps = new ArrayList<>();

            System.out.println("Available Servers:");
            for (int i = 0; i < ipArray.length; i++) {
                String ip = ipArray[i].trim();
                availableIps.add(ip);
                System.out.printf("%d. %s%n", i + 1, ip);
            }

            System.out.print("\nChoose the server to establish connection or press ( 0 to abort): ");
            int choice = Integer.parseInt(scanner.nextLine());

            if (choice > 0 && choice <= availableIps.size()) {
                selectedIp = availableIps.get(choice - 1);
                connectToServer(selectedIp);
                System.out.println("\n Connected successfully to the server with ip: " + selectedIp);
            } else {
                System.out.println("Invalid Selection.");
            }

        } catch (Exception e) {
            System.out.println("Failed to communicate with the Lookup Service: " + e.getMessage());
        }
        pause();
    }

    private void connectToServer(String selectedIp) {
        // If exits an old connection, it is terminated before a new one is being created
        if (currentChannel != null && !currentChannel.isShutdown()) {
            currentChannel.shutdownNow();
        }

        // Creates a new connection for the selectedIp
        currentChannel = ManagedChannelBuilder.forTarget(selectedIp + ":8080")
                .usePlaintext()
                .build();

        grpcClient = new LabelsGrpcClient(currentChannel);
    }
}