import java.awt.FileDialog;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class server_frame extends JFrame implements ActionListener {


	//buttons
	JButton start_server =  new JButton("start server");
	JButton stop_server =  new JButton("stop server");
	JButton send_message_button =  new JButton("send message");
	JButton get_file =  new JButton("get file");
	JButton send_file =  new JButton("send file");
	JButton screenshot_button =  new JButton("take ss");
	
	//labels
	static JLabel info_label = new JLabel();

	//textfieldes
	JTextField path_field = new JTextField();


	Server_Thread server_thread;


	FileDialog filedialog = new FileDialog(this, "Select File To Send");



	static boolean isconnected = false;
	static boolean iswaiting = false;

	server_frame(){


		create_frame();

		buttons();
		labels();
		textfields();
	}

	void create_frame() {
		setSize(300,280);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		setVisible(true);
		//setAlwaysOnTop(true);
		this.setLayout(null);
		setFocusable(true);
		setLocationRelativeTo(null);
	}



	void buttons() {

		add(get_file);
		get_file.setBounds(150,90,130,30);
		get_file.setFont(new Font("arial",Font.BOLD,13));
		get_file.addActionListener(this);		
		get_file.setFocusable(false);

		add(send_file);
		send_file.setBounds(10,90,130,30);
		send_file.setFont(new Font("arial",Font.BOLD,13));
		send_file.addActionListener(this);		
		send_file.setFocusable(false);

		add(send_message_button);
		send_message_button.setBounds(10,130,130,30);
		send_message_button.setFont(new Font("arial",Font.BOLD,13));
		send_message_button.addActionListener(this);		
		send_message_button.setFocusable(false);

		add(screenshot_button);
		screenshot_button.setBounds(150,130,130,30);
		screenshot_button.setFont(new Font("arial",Font.BOLD,13));
		screenshot_button.addActionListener(this);		
		screenshot_button.setFocusable(false);
		
		add(start_server);
		start_server.setBounds(150,200,130,30);
		start_server.setFont(new Font("arial",Font.BOLD,13));
		start_server.addActionListener(this);		
		start_server.setFocusable(false);

		add(stop_server);
		stop_server.setBounds(10,200,130,30);
		stop_server.setFont(new Font("arial",Font.BOLD,13));
		stop_server.addActionListener(this);		
		stop_server.setFocusable(false);

	}

	void labels() {
		add(info_label);
		info_label.setBounds(10,10,270,35);
		info_label.setFont(new Font("arial",Font.BOLD,25));
		//info_label.setForeground(green);
		info_label.setText("not connected");
		info_label.setHorizontalAlignment(SwingConstants.CENTER);
	}

	void textfields() {
		add(path_field);
		path_field.setBounds(10,50,270,30);
		path_field.setFont(new Font("arial",Font.BOLD,20));
		path_field.setFocusable(true);
		//get_file_field.setBackground(background_color);
		//get_file_field.setForeground(white);
		//get_file_field.setCaretColor(white);
	}





	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == start_server) {
			if(!isconnected && !iswaiting) {
				server_thread = new Server_Thread();
				server_thread.start();
				iswaiting = true;
				info_label.setText("waiting");
			}
		}

		if(e.getSource() == stop_server) {
			if(isconnected || iswaiting) {
				try {
					server_thread.server_socet.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				server_thread.stop_thread();
				isconnected = false;
				iswaiting = false;
				info_label.setText("not connected");
			}
		}

		if(e.getSource() == send_message_button) {
			if(isconnected) {
				server_thread.OPERATION_show_message(path_field.getText().toString());
			}
		}

		if(e.getSource() == screenshot_button) {
			if(isconnected) {
				server_thread.OPERATION_screenshot("downloaded");
			}
		}
		
		

		if(e.getSource() == get_file) {
			if(isconnected) {
				server_thread.OPERATION_send_clients_file_to_server(path_field.getText().toString(), "downloaded");
			}
		}


		if(e.getSource() == send_file) {
			if(isconnected) {
				filedialog.setMode(FileDialog.LOAD);
				filedialog.setVisible(true);
				String filedir = filedialog.getDirectory();
				String filename = filedialog.getFile();
				String abs_path = filedir + filename;
				
				if(filedir != null) {
					server_thread.OPERATION_send_servers_file_to_client(abs_path, path_field.getText().toString());
				}
				
			}
		}

		


	}




}
