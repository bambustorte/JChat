import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Client implements Runnable {

	Socket socket;
	private String userName = null;
	private ObjectInputStream clientInStream;
	private ObjectOutputStream clientOutStream;
	private Message newMessage;
	private Scanner scan;
	private int id;

	public Client(String ip, int port) {
		try {
			scan = new Scanner(System.in);
			socket = new Socket(ip, port);
			clientInStream = new ObjectInputStream(socket.getInputStream());
			clientOutStream = new ObjectOutputStream(socket.getOutputStream());
		} catch (Exception e) {
			System.err.println("failed to initialize client");
			e.printStackTrace();
		}

		id = getMessage().getReceiver();

		System.out.print("Enter username: ");
		while ((userName = scan.nextLine()) == null)
			;
		// userName = scan.nextLine();
		if (userName.equals("")) {
			userName = "anonymous";
		}
		serverMessage(userName);
	}

	public void run() {
		// try {
		// Thread.sleep(500);
		// } catch (Exception e) {
		//
		// }
		unicastMessage("Welcome " + userName + "! Your ID is " + this.id, id);
		unicastMessage("To get help, type /help", id);

		multicastMessage("has come online!");
		Thread gM = new Thread(new Runnable() {
			public void run() {
				while (true) {
					System.out.println("\r\r" + getMessage());
					System.out.print("$ ");
				}
			}
		});
		gM.start();
		String input;
		while (true) {
			input = scan.nextLine();
			if (!input.equals("")) {
				if (input.charAt(0) == '/') {
					commandMessage(input);
				} else if (Pattern.compile("@\\d+ .+").matcher(input).matches()) {
					
					//System.out.println("match!");

					int rec = Integer.parseInt(input.substring(1, input.indexOf(" ")));
					String cont = input.substring(input.indexOf(" ") + 1);
					unicastMessage(cont, rec);
				} else {
					broadcastMessage(input);
				}
			}
			System.out.print("$ ");
		}
	}

	private void broadcastMessage(String content) {
		Message msg = new Message(0, this.userName, content, -1, id);
		sendMessage(msg);
	}

	private void unicastMessage(String content, int cID) {
		Message msg = new Message(1, this.userName, content, cID, id);
		sendMessage(msg);
	}

	private void multicastMessage(String content) {
		Message msg = new Message(2, this.userName, content, id, id);
		sendMessage(msg);
	}

	private void serverMessage(String content) {
		Message msg = new Message(3, this.userName, content, 0, id);
		sendMessage(msg);
	}

	private void commandMessage(String content) {
		Message msg = new Message(5, this.userName, content, 0, id);
		sendMessage(msg);
	}

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

	private Message getMessage() {
		try {
			newMessage = (Message) clientInStream.readObject();
			return newMessage;
		} catch (Exception e) {
			System.err.println("could not read message, connection dead");
			e.printStackTrace();
			System.exit(5);
		}
		return null;
	}

}
