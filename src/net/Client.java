package net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;

import gui.ClientGUI;

public class Client implements Runnable {

	Socket socket;
	private String userName = null;
	private ObjectInputStream clientInStream;
	private ObjectOutputStream clientOutStream;
	private Message newMessage;
	private Scanner scan;
	public boolean waitForName = true;
	private int id;
	private boolean hasGui = true;
	ClientGUI gui;

	// if no gui is given, gui = null
	public Client(String ip, int port) {
		this(ip, port, null);
	}

	public Client(String ip, int port, ClientGUI gui) {

		try {
			scan = new Scanner(System.in);
			socket = new Socket(ip, port);
			clientInStream = new ObjectInputStream(socket.getInputStream());
			clientOutStream = new ObjectOutputStream(socket.getOutputStream());
		} catch (Exception e) {
			System.err.println("failed to initialize client");
			e.printStackTrace();
		}

		// listen for a message from the server to get clients ID
		id = getMessage().getReceiver();
	}

	public void run() {
		// short wait period for gui to tell the username, now deprecated
		// try {
		// Thread.sleep(500);
		// } catch (Exception e) {
		//
		// }

		// if gui == null, we have no gui...
		if (gui == null)
			hasGui = false;

		// user name input from user
		// via gui
		if (hasGui) {
			gui.addMessage(new Message(4, "server", "Please enter an username", this.id));
			while (waitForName)
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			serverMessage(userName);
		} else { // or via cli
			System.out.print("Enter username: ");
			while ((userName = scan.nextLine()) == null)
				;
			// userName = scan.nextLine();
			if (userName.equals("")) {
				userName = "anonymous";
			}
			serverMessage(userName);
		}

		// messages for the client
		unicastMessage("Welcome " + userName + "! Your ID is " + this.id, id);
		unicastMessage("To get help, type /help", id);

		// message for everyone else in the chat
		multicastMessage("has come online!");

		// message getter thread
		Thread gM = new Thread(new Runnable() {
			public void run() {
				if (hasGui) {
					// again for gui...
					while (true) {
						gui.addMessage(getMessage());
					}
				} else {
					// ...and cli
					while (true) {
						// not the best solution; if you type and someone
						// sends a message, your last two typed chars will get hidden
						System.out.println("\r\r" + getMessage());
						System.out.print("$ ");
					}
				}
			}
		});
		// and start the message getter thread
		gM.start();

		String input;
		// when in cli mode, read from System.in forever
		if (!hasGui) {
			while (true) {
				input = scan.nextLine();
				process(input);
				System.out.print("$ ");
			}
		}
	}

	// method for processing the user input
	public void process(String input) {
		// only process, if input is not empty ("")
		if (!input.equals("")) {
			if (input.charAt(0) == '/') {
				// if first char is a '/', send message from type command
				commandMessage(input);

			} else if (Pattern.compile("@\\d+ .+").matcher(input).matches()) {
				// check against regex, if user wants to send a PM
				int rec = Integer.parseInt(input.substring(1, input.indexOf(" ")));
				String cont = input.substring(input.indexOf(" ") + 1);
				// message gets split into content and receiver
				unicastMessage(cont, rec);
			} else {
				// in every other case, send message as broadcast
				broadcastMessage(input);
			}
		}
	}

	// message to everyone (also to self)
	private void broadcastMessage(String content) {
		Message msg = new Message(0, this.userName, content, -1, id);
		sendMessage(msg);
	}

	// send a message to a specified user via ID (PM)
	private void unicastMessage(String content, int cID) {
		Message msg = new Message(1, this.userName, content, cID, id);
		sendMessage(msg);
	}

	// false name, send message to everyone except self
	private void multicastMessage(String content) {
		Message msg = new Message(2, this.userName, content, id, id);
		sendMessage(msg);
	}

	// send message just to the server
	private void serverMessage(String content) {
		Message msg = new Message(3, this.userName, content, 0, id);
		sendMessage(msg);
	}

	// special command message
	private void commandMessage(String content) {
		Message msg = new Message(5, this.userName, content, 0, id);
		sendMessage(msg);
	}

	// method to send message as object
	private void sendMessage(Message msg) {
		try {
			clientOutStream.writeObject(msg);
			clientOutStream.flush();
		} catch (IOException e) {
			System.err.println("could not send message");
			e.printStackTrace();
			System.exit(5);
		}
	}

	// method for getting message objects
	private Message getMessage() {
		try {
			newMessage = (Message) clientInStream.readObject();
			return newMessage;
		} catch (Exception e) {
			// if server shuts down or connection gets lost
			System.err.println("could not read message, connection dead");
			// e.printStackTrace();
			System.exit(5);
		}
		return null;
	}

	// set username, called just once, from gui, so far
	public void setUserName(String name) {
		this.userName = name;
	}

	// set gui, called just once, from gui, so far
	public void setGui(ClientGUI gui) {
		this.gui = gui;
	}
}
