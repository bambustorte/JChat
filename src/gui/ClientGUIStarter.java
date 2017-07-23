package gui;

//import javax.swing.SwingUtilities;

import net.Client;

public class ClientGUIStarter {

	public static void main(String[] args) {

		Client c1 = new Client("localhost", 8888);

		// SwingUtilities.invokeLater(new Runnable() {
		//
		// @Override
		// public void run() {
		ClientGUI cGui = new ClientGUI(c1);
		cGui.setVisible(true);
		c1.setGui(cGui);
		// }
		// });

		Thread t1 = new Thread(c1);
		t1.start();
	}
}
