import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Server_Thread extends Thread {



	boolean stop = false;


	private ObjectOutputStream output;
	private ObjectInputStream input;
	public ServerSocket server_socet;
	private Socket connection_socet;
	

	
	
	public Server_Thread(){

	}



	public void run(){

		
		while(true) {

			if(stop) {
				closeConnection();
				return;
			}
			if(!server_frame.isconnected) {
				startServer();
			}
			if(server_frame.isconnected) {
				System.out.println("bagli");
			}






			try {
				TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			}

		}
		
	}


	


	
	
	
	
	
	
	

	
	//--------------------------------------OPERATION functions------------------------------------------------------------
 
	

	/*
	 * first a sendfile request and requested file path sent to client
	 * than client looks for the path and returns an answer
	 * if answer is yes than path is exists so clients waits for sending and server waits for receiving
	 * if answer is no than both client and server quits operation so no one hangs 
	 */
	public void OPERATION_send_clients_file_to_server(String clients_abs_file_dir, String servers_file_dir) {


		//send the sendfile request and file path
		Message m;
		m = set_message("sendfile", clients_abs_file_dir);

		try {
			send_message(m);
		} 
		catch (ConnectionLostException e1) {
			return;
		}


		//get the answer from client
		try {
			m = receive_message();
		} 
		catch (ConnectionLostException e1) {
			return;
		}



		//if answer is returned control value should be ispathexists
		if(m.control.equals("ispathexists")) {

			//if path exists clients sends a yes value and it is ready to send file
			if(m.value.equals("yes")) {

				servers_file_dir = create_new_dir(servers_file_dir);

				
				//seperate clients file name and use it again for saving file to server
				Path p = Paths.get(clients_abs_file_dir);
				String client_dir = p.getParent().toString();
				String client_file_name = p.getFileName().toString();
				
				String abs_path = servers_file_dir + File.separator + "new-" + client_file_name;
				
				
				receive_file(abs_path);

				JOptionPane.showMessageDialog(null, "File downloaded\n" + abs_path, "alert",JOptionPane.PLAIN_MESSAGE);


			}

			//if value is no than path does not exists 
			else if(m.value.equals("no")) {
				System.out.println("PATH DOES NOT EXISTS");
				JOptionPane.showMessageDialog(null, "PATH DOES NOT EXISTS", "alert",JOptionPane.ERROR_MESSAGE);
			}
			else {
				System.out.println("answer error");
			}

		}

	}
	
	
	public void OPERATION_send_servers_file_to_client(String servers_abs_file_dir_to_send, String clients_folder_dir_to_save) {
		
		//send the receivefile request and file path
		Message m;
		m = set_message("receivefile", clients_folder_dir_to_save);

		try {
			send_message(m);
		} 
		catch (ConnectionLostException e1) {
			return;
		}
		
		send_file(servers_abs_file_dir_to_send);
		
		JOptionPane.showMessageDialog(null, "File saved", "alert",JOptionPane.PLAIN_MESSAGE);

	}
	
	public void OPERATION_screenshot(String servers_file_dir) {
		
		//send the receivefile request and file path
		Message m;
		m = set_message("screenshot", "");

		try {
			send_message(m);
		} 
		catch (ConnectionLostException e1) {
			return;
		}
		
		servers_file_dir = create_new_dir(servers_file_dir);
		
		String ss_name = check_dir_for_existing_screenshoots(servers_file_dir,"png");
		
		String abs_path = servers_file_dir + File.separator + ss_name;
		
		receive_file(abs_path);
		
		JOptionPane.showMessageDialog(null, "ss saved\n" + abs_path, "alert",JOptionPane.PLAIN_MESSAGE);

	}
	

	public void OPERATION_show_message(String message) {
		Message m;
		m = set_message("showmessage",message);

		try {
			send_message(m);
		} 
		catch (ConnectionLostException e) {
			return;
		}

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
			//e.printStackTrace();
			System.out.println("\nmessage is broken IOexception\n");

			closeConnection();

			throw new ConnectionLostException();
		}


	}



	private Message receive_message() throws ConnectionLostException {

		Message m;
		try {
			m = (Message) input.readObject();
		} catch (ClassNotFoundException | IOException e) {
			//e.printStackTrace();
			
			closeConnection();

			throw new ConnectionLostException();
		}
		return m;

	}
	
	
	

	//only sends file
	void send_file(String path){
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
	void receive_file(String path) {
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


	String create_new_dir(String name) {

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

	String check_dir_for_existing_screenshoots(String theDir, String file_type) {

		int counter = 0;

		//loop until a possible ss file name
		while(true) {

			//directory + basefilename + counter + filetype from combobox 
			File file = new File(theDir + File.separator + "ss" + counter + "." + file_type);
			if(!file.exists()) { 
				return "ss" + counter + "." + file_type;
			}
			else {
				counter++;
			}

		}
	}

	Message set_message(String control, String value) {
		Message m = new Message();
		m.control = control;
		m.value = value;
		return m;
	}

	
	void stop_thread() {
		stop = true;
	}
	
	//----------------------------------------------------------------------------------------------------------------------


	
	//-------------------------------------------connection functions-----------------------------------------------------



	public void startServer(){
		try {
			server_socet = new ServerSocket(12349, 100);
				openConnection();
		} 
		catch (IOException e) {
			displayMessage("\nIOexception\n");
		}
	}




	private void openConnection() throws IOException{
		displayMessage("\nWaiting for connection\n");
		
		connection_socet = server_socet.accept();
		
		
		//ObjectOutputStream **MUST** be created first
		output = new ObjectOutputStream(connection_socet.getOutputStream());
		output.flush();
		input = new ObjectInputStream(connection_socet.getInputStream());
		
		
		server_frame.isconnected = true;
		server_frame.iswaiting = false;
		server_frame.info_label.setText("connected");

		displayMessage("Connection received from: " + connection_socet.getInetAddress().getHostName());		
	}




	private void closeConnection(){
		displayMessage("\nTerminating connection\n");
		
		try{
			output.close();
			input.close();
			connection_socet.close();
		}
		catch (Exception e) {
			System.out.println("\nconnection either already closed or can not be closed\n");
		}

		try{
			server_socet.close();
		}
		catch (Exception e) {
			
		}
		finally {
			server_frame.isconnected = false;
			server_frame.iswaiting = false;
			server_frame.info_label.setText("not connected");
		}

	}




	private void displayMessage(final String messageToDisplay){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				System.out.println(messageToDisplay);
			}
		});
	}


	//----------------------------------------------------------------------------------------------------------------------










	


















	private void processConnection() throws IOException{


		while(true){
			try {




				/*

					m.yilan = frame_server.rect;

					m.foods = frame_server.foods;

					m.antifoods = frame_server.antifoods;

					m.snakecolor1 =  frame_server.snakecolor1;

				    m.snakecolor2 =  frame_server.snakecolor2;

					m.snakecolor3 =  frame_server.snakecolor3;

					m.snakecolor3 =  frame_server.snakecolor3;

					m.points_server = frame_server.foodeaten;

					m.name_server =  frame_server.name;

					//m.clientwin = frame_server.clientwin;

					m.yedin = frame_server.yedin;

					m.foodeaten_server_to_client = frame_server.foodeaten3;


					sendData(m);





					Message m2 = (Message) input.readObject();

					frame_server.rect2 = m2.yilan;

					frame_server.foodeaten2 = m2.points_client;

					frame_server.name2 = m2.name_client;

					//frame_server.serverwin = m2.serverwin;

					frame_server.gameover = Message.gameover;


				 */

			} 
			/*
			catch (IOException e) {
				//e.printStackTrace();
				System.out.println("\nmessage is broken IOexception\n");
				break;
			}
			 */
			catch (Exception e) {
				System.out.println("server saçma sapan biþey aldý");
				displayMessage("\nUnknown object type recevied");


			}
		}

	}







}
