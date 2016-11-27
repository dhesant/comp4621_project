// FTP Server
// COMP4621
// Dhesant Nakka
// 20146587
// 29 Nov 2016

package ftpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ftpServer {
	// Define FTP variables
	private int ctrlPort = 2121;
	private int dataPortOffset = 5000;

	private boolean exitFlag = false;

	// Create FTP socket
	private ServerSocket ctrlSocket;

	// Start ftpServer in main
	public static void main(String[] args) {
		new ftpServer();
	}

	// ftpServer handler
	public ftpServer() {
		// Attempt to open FTP control port
		try {
			ctrlSocket = new ServerSocket(ctrlPort);
		}
		catch (IOException e) {
			System.out.println("Could not open server on port " + ctrlPort + ".");
			System.exit(1);
		}

		System.out.println("FTP Server started on port " + ctrlPort + ".");


		// Handle incomming connections
		int threadCount = 0;

		while (!exitFlag) {
			try {
				Socket client = ctrlSocket.accept();

				int dataPort = dataPortOffset + threadCount;

				// Create worker thread
				ftpWorker w = new ftpWorker(client, dataPort);

				threadCount++;

				// Start worker thread
				w.start();
			}
			catch (IOException e) {
				e.printStackTrace();
				System.out.println("Error occured on connection attempt");
			}
		}

		// Attempt to close FTP server
		try {
			ctrlSocket.close();
			System.out.println("FTP Server stopped");
		}
		catch (IOException e) {
			System.out.println("Error stopping FTP Server");
			System.exit(1);
		}
	}
}
