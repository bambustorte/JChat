import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class Server implements Runnable {

	private ArrayList<Message> messages;
	private ArrayList<ClientThread> clients;
	private ServerSocket serverSocket;
	private static Server instance = null;
	private int id;
	private SimpleDateFormat time;

	private Server() {
		messages = new ArrayList<Message>();
		clients = new ArrayList<ClientThread>();
		id = 0;
		time = new SimpleDateFormat("HH:mm:ss");
		try {
			serverSocket = new ServerSocket(8888);
		} catch (IOException e) {
			System.err.println("unable to create server socket:");
			e.printStackTrace();
			System.exit(10);
		}
	}

	public static Server getInstance() {
		if (instance == null) {
			instance = new Server();
		}
		return instance;
	}

	private synchronized void writeToClients(Message message) {
		for (int i = (clients.size() - 1); i >= 0; i--) {
			ClientThread client = clients.get(i);
			if (!client.sendMessage(message)) {
				clients.remove(i);
				System.out.println("removed " + client.userName + " with id " + client.clientId);
			}
		}
	}

	private synchronized void writeToClients(Message message, int cID) {
		ClientThread client = clients.get(cID);
		if (!client.sendMessage(message)) {
			clients.remove(cID);
			System.out.println("removed " + client.userName + " with id " + client.clientId);
		}
	}

	public void run() {

		while (true) {
			try {
				Socket tempSocket = serverSocket.accept();
				ClientThread client = new ClientThread(id++, "anon", tempSocket);
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

		public ClientThread(int id, String userName, Socket socket) {
			this.clientId = id;
			this.userName = userName;
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
			while (running)
				listenForMessages();
		}

		private void listenForMessages() {
			try {
				Message msg = (Message) clientInStream.readObject();
				messages.add(msg);
				System.out.println(
						"[" + time.format(msg.getTime()) + "]" + " <" + msg.getName() + "> " + msg.getContent());
				if (msg.getReceiver() == -1)
					writeToClients(msg);
				else
					writeToClients(msg, msg.getReceiver());
			} catch (ClassNotFoundException | IOException e) {
				System.err.println("server cannot read object");
				e.printStackTrace();
				running = false;
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
	}
}
