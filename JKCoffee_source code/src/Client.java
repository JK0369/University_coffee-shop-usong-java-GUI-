import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/* <JKCoffe> - Client class
 * 201352048 ������
 * 
 * (�߿� �������� �� �޼ҵ� ����)
 * 1. dbAdm : ������ Ŭ������ ��ȣ�ۿ��ϱ� ���� ���� 
 * 2. con : DB�� �����ϱ� ���� ����
 * 3. northPanel : Ŀ�� �޴� ���� GUI
 * 4. serverPanel : ���� GUI ����  (���� ������ ThreadŬ������ ����)	
 * 5. MyThread : ��ſ� ���� ������ ���� Ŭ����
 * 6. Client ������ : ��ü���� ����(���⼭ AdministratorDB���� ����)
 * 
 * */

public class Client extends JFrame implements ItemListener, ActionListener {
	
	// 1. ������ Ŭ������ ��ȣ�ۿ� 
	// DB & GUI (Client) <-> ������ GUI (Server) - DB������ Ŭ������ �����ϱ� ���� ����
	private AdministratorDB dbAdm;
	
	// 2. DB�� �����ϱ� ���� ����
	// DB���� ����
	private Connection con = null;
	private Statement stmt;
	private Vector data;
	
	// 3. Ŀ�� �޴� ���� GUI
	// Ŀ�Ǹ޴��� ���� �ֹ���Ȳ�� ���δ� Panel
	JPanel northPanel;
	
	// Ŀ�� �޴��� ice, hot�� �����ϴµ� �ʿ��� ����
	JPanel menuPanel;  // Ŀ�� �޴��� ���� Panel		
	public String [] textMenu = {"����������", "�Ƹ޸�ī��", "ī���", "īǪġ��","ī���ī","�׸�Ƽ��"}; // �޴� ����
	public JRadioButton [] menu = new JRadioButton[textMenu.length]; // ������ư���� ������ �޴�
	public ButtonGroup bg; 		// Ŀ�� �޴��� �׷�
	public JPanel typePanel; 	// ice���� hot���� �����ϴ� ���� ��ư�� ���� Panel
	public ButtonGroup bg2; 	// ice, hot�� �׷�		
	public JRadioButton ice;
	public JRadioButton hot;

	// Advanced GUI - JTable(�ֹ���Ȳ) ������ ���̴� ����	
	public JTable stateTable; 	// �޴� �߰� ��Ȳ�� ǥ���ϴ� ���̺�
	public Vector stateTitle;	// ���̺� ǥ���� �̸�
	public static DefaultTableModel orderListModel; // �ֹ���Ȳ ���̺� : ������ Ŭ�������� �����ϱ� ���� static ����	
	public Vector orderResult;	// DB�� orderList ���̺� ��������� ������ ���� : ���̺� ǥ��
	
	// ���� �ֹ��� �Ѿװ� �߰� ��ư�� �� Panel�� ����
	public JPanel centerPanel;
	public JTextField sumTF;		  	// "�Ѿ�" 
	public static JTextField valueTF; 	// �Ѿ� �� - ������ Ŭ�������� �����ϱ� ���� static ����
	public JButton addBtn;				// �߰� ��ư
	
	// 4. ���� GUI ����  (���� ������ ThreadŬ������ ����)	
	// ���� ���õ� ������ �� Panel
	JPanel serverPanel;	
	JTextField textFieldChat;			// Ű���� �Է��� �޾��� ����
	JTextArea textAreaChat;				// ä�� ����
	InetAddress address = null;
	BufferedReader in = null;
	BufferedWriter out = null;
	Socket socket = null;
	
	// 5.��ſ� ���� ������ Ŭ���� : �� Ŭ������  ���� ������ �����ϱ� ���� ����Ŭ������ ����
	class MyThread extends Thread{
		public void run() {
			String inputMessage;
			try {
				socket = new Socket("localhost", 9999);
				System.out.println("�����");
				in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // �����κ����� �Է� ��Ʈ��
				out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); // �������� ��� ��Ʈ��
				
				while (true){
					try {
						inputMessage = in.readLine();						
						textAreaChat.append("[RECEIVED]: " + inputMessage + "\n");
						textFieldChat.selectAll();
						textAreaChat.setCaretPosition(textAreaChat.getDocument().getLength());						
					} catch (IOException e) {
						e.printStackTrace();
					}
				}				
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Unknown Host Error");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Buffer Error");
			}
		}			
	}
	
	// 6. ��ü���� ����(���⼭ AdministratorDB���� ����)
	public Client(){
		
		// db�����ڿ� ������ ���ؼ� ��ü ����
		dbAdm = new AdministratorDB();
		
		// DB����
		con = makeConnection();		
		
		// �����͸� ���� ����
		data = new Vector<>();	
		
		setTitle("Client");
		setSize(530,600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		northPanel = new JPanel();
		centerPanel = new JPanel();
				
		// �޴� �гο� �ֱ�
		menuPanel = new JPanel();
		menuPanel.setLayout(new GridLayout(0,2));
		bg = new ButtonGroup();	
		bg2 = new ButtonGroup();

		for (int i=0; i < menu.length; i++) {
			menu[i] = new JRadioButton(textMenu[i]);
			menuPanel.add(menu[i]);
			bg.add(menu[i]);
			menu[i].addItemListener(this);
		}
		
		// �޴� ���� ����
		menu[0].setBackground(Color.WHITE);
		menu[1].setBackground(Color.LIGHT_GRAY);
		menu[2].setBackground(Color.LIGHT_GRAY);
		menu[3].setBackground(Color.WHITE);
		menu[4].setBackground(Color.WHITE);
		menu[5].setBackground(Color.LIGHT_GRAY);
		
		ice = new JRadioButton("ice");
		hot = new JRadioButton("hot");
		bg2.add(ice); bg2.add(hot);
		
		typePanel = new JPanel();
		typePanel.setLayout(new GridLayout(2,0));
		typePanel.add(ice);
		typePanel.add(hot);
		typePanel.setBorder(new TitledBorder(new LineBorder(Color.darkGray,1),"type")); // �г� �׵θ� �׸��� �޼ҵ�
		
		menuPanel.add(typePanel, BorderLayout.SOUTH);
		menuPanel.setBorder(new TitledBorder(new LineBorder(Color.gray,3),"�޴�����"));
		northPanel.add(menuPanel);
		
		// stateTable
		stateTitle = new Vector<>();
		stateTitle.add("��ȣ");		
		stateTitle.add("����");
		stateTitle.add("ǰ��");
		stateTitle.add("����");
		
		orderResult = selectAll("orderList");
		orderListModel = new DefaultTableModel(orderResult, stateTitle);
		stateTable = new JTable(orderListModel);
		
		JScrollPane scroll = new JScrollPane(stateTable);
		scroll.setPreferredSize(new Dimension(300,310)); 	// tableũ�� ����
		northPanel.add(scroll);
		add(northPanel, BorderLayout.NORTH);
		
		// centerPanel : TextField �� ��, Button �� ��
		// TextField �� ��
		sumTF = new JTextField("�Ѿ�",25);
		valueTF = new JTextField("0 ��",15);		
		centerPanel.add(sumTF);
		centerPanel.add(valueTF);			
		// �߰� ��ư
		addBtn = new JButton("�߰�");		
		addBtn.addActionListener(this);				
		centerPanel.add(addBtn);		
		add(centerPanel, BorderLayout.CENTER);
		
		// ���� �ձ��ϱ�  : ȭ�� ����� ��, ���õ� �޴��� �ִٸ� ������ ���ϴ� �۾�
		int sum=0;
		for(int i=0;i<orderListModel.getRowCount();i++) {
			sum+=Integer.parseInt(orderListModel.getValueAt(i, 3)+"");
		}
		valueTF.setText(sum+"��");

		// serverPanel����  - server GUI
		serverPanel = new JPanel();
	
		textFieldChat = new JTextField(45);
		textFieldChat.addActionListener(this);

		textAreaChat = new JTextArea();
		textAreaChat.setEditable(false);
		
		JScrollPane sp = new JScrollPane(textAreaChat);
		sp.setPreferredSize(new Dimension(400,100)); 	// TextAreaũ�� ����
		
		serverPanel.setLayout(new BorderLayout());
		serverPanel.add(sp, BorderLayout.CENTER);
		serverPanel.add(textFieldChat, BorderLayout.SOUTH);
		serverPanel.setBorder(new TitledBorder(new LineBorder(Color.LIGHT_GRAY,2),"Chat to JKCoffee"));
		add(serverPanel, BorderLayout.SOUTH);
		
		pack();
		setVisible(true);
		
		// �����带 ���� ä�� ��� ����
		MyThread thread = new MyThread();
		thread.start();
	}
	
	private Connection makeConnection() {
		
		// db����
		String url = "jdbc:mysql://localhost/JKCoffee";
        String id = "root";
        String password = "onlyroot";
        Connection con = null;
		
		// ����̹� ���ε�
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Client ����̹� ���� ����");
			
			// connection
			con = (Connection) DriverManager.getConnection(url, id, password);
			System.out.println("Client �����ͺ��̽� ���� ����");
		} catch (ClassNotFoundException e) { // ����̹� ���翡 ���� ����
			e.printStackTrace();
			System.out.println("Client ����̹� ���� ����");
		} catch (SQLException e) { // connection�� ���� ����
			e.printStackTrace();
			System.out.println("Client �����ͺ��̽� ���� ����");
		}
		return con;
	}
	
	private Vector selectAll(String dbTable) {
		try {
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select * from " + dbTable +";");
			
			data.clear(); // ������� ������ ���� ������ �ʴ°��� ��� ��������
			
			// JFrame�� �ִ� ���̺� �� row�� ��� �������
			while(rs.next()) {
				Vector<String> in = new Vector<String>();
				in.add(rs.getString("num"));
				in.add(rs.getString("t_type"));
				in.add(rs.getString("item"));
				in.add(rs.getString("price"));			
				data.add(in); // data�� ���������̹Ƿ� ��� ������
				
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return data;
	}

	public static void main(String[] args) {
		new Client();
	}

	@Override
	public void itemStateChanged(ItemEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		// �߰� ��ư
		if(arg0.getSource()==addBtn) {
			
			// ���� ��ư�� üũ�� �͵��� �ֹ�����Ʈ ���̺� ����
			String currType = getCheckType();
			String currMenu = getCheckMenu();
			String currPrice = getPrice(currMenu); // getPrice : ���� �޴��� ������ �˾Ƴ��� �޼ҵ� 

			insert(currType, currMenu, currPrice);
			
			// AdministratorDB�� orderList���̺� ����
			dbAdm.changeDB("orderList");
		}
		
		// ä�ñ�ɿ� ���� �ؽ�Ʈ �̺�Ʈó��
		if(arg0.getSource() == textFieldChat) {
			try {
				this.sendMessage();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.out.println("Error process");
			}
		}
	}
	
	// �μ��� ���� �޴��� ���� ����
	private String getPrice(String currMenu) {
		switch(currMenu) {
		case "����������": return "2000";
		case "�Ƹ޸�ī��": return "3500";
		case "ī���": return "4000";
		case "īǪġ��": return "5000";
		case "ī���ī": return "5500";
		case "�׸�Ƽ��": return "6000";				
		}
		System.out.println("getPrice--�޴� ������ �ȵ�");
		return null;
	}

	// Ŀ�� �޴� ������ư���� ���� üũ�� �� ����
	private String getCheckMenu() {
		for(int i=0;i<menu.length;i++) {
			if(menu[i].isSelected()) return menu[i].getText();
		}
		
		System.out.println("�޴� ���� �ȵ�");
		return null;
	}

	// type ������ư���� ���� üũ�� �� ����
	private String getCheckType() {
		if(ice.isSelected()) return ice.getText();
		if(hot.isSelected()) return hot.getText();
		System.out.println("type ���� �ȵ�");
		return null;
	}

	// orderList���̺� �����ϴ� �޼ҵ�
	private void insert(String type, String item, String price) {
		PreparedStatement pstmt;
		try {
			pstmt = (PreparedStatement) con.prepareStatement("insert into orderList(t_type,item,price) values(?,?,?);");
			
			pstmt.setString(1, type);
			pstmt.setString(2, item);
			pstmt.setString(3, price);
			
			pstmt.executeUpdate();
			System.out.println("�ֹ�����Ʈ ���̺� insert ����");
			
			Vector newResult = selectAll("orderList");	
			orderListModel.setDataVector(newResult, stateTitle);
			
			// ���� �ձ��ϱ� 
			int sum=0;
			for(int i=0;i<orderListModel.getRowCount();i++) {
				sum+=Integer.parseInt(orderListModel.getValueAt(i, 3)+"");
			}
			
			valueTF.setText(sum+"��");
					
		} catch (SQLException e) {

			e.printStackTrace();
			System.out.println("insert ����");
		}
		
	}
	
	// chat�� ���� �޼ҵ�
	public void sendMessage() throws IOException {
		
		String outputMessage = textFieldChat.getText();
		out.write(outputMessage+"\n"); // Ű���忡�� ���� ���ڿ� ����
		out.flush();
		textAreaChat.append("[SENT]: " + outputMessage + "\n");
		textFieldChat.setText("");
	}
}

