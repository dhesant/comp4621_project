// FTP Server
// COMP4621
// Dhesant Nakka
// 20146587
// 29 Nov 2016

package ftpServer;

//import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ftpWorker extends Thread {
	// Enable debugging mode
	private boolean enableDebug = true;

	// Helper enums
	private enum userStat {
		ANONYMOUS, AUTHENTICATING, AUTHENTICATED
	}

	// Path variables
	private String jailedDir = "~/ftp_share";
	private String currentDir = "/";
	private String fileSeparator = "/";			
	
	// control socket variables
	private Socket ctrlSocket;
	private PrintWriter ctrlOut;
	private BufferedReader ctrlIn;

	// data socket variables
	private ServerSocket dataSocket;
	private Socket dataConnection;
	private PrintWriter dataOut;
	private int dataPort;

	// user variables    
	private userStat currentUserStat = userStat.ANONYMOUS;
	private String username = "comp4621";
	private String password = "comp4621";

	private boolean exitFlag = false;


	public ftpWorker(Socket client, int dataPort) {
		super();
		this.ctrlSocket = client;
		this.dataPort = dataPort;
	}

	public void run() {
		try {
			// Get control input
			ctrlIn = new BufferedReader(new InputStreamReader(ctrlSocket.getInputStream()));

			// Get control output
			ctrlOut = new PrintWriter(ctrlSocket.getOutputStream(), true);

			sendCtrlMsg("220 Welcome to djnakka's COMP4621 Java FTP Server");

			// Run commands until exitFlag is tripped
			while (!exitFlag) {
				runCmd(ctrlIn.readLine());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// Close sockets before exiting
		finally {
			try {
				ctrlIn.close();
				ctrlOut.close();
				ctrlSocket.close();
				sendDebugMsg("Closed control sockets and exited");
			}
			catch (IOException e) {
				e.printStackTrace();
				sendDebugMsg("Could not close control sockets");
			}
		}
	}


	private boolean runCmd(String str) throws IOException {
		int index = str.indexOf(' ');
		String cmd;
		String args;

		if (index == -1) {
			cmd = str.toUpperCase();
			args = null;
		}
		else {
			cmd = str.substring(0, index).toUpperCase();
			args = str.substring(index+1, str.length());
		}
		
		sendDebugMsg("Command: " + cmd + " Args: " + args);

		switch (cmd) {
		case "ABOR":
			aborHandler(args);
			break;

		case "CWD":
			cwdHandler(args);
			break;

		case "DELE":
			deleHandler(args);
			break;

		case "LIST":
			listHandler(args);
			break;

		case "MDTM":
			mdtmHandler(args);
			break;

		case "MKD":
			mkdHandler(args);
			break;

		case "NLST":
			nlstHandler(args);
			break;

		case "PASS":
			passHandler(args);
			break;

		case "PASV":
			pasvHandler(args);
			break;

		case "PORT":
			portHandler(args);
			break;

		case "PWD":
			pwdHandler(args);
			break;

		case "QUIT":
			quitHandler(args);
			break;

		case "RETR":
			retrHandler(args);
			break;

		case "RMD":
			rmdHandler(args);
			break;

		case "RNFR":
			rnfrHandler(args);
			break;

		case "RNTO":
			rntoHandler(args);
			break;

		case "SITE":
			siteHandler(args);
			break;

		case "SIZE":
			sizeHandler(args);
			break;

		case "STOR":
			storHandler(args);
			break;

		case "TYPE":
			typeHandler(args);
			break;

		case "USER":
			userHandler(args);
			break;

		default:
			sendCtrlMsg("501 Unknown command");
			break;
		}

		return true;
	}

	private void sendCtrlMsg(String msg) {
		ctrlOut.println(msg);
	}

	private void sendDataMsg(String msg) {
		if (dataConnection == null || dataConnection.isClosed()) {
			sendCtrlMsg("425 No data connetcion was establised");
			sendDebugMsg("Failed to send data message, no data connetcion was establised");
		}
		else {
			dataOut.println(msg);
		}
	}

	private void sendDebugMsg(String msg) {
		if (enableDebug) {
			System.out.println("ftpWorker " + this.getId() + ": " + msg);
		}
	}

	private void openPassiveDataConnection(int port) {
		try {
			dataSocket = new ServerSocket(port);
			dataConnection = dataSocket.accept();
			dataOut = new PrintWriter(dataConnection.getOutputStream(), true);

			sendDebugMsg("Opened Passive Data Connection");
		}
		catch (IOException e) {
			e.printStackTrace();
			sendDebugMsg("Could not open passive data connection");
		}
	}

	private void openActiveDataConncetion(String ipAddr, int port) {
		try {
			dataConnection = new Socket(ipAddr, port);
			dataOut = new PrintWriter(dataConnection.getOutputStream(), true);

			sendDebugMsg("Opened Active Data Connection");
		}
		catch (IOException e) {
			e.printStackTrace();
			sendDebugMsg("Could not open passive data connection");
		}
	}

	private void closeDataConnection() {
		try {
			dataOut.close();
			dataConnection.close();
			if (dataSocket != null) {
				dataSocket.close();
			}

			sendDebugMsg("Data connection was closed");
		}
		catch (IOException e) {
			e.printStackTrace();
			sendDebugMsg("Could not close data connection");
		}
	}

	private void aborHandler(String str) {

	}

	private void cwdHandler(String str) {

	}

	private void deleHandler(String str) {

	}

	private void listHandler(String str) {
		nlstHandler(str);
	}

	private void mdtmHandler(String str) {

	}

	private void mkdHandler(String str) {

	}

	private void nlstHandler(String str) {

	}

	private void passHandler(String str) {
		if (currentUserStat == userStat.AUTHENTICATING && str.equals(password)) {
			currentUserStat = userStat.AUTHENTICATED;
			sendCtrlMsg("230 Welcome \"" + username + "\" to the COMP4621 Java FTP Server");
		}
		else if (currentUserStat == userStat.AUTHENTICATED) {
			sendCtrlMsg("202 User already logged in");
		}
		else {
			sendCtrlMsg("530 Not logged in");
		}
	}

	private void pasvHandler(String str) {
		String ipAddr = "127.0.0.1";
		String[] ipAddrSplit = ipAddr.split("\\.");

		int p1 = dataPort / 256;
		int p2 = dataPort % 256;

		sendCtrlMsg("227 Entering Passive Mode (" + ipAddrSplit[0] +"," + ipAddrSplit[1] + "," + ipAddrSplit[2] + "," + ipAddrSplit[3] + "," + p1 + "," + p2 +")");

		openPassiveDataConnection(dataPort);
	}


	private void portHandler(String str) {

	}

	private void pwdHandler(String str) {
        sendCtrlMsg("257 \"" + currentDir + "\"");
	}

	private void quitHandler(String str) {
		sendCtrlMsg("221 Closing connection");
		exitFlag = true;
	}

	private void retrHandler(String str) {

	}

	private void rmdHandler(String str) {

	}

	private void rnfrHandler(String str) {

	}

	private void rntoHandler(String str) {

	}

	private void siteHandler(String str) {

	}

	private void sizeHandler(String str) {

	}

	private void storHandler(String str) {

	}

	private void typeHandler(String str) {

	}

	private void userHandler(String str) {
		if(str.toLowerCase().equals(username)) {
			sendCtrlMsg("331 User name okay, need password");
			currentUserStat = userStat.AUTHENTICATING;
		}
		else if (currentUserStat == userStat.AUTHENTICATED) {
			sendCtrlMsg("530 User already logged in");
		}
		else {
			sendCtrlMsg("530 Not logged in");
		}
	}
}
