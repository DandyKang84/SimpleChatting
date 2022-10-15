package chat;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatClientWindow {

	// 멤버변수
    private String name;
    
    // UI에 관련된 클래스
    private Frame frame; // 전체 틀
    private Panel pannel; // 하단 부분
    private Button buttonSend; // 메세지 보내는 버튼
    private TextField textField; // 채팅 입력하는 창
    private TextArea textArea; // 채팅 보이는 창
    private Socket socket;

    // 생성자
    public ChatClientWindow(String name, Socket socket) {
        this.name = name;
        frame = new Frame(name);
        pannel = new Panel();
        buttonSend = new Button("Send");
        textField = new TextField();
        textArea = new TextArea(30, 80);
        this.socket = socket;

        /* 스레드 작동
        1. 내부 클래스, 'UI에서 많이 쓰이는구나'라고 이해)
        2. 서버에서 보내온 데이터를 읽어서 TextArea에 출력해주는 스레드 */
        new ChatClientReceiveThread(socket).start();
    }

    public void show() {
    	
        // Button(버튼 객체를 가리킴)
        buttonSend.setBackground(Color.GRAY); // 배경
        buttonSend.setForeground(Color.BLACK); // 글씨, 폰트 사이즈는 Default 상태
        buttonSend.addActionListener( new ActionListener() { // 이벤트 처리
            @Override
            public void actionPerformed( ActionEvent actionEvent ) {
                sendMessage();
            }
        });
        
        /* 람다식으로 하면,
        buttonSend.addActionListener( (ActionEvent actionEvent) -> {
                sendMessage();
            }
        ); */

        // Textfield(채팅 입력하는 창)
        textField.setColumns(80); // 입력창 길이가 '80글자'라는 뜻
        textField.addKeyListener( new KeyAdapter() { // 이벤트 처리, KeyAdapter는 키보드를 쓸 때마다 이벤트 발생
            public void keyReleased(KeyEvent e) {
                char keyCode = e.getKeyChar();
                if (keyCode == KeyEvent.VK_ENTER) { // 엔터 치면 보내지는 이유, 엔터 치면 sendMessage()로 메세지 보내짐
                    sendMessage();
                }
            }
        });

        // Pannel(하단 부분에 가로로 textField, buttonSend 추가(default가 가로))
        pannel.setBackground(Color.LIGHT_GRAY); // 판넬 배경 색 밝은 회색
        pannel.add(textField); // 판넬 속에 textField 추가
        pannel.add(buttonSend); // 판넬 속에 buttonSend 추가
        frame.add(BorderLayout.SOUTH, pannel); // 판넬을 프레임 남쪽 위치에 추가

        // TextArea(채팅 보이는 창)
        textArea.setEditable(false); // 글씨 못 쓰게 함(editable - false = 편집 불가능)
        frame.add(BorderLayout.CENTER, textArea); // textArea를 프레임 중앙에 추가

        // Frame(중요)
        frame.addWindowListener(new WindowAdapter() { // 이벤트 처리
            public void windowClosing(WindowEvent e) { // '윈도우가 닫히면~'이라는 이벤트(닫으면 발생)
                PrintWriter pw;
                try {
                    pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                    String request = "quit\r\n";
                    pw.println(request);
                    System.exit(0); // 클라이언트 전체를 죽임
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        frame.setVisible(true); // 프레임을 보여줌
        frame.pack(); // 프레임 사이즈에 맞게 구성 요소들
    }

    // 서버로 전송하는 메소드
    private void sendMessage() {
        PrintWriter pw;
        try {
        	
        	/* 아래 내용을 한번에 한 것
        	OutputStream outputStream = socket.getOutputStream();
    		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
    		PrintWriter printWriter = new PrintWriter(outputStreamWriter, true); // ,true를 주면 '자동 flush'됨
    		*/        	
            pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            String message = textField.getText() + " "; // 공백도 보내지도록 '+ " "' 해준다.
            String request = "message:" + message + "\r\n"; // 프로토콜 'message'
            pw.println(request);

            textField.setText(""); // 보내고 Send하면 없어짐
            textField.requestFocus(); // 보내고 커서 제일 앞에서 깜빡 거림
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 인스턴스 내부 클래스(스레드(에서 작동 중), 4번 방식)
    private class ChatClientReceiveThread extends Thread{
        Socket socket = null;

        ChatClientReceiveThread(Socket socket){
            this.socket = socket;
        }

        // 값을 주면 BufferedReader로 읽어서 TextArea에 뿌린다.
        public void run() {
            try {
            	
            	/* 아래 내용을 한번에 한 것
            	InputStream inputStream = socket.getInputStream();
    			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
    			bufferedReader bufferedReader = new BufferedReader(inputStreamReader);
    			*/
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                
                while(true) {
                    String msg = br.readLine();
                    textArea.append(msg);
                    textArea.append("\n");
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
