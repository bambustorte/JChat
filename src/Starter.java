
public class Starter {

	public static void main(String[] args) {
		/*
		Server server = Server.getInstance();
		Thread serv = new Thread(server);
		serv.start();
		try {
			Thread.sleep(4000);
		} catch (Exception e) {

		}
		*/
		Thread c1t = new Thread(new Client());
		c1t.start();
	}

}
