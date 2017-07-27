package net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * 
 * @author max This server class is a singleton. It runs as one main Thread.
 *         accepting This Thread accepts connections from clients and creates
 *         one separate client Thread for each new connection.
 * 
 *         The client Threads are stored in an ArrayList and dead connections
 *         are marked as null objects in this list.
 * 
 *         Every client thread has an unique id.
 * 
 */

public class Server implements Runnable {

	private ArrayList<Message> messages;
	private ArrayList<ClientThread> clients;
	private ServerSocket serverSocket;
	private static Server instance = null;
	private int id;

	// private constructor
	private Server(int port) {
		messages = new ArrayList<Message>();
		clients = new ArrayList<ClientThread>();
		clients.add(null);
		id = 1;
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("unable to create server socket: ");
			e.printStackTrace();
			System.exit(10);
		}
	}

	// only way to create an instance of this object
	public static Server getInstance(int port) {
		if (instance == null) {
			instance = new Server(port);
		}
		return instance;
	}

	// write given message to clients with different logics
	private synchronized void writeToClients(Message message) {
		ClientThread client;
		switch (message.getType()) {
		case 0: // broadcast
			for (int i = (clients.size() - 1); i > 0; i--) {
				client = clients.get(i);
				if (client == null)
					continue;
				if (!client.sendMessage(message)) {
					remove(i, client);
					System.out.println("removed " + client.userName + " with id " + client.clientId);
				}
			}
			break;
		case 1: // unicast
			if (message.getReceiver() > id) {
				ClientThread clientRetour = clients.get(message.getSender());
				if (clientRetour == null)
					return;
				if (!clientRetour.sendMessage(new Message(1, "server", "Client not available", message.getSender()))) {
					remove(message.getSender(), clientRetour);
				}
				break;
			}
			client = clients.get(message.getReceiver());
			if (client == null) {
				ClientThread clientRetour = clients.get(message.getSender());
				if (clientRetour == null)
					return;
				if (!clientRetour.sendMessage(new Message(1, "server", "Client not available", message.getSender()))) {
					remove(message.getSender(), clientRetour);
				}
				return;
			}
			if (!client.sendMessage(message)) {
				remove(message.getReceiver(), client);
			}
			break;
		case 2: // broadcast except from given id
			for (int i = (clients.size() - 1); i > 0; i--) {
				client = clients.get(i);
				if (client == null || client.clientId == message.getReceiver())
					continue;
				if (!client.sendMessage(message)) {
					remove(i, client);
				}
			}
			break;
		case 3: // msg to server

			break;
		case 4: // msg to specified client
			client = clients.get(message.getReceiver());
			if (client == null) {
				return;
			}
			if (!client.sendMessage(message)) {
				remove(message.getReceiver(), client);
			}
			break;
		default:
			System.out.println("failed to get message type");
			break;
		}
	}

	// replaces client with "null" in the list at index i and informs the other
	// clients
	public void remove(int i, ClientThread client) {
		clients.add(i, null);
		clients.remove(i + 1);
		System.err.println("removed " + client.userName + " with id " + client.clientId);
		writeToClients(new Message(0, client.userName, "has gone offline.", -1, 0));
	}

	// thread for accepting connections and creating ClientThreads
	public void run() {
		System.out.println("Server started successfully!");
		while (true) {
			try {
				Socket tempSocket = serverSocket.accept();
				ClientThread client = new ClientThread(id++, tempSocket);
				clients.add(client);
				client.start();
				System.out.println("added client");
			} catch (IOException e) {
				System.err.println("failed to add client");
				e.printStackTrace();
			}
		}
	}

	/**
	 * each client gets a client thread with an unique id and socket
	 * 
	 * @author max
	 * 
	 */
	class ClientThread extends Thread {

		private int clientId;
		private String userName;
		private ObjectInputStream clientInStream;
		private ObjectOutputStream clientOutStream;
		private Socket socket;
		private boolean running = true;

		public ClientThread(int id, Socket socket) {
			this.clientId = id;
			this.socket = socket;
			try {
				clientOutStream = new ObjectOutputStream(this.socket.getOutputStream());
				clientInStream = new ObjectInputStream(this.socket.getInputStream());
			} catch (Exception e) {
				System.err.println("failed to get in out streams for client");
				e.printStackTrace();
			}
		}

		public void run() {
			// tell the client its id
			writeToClients(new Message(4, "server", "", clientId, 0));

			try {
				// get the name of the user
				Message nameMessage = (Message) clientInStream.readObject();
				this.userName = nameMessage.getName();

				// get the "welcome" and "has come online" messages
				sendMessage((Message) clientInStream.readObject());
				sendMessage((Message) clientInStream.readObject());

			} catch (Exception e) {
			}

			// then read messages forever
			while (running)
				listenForMessages();
		}

		// listen for one message
		private void listenForMessages() {
			Message msg = null;
			try {
				// read object blocks the thread until it reads something
				msg = (Message) clientInStream.readObject();
				messages.add(msg);
			} catch (ClassNotFoundException | IOException e) {
				// if the method couldn't read anything, remove the client, its probably dead
				remove(this.clientId, this);
				// e.printStackTrace();
				// and stop the thread
				running = false;
				return;
			}

			// process different message types
			switch (msg.getType()) {
			case 0:// broadcast
				System.out.println(msg);
				writeToClients(msg);
				break;
			case 1:// unicast
				System.out.println("ucast: " + msg);
				writeToClients(msg);
				break;
			case 2:// multicast
				System.out.println("mcast: " + msg);
				writeToClients(msg);
				break;
			case 3:// server message
				System.out.println("msg for me!" + msg);
				break;
			case 4:// client message
				System.err.println("i shouldnt be receiving this");
				break;
			case 5:// command message
				doCommand(msg);
				break;
			default:
				System.err.println("fail in determining message type:\n" + msg);
				break;
			}
		}

		// send messages as objects
		private boolean sendMessage(Message message) {
			try {
				clientOutStream.writeObject(message);
				clientOutStream.flush();
			} catch (IOException e) {
				System.err.println("failed to send message, client dead");
				return false;
			}
			return true;
		}

		// command processing routine
		private void doCommand(Message msg) {
			String command = msg.getContent().substring(1);
			Message answer;

			// command switch
			switch (command) {
			// if command was empty ("/")
			case "":
				// or if command was "/help"
				// show help message
			case "help":
				String help = "\n\n_Help_message_\n" + "To execute commands write '/COMMAND' "
						+ "where COMMAND is one of the following:\n" + "- help  => show this help\n"
						+ "- users => display users\n" + "\n" + "You can PM others with '@ID' where ID "
						+ "is the id of the client you want to PM. " + "To find out, what id the client has, "
						+ "use the command /users.";
				answer = new Message(4, "server", help, this.clientId);
				writeToClients(answer);
				break;
			//generate user list if command was "/users"
			case "users":
				String users = "Showing the users currently online:\n";
				for (ClientThread clientThread : clients) {
					if (clientThread == null)
						continue;
					users += "\n" + clientThread.clientId + " is " + clientThread.userName;
					if (clientThread.clientId == msg.getSender())
						users += "(<== you)";
				}
				answer = new Message(4, "server", users, this.clientId);
				writeToClients(answer);
				break;
			case "exit":
			case "stop":
				writeToClients(new Message(4, "server", "exit", msg.getSender(), 0));
				break;
			}
		}
	}
}
