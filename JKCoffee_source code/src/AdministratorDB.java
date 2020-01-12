import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/* <JKCoffe> - AdministratorDB class
 * 
 * (�߿� �������� �� �޼ҵ� ����)
 * 1. DB�� �����ϱ� ���� ���� & DB GUI
 * 2. DB table�� ��ü�ϴµ� ���� ��ư
 * 3. �Ѿ� �Ǵ� ��� �ֹ������� ���� ���� ǥ��
 * 4. �ֹ� ���� & ��� �ֹ� ��ư / �ֹ� ��ҹ�ư
 * 5. orderWhat : orderAcceptBtn�� �뵵 ���ϴ� ����
 * 6. ���� GUI ����  (���� ������ ThreadŬ������ ����)
 * 7. ��ſ� ���� ������ Ŭ���� : �� Ŭ������ ���� ������ �����ϱ� ���� ����Ŭ������ ����
 * 8. ��ü���� ����
 * 
 * */

public class AdministratorDB extends JFrame implements ActionListener {
	
	// 1. DB�� �����ϱ� ���� ���� & DB GUI
	private Connection con = null;
	private JTable table;
	private Vector title;	
	private Vector data;
	private Statement stmt;
	public Vector result;
	public DefaultTableModel model;	
	private PreparedStatement pstmt;
	
	// 2. DB table�� ��ü�ϴµ� ���� ��ư
	private JPanel infoPanel; 		// DB table��ü ��ư�� ���� �г�
	public JButton firstInfoBtn;	// �ֹ�����Ʈ ���̺�
	public JButton secondInfoBtn;	// �Ǹ���Ȳ ���̺�
	public JButton thirdInfoBtn;	// �����Ȳ ���̺�	
	public JTextField infoTf;		// � table���� ������ִ� ����
	
	// 3. �Ѿ� �Ǵ� ��� �ֹ������� ���� ���� ǥ��
	public JTextField sumTf;		// "�Ѿ�", ��� �ֹ� ������ ���� ���� 
	public JTextField sumValueTf;	// �Ѿ� ��
	
	// 4. �ֹ� ���� & ��� �ֹ� ��ư / �ֹ� ��ҹ�ư
	public JButton orderAcceptBtn;	
	public JButton delBtn;

	// 5. orderWhat : orderAcceptBtn�� �뵵 ���ϴ� ����
	// 0�̸� Client�� �ֹ� �¶� = �Ǹ���Ȳ ���̺� ������Ʈ
	// 1�̸� ��� �ֹ� = �����Ȳ ���̺� ������Ʈ
	private int orderWhat = 0;
	
	// 6. ���� GUI ����  (���� ������ ThreadŬ������ ����)
	JPanel serverPanel;
	
	protected JTextField textFieldChat;
	protected JTextArea textAreaChat;
	InetAddress address = null;

	BufferedReader in = null;
	BufferedWriter out = null;
	ServerSocket listener = null;
	Socket socket = null;
	
	// 7. ��ſ� ���� ������ Ŭ���� : �� Ŭ������ ���� ������ �����ϱ� ���� ����Ŭ������ ����
	class MyThread extends Thread{
		public void run() {
			String inputMessage;
			
			try {
				listener = new ServerSocket(9999);
				socket = listener.accept(); // Ŭ���̾�Ʈ�κ��� ���� ��û ���
				System.out.println("�����");
				in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Ŭ���̾�Ʈ�κ����� �Է� ��Ʈ��
				out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); // Ŭ���̾�Ʈ���� ��� ��Ʈ��
				
				while (true){
					try {
						inputMessage = in.readLine();
						textAreaChat.append("[RECEIVED]: " + inputMessage + "\n");
						textFieldChat.selectAll();
						textAreaChat.setCaretPosition(textAreaChat.getDocument().getLength());
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}			
				}
				//// 
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Buffer Error");
			} finally {
				try {
					socket.close();
					System.out.println("Ŭ���̾�Ʈ�� ���� ����");
					textAreaChat.append("Ŭ���̾�� ���� ����");
					listener.close();
					System.out.println("���� ���� ����");

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	}
	
	// 8. ��ü���� ����
	public AdministratorDB() {
						
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(450,630);
		setTitle("JKCoffee's Server");
		setLayout(new FlowLayout());
		
		// DB ����
		con  = makeConnection();		
		data = new Vector<>();		
		title = new Vector<>(); 

		// �ֹ�����Ʈ, �Ǹ���Ȳ, �����Ȳ ��ư
		infoPanel = new JPanel();

		firstInfoBtn = new JButton("�ֹ�����Ʈ");
		secondInfoBtn = new JButton("�Ǹ���Ȳ");
		thirdInfoBtn = new JButton("�����Ȳ");		
		
		firstInfoBtn.addActionListener(this);
		secondInfoBtn.addActionListener(this);
		thirdInfoBtn.addActionListener(this);
		
		infoPanel.add(firstInfoBtn);
		infoPanel.add(secondInfoBtn);
		infoPanel.add(thirdInfoBtn);
		add(infoPanel);
		
		result = selectAll("orderList");	// orderList ���̺��� ������� �������� �޼ҵ�
				
		// ���� ���� ��ư���� ǥ�� : Default���� �ֹ�����Ʈ
		infoTf = new JTextField("\t        �ֹ�����Ʈ",30);
		add(infoTf);
			
		// DB ���̺� ����
		title.add("��ȣ");
		title.add("����");
		title.add("ǰ��");
		title.add("�ݾ�");
		model = new DefaultTableModel(result, title);
		table = new JTable(model);
		JScrollPane sp = new JScrollPane(table);
		sp.setPreferredSize(new Dimension(400,300)); 	// JTableũ�� ����		
		add(sp);
		
		// �׼��� ���� ������ TF
		sumTf = new JTextField("�Ѿ�",20);
		sumValueTf = new JTextField("��",15);
		add(sumTf);
		add(sumValueTf);
		
		// �ֹ� ��ư
		orderAcceptBtn = new JButton("�ֹ�����");
		orderAcceptBtn.addActionListener(this);
		
		// �޴� ��� ��ư
		delBtn = new JButton("�޴����");
		delBtn.addActionListener(this);	
		
		// ���� �ձ��ϱ�  : ȭ�� ����� ��, ���õ� �޴��� �ִٸ� ������ ���ϴ� �޼ҵ�
		int sum=0;
		for(int i=0;i<model.getRowCount();i++) {
			sum+=Integer.parseInt(model.getValueAt(i, 3)+"");
		}
		sumValueTf.setText(sum+"��");
		
		///// serverPanel ������ ����
		serverPanel = new JPanel();
	
		textFieldChat = new JTextField(42);
		textFieldChat.addActionListener(this);

		textAreaChat = new JTextArea();
		textAreaChat.setEditable(false);
		
		JScrollPane scroll = new JScrollPane(textAreaChat);
		scroll.setPreferredSize(new Dimension(400,100)); 	// TextAreaũ�� ����
		
		serverPanel.setLayout(new BorderLayout());
		serverPanel.add(scroll, BorderLayout.CENTER);
		serverPanel.add(textFieldChat, BorderLayout.SOUTH);
		serverPanel.setBorder(new TitledBorder(new LineBorder(Color.LIGHT_GRAY,2),"Chat to Client"));
		add(serverPanel, BorderLayout.SOUTH);
		
		add(orderAcceptBtn);
		add(delBtn);
	
		setVisible(true);
		
		// �����带 ���� ä�� ��� ����
		MyThread thread = new MyThread();
		thread.start();
	}
	
	// �ڷḦ ��ȯ �ϴ� �޼ҵ�
	public Vector selectAll(String dbTable) {
		try {
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select * from " + dbTable +";");
			
			data.clear();
			
			// �����Ȳ ���̺� �����ϴ� ���
			if(dbTable.equals("stock")) {
				// JFrame�� �ִ� ���̺� �� row�� ��� �������
				while (rs.next()) {
					Vector<String> in = new Vector<String>();
					String num = rs.getString("num");
					in.add(rs.getString("num"));
					in.add(rs.getString("item"));
					in.add(rs.getString("amount"));
					in.add(rs.getString("measure"));
					data.add(in); // data�� ���������̹Ƿ� ��� ������.
				}
			}
			
			// �ֹ�����Ʈ�� �Ǹ���Ȳ ���̺� �����ϴ� ���
			else {
				// JFrame�� �ִ� ���̺� �� row�� ��� �������
				while (rs.next()) {
					Vector<String> in = new Vector<String>();
					String num = rs.getString("num");
					in.add(rs.getString("num"));
					in.add(rs.getString("t_type"));
					in.add(rs.getString("item"));
					in.add(rs.getString("price"));
					data.add(in); // data�� ���������̹Ƿ� ��� ������.
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return data;
	}

	private Connection makeConnection() {
		
		// 1. db����
		String url = "jdbc:mysql://localhost/JKCoffee";
        String id = "root";
        String password = "onlyroot";
        Connection con = null;
		
		// 2. ����̹� ���ε�
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("AdministratorDB ����̹� ���� ����");
			
			// 3. connection
			con = (Connection) DriverManager.getConnection(url, id, password);
			System.out.println("AdministratorDB �����ͺ��̽� ���� ����");
		} catch (ClassNotFoundException e) { // ����̹� ���翡 ���� ����
			e.printStackTrace();
			System.out.println("AdministratorDB ����̹� ���� ����");
		} catch (SQLException e) { // connection�� ���� ����
			e.printStackTrace();
			System.out.println("AdministratorDB �����ͺ��̽� ���� ����");
		}
		return con;
	}
	
	public static void main(String[] args) {
		 // new AdministratorDB();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		// �ֹ� ������ư = �Ǹ���Ȳ ���̺� ������Ʈ
		if(arg0.getSource() == orderAcceptBtn) {	
				
			// orderWhat�� 0�� ��� : �ֹȽ¶���ư
			if(orderWhat==0) orderAccept();
			
			// orderWhat�� 0�� ��� : ����ֹ���ư
			if(orderWhat==1) orderMeasure();

		}
		
		//���� ��ư
		if(arg0.getSource()==delBtn) {
			
			int index = table.getSelectedRow();
			String currNum = -1 + "";

			if (index != -1) {
				Vector<String> in = (Vector<String>) data.get(index); // data ���� ������(Ʃ��), data�� ���̺�ȿ� �ִ� �ڷ�
				currNum = in.get(0);
				delete(currNum);				
			} else {
				if (currNum == -1 + "")
					System.out.println("������ ������ �ȵ�");
			}
			
		}
		
		// �ֹ�����Ʈ ���̺� �ҷ����� ��
		if(arg0.getSource() == firstInfoBtn) {
			changeDB("orderList");
		}
		
		// �Ǹ���Ȳ ���̺� �ҷ����� ��
		if(arg0.getSource() == secondInfoBtn) {
			changeDB("sale");
		}
		
		// �����Ȳ ���̺� �ҷ����� ��
		if(arg0.getSource() == thirdInfoBtn) {
			changeDB("stock");
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
	
	// ����
	private void delete(String num) {
		PreparedStatement pstmt;
		try {
			pstmt = (PreparedStatement) con.prepareStatement("delete from orderList where num = ?;");
			pstmt.setString(1, num);
			pstmt.executeUpdate();
			System.out.println("Delete ����");
			
			Vector newResult = selectAll("orderList");			
			model.setDataVector(newResult, title);
						
			// ���� �ձ��ϱ� 
			int sum=0;
			for(int i=0;i<model.getRowCount();i++) {
				sum+=Integer.parseInt(model.getValueAt(i, 3)+"");
			}
			
			sumValueTf.setText(sum+"��");
			
			// Clinet�ʵ� ����
			Client.orderListModel.setDataVector(newResult, title);
			Client.valueTF.setText(sum+"��");
			
		} catch (SQLException e) {
			
			e.printStackTrace();
			System.out.println("Delete ����");
		}
	}
	
	// ��� �ֹ� : �����Ȳ ���̺� ����
	private void orderMeasure() {
		try {
			ResultSet rs = stmt.executeQuery("select * from stock;");
			
			// ice������ ã�� �۾�
			int currValue;
			while(rs.next()) {
				currValue = rs.getInt("amount");
				
				// ������ 500ml�߰�
				if(rs.getString("item").equals("����")) {
					pstmt = (PreparedStatement) con.prepareStatement("update stock set amount = ? where item='����';");
					pstmt.setInt(1,currValue+500);	
					String currMeasure = rs.getString("item");
					System.out.println(currMeasure+" 500ml �߰�");
				}
				// ������ 100g�߰�
				else {
					pstmt = (PreparedStatement) con.prepareStatement("update stock set amount = ? where item = '"+rs.getString("item")+"';");
					pstmt.setInt(1,currValue+100);
					String currMeasure = rs.getString("item");
					System.out.println(currMeasure+" 100g �߰�");
				}
				pstmt.executeUpdate();
				changeDB("stock");
			}			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	// �ֹ��¶� : �Ǹ���Ȳ ���̺� ����
	private void orderAccept() {
		try {
			ResultSet rs = stmt.executeQuery("select * from orderList;");

			pstmt = (PreparedStatement) con.prepareStatement("insert into sale(t_type,item,price) values(?,?,?);");
			
			// �ֹ�����Ʈ ���̺� �ִ� ������ �Ǹ���Ȳ ���̺� �������
			while(rs.next()) {		
				pstmt.setString(1, rs.getString("t_type"));
				pstmt.setString(2, rs.getString("item"));
				pstmt.setString(3, rs.getString("price"));
				pstmt.executeUpdate();
			}
			
			// �����Ȳ ���̺��� ��� �Һ�
			updateStockTable();
			
			// �ֹ��Ϸ� = �ֹ�����Ʈ ����
			pstmt = (PreparedStatement) con.prepareStatement("delete from orderList;");
			pstmt.executeUpdate();
			
			Vector newResult = selectAll("orderList");
			model.setDataVector(newResult, title);
			
			sumTf.setText("�Ѿ�");
			sumValueTf.setText("0��");		
			
			// Client�� Table �ʱ�ȭ
			Client.orderListModel.setDataVector(newResult,title);				
			Client.valueTF.setText("0��");
			
			// Client���� �ֹ����� �޼���
			textFieldChat.setText("�ֹ����� �Ǿ����ϴ�.");
			sendMessage();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void updateStockTable() {
		Statement stmt;
		try {
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select * from orderList;");

			while (rs.next()) {
				switch (rs.getString("item")) {
				// ���� ���
				case "����������": case "�Ƹ޸�ī��": useBean(); break;
					
				// ����, ���� ���	
				case "ī���": case "īǪġ��": useBean(); useMilk(); break;
					
				// ����, ����, �������
				case "ī���ī": useBean(); useMilk(); useSugar(); break;
					
				// ����, �׸�Ƽ �и����� ���
				case "�׸�Ƽ��": useBean(); usePowder(); break;
				}
				
				// ice�� ���� �Һ�
				if(rs.getString("t_type").equals("ice")) useIce();
			}
	
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ���� 200�׶� �Һ�
	private void useIce() {
		consumnAmount("����", 200);	
		System.out.println("���� 200g �Һ�");
	}

	// �������� 10�׶� �Һ�
	private void usePowder() {
		consumnAmount("�׸�Ƽ �и�����", 10);		
		System.out.println("�׸�Ƽ �и����� 10g �Һ�");
	}
	
	// ���� 5�׶� �Һ�
	private void useSugar() {
		consumnAmount("����", 5);
		System.out.println("���� 5g �Һ�");
	}

	// ���� 120ml �Һ�
	private void useMilk() {
		consumnAmount("����", 120);
		System.out.println("���� 120ml �Һ�");
	}
	
	// ���� 7�׶� �Һ�
	private void useBean() {
		consumnAmount("����", 7);
		System.out.println("���� 7g �Һ�");
	}

	// ��Ḧ �Һ��ϴ� �޼ҵ� : �μ�(���, �Һ�)
	private void consumnAmount(String itemIn, int consumnValue) {
		try {
			ResultSet rs = stmt.executeQuery("select * from stock;");
			
			// ��ῡ �ش��ϴ� amount�� ã�� ����
			while(rs.next()) {				
				if(rs.getString("item").equals(itemIn)) break;
			}
			int currValue = rs.getInt("amount");
			
			pstmt = (PreparedStatement) con.prepareStatement("update stock set amount = ? where item='"+itemIn+"';");
			pstmt.setInt(1, currValue-consumnValue);
			pstmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// ��ư Ŭ���� DB�� �ҷ��ͼ� ȭ�鿡 ����ִ� �޼ҵ�
	public void changeDB(String tableName) {
		
		// �ֹ�����Ʈ ���̺��� �ҷ����� ���
		if (tableName.equals("orderList")) {
			infoTf.setText("\t        �ֹ�����Ʈ");
			result = selectAll(tableName);
			model.setDataVector(result, title);
			
			// ���� �� 
			int sum=0;
			for(int i=0;i<model.getRowCount();i++) {
				sum+=Integer.parseInt(model.getValueAt(i, 3)+"");
			}
			sumTf.setText("�Ѿ�");
			sumValueTf.setText(sum+"��");
			
			// �ֹ���ư ���� = �ֹ�����
			orderAcceptBtn.setText("�ֹ�����");
			orderAcceptBtn.setEnabled(true);
			
			// �޴���� ��ư 
			delBtn.setEnabled(true);
			
			// ���̺� ��� ����
			table.getTableHeader().getColumnModel().getColumn(0).setHeaderValue("��ȣ");
			table.getTableHeader().getColumnModel().getColumn(1).setHeaderValue("����");
			table.getTableHeader().getColumnModel().getColumn(2).setHeaderValue("ǰ��");
			table.getTableHeader().getColumnModel().getColumn(3).setHeaderValue("�ݾ�");
			
			orderWhat=0; // ���簡 �ֹ�����Ʈ ���̺��̶�� �ǹ�
		}
		
		// �Ǹ���Ȳ ���̺��� �ҷ����� ���
		if (tableName.equals("sale")) {
			infoTf.setText("\t        �Ǹ���Ȳ");
			result = selectAll(tableName);
			model.setDataVector(result, title);
			
			// ���� �� 
			int sum=0;
			for(int i=0;i<model.getRowCount();i++) {
				sum+=Integer.parseInt(model.getValueAt(i, 3)+"");
			}
			
			sumTf.setText("�Ѿ�");
			sumValueTf.setText(sum+"��");
			
			orderAcceptBtn.setEnabled(false);
			delBtn.setEnabled(false);			
			
			// ���̺� ��� ����
			table.getTableHeader().getColumnModel().getColumn(0).setHeaderValue("��ȣ");
			table.getTableHeader().getColumnModel().getColumn(1).setHeaderValue("����");
			table.getTableHeader().getColumnModel().getColumn(2).setHeaderValue("ǰ��");
			table.getTableHeader().getColumnModel().getColumn(3).setHeaderValue("�ݾ�");

		}
		
		// �����Ȳ ���̺��� �ҷ����� ���
		if (tableName.equals("stock")) {
			infoTf.setText("\t        �����Ȳ");
			result = selectAll(tableName);
			model.setDataVector(result, title);
			
			sumTf.setText("����(500ml), ������(100g)");
			sumValueTf.setText("��� �ֹ� �Ͻðڽ��ϱ�?");	
			
			// �ֹ���ư ���� = ��� �ֹ�
			orderAcceptBtn.setText("����ֹ�");
			orderAcceptBtn.setEnabled(true);
			orderWhat = 1; // ���簡 �����Ȳ ���̺��̶�� �ǹ�

			// �޴���� ��ư 
			delBtn.setEnabled(false);						
			
			// ���̺�  ��� ����
			table.getTableHeader().getColumnModel().getColumn(0).setHeaderValue("��ȣ");
			table.getTableHeader().getColumnModel().getColumn(1).setHeaderValue("ǰ��");
			table.getTableHeader().getColumnModel().getColumn(2).setHeaderValue("����");
			table.getTableHeader().getColumnModel().getColumn(3).setHeaderValue("����");
			
		}
	}
	
	public void sendMessage() throws IOException{

		String outputMessage = textFieldChat.getText(); // �Է� �ؽ�Ʈ�ʵ忡�� �� ���� ���ڿ� ����
		out.write(outputMessage+"\n"); // Ű���忡�� ���� ���ڿ� ����
		out.flush();
		textAreaChat.append("[SENT]: " + outputMessage + "\n");
		textFieldChat.setText("");
	}
	
}