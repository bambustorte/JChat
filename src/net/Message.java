package net;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Message class representing message objects.
 * 
 * 
 * Type can be one of the following:
 * 
 * 0 => broadcast message
 * 
 * 1 => message to just the given receiver
 * 
 * 2 => message to all except given receiver
 * 
 * 3 => message to server
 * 
 * 4 => message to client
 * 
 * 5 => message is a command
 * 
 * @author max
 * 
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 42L;

	private int type;
	private int receiver;
	private String name;
	private String content;
	private Date time;
	private SimpleDateFormat sdf;
	private int sender;

	
	public Message(int type, String name, String content, int receiver) {
		this(type, name, content, receiver, -1);
	}

	public Message(int type, String name, String content, int receiver, int sender) {
		super();
		this.receiver = receiver;
		this.type = type;
		this.name = name;
		this.content = content;
		this.time = new Date();
		this.sender = sender;
		this.sdf = new SimpleDateFormat("HH:mm:ss");
	}

	public int getSender() {
		return sender;
	}

	public void setSender(int sender) {
		this.sender = sender;
	}

	public int getReceiver() {
		return receiver;
	}

	public void setReceiver(int receiver) {
		this.receiver = receiver;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String toString() {
		return "[" + sdf.format(this.getTime()) + "]" + " <" + this.getName() + "> " + this.getContent();
	}

}
