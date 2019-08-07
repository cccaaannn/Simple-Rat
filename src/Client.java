import java.awt.AWTException;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;



public class Client {


	private ObjectOutputStream output;
	private ObjectInputStream input;


	private String serverIP;
	private int serverPORT;
	
	
	private Socket client;




	Client(String serverIP, int serverPORT){
		this.serverIP = serverIP;
		this.serverPORT = serverPORT;
		startClient();
	}	





	//--------------------------------------OPERATION functions------------------------------------------------------------

	/*
	 * first a sendfile request and requested file path sent to client
	 * than client looks for the path and returns an answer
	 * if answer is yes than path is exists so clients waits for sending and server waits for receiving
	 * if answer is no than both client and server quits operation so no one hangs 
	 */
	public void OPERATION_send_clients_file_to_server(Message m) {

		//checks files existance
		String path = m.value;
		File file = new File(path);


		if(file.exists()) {
			m = set_message("ispathexists","yes");

			try {
				send_message(m);
			} 
			catch (ConnectionLostException e) {
				return;			
			}

			send_file(path);

		}

		else {
			m = set_message("ispathexists","no");

			try {
				send_message(m);
			} 
			catch (ConnectionLostException e) {
				return;			
			}

		}

	}

	public void OPERATION_send_servers_file_to_client(Message m) {

		String path = m.value;

		if(!path.equals("") || !path.replaceAll(" ","").equals("")) {
			//seperate path from filename
			Path p = Paths.get(path);
			String dir = p.getParent().toString();
			String name = p.getFileName().toString();

			
			
			//checks folders existance
			File folder = new File(dir);
			File file = new File(path);
			

			if(folder.isDirectory() && !file.exists()) {
				
				receive_file(path);
			}
			else {
				path = "NEW.can";
				receive_file(path);
			}
		}
		else {
			path = "NEW.can";
			receive_file(path);
		}

	}


	public void OPERATION_screenshot(Message m) {

		String ss_path = "screenshot.png";

		try {
			BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
			ImageIO.write(image, "png", new File(ss_path));
		} catch (HeadlessException | AWTException | IOException e1) {
			System.out.println("ss error");
			e1.printStackTrace();
		}

		send_file(ss_path);

		File file = new File(ss_path);
		file.delete();

	}


	public void OPERATION_show_message(Message m) {
		JOptionPane.showMessageDialog(null,  m.value, "alert",JOptionPane.ERROR_MESSAGE);
	}

	//----------------------------------------------------------------------------------------------------------------------




	//-------------------------------------send receive functions---------------------------------------------------------


	private void send_message(Message message) throws ConnectionLostException{
		try {
			output.writeObject(message);
			output.reset();
			output.flush();
		} 
		catch (IOException e) {
			System.out.println("\nIOexception\n");

			closeConnection();

			throw new ConnectionLostException();
		}

	}



	private Message receive_message() throws ConnectionLostException {

		Message m;
		try {
			m = (Message) input.readObject();
		} catch (ClassNotFoundException | IOException e) {

			closeConnection();

			throw new ConnectionLostException();
		}
		return m;

	}




	//only sends file
	private void send_file(String path){
		File f = new File(path);
		byte[] content;

		try {
			content = Files.readAllBytes(f.toPath());
			output.writeObject(content);
			output.reset();
			output.flush();

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("file write error");
		}
	}

	//only receives file
	private void receive_file(String path) {
		File f = new File(path);

		byte[] content;

		try {
			content = (byte[]) input.readObject();
			Files.write(f.toPath(), content);
		} 
		catch (ClassNotFoundException | IOException e1) {
			e1.printStackTrace();
			System.out.println("file read error");
		}
	}



	//----------------------------------------------------------------------------------------------------------------------




	//------------------------------------------------utility functions---------------------------------------------------


	private String create_new_dir(String name) {

		File theDir = new File(name);

		// if the directory does not exist, create it
		if (!theDir.exists()) {
			System.out.println("creating directory: " + theDir.getName());
			try{
				theDir.mkdir();
				System.out.println("DIR created"); 
			} 
			catch(SecurityException se){
				System.out.println("DIR is not created"); 
			}        
		}

		return theDir.toString();
	}


	private Message set_message(String control, String value) {
		Message m = new Message();
		m.control = control;
		m.value = value;
		return m;
	}


	//----------------------------------------------------------------------------------------------------------------------





	//-------------------------------------------connection functions-----------------------------------------------------


	private void client_connection_loop() throws IOException{

		while(true){
			try {

				Message m;

				//get the message 
				try {
					m = receive_message();
				} 
				catch (ConnectionLostException e1) {
					return;
				}


				//decide the process
				if(m.control.equals("showmessage")) {
					OPERATION_show_message(m);
				}

				if(m.control.equals("sendfile")) {
					OPERATION_send_clients_file_to_server(m);
				}

				if(m.control.equals("receivefile")) {
					OPERATION_send_servers_file_to_client(m);
				}

				if(m.control.equals("screenshot")) {
					OPERATION_screenshot(m);
				}

			} 
			catch (Exception e) {
				System.out.println("\nUnknown object type recevied");
				break;
			}
		}

	}



	private void startClient(){
		while(true){
			try {
				openConnection();
				client_connection_loop();
			} 
			catch (IOException e) {
				System.out.println("\nIOexception\n");
			}
		}
	}



	private void openConnection() throws IOException{
		System.out.println("Attempting connection\n");
		client = new Socket(InetAddress.getByName(serverIP), serverPORT);		
		System.out.println("Connected to: " + client.getInetAddress().getHostName());

		//ObjectOutputStream **MUST** be created first
		output = new ObjectOutputStream(client.getOutputStream());
		output.flush();
		input = new ObjectInputStream(client.getInputStream());

		System.out.println("\nGot I/O streams\n");

	}


	private void closeConnection(){
		System.out.println("\nClosing connection\n");

		try{
			output.close();
			input.close();
			client.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	//----------------------------------------------------------------------------------------------------------------------





}








