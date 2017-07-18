
public class starter2 {

	public static void main(String[] args) {		 
		Server server = Server.getInstance();
		Thread serv = new Thread(server);
		serv.start();
	}

}
