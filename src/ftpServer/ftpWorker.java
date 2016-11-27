// FTP Server
// COMP4621
// Dhesant Nakka
// 20146587
// 29 Nov 2016

package ftpServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;


public class ftpWorker extends Thread {
	// Enable debugging mode
	private boolean enableDebug = true;

	// Helper enums
	private enum userStat {
		ANONYMOUS, AUTHENTICATING, AUTHENTICATED
	}

	// Path variables
	private String jailedDir = "/home/dhesant/ftp_share";
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

	// flags and inter-function variables
	private boolean exitFlag = false;
	private boolean binaryFlag = false;
	private File renameFile;

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
		case "PASS":
			passHandler(args);
			break;

		case "PASV":
			pasvHandler(args);
			break;

		case "PORT":
			portHandler(args);
			break;

		case "QUIT":
			quitHandler(args);
			break;

		case "REIN":
			reinHandler(args);
			break;

		case "USER":
			userHandler(args);
			break;
			
		default:
			runCmdAuth(cmd, args);
			break;
		}

		return true;
	}

	private void runCmdAuth(String cmd, String args) {
		if (currentUserStat != userStat.AUTHENTICATED) {
			sendCtrlMsg("530 Not Logged In");
			sendDebugMsg("User not logged in");
		}
		
		else {
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

			case "REIN":
				reinHandler(args);
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
		}
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
	
	private void sendDataMsgRaw(String msg) {
		if (dataConnection == null || dataConnection.isClosed()) {
			sendCtrlMsg("425 No data connetcion was establised");
			sendDebugMsg("Failed to send data message, no data connetcion was establised");
		}
		else {
			dataOut.print(msg);
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

	private void openActiveDataConnection(String ipAddr, int port) {
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
		String targetDir = currentDir;

		if (str.equals("..")) {
			int index = targetDir.lastIndexOf(fileSeparator);
			if (index > 0) {
				targetDir = targetDir.substring(0, index);
			}
			else {
				targetDir = "/";
			}
		}

		else if ((str != null) && (!str.equals("."))) {
			if (currentDir.endsWith("/")) {
				targetDir = targetDir + str;
			}
			else {
				targetDir = targetDir + fileSeparator + str;
			}
		}

		File f = new File(jailedDir + targetDir);

		if (f.exists() && f.isDirectory()) {
			currentDir = targetDir;
			sendCtrlMsg("250 " + currentDir);
		}
		else {
			sendCtrlMsg("550 " + targetDir + ": Directory not found");
		}
	}

	private void deleHandler(String str) {
		if (str == null) {
			sendCtrlMsg("501 No filename given");
		}
		else {
			String filename = currentDir;
			if (currentDir.endsWith("/")) {
				filename = filename + str;
			}
			else {
				filename = filename + fileSeparator + str;
			}

			File f = new File(jailedDir + filename);

			if(f.exists() && f.isFile()) {
				f.delete();
				sendCtrlMsg("250 File removed");
			}

			else {
				sendCtrlMsg("550 File does not exist");
			}
		}
	}

	private void listHandler(String str) {
		nlstHandler(str);
	}

	private void mdtmHandler(String str) {
		if (str == null) {
			sendCtrlMsg("501 No filename given");
		}
		else {
			String filename = currentDir;
			if (currentDir.endsWith("/")) {
				filename = filename + str;
			}
			else {
				filename = filename + fileSeparator + str;
			}

			File f = new File(jailedDir + filename);

			if(f.exists() && f.isFile()) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
				sendCtrlMsg("213 " + sdf.format(f.lastModified()));
			}

			else {
				sendCtrlMsg("550 File does not exist");
			}
		}
	}

	private void mkdHandler(String str) {
		if (str != null && str.matches("^[a-zA-Z0-9]+$")) {
			String targetDir = currentDir;

			if (currentDir.endsWith("/")) {
				targetDir = targetDir + str;
			}
			else {
				targetDir = targetDir + fileSeparator + str;
			}

			File newDir = new File(jailedDir + targetDir);

			if(!newDir.mkdir()) {
				sendCtrlMsg("550 Failed to create directory");
				sendDebugMsg("Failed to create new directory " + jailedDir + targetDir);
			}

			else {
				sendCtrlMsg("250 Directory successfully created");
			}
		}

		else {
			sendCtrlMsg("550 Invalid name");
		}
	}

	private void nlstHandler(String str) {
		if (dataConnection == null || dataConnection.isClosed()) {
			sendCtrlMsg("425 No data connection was established");
		}
		
		else {
			String[] lsDir = nlstHelper(str);

			if (lsDir == null) {
				sendCtrlMsg("550 File does not exist.");
			}
			
			else {
				sendCtrlMsg("125 Opening ASCII mode data connection for file list.");

				for (int i = 0; i < lsDir.length; i++) {
					sendDataMsgRaw(lsDir[i] + "\r\n");
				}

				sendCtrlMsg("226 Transfer complete.");
				closeDataConnection();
			}
		}
	}
	
	private String[] nlstHelper(String str) {
		String targetDir = currentDir;
		
		if (str != null) {
			if (currentDir.endsWith("/")) {
				targetDir = targetDir + str;
			}
			else {
				targetDir = targetDir + fileSeparator + str;
			}
		}


		// Now get a File object, and see if the name we got exists and is a
		// directory.
		File f = new File(jailedDir + targetDir);

		if (f.exists() && f.isDirectory()) {
			return f.list();
		}
		
		else if (f.exists() && f.isFile()) {
			String[] tmp = new String[1];
			tmp[0] = f.getName();
			return tmp;
		}
		
		else {
			return null;
		}
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
        // Extract IP address and port number from arguments
        String[] strSplit = str.split(",");
        String ipAddr = strSplit[0] + "." + strSplit[1] + "." + strSplit[2] + "." + strSplit[3];
    
        int port = Integer.parseInt(strSplit[4])*256 + Integer.parseInt(strSplit[5]);
        
        // Initiate data connection to client
        openActiveDataConnection(ipAddr, port);
        sendCtrlMsg("200 Command OK");
	}

	private void pwdHandler(String str) {
        sendCtrlMsg("257 \"" + currentDir + "\"");
	}

	private void quitHandler(String str) {
		sendCtrlMsg("221 Closing connection");
		exitFlag = true;
	}
	
	private void reinHandler(String str) {
		currentUserStat = userStat.ANONYMOUS;
		sendCtrlMsg("220 OK");
	}

	private void retrHandler(String str) {
		if (str == null) {
			sendCtrlMsg("501 No filename given");
		}
		else {
			String filename = currentDir;
			if (currentDir.endsWith("/")) {
				filename = filename + str;
			}
			else {
				filename = filename + fileSeparator + str;
			}
			
			File f = new File(jailedDir + filename);
			
			if(!f.exists()) {
				sendCtrlMsg("550 File does not exist");
			}
			
			else if (dataConnection == null || dataConnection.isClosed()) {
				sendCtrlMsg("425 No data connection was established");
			}
			
			else {
				// Binary mode
				if (binaryFlag) {
					BufferedInputStream fIn = null;
					BufferedOutputStream fOut = null;
					
					sendCtrlMsg("150 Opening binary mode data connection for requested file " + f.getName());

					try {
						fIn = new BufferedInputStream(new FileInputStream(f));
						fOut = new BufferedOutputStream(dataConnection.getOutputStream());
					}
					catch (Exception e) {
						sendDebugMsg("Cannot open file streams for file " + filename);
					}

					byte[] buffer = new byte[1024];
					int l = 0;

					try {
						while ((l = fIn.read(buffer,0,1024)) != -1) {
							fOut.write(buffer,0,l);
						}
					}
					catch (IOException e) {
						e.printStackTrace();
						sendDebugMsg("Could not read or write to file " + filename);
					}

					//close streams
					try {
						fIn.close();
						fOut.close();
					}
					catch (IOException e) {
						e.printStackTrace();
						sendDebugMsg("Could not close file streams for file " + filename);
					}

					sendCtrlMsg("226 File transfer successful. Closing data connection.");
				}
				
				// ASCII mode
				else {
					sendCtrlMsg("150 Opening ASCII mode data connection for file " + f.getName());
					
					BufferedReader rIn = null;
					PrintWriter rOut = null;
					
					try {
						rIn = new BufferedReader(new FileReader(f));
						rOut = new PrintWriter(dataConnection.getOutputStream(), true);						
					}
					catch (IOException e) {
						sendDebugMsg("Cannot open file streams for file " + filename);
					}
					
					String buffer;
					
					try {
						while ((buffer = rIn.readLine()) != null) {
							rOut.println(buffer);
						}
					}
					catch (IOException e) {
						e.printStackTrace();
						sendDebugMsg("Could not read or write to file " + filename);
					}
					
					try {
						rIn.close();
						rOut.close();
					}
					catch (IOException e) {
						e.printStackTrace();
						sendDebugMsg("Could not close file streams for file " + filename);
					}
                    sendCtrlMsg("226 File transfer successful. Closing data connection.");
				}
				closeDataConnection();
			}
		}
	}

	private void rmdHandler(String str) {
		if (str != null && str.matches("^[a-zA-Z0-9]+$")) {
			String targetDir = currentDir;

			if (currentDir.endsWith("/")) {
				targetDir = targetDir + str;
			}
			else {
				targetDir = targetDir + fileSeparator + str;
			}

			File newDir = new File(jailedDir + targetDir);

			if (newDir.exists() && newDir.isDirectory()) {
				newDir.delete();
				sendCtrlMsg("250 Directory removed");
			}
			else {
				sendCtrlMsg("550 Requested action not taken. Directory unavailable.");
			}

		}

		else {
			sendCtrlMsg("550 Invalid name");
		}
	}

	private void rnfrHandler(String str) {
		if (str == null) {
			sendCtrlMsg("501 No filename given");
		}
		else {
			String filename = currentDir;
			if (currentDir.endsWith("/")) {
				filename = filename + str;
			}
			else {
				filename = filename + fileSeparator + str;
			}

			renameFile = new File(jailedDir + filename);

			if(renameFile.exists() && renameFile.isFile()) {
				sendCtrlMsg("220 OK");
			}

			else {
				renameFile = null;
				sendCtrlMsg("550 File does not exist");
			}
		}
	}

	private void rntoHandler(String str) {
		if (str == null) {
			sendCtrlMsg("501 No filename given");
		}
		
		else if (renameFile == null) {	
			sendCtrlMsg("550 File does not exist");
		}
		
		else {
			String filename = currentDir;
			if (currentDir.endsWith("/")) {
				filename = filename + str;
			}
			else {
				filename = filename + fileSeparator + str;
			}

			File f = new File(jailedDir + filename);

			if(!f.exists()) {
				if(renameFile.renameTo(f)) {
					sendCtrlMsg("213 Renamed " + renameFile.getName() + " to " + f.getName());
				}
				else {
					sendCtrlMsg("550 Cannot rename file");
				}
			}

			else {
				sendCtrlMsg("550 File already exists");
			}
		}
	}

	private void sizeHandler(String str) {
		if (str == null) {
			sendCtrlMsg("501 No filename given");
		}

		else {
			String filename = currentDir;
			if (currentDir.endsWith("/")) {
				filename = filename + str;
			}
			else {
				filename = filename + fileSeparator + str;
			}

			File f = new File(jailedDir + filename);

			if(f.exists() && f.isFile()) {
				long bytes = f.length();
				sendCtrlMsg("213 " + Long.toString(bytes));
			}

			else {
				sendCtrlMsg("550 File does not exist");
			}
		}
	}

	private void storHandler(String str) {
		if (str == null) {
			sendCtrlMsg("501 No filename given");
		}
		else {
			String filename = currentDir;
			if (currentDir.endsWith("/")) {
				filename = filename + str;
			}
			else {
				filename = filename + fileSeparator + str;
			}
			
			File f = new File(jailedDir + filename);
			
			if(f.exists()) {
				sendCtrlMsg("550 File already exists");
			}
			
			else if (dataConnection == null || dataConnection.isClosed()) {
				sendCtrlMsg("425 No data connection was established");
			}
			
			else {
				// Binary mode
				if (binaryFlag) {
					BufferedInputStream fIn = null;
					BufferedOutputStream fOut = null;
					
					sendCtrlMsg("150 Opening binary mode data connection for requested file " + f.getName());

					try {
						fIn = new BufferedInputStream(dataConnection.getInputStream());
						fOut = new BufferedOutputStream(new FileOutputStream(f));
					}
					catch (Exception e) {
						sendDebugMsg("Cannot open file streams for file " + filename);
					}

					byte[] buffer = new byte[1024];
					int l = 0;

					try {
						while ((l = fIn.read(buffer,0,1024)) != -1) {
							fOut.write(buffer,0,l);
						}
					}
					catch (IOException e) {
						e.printStackTrace();
						sendDebugMsg("Could not read or write to file " + filename);
					}

					//close streams
					try {
						fIn.close();
						fOut.close();
					}
					catch (IOException e) {
						e.printStackTrace();
						sendDebugMsg("Could not close file streams for file " + filename);
					}

					sendCtrlMsg("226 File transfer successful. Closing data connection.");
				}
				
				// ASCII mode
				else {
					sendCtrlMsg("150 Opening ASCII mode data connection for file " + f.getName());
					
					BufferedReader rIn = null;
					PrintWriter rOut = null;
					
					try {
						rIn = new BufferedReader(new InputStreamReader(dataConnection.getInputStream()));
						rOut = new PrintWriter(new FileOutputStream(f), true);						
					}
					catch (IOException e) {
						sendDebugMsg("Cannot open file streams for file " + filename);
					}
					
					String buffer;
					
					try {
						while ((buffer = rIn.readLine()) != null) {
							rOut.println(buffer);
						}
					}
					catch (IOException e) {
						e.printStackTrace();
						sendDebugMsg("Could not read or write to file " + filename);
					}
					
					try {
						rIn.close();
						rOut.close();
					}
					catch (IOException e) {
						e.printStackTrace();
						sendDebugMsg("Could not close file streams for file " + filename);
					}
                    sendCtrlMsg("226 File transfer successful. Closing data connection.");
				}
				closeDataConnection();
			}
		}
	}

	private void typeHandler(String str) {
		if(str.toUpperCase().equals("A") || str.toUpperCase().equals("A N")) {
			binaryFlag = false;
			sendCtrlMsg("200 OK");
		}

		else if(str.toUpperCase().equals("I") || str.toUpperCase().equals("L 8")) {
			binaryFlag = true;
			sendCtrlMsg("200 OK");
		}

		else {
			sendCtrlMsg("504 Not OK");;
		}
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
