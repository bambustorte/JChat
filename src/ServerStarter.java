import net.Server;

public class ServerStarter {

	public static void main(String[] args) {
		
		Server server;
		
		switch (args.length) {
		case 1:
			server = Server.getInstance(Integer.parseInt(args[0]));
			break;
		case 0:
			server = Server.getInstance(8888);
			break;
		default:
			System.out.println("Usage: 'java ServerStarter [portnumber]'");
			return;
		}
		
		
		Thread serv = new Thread(server);
		serv.start();
	}

}
