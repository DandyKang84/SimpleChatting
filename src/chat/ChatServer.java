package chat;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Vector;

public class ChatServer {
	
	// 0-2. 서버 포트 번호 설정(1024~65535)
	public static final int PORT = 1126;
	
	public static void main(String[] args) {
		
		String ip = null;
		
		// 0-1. 서버 IP(통신용) 확인(InetAddress)
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		ServerSocket serverSocket = null;
		
		/* 여러 클라이언트와의 여러 스트림(흐름)을 리스트화하여 저장
		   이 스트림(흐름)은 클라이언트가 공동으로 사용하는 공유 자원(컬렉션 프레임 워크) */
		/* PrintWriter: Writer를 상속하여 Writer 대비 다양한 출력 방식 제공
		   (print/println/printf 중, 특히 println()(개행 포함)으로 출력한 내용을 readLine()(개행 앞까지 읽음)로 읽으면 편리)
		   클라이언트에 2byte 단위로 출력(문자 기반 출력 스트림과 연결됨)  */
		Vector<PrintWriter> printWriterList = new Vector<>();
		
		try {
			// 1. 서버 소켓 생성
			serverSocket = new ServerSocket();
			
			// 2. 서버 소켓 바인딩
			serverSocket.bind(new InetSocketAddress(ip, PORT));
			System.out.println("[연결을 기다립니다.] " + Thread.currentThread().getId() + "의 IP: " + ip + ", " + 
			                                        Thread.currentThread().getId() + "의 포트번호: " + PORT);
			/* Thread.currentThread().getName(): 'main' 리턴
			   Thread.currentThread().getId(): '1' 리턴 */
			
			// 3. 클라이언트로부터 연결 요청 받기(무한 반복)
			while(true) {
				// 3-1. 클라이언트 연결 요청 대기 → 연결 시 통신용 소켓 생성(대기 해제)
				Socket socket = serverSocket.accept(); 	
				
				// 3-2. (동시에 여러 사람과 대화하기 위해) 클라이언트와의 통신을 스레드로 생성(소켓, 스트림(흐름) 활용)
				ChatServerThread chatServerThread = new ChatServerThread(socket, printWriterList);
				chatServerThread.start();
				
				// 3-3. 클라이언트의 소켓 주소(IP 주소와 포트 번호) 확인
				SocketAddress socketAddress = socket.getRemoteSocketAddress();
				InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
				System.out.println("[연결을 수락합니다.] " + "클라이언트의 IP: " + inetSocketAddress.getHostString() + 
						                                ", 클라이언트의 포트번호: " + inetSocketAddress.getPort());
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		} finally {
			// 4. (작동 중이면) 서버 소켓 닫기
			try {
				if(serverSocket != null && !serverSocket.isClosed()) {
					serverSocket.close();
				} 
			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			
			}
			
		}

	}

}