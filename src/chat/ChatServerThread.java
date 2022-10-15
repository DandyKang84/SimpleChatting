package chat;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

public class ChatServerThread extends Thread {

	// 멤버변수
	private String nickname;
	private Socket socket;
	
	// 클라이언트와 통신할 PrintWriter(스트림, 내보내는 흐름)의 모음(공유 자원)
	private Vector<PrintWriter> printWriterList;
	
	// 생성자
	// 스레드 객체 생성 시 소켓과 스트림(흐름) 정보는 무조건 필요하므로 Default 생성자 만들면 안됨
	public ChatServerThread(Socket socket, Vector<PrintWriter> printWriterList) {
		this.nickname = null;
		this.socket = socket;
		this.printWriterList = printWriterList;
	}
	
	// 멤버함수(Getter/Setter, 오버라이딩 사용 X)
	@Override
	public void run() {		
		// 1. 소켓(소켓에는 InputStream, OutputStream이 존재)을 통해 원하는 데이터 형식으로 입/출력
		// 1-1. 소켓을 통해 읽기 기능을 부여(InputStream - InputStreamReader - BufferedReader)
		InputStream inputStream = null;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		
		// 1-2. 소켓을 통해 출력 기능을 부여(OutputStream - OutputStreamWriter - BufferedWriter)
		OutputStream outputStream = null;
		OutputStreamWriter outputStreamWriter = null;
		PrintWriter printWriter = null; // 속도보다는 쓰기 편리하게 BufferedWriter가 아닌 'PrintWriter' 사용
		
		try {
			inputStream = this.socket.getInputStream();
			/* BufferedReader는 속도 개선 스트림이므로 InputStreamReader에서 문자셋 정해줘야 함
			   UTF-8은 1byte 또는 3byte로 읽고, UTF-16은 모두 2byte로 읽음(낭비 발생하므로 UTF-8 사용) */
			inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			bufferedReader = new BufferedReader(inputStreamReader);
			
			outputStream = this.socket.getOutputStream();
			outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
			printWriter = new PrintWriter(outputStreamWriter);
			
			// 2. 클라이언트와 통신을(대화를) 주고 받음(무한 반복)
			while(true) {
				
				/* 2-0. 상대방이 엔터치는 것까지 다 인식(= scanner.nextLine()), 입력 받을 때까지 무한 대기.
					    여기서 소켓을 닫으면 예외처리로 넘어감 */
				String messageReceive = bufferedReader.readLine();
				
				/* 2-1. 클라이언트가 갑자기 연락(클라이언트 소켓)이 끊어지면 readLine()은 'null'을 읽어온다.
				        클라이언트가 글을 쓴 후 Enter 칠 때까지 무한 대기 */
				if (messageReceive == null) {
					System.out.println("클라이언트가 연결을 종료했습니다. " + 
									   "클라이언트 IP: " + this.socket.getInetAddress().getHostAddress() + 
									   ", 클라이언트 포트번호: " + this.socket.getPort());
					
					// 1) 사용하지 않으므로 공유자원 Vector<PrintWriter> list에서 삭제한다.
					synchronized (printWriterList) { // Vector라서 'list.remove(printWriter);'만 해도 되나 더 안전하도록 한번 더 동기화
						printWriterList.remove(printWriter);
					}
					
					// 2) 클라이언트가 연락을 끊었음을 다른 클라이언트에게 알림 전송
					String messageDisconnect = this.nickname + "님이 퇴장했습니다.";
					sendMessage(messageDisconnect);
					
					break;
				}
				
				// 2-2. 클라이언트가 요청하여 정상 작동하는 경우(프로토콜 - 1. join, 2. message, 3. quit)
				// tokens[0]은 join, message, quit 중 하나
				String[] tokens = messageReceive.split(":");
				
				/* 2-2-1. join(클라이언트가 메세지 방에 참가하기) - 패턴화 되어있음
		          tokens[0]: join, tokens[1]: nickname */
				if("join".equals(tokens[0])) {
					
					// 1) 닉네임 정함
					this.nickname = tokens[1];
					
					// 2) 공유 데이터에 PrintWriter(스트림) 추가
					synchronized(printWriterList) {
						printWriterList.add(printWriter); // 클라이언트가 추가되면 스트림 추가							
					}
					
					// 3) 메세지를 정함
					String messageJoin = this.nickname + "님이 입장했습니다.";
					System.out.println(messageJoin); // 서버 화면에 출력
					
					// 4) 클라이언트가 입장했음을 다른 클라이언트에게 입장 알림 전송
					sendMessage(messageJoin);
				
				// 2-2-2. meessage(메세지 주고 받기, 쓰고 싶은 말)	
				} else if ("message".equals(tokens[0])) {
					
					// 1) 다른 클라이언트에 메세지 전송
					String messageSend = this.nickname + ": " + tokens[1];
					sendMessage(messageSend);
				
				// 2-2-3. quit(클라이언트가 직접 나가기) - 패턴화 되어있음
				} else if ("quit".equals(tokens[0])) {
										
					// 1) 사용하지 않으므로 공유자원 Vector<PrintWriter> list에서 삭제한다.
					synchronized (printWriterList) {
						printWriterList.remove(printWriter);
					}
					
					// 2) 클라이언트가 퇴장했음을 다른 클라이언트에게 퇴장 알림 전송
					String messageQuit = this.nickname + "님이 퇴장했습니다.";
					sendMessage(messageQuit);
					
					break; // while문 밖으로 나감 → finally로 가서 socket 닫음
				} 
			
			} 
			
		} catch (IOException e) {
			System.out.println(this.nickname + "의 소켓이 이상이 있어서 종료되었습니다.");
			
		} finally {
			if(this.socket != null && !this.socket.isClosed()) {
				try {
					this.socket.close();
				
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			synchronized(printWriterList) {
				printWriterList.remove(printWriter);
			}
			
		}
	
	} // end of run

	private void sendMessage(String message) {
		synchronized(printWriterList) {
			for(PrintWriter pw : printWriterList) {
				pw.println(message);
				pw.flush();
			}						
		}
	}
		
} // end of class