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
 * (중요 전역변수 및 메소드 순서)
 * 1. DB와 연결하기 위한 변수 & DB GUI
 * 2. DB table을 교체하는데 쓰일 버튼
 * 3. 총액 또는 재료 주문수량에 대한 정보 표시
 * 4. 주문 수락 & 재료 주문 버튼 / 주문 취소버튼
 * 5. orderWhat : orderAcceptBtn의 용도 정하는 변수
 * 6. 서버 GUI 구현  (서버 실행은 Thread클래스에 정의)
 * 7. 통신에 사용될 쓰레드 클래스 : 본 클래스의 전역 변수에 접근하기 위해 내부클래스로 구현
 * 8. 전체적인 실행
 * 
 * */

public class AdministratorDB extends JFrame implements ActionListener {
	
	// 1. DB와 연결하기 위한 변수 & DB GUI
	private Connection con = null;
	private JTable table;
	private Vector title;	
	private Vector data;
	private Statement stmt;
	public Vector result;
	public DefaultTableModel model;	
	private PreparedStatement pstmt;
	
	// 2. DB table을 교체하는데 쓰일 버튼
	private JPanel infoPanel; 		// DB table교체 버튼을 감쌀 패널
	public JButton firstInfoBtn;	// 주문리스트 테이블
	public JButton secondInfoBtn;	// 판매현황 테이블
	public JButton thirdInfoBtn;	// 재고현황 테이블	
	public JTextField infoTf;		// 어떤 table인지 명시해주는 변수
	
	// 3. 총액 또는 재료 주문수량에 대한 정보 표시
	public JTextField sumTf;		// "총액", 재료 주문 수량에 대한 정보 
	public JTextField sumValueTf;	// 총액 값
	
	// 4. 주문 수락 & 재료 주문 버튼 / 주문 취소버튼
	public JButton orderAcceptBtn;	
	public JButton delBtn;

	// 5. orderWhat : orderAcceptBtn의 용도 정하는 변수
	// 0이면 Client의 주문 승락 = 판매현황 테이블 업데이트
	// 1이면 재료 주문 = 재고현황 테이블 업데이트
	private int orderWhat = 0;
	
	// 6. 서버 GUI 구현  (서버 실행은 Thread클래스에 정의)
	JPanel serverPanel;
	
	protected JTextField textFieldChat;
	protected JTextArea textAreaChat;
	InetAddress address = null;

	BufferedReader in = null;
	BufferedWriter out = null;
	ServerSocket listener = null;
	Socket socket = null;
	
	// 7. 통신에 사용될 쓰레드 클래스 : 본 클래스의 전역 변수에 접근하기 위해 내부클래스로 구현
	class MyThread extends Thread{
		public void run() {
			String inputMessage;
			
			try {
				listener = new ServerSocket(9999);
				socket = listener.accept(); // 클라이언트로부터 연결 요청 대기
				System.out.println("연결됨");
				in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // 클라이언트로부터의 입력 스트림
				out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); // 클라이언트로의 출력 스트림
				
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
					System.out.println("클라이언트와 연결 종료");
					textAreaChat.append("클라이언와 연결 종료");
					listener.close();
					System.out.println("서버 소켓 종료");

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	}
	
	// 8. 전체적인 실행
	public AdministratorDB() {
						
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(450,630);
		setTitle("JKCoffee's Server");
		setLayout(new FlowLayout());
		
		// DB 연동
		con  = makeConnection();		
		data = new Vector<>();		
		title = new Vector<>(); 

		// 주문리스트, 판매현황, 재고현황 버튼
		infoPanel = new JPanel();

		firstInfoBtn = new JButton("주문리스트");
		secondInfoBtn = new JButton("판매현황");
		thirdInfoBtn = new JButton("재고현황");		
		
		firstInfoBtn.addActionListener(this);
		secondInfoBtn.addActionListener(this);
		thirdInfoBtn.addActionListener(this);
		
		infoPanel.add(firstInfoBtn);
		infoPanel.add(secondInfoBtn);
		infoPanel.add(thirdInfoBtn);
		add(infoPanel);
		
		result = selectAll("orderList");	// orderList 테이블의 결과집합 가져오는 메소드
				
		// 현재 무슨 버튼인지 표시 : Default값은 주문리스트
		infoTf = new JTextField("\t        주문리스트",30);
		add(infoTf);
			
		// DB 테이블 정의
		title.add("번호");
		title.add("종류");
		title.add("품목");
		title.add("금액");
		model = new DefaultTableModel(result, title);
		table = new JTable(model);
		JScrollPane sp = new JScrollPane(table);
		sp.setPreferredSize(new Dimension(400,300)); 	// JTable크기 조절		
		add(sp);
		
		// 액수에 대한 정보인 TF
		sumTf = new JTextField("총액",20);
		sumValueTf = new JTextField("원",15);
		add(sumTf);
		add(sumValueTf);
		
		// 주문 버튼
		orderAcceptBtn = new JButton("주문수락");
		orderAcceptBtn.addActionListener(this);
		
		// 메뉴 취소 버튼
		delBtn = new JButton("메뉴취소");
		delBtn.addActionListener(this);	
		
		// 가격 합구하기  : 화면 살행될 때, 선택된 메뉴가 있다면 총합을 구하는 메소드
		int sum=0;
		for(int i=0;i<model.getRowCount();i++) {
			sum+=Integer.parseInt(model.getValueAt(i, 3)+"");
		}
		sumValueTf.setText(sum+"원");
		
		///// serverPanel 디자인 구현
		serverPanel = new JPanel();
	
		textFieldChat = new JTextField(42);
		textFieldChat.addActionListener(this);

		textAreaChat = new JTextArea();
		textAreaChat.setEditable(false);
		
		JScrollPane scroll = new JScrollPane(textAreaChat);
		scroll.setPreferredSize(new Dimension(400,100)); 	// TextArea크기 조절
		
		serverPanel.setLayout(new BorderLayout());
		serverPanel.add(scroll, BorderLayout.CENTER);
		serverPanel.add(textFieldChat, BorderLayout.SOUTH);
		serverPanel.setBorder(new TitledBorder(new LineBorder(Color.LIGHT_GRAY,2),"Chat to Client"));
		add(serverPanel, BorderLayout.SOUTH);
		
		add(orderAcceptBtn);
		add(delBtn);
	
		setVisible(true);
		
		// 쓰레드를 통한 채팅 기능 구현
		MyThread thread = new MyThread();
		thread.start();
	}
	
	// 자료를 반환 하는 메소드
	public Vector selectAll(String dbTable) {
		try {
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select * from " + dbTable +";");
			
			data.clear();
			
			// 재고현황 테이블에 접근하는 경우
			if(dbTable.equals("stock")) {
				// JFrame에 있는 테이블에 한 row씩 계속 집어넣음
				while (rs.next()) {
					Vector<String> in = new Vector<String>();
					String num = rs.getString("num");
					in.add(rs.getString("num"));
					in.add(rs.getString("item"));
					in.add(rs.getString("amount"));
					in.add(rs.getString("measure"));
					data.add(in); // data는 전역변수이므로 계속 누적됨.
				}
			}
			
			// 주문리스트와 판매현황 테이블에 접근하는 경우
			else {
				// JFrame에 있는 테이블에 한 row씩 계속 집어넣음
				while (rs.next()) {
					Vector<String> in = new Vector<String>();
					String num = rs.getString("num");
					in.add(rs.getString("num"));
					in.add(rs.getString("t_type"));
					in.add(rs.getString("item"));
					in.add(rs.getString("price"));
					data.add(in); // data는 전역변수이므로 계속 누적됨.
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return data;
	}

	private Connection makeConnection() {
		
		// 1. db연결
		String url = "jdbc:mysql://localhost/JKCoffee";
        String id = "root";
        String password = "onlyroot";
        Connection con = null;
		
		// 2. 드라이버 업로드
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("AdministratorDB 드라이버 적재 성공");
			
			// 3. connection
			con = (Connection) DriverManager.getConnection(url, id, password);
			System.out.println("AdministratorDB 데이터베이스 연결 성공");
		} catch (ClassNotFoundException e) { // 드라이버 적재에 대한 예외
			e.printStackTrace();
			System.out.println("AdministratorDB 드라이버 적재 실패");
		} catch (SQLException e) { // connection에 대한 예외
			e.printStackTrace();
			System.out.println("AdministratorDB 데이터베이스 연결 실패");
		}
		return con;
	}
	
	public static void main(String[] args) {
		 // new AdministratorDB();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		// 주문 수락버튼 = 판매현황 테이블에 업데이트
		if(arg0.getSource() == orderAcceptBtn) {	
				
			// orderWhat이 0인 경우 : 주믄승락버튼
			if(orderWhat==0) orderAccept();
			
			// orderWhat이 0인 경우 : 재료주문버튼
			if(orderWhat==1) orderMeasure();

		}
		
		//삭제 버튼
		if(arg0.getSource()==delBtn) {
			
			int index = table.getSelectedRow();
			String currNum = -1 + "";

			if (index != -1) {
				Vector<String> in = (Vector<String>) data.get(index); // data 한줄 가져옴(튜플), data는 테이블안에 있는 자료
				currNum = in.get(0);
				delete(currNum);				
			} else {
				if (currNum == -1 + "")
					System.out.println("데이터 선택이 안됨");
			}
			
		}
		
		// 주문리스트 테이블 불러오는 것
		if(arg0.getSource() == firstInfoBtn) {
			changeDB("orderList");
		}
		
		// 판매현황 테이블 불러오는 것
		if(arg0.getSource() == secondInfoBtn) {
			changeDB("sale");
		}
		
		// 재고현황 테이블 불러오는 것
		if(arg0.getSource() == thirdInfoBtn) {
			changeDB("stock");
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
	
	// 삭제
	private void delete(String num) {
		PreparedStatement pstmt;
		try {
			pstmt = (PreparedStatement) con.prepareStatement("delete from orderList where num = ?;");
			pstmt.setString(1, num);
			pstmt.executeUpdate();
			System.out.println("Delete 성공");
			
			Vector newResult = selectAll("orderList");			
			model.setDataVector(newResult, title);
						
			// 가격 합구하기 
			int sum=0;
			for(int i=0;i<model.getRowCount();i++) {
				sum+=Integer.parseInt(model.getValueAt(i, 3)+"");
			}
			
			sumValueTf.setText(sum+"원");
			
			// Clinet쪽도 갱신
			Client.orderListModel.setDataVector(newResult, title);
			Client.valueTF.setText(sum+"원");
			
		} catch (SQLException e) {
			
			e.printStackTrace();
			System.out.println("Delete 실패");
		}
	}
	
	// 재료 주문 : 재고현황 테이블 갱신
	private void orderMeasure() {
		try {
			ResultSet rs = stmt.executeQuery("select * from stock;");
			
			// ice정보를 찾는 작업
			int currValue;
			while(rs.next()) {
				currValue = rs.getInt("amount");
				
				// 우유면 500ml추가
				if(rs.getString("item").equals("우유")) {
					pstmt = (PreparedStatement) con.prepareStatement("update stock set amount = ? where item='우유';");
					pstmt.setInt(1,currValue+500);	
					String currMeasure = rs.getString("item");
					System.out.println(currMeasure+" 500ml 추가");
				}
				// 나머지 100g추가
				else {
					pstmt = (PreparedStatement) con.prepareStatement("update stock set amount = ? where item = '"+rs.getString("item")+"';");
					pstmt.setInt(1,currValue+100);
					String currMeasure = rs.getString("item");
					System.out.println(currMeasure+" 100g 추가");
				}
				pstmt.executeUpdate();
				changeDB("stock");
			}			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	// 주문승락 : 판매현황 테이블 갱신
	private void orderAccept() {
		try {
			ResultSet rs = stmt.executeQuery("select * from orderList;");

			pstmt = (PreparedStatement) con.prepareStatement("insert into sale(t_type,item,price) values(?,?,?);");
			
			// 주문리스트 테이블에 있는 정보를 판매현황 테이블에 집어넣음
			while(rs.next()) {		
				pstmt.setString(1, rs.getString("t_type"));
				pstmt.setString(2, rs.getString("item"));
				pstmt.setString(3, rs.getString("price"));
				pstmt.executeUpdate();
			}
			
			// 재고현황 테이블에서 재료 소비
			updateStockTable();
			
			// 주문완료 = 주문리스트 삭제
			pstmt = (PreparedStatement) con.prepareStatement("delete from orderList;");
			pstmt.executeUpdate();
			
			Vector newResult = selectAll("orderList");
			model.setDataVector(newResult, title);
			
			sumTf.setText("총액");
			sumValueTf.setText("0원");		
			
			// Client쪽 Table 초기화
			Client.orderListModel.setDataVector(newResult,title);				
			Client.valueTF.setText("0원");
			
			// Client에게 주문수락 메세지
			textFieldChat.setText("주문수락 되었습니다.");
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
				// 원두 사용
				case "에소프레소": case "아메리카노": useBean(); break;
					
				// 원두, 우유 사용	
				case "카페라떼": case "카푸치노": useBean(); useMilk(); break;
					
				// 원두, 우유, 설탕사용
				case "카페모카": useBean(); useMilk(); useSugar(); break;
					
				// 우유, 그린티 분말가루 사용
				case "그린티라떼": useBean(); usePowder(); break;
				}
				
				// ice면 얼음 소비
				if(rs.getString("t_type").equals("ice")) useIce();
			}
	
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// 얼음 200그람 소비
	private void useIce() {
		consumnAmount("얼음", 200);	
		System.out.println("얼음 200g 소비");
	}

	// 녹차가루 10그람 소비
	private void usePowder() {
		consumnAmount("그린티 분말가루", 10);		
		System.out.println("그린티 분말가루 10g 소비");
	}
	
	// 설탕 5그람 소비
	private void useSugar() {
		consumnAmount("설탕", 5);
		System.out.println("설탕 5g 소비");
	}

	// 우유 120ml 소비
	private void useMilk() {
		consumnAmount("우유", 120);
		System.out.println("우유 120ml 소비");
	}
	
	// 원두 7그람 소비
	private void useBean() {
		consumnAmount("원두", 7);
		System.out.println("원두 7g 소비");
	}

	// 재료를 소비하는 메소드 : 인수(재료, 소비량)
	private void consumnAmount(String itemIn, int consumnValue) {
		try {
			ResultSet rs = stmt.executeQuery("select * from stock;");
			
			// 재료에 해당하는 amount값 찾는 과정
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

	// 버튼 클릭시 DB를 불러와서 화면에 띄워주는 메소드
	public void changeDB(String tableName) {
		
		// 주문리스트 테이블을 불러오는 경우
		if (tableName.equals("orderList")) {
			infoTf.setText("\t        주문리스트");
			result = selectAll(tableName);
			model.setDataVector(result, title);
			
			// 가격 합 
			int sum=0;
			for(int i=0;i<model.getRowCount();i++) {
				sum+=Integer.parseInt(model.getValueAt(i, 3)+"");
			}
			sumTf.setText("총액");
			sumValueTf.setText(sum+"원");
			
			// 주문버튼 수정 = 주문승인
			orderAcceptBtn.setText("주문수락");
			orderAcceptBtn.setEnabled(true);
			
			// 메뉴취소 버튼 
			delBtn.setEnabled(true);
			
			// 테이블 헤더 수정
			table.getTableHeader().getColumnModel().getColumn(0).setHeaderValue("번호");
			table.getTableHeader().getColumnModel().getColumn(1).setHeaderValue("종류");
			table.getTableHeader().getColumnModel().getColumn(2).setHeaderValue("품목");
			table.getTableHeader().getColumnModel().getColumn(3).setHeaderValue("금액");
			
			orderWhat=0; // 현재가 주문리스트 테이블이라는 의미
		}
		
		// 판매현황 테이블을 불러오는 경우
		if (tableName.equals("sale")) {
			infoTf.setText("\t        판매현황");
			result = selectAll(tableName);
			model.setDataVector(result, title);
			
			// 가격 합 
			int sum=0;
			for(int i=0;i<model.getRowCount();i++) {
				sum+=Integer.parseInt(model.getValueAt(i, 3)+"");
			}
			
			sumTf.setText("총액");
			sumValueTf.setText(sum+"원");
			
			orderAcceptBtn.setEnabled(false);
			delBtn.setEnabled(false);			
			
			// 테이블 헤더 수정
			table.getTableHeader().getColumnModel().getColumn(0).setHeaderValue("번호");
			table.getTableHeader().getColumnModel().getColumn(1).setHeaderValue("종류");
			table.getTableHeader().getColumnModel().getColumn(2).setHeaderValue("품목");
			table.getTableHeader().getColumnModel().getColumn(3).setHeaderValue("금액");

		}
		
		// 재고현황 테이블을 불러오는 경우
		if (tableName.equals("stock")) {
			infoTf.setText("\t        재고현황");
			result = selectAll(tableName);
			model.setDataVector(result, title);
			
			sumTf.setText("우유(500ml), 나머지(100g)");
			sumValueTf.setText("재료 주문 하시겠습니까?");	
			
			// 주문버튼 수정 = 재료 주문
			orderAcceptBtn.setText("재료주문");
			orderAcceptBtn.setEnabled(true);
			orderWhat = 1; // 현재가 재고현황 테이블이라는 의미

			// 메뉴취소 버튼 
			delBtn.setEnabled(false);						
			
			// 테이블  헤더 수정
			table.getTableHeader().getColumnModel().getColumn(0).setHeaderValue("번호");
			table.getTableHeader().getColumnModel().getColumn(1).setHeaderValue("품목");
			table.getTableHeader().getColumnModel().getColumn(2).setHeaderValue("단위");
			table.getTableHeader().getColumnModel().getColumn(3).setHeaderValue("수량");
			
		}
	}
	
	public void sendMessage() throws IOException{

		String outputMessage = textFieldChat.getText(); // 입력 텍스트필드에서 한 행의 문자열 읽음
		out.write(outputMessage+"\n"); // 키보드에서 읽은 문자열 전송
		out.flush();
		textAreaChat.append("[SENT]: " + outputMessage + "\n");
		textFieldChat.setText("");
	}
	
}