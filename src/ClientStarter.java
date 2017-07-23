import net.Client;

public class ClientStarter {

	public static void main(String[] args) {
		Thread c1t;
		
		switch (args.length) {
		case 0:
			c1t = new Thread(new Client("localhost", 8888));
			break;
		case 1:
			c1t = new Thread(new Client(args[0], 8888));
			break;
		case 2:
			c1t = new Thread(new Client(args[0], Integer.parseInt(args[1])));
			break;
		default:
			System.out.println("Usage: 'java ClientStarter [hostname [portnumber]]'");
			return;
		}
		
		c1t.start();
	}

}
