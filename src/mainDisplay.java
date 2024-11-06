
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;



// ShootingGame 클래스
public class mainDisplay extends JPanel implements ActionListener, KeyListener {
    private Timer timer; // 게임 업데이트를 위한 타이머
    private int X, Y; // 플레이어 위치 좌표
    private final int SPEED = 5; // 플레이어의 이동 속도
    private boolean[] keys; // 키 입력 상태를 추적하는 불리언 배열
    private int size =10;
    private ArrayList<Food> Foods = new ArrayList<>();
    private int MaxMapSize = 1600;
    private Random random = new Random();


    public mainDisplay() {
        keys = new boolean[256]; // 키 입력 상태를 저장할 충분한 크기의 배열을 초기화

        // 초기 플레이어 위치 설정
        X = random.nextInt(MaxMapSize);
        Y = random.nextInt(MaxMapSize);

        for(int i=0;i<100;i++){ // 임시 먹이 생성
            Foods.add(new Food(MaxMapSize));
        }

        timer = new Timer(5, this); // 타이머를 5ms 간격으로 설정, 두번째 인자는 ActionListener를 구현한 현재 객체
        timer.start(); // 타이머 시작

        addKeyListener(this); // 키 리스너 추가
        setFocusable(true); // 키 입력을 받기 위해 포커스 가능하도록 설정
        setPreferredSize(new Dimension(400, 400)); // 패널의 선호 사이즈 설정
    }

    // 컴포넌트를 그릴 때 호출되는 메소드
    public void paintComponent(Graphics g) {
        int x,y;
        super.paintComponent(g); // 상위 클래스의 paintComponent 호출
        // 플레이어 이미지를 그립니다.
        g.setColor(Color.BLUE);
        g.fillOval(200, 200, size,size);
        g.setColor(Color.GREEN);
        for(Food F : Foods) {
            x = F.getX();
            y = F.getY();
            //System.out.println(x+","+y);
            if(x > X-200&&x<X+200 && y > Y-200&&y<Y+200)
                g.fillOval(200-(X-x),200-(Y-y),6,6);

        }
    }

    // 플레이어의 위치는 200,200고정, 현제 위치좌표가 800,800이라면 600,600~1000,1000 먹이만 표기.
    // 750,610에 있는 먹이의 위치는 150,10 200 - (P - F)

    // ActionListener 인터페이스를 구현한 메소드, 타이머 이벤트가 발생할 때마다 호출
    public void actionPerformed(ActionEvent e) {
        updatePlayerPosition(); // 플레이어 위치를 업데이트
        //System.out.println("X: "+X+"  Y: "+Y);
        repaint(); // 패널을 다시 그림 (paintComponent 호출)
    }

    // 플레이어의 위치를 업데이트하는 메소드
    private void updatePlayerPosition() {
    	
        if (keys[KeyEvent.VK_LEFT]) {
        	if(X>0)
            X -= SPEED; // 왼쪽 키가 눌리면 왼쪽으로 이동
        }
        if (keys[KeyEvent.VK_RIGHT]) {
        	if(X<MaxMapSize)
            X += SPEED; // 오른쪽 키가 눌리면 오른쪽으로 이동
        }
        if (keys[KeyEvent.VK_UP]) {
        	if(Y>0)
            Y -= SPEED; // 위쪽 키가 눌리면 위로 이동
        }
        if (keys[KeyEvent.VK_DOWN]) {
        	if(Y<MaxMapSize)
            Y += SPEED; // 아래쪽 키가 눌리면 아래로 이동
        }
        
        // 플레이어가 창의 경계를 넘지 않도록 위치를 조정
       /* playerX = Math.max(playerX, 0);
        playerX = Math.min(playerX, getWidth() - playerImage.getWidth(null));
        playerY = Math.max(playerY, 0);
        playerY = Math.min(playerY, getHeight() - playerImage.getHeight(null)); */
    }

    // KeyListener 인터페이스를 구현한 메소드, 키가 눌렸을 때 호출
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true; // 해당 키가 눌렸다면 배열에 true를 설정
    }

    // KeyListener 인터페이스를 구현한 메소드, 키에서 손을 떼었을 때 호출
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false; // 해당 키에서 손을 떼었다면 배열에 false를 설정
    }
    
    // KeyListener 인터페이스의 메소드, 키 타이핑 이벤트를 처리, 여기서는 구현x
    public void keyTyped(KeyEvent e) { }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Agar.Io"); // 게임 윈도우를 생성
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 윈도우 닫힐 때 프로그램 종료
        mainDisplay gamePanel = new mainDisplay(); // 게임 패널 객체 생성
        frame.add(gamePanel); // 프레임에 게임 패널 추가
        frame.pack(); 
        frame.setLocationRelativeTo(null); // 윈도우를 화면 가운데에 위치
        frame.setVisible(true); // 윈도우를 보이게 설정
        gamePanel.requestFocusInWindow(); // 키 입력을 받기 위해 게임 패널에 포커스 요청
    }
}
