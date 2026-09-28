# Distributed Image Processing System (Google Cloud)

Cloud Computing project based on a distributed system in **Google Cloud Platform (GCP)**. 

The system receives images submitted by a client, detects their features using **Cloud Vision**, translates the results with **Cloud Translate**, and asynchronously stores the processed data in **Firestore**. All communication and orchestration are handled via **gRPC**, **Cloud Storage**, and **Pub/Sub**.

---

## Architecture

The system is divided into the following components:
1. **Local Client (TUI):** Interactive text interface where users submit images for processing.
2. **Lookup Service (Cloud Run Function):** Serverless function that dynamically provides the available IPs of the gRPC servers.
3. **gRPC Server (VM - Compute Engine):** Receives requests, saves the submitted images to **Cloud Storage**, and publishes an event notification to **Pub/Sub**.
4. **Worker (VM - Compute Engine):** Consumes events from the Pub/Sub queue, invokes the Vision and Translate APIs to process the image, and stores the final result in the **Firestore** database.

---

## Main Technologies
- **Language:** Java
- **RPC & Messaging:** gRPC, Google Cloud Pub/Sub
- **Storage:** Google Cloud Storage, Firestore (NoSQL)
- **AI / Machine Learning:** Cloud Vision API, Cloud Translate API

---

## How to Setup and Run

### 1. Prerequisites
- **Java 17+** (or higher) and **Maven** installed on your system.
- GCP credentials file (Service Account Key in `.json`) downloaded to your machine.
- Set the credentials environment variable before running the application:
  ```bash
  # Windows
  set GOOGLE_APPLICATION_CREDENTIALS=path\to\your\key.json
  
  # Linux/Mac
  export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/key.json"
  ```
  > **Security Note:** Never commit your credentials file to public repositories.

### 2. Variables Configuration
Validate and update your GCP project IDs, topic names, databases, and Cloud Function URLs in the central `Config.java` file (located inside the `contract` module folder).

### 3. Build
The contract module (`contract`) shares important definitions (like `.proto` files) and must be installed first.

At the root of the project, run:
```bash
# Enter and build the contract
cd contract
mvn clean install

# Return to root and build the rest of the project
cd ..
mvn clean package
```

### 4. Execution
The `.jar` files with their respective dependencies will be generated in the `target` folders of each module (`server`, `client`, `labelsApp`).

To start the system components, run the respective generated JAR files. For example:
```bash
java -jar server/target/ServerFileName.jar
java -jar client/target/ClientFileName.jar
java -jar labelsApp/target/AppFileName.jar
```