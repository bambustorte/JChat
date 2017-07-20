import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server implements Runnable {

	private ArrayList<Message> messages;
	private ArrayList<ClientThread> clients;
	private ServerSocket serverSocket;
	private static Server instance = null;
	private int id;

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

	public static Server getInstance(int port) {
		if (instance == null) {
			instance = new Server(port);
		}
		return instance;
	}

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

	public void remove(int i, ClientThread client) {
		clients.add(i, null);
		clients.remove(i + 1);
		System.err.println("removed " + client.userName + " with id " + client.clientId);
	}

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
				Message nameMessage = (Message) clientInStream.readObject();
				this.userName = nameMessage.getName();
				//System.out.println(this.userName);
				sendMessage((Message) clientInStream.readObject());
				sendMessage((Message) clientInStream.readObject());
				
			} catch (Exception e) {

			}
			while (running)
				listenForMessages();
		}

		private void listenForMessages() {
			Message msg = null;
			try {
				msg = (Message) clientInStream.readObject();
				messages.add(msg);
			} catch (ClassNotFoundException | IOException e) {
				//System.err.println("server cannot read object, maybe clients dead");
				remove(this.clientId, this);
				// e.printStackTrace();
				running = false;
				return;
			}

			switch (msg.getType()) {
			case 0:
				System.out.println(msg);
				writeToClients(msg);
				break;
			case 1:
				System.out.println("ucast: " + msg);
				writeToClients(msg);
				break;
			case 2:
				System.out.println("mcast: " + msg);
				writeToClients(msg);
				break;
			case 3:
				System.out.println("msg for me!" + msg);
				break;
			case 4:
				System.out.println("i shouldnt be receiving this");
				break;
			case 5:
				doCommand(msg);
				break;
			default:
				System.out.println("fail in determining message type");
				break;
			}
		}

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
		
		private void doCommand(Message msg) {
			String command = msg.getContent().substring(1);
			Message answer;
			
			switch(command) {
			case "help":
				String help = "\n\n_Help_message_\n"
						+ "To execute commands write '/COMMAND' "
						+ "where COMMAND is one of the following:\n"
						+ "- help  => show this help\n"
						+ "- users => display users\n"
						+ "\n"
						+ "You can PM others with '@ID' where ID "
						+ "is the id of the client you want to PM. "
						+ "To find out, what id the client has, "
						+ "use the command /users.";
				answer = new Message(4, "server", help, this.clientId);
				writeToClients(answer);
				break;
			case "users":
				String users = "Showing the users currently online:\n";
				for (ClientThread clientThread : clients) {
					if(clientThread == null)
						continue;
					users += "\n" + clientThread.clientId + " is " + clientThread.userName;
				}
				answer = new Message(4, "server", users, this.clientId);
				writeToClients(answer);
				break;
			}
		}
	}
}
