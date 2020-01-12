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
 * 201352048 김종권
 * 
 * (중요 전역변수 및 메소드 순서)
 * 1. dbAdm : 관리자 클래스와 상호작용하기 위한 선언 
 * 2. con : DB와 연결하기 위한 변수
 * 3. northPanel : 커피 메뉴 선택 GUI
 * 4. serverPanel : 서버 GUI 구현  (서버 실행은 Thread클래스에 정의)	
 * 5. MyThread : 통신에 사용될 쓰레드 내부 클래스
 * 6. Client 생성자 : 전체적인 실행(여기서 AdministratorDB역시 실행)
 * 
 * */

public class Client extends JFrame implements ItemListener, ActionListener {
	
	// 1. 관리자 클래스와 상호작용 
	// DB & GUI (Client) <-> 관리자 GUI (Server) - DB관리자 클래스에 접근하기 위해 선언
	private AdministratorDB dbAdm;
	
	// 2. DB와 연결하기 위한 변수
	// DB관련 변수
	private Connection con = null;
	private Statement stmt;
	private Vector data;
	
	// 3. 커피 메뉴 선택 GUI
	// 커피메뉴와 현지 주문현황을 감싸는 Panel
	JPanel northPanel;
	
	// 커피 메뉴와 ice, hot을 선택하는데 필요한 변수
	JPanel menuPanel;  // 커피 메뉴를 감쌀 Panel		
	public String [] textMenu = {"에소프레소", "아메리카노", "카페라떼", "카푸치노","카페모카","그린티라떼"}; // 메뉴 설정
	public JRadioButton [] menu = new JRadioButton[textMenu.length]; // 라디오버튼으로 구성한 메뉴
	public ButtonGroup bg; 		// 커피 메뉴의 그룹
	public JPanel typePanel; 	// ice인지 hot인지 선택하는 라디오 버튼을 감쌀 Panel
	public ButtonGroup bg2; 	// ice, hot의 그룹		
	public JRadioButton ice;
	public JRadioButton hot;

	// Advanced GUI - JTable(주문현황) 구성에 쓰이는 변수	
	public JTable stateTable; 	// 메뉴 추가 현황을 표시하는 테이블
	public Vector stateTitle;	// 테이블에 표시할 이름
	public static DefaultTableModel orderListModel; // 주문현황 테이블 : 관리자 클래스에서 접근하기 위해 static 선언	
	public Vector orderResult;	// DB의 orderList 테이블 결과집합을 가져올 변수 : 테이블에 표시
	
	// 현재 주문한 총액과 추가 버튼이 들어갈 Panel과 변수
	public JPanel centerPanel;
	public JTextField sumTF;		  	// "총액" 
	public static JTextField valueTF; 	// 총액 값 - 관리자 클래스에서 접근하기 위해 static 선언
	public JButton addBtn;				// 추가 버튼
	
	// 4. 서버 GUI 구현  (서버 실행은 Thread클래스에 정의)	
	// 서버 관련된 변수가 들어갈 Panel
	JPanel serverPanel;	
	JTextField textFieldChat;			// 키보드 입력이 받아질 변수
	JTextArea textAreaChat;				// 채팅 내역
	InetAddress address = null;
	BufferedReader in = null;
	BufferedWriter out = null;
	Socket socket = null;
	
	// 5.통신에 사용될 쓰레드 클래스 : 본 클래스의  전역 변수에 접근하기 위해 내부클래스로 구현
	class MyThread extends Thread{
		public void run() {
			String inputMessage;
			try {
				socket = new Socket("localhost", 9999);
				System.out.println("연결됨");
				in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // 서버로부터의 입력 스트림
				out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); // 서버로의 출력 스트림
				
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
	
	// 6. 전체적인 실행(여기서 AdministratorDB역시 실행)
	public Client(){
		
		// db관리자와 연동을 위해서 객체 생성
		dbAdm = new AdministratorDB();
		
		// DB연동
		con = makeConnection();		
		
		// 데이터를 받을 변수
		data = new Vector<>();	
		
		setTitle("Client");
		setSize(530,600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		northPanel = new JPanel();
		centerPanel = new JPanel();
				
		// 메뉴 패널에 넣기
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
		
		// 메뉴 색깔 지정
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
		typePanel.setBorder(new TitledBorder(new LineBorder(Color.darkGray,1),"type")); // 패널 테두리 그리는 메소드
		
		menuPanel.add(typePanel, BorderLayout.SOUTH);
		menuPanel.setBorder(new TitledBorder(new LineBorder(Color.gray,3),"메뉴선택"));
		northPanel.add(menuPanel);
		
		// stateTable
		stateTitle = new Vector<>();
		stateTitle.add("번호");		
		stateTitle.add("종류");
		stateTitle.add("품목");
		stateTitle.add("가격");
		
		orderResult = selectAll("orderList");
		orderListModel = new DefaultTableModel(orderResult, stateTitle);
		stateTable = new JTable(orderListModel);
		
		JScrollPane scroll = new JScrollPane(stateTable);
		scroll.setPreferredSize(new Dimension(300,310)); 	// table크기 조절
		northPanel.add(scroll);
		add(northPanel, BorderLayout.NORTH);
		
		// centerPanel : TextField 두 개, Button 한 개
		// TextField 두 개
		sumTF = new JTextField("총액",25);
		valueTF = new JTextField("0 원",15);		
		centerPanel.add(sumTF);
		centerPanel.add(valueTF);			
		// 추가 버튼
		addBtn = new JButton("추가");		
		addBtn.addActionListener(this);				
		centerPanel.add(addBtn);		
		add(centerPanel, BorderLayout.CENTER);
		
		// 가격 합구하기  : 화면 실행될 때, 선택된 메뉴가 있다면 총합을 구하는 작업
		int sum=0;
		for(int i=0;i<orderListModel.getRowCount();i++) {
			sum+=Integer.parseInt(orderListModel.getValueAt(i, 3)+"");
		}
		valueTF.setText(sum+"원");

		// serverPanel구현  - server GUI
		serverPanel = new JPanel();
	
		textFieldChat = new JTextField(45);
		textFieldChat.addActionListener(this);

		textAreaChat = new JTextArea();
		textAreaChat.setEditable(false);
		
		JScrollPane sp = new JScrollPane(textAreaChat);
		sp.setPreferredSize(new Dimension(400,100)); 	// TextArea크기 조절
		
		serverPanel.setLayout(new BorderLayout());
		serverPanel.add(sp, BorderLayout.CENTER);
		serverPanel.add(textFieldChat, BorderLayout.SOUTH);
		serverPanel.setBorder(new TitledBorder(new LineBorder(Color.LIGHT_GRAY,2),"Chat to JKCoffee"));
		add(serverPanel, BorderLayout.SOUTH);
		
		pack();
		setVisible(true);
		
		// 쓰레드를 통한 채팅 기능 구현
		MyThread thread = new MyThread();
		thread.start();
	}
	
	private Connection makeConnection() {
		
		// db연결
		String url = "jdbc:mysql://localhost/JKCoffee";
        String id = "root";
        String password = "onlyroot";
        Connection con = null;
		
		// 드라이버 업로드
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Client 드라이버 적재 성공");
			
			// connection
			con = (Connection) DriverManager.getConnection(url, id, password);
			System.out.println("Client 데이터베이스 연결 성공");
		} catch (ClassNotFoundException e) { // 드라이버 적재에 대한 예외
			e.printStackTrace();
			System.out.println("Client 드라이버 적재 실패");
		} catch (SQLException e) { // connection에 대한 예외
			e.printStackTrace();
			System.out.println("Client 데이터베이스 연결 실패");
		}
		return con;
	}
	
	private Vector selectAll(String dbTable) {
		try {
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select * from " + dbTable +";");
			
			data.clear(); // 비워주지 않으면 눈에 보이지 않는값이 계속 남아있음
			
			// JFrame에 있는 테이블에 한 row씩 계속 집어넣음
			while(rs.next()) {
				Vector<String> in = new Vector<String>();
				in.add(rs.getString("num"));
				in.add(rs.getString("t_type"));
				in.add(rs.getString("item"));
				in.add(rs.getString("price"));			
				data.add(in); // data는 전역변수이므로 계속 누적됨
				
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
		
		// 추가 버튼
		if(arg0.getSource()==addBtn) {
			
			// 라디오 버튼에 체크된 것들을 주문리스트 테이블에 삽입
			String currType = getCheckType();
			String currMenu = getCheckMenu();
			String currPrice = getPrice(currMenu); // getPrice : 현재 메뉴의 가격을 알아내는 메소드 

			insert(currType, currMenu, currPrice);
			
			// AdministratorDB에 orderList테이블 갱신
			dbAdm.changeDB("orderList");
		}
		
		// 채팅기능에 사용될 텍스트 이벤트처리
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
	
	// 인수로 들어온 메뉴의 가격 리턴
	private String getPrice(String currMenu) {
		switch(currMenu) {
		case "에소프레소": return "2000";
		case "아메리카노": return "3500";
		case "카페라떼": return "4000";
		case "카푸치노": return "5000";
		case "카페모카": return "5500";
		case "그린티라떼": return "6000";				
		}
		System.out.println("getPrice--메뉴 선택이 안됨");
		return null;
	}

	// 커피 메뉴 라디오버튼에서 현재 체크된 것 리턴
	private String getCheckMenu() {
		for(int i=0;i<menu.length;i++) {
			if(menu[i].isSelected()) return menu[i].getText();
		}
		
		System.out.println("메뉴 선택 안됨");
		return null;
	}

	// type 라디오버튼에서 현재 체크된 것 리턴
	private String getCheckType() {
		if(ice.isSelected()) return ice.getText();
		if(hot.isSelected()) return hot.getText();
		System.out.println("type 선택 안됨");
		return null;
	}

	// orderList테이블에 삽입하는 메소드
	private void insert(String type, String item, String price) {
		PreparedStatement pstmt;
		try {
			pstmt = (PreparedStatement) con.prepareStatement("insert into orderList(t_type,item,price) values(?,?,?);");
			
			pstmt.setString(1, type);
			pstmt.setString(2, item);
			pstmt.setString(3, price);
			
			pstmt.executeUpdate();
			System.out.println("주문리스트 테이블에 insert 성공");
			
			Vector newResult = selectAll("orderList");	
			orderListModel.setDataVector(newResult, stateTitle);
			
			// 가격 합구하기 
			int sum=0;
			for(int i=0;i<orderListModel.getRowCount();i++) {
				sum+=Integer.parseInt(orderListModel.getValueAt(i, 3)+"");
			}
			
			valueTF.setText(sum+"원");
					
		} catch (SQLException e) {

			e.printStackTrace();
			System.out.println("insert 실패");
		}
		
	}
	
	// chat에 사용될 메소드
	public void sendMessage() throws IOException {
		
		String outputMessage = textFieldChat.getText();
		out.write(outputMessage+"\n"); // 키보드에서 읽은 문자열 전송
		out.flush();
		textAreaChat.append("[SENT]: " + outputMessage + "\n");
		textFieldChat.setText("");
	}
}

