package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.*;
import javax.swing.text.DefaultCaret;

import net.Client;
import net.Message;

public class ClientGUI extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	int width = 500, height = 300;
	private JButton buttonSend;
	private JTextArea textAreaMessages;
	private JTextField textFieldMessage;
	Client client;
	private boolean firstInput = true;
	Font font = new Font("sans-serif", 0, 20);
	
	

	public ClientGUI(Client client) {
		this.client = client;
		createView();
		setMinimumSize(new Dimension(width, height));
		setTitle("ClientGUI");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		// pack();
	}

	private void createView() {
		// main content panel
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setMinimumSize(new Dimension(width, height));
		getContentPane().add(mainPanel);

		// panel on the bottom containing send button and text field
		JPanel bottomPanel = new JPanel(new BorderLayout());
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);

		// textarea for all incoming messages
		textAreaMessages = new JTextArea();
		textAreaMessages.setEditable(false);
		textAreaMessages.setLineWrap(true);
		textAreaMessages.setWrapStyleWord(true);
		textAreaMessages.setFont(font);

		// autoscroll
		DefaultCaret caret = (DefaultCaret) textAreaMessages.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		// scroll pane wrapping the above text area
		JScrollPane scrollPane = new JScrollPane(textAreaMessages);
		// scrollPane.setPreferredSize(new Dimension(width, 200));
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setAutoscrolls(true);
		mainPanel.add(scrollPane, BorderLayout.CENTER);

		// field for writing and reviewing messages before sending
		textFieldMessage = new JTextField(12);
		// keylistener on the field, listening for the 'enter' key, more specifically on
		// '\n'
		textFieldMessage.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == '\n') {
					String text = textFieldMessage.getText();
					textFieldMessage.setText("");
					if (firstInput) {
						client.setUserName(text);
						client.waitForName = false;
						firstInput = false;
						return;
					}

					if (!text.equals("")) {
						// textAreaMessages.append(text + "\n");
						client.process(text);
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}
		});
		bottomPanel.add(textFieldMessage, BorderLayout.CENTER);

		// button to send the text displayed in the text field
		buttonSend = new JButton("send");
		// action listener to listen for clicks and then sending the message, if not
		// empty
		buttonSend.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String text = textFieldMessage.getText();
				textFieldMessage.setText("");
				if (firstInput) {
					client.setUserName(text);
					client.waitForName = false;
					firstInput = false;
					return;
				}

				if (!text.equals("")) {
					// textAreaMessages.append(text + "\n");
					client.process(text);
				}
			}
		});
		bottomPanel.add(buttonSend, BorderLayout.EAST);
	}

	// add given message to the text area
	public void addMessage(Message msg) {
		this.textAreaMessages.append(msg.toString() + "\n");
	}

}
