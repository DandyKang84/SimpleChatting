package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ChatClient {
	
	public static final String IP_SERVER = " . . . ";
	public static final int PORT_SERVER = 1126;
	public static final Scanner scanner = new Scanner(System.in);
	
	public static void main(String[] args) {
		
		Socket socket = null;
		
		OutputStream outputStream = null;
		OutputStreamWriter outputStreamWriter = null;
		PrintWriter printWriter = null;
		
		InputStream inputStream = null;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		
		String nickname = null;
			
		try {
			// 1. 클라이언트 소켓 생성
			socket = new Socket();

			// 2. 서버에 접속 요청(connect), 접속될 때까지 대기 후 일정 시간 지나면 예외처리 진행
			socket.connect(new InetSocketAddress(IP_SERVER, PORT_SERVER));
			System.out.println("[채팅방에 입장했습니다.]"); // 접속 요청 허가 시
			
			// 3-1. join
			// 3-1-1. 닉네임을 결정한다.
			while(true) {
				System.out.print("[닉네임을 입력하세요.]: ");
				nickname = scanner.nextLine();
				
				if(nickname.isEmpty() == false) {
					break;
				}
				
				System.out.println("[닉네임은 공백 없이 한 글자 이상 입력하세요.]");
				
			}
			scanner.close();
			
			// 3-1-2. 소켓을 통해 출력 보조 스트림을 가져온다.(서버가 PrintWriter로 쓰고, BufferedReader로 읽으므로 똑같이 만듬)
			outputStream = socket.getOutputStream();
			outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
			printWriter = new PrintWriter(outputStreamWriter, true); // ,true를 주면 '자동 flush'됨
			
			inputStream = socket.getInputStream();
			inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			bufferedReader = new BufferedReader(inputStreamReader);
			
			// 3-1-3. 닉네임을 서버로 보낸다.
			String nicknameConvert = "join:" + nickname + "\r\n"; // join 없앤 공백을 채움
			printWriter.println(nicknameConvert);
			
			/* 4. UI(AWT) 창에서 BufferedReader, PrintWriter 서버에게 요청하고 응답을 받고 무한루프 속에서 진행
		      UI창에서 죽지 않고 계속 떠서 4가지 업무를 진행한다. */ 
			// 내부 스레드 작동을 위한 객체 생성: 서버로부터 온 데이터를 계속 읽고, TextArea에 출력하는 스레드
			ChatClientWindow chatClientWindow = new ChatClientWindow(nickname, socket);
			// UI창을 보여준다.
			chatClientWindow.show();
			
		} catch (IOException e) {
			System.out.println("[서버 측에 문제가 발생했습니다.]"); // 접속 요청 불가 시
		
		}
			
	}

}
