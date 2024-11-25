import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import static java.lang.Thread.sleep;

public class MainGame extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private int X, Y;
    private int previousX = -1, previousY = -1;
    private final int SPEED = 5;
    private boolean[] keys;
    private int size = 300;
    private JFrame frame;
    private ArrayList<Food> Foods = new ArrayList<>();
    private Players Players;
    private boolean isColor = false;
    private int UserId;
    private Color Pcolor = Color.BLACK;
    private int MaxMapSize = 1600;
    private Random random = new Random();
    private boolean isGameOver = false;
    private boolean eatingF = false;
    private String username;
    private Image backgroundImage;
    private boolean overlayVisible = false;
    private JPanel overlayPanel;
    private JLayeredPane layeredPane;
    private JTextPane chatPane;
    private JTextField chatInput;
    private Point point;
    private static final int BUF_LEN = 128;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private ObjectInputStream ois;
    private Clip backgroundClip;
    private Clip eatingClip;


    public MainGame(String Username,String ip_addr, String port_no,Point point) {
        username = Username;
        backgroundImage = new ImageIcon(getClass().getResource("/images/space.jpg")).getImage();
        keys = new boolean[256];
        X = random.nextInt(MaxMapSize);
        Y = random.nextInt(MaxMapSize);
        this.point = point;
        timer = new Timer(30, this);
        timer.start();

       // playBackgroundMusic("/sounds/space.wav");
        new Thread(() -> playBackgroundMusic("/sounds/space.wav")).start();
        addKeyListener(this);
        setFocusable(true);
        setPreferredSize(new Dimension(600, 400));

        try {
            socket = new Socket(ip_addr, Integer.parseInt(port_no));
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            String msg = String.format("/PS %d %d %d %s", X, Y, size, username);
            SendMessage(msg);

            msg = dis.readUTF();
            String[] cmsg = msg.split(" ");
            UserId = Integer.parseInt(cmsg[1]);

            new ListenDatas().start();
            SendMessage("start");
        } catch (IOException e) {
            e.printStackTrace();
            
        }
        
        create_game();
    }

    private void SendMessage(String msg) {
        if (socket.isClosed()) return;
        try {
            dos.writeUTF(msg);
        } catch (IOException e) {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            timer.stop();
            ois.close();
            dos.close();
            dis.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ListenDatas extends Thread {
        private int beforeSize = -1;

        public void run() {
            while (!socket.isClosed()) {
                try {
                    Object obj = ois.readObject();
                    if (obj instanceof Datas) {
                        Datas data = (Datas) obj;
                        synchronized (Foods) {
                            Foods = data.getFoods();
                        }
                        Players = data.getPlayers();
                        if (eatingF && beforeSize != Players.getPlayers().size()) eatingF = false;
                        beforeSize = Players.getPlayers().size();
                        if (!isColor) {
                            for (Player p : Players.getPlayers()) {
                                if (p.getID() == UserId) {
                                    Pcolor = p.getColor();
                                    isColor = true;
                                }
                            }
                        }
                    } else if (obj instanceof Massage) {
                        Massage message = (Massage) obj;
                        setMessage(message.getName() + ": " + message.getMassage());
                    } else if (obj instanceof ChFoods) {
                        ChFoods f = (ChFoods) obj;
                        synchronized (Foods) {
                           // System.out.println("Removing food: " + f.getDeleted().getId());
                            Foods.removeIf(food -> food.getId() == f.getDeleted().getId());
                           // System.out.println("Adding food: " + f.getNew().getId());
                            Foods.add(f.getNew());
                        }
                    } else if (obj instanceof Players) {
                        Players = (Players) obj;
                    }
                } catch (IOException | ClassNotFoundException e) {
                    cleanup();
                    break;
                }
            }
        }
    }

    private void setMessage(String message) {
        StyledDocument doc = chatPane.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), message + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        chatPane.setCaretPosition(doc.getLength());
    }

    private int lengCul(int x1, int y1, int x2, int y2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    public boolean isItHit(Objects ob) {
        int obSize = ob.getSize();
        return lengCul(X, Y, ob.getX(), ob.getY()) < (size + obSize) * (size + obSize) / 400;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        int backgroundX = 300 - X % backgroundImage.getWidth(null);
        int backgroundY = 200 - Y % backgroundImage.getHeight(null);

        //배경이미지 로드
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                g.drawImage(backgroundImage, backgroundX + i * backgroundImage.getWidth(null),
                        backgroundY + j * backgroundImage.getHeight(null), null);
            }
        }


        // 플레이어 자신의 크기와 위치
        int x, y, s;
        g.setColor(Pcolor);
        g.fillOval(300 - size / 20, 200 - size / 20, size / 10 + 1, size / 10 + 1);
        g.setColor(Color.WHITE);
        g.drawOval(300 - size / 20, 200 - size / 20, size / 10 + 1, size / 10 + 1);

        g.setColor(Color.lightGray);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString(username,  300 - username.length()*(4)+2,200 - size / 20 - 6 );
        //X + 반지름 - 글자길이/2, Y + 반지름 + 글자높이/2 



        Iterator<Food> iterator1 = Foods.iterator();
        while (iterator1.hasNext()) {
            Food F = iterator1.next();
            x = F.getX();
            y = F.getY();
            s = F.getSize();
            if (isItHit(F)) {
        
                playEatingSound("/sounds/eatingSound.wav");
                size += 2;
                String msg = String.format("/FD %d %d %d", F.getId(), size, UserId);
                SendMessage(msg);
                
            iterator1.remove();
            }
            if (x > X - 300 && x < X + 300 && y > Y - 200 && y < Y + 200) {
                g.setColor(Color.GREEN);
                g.fillOval(300 - (X - x) - s / 20, 200 - (Y - y) - s / 20, s / 10, s / 10);
                g.setColor(Color.WHITE);
                g.drawOval(300 - (X - x) - s / 20, 200 - (Y - y) - s / 20, s / 10, s / 10);
            }
        }



        // 다른 플레이어 표시

        Iterator<Player> playerIterator = Players.getPlayers().iterator();  // Create an iterator

        while (playerIterator.hasNext()) {
            Player P = playerIterator.next();

            if (P.getID() != UserId) {
                x = P.getX();
                y = P.getY();
                s = P.getSize();
                Color c = P.getColor();
                g.setColor(c);

                if (isItHit(P)) {
                    if (P.getSize() > size) {
                        isGameOver = true;
                        if (isGameOver) {
                            cleanup();
                            MainClient client = new MainClient();
                            client.setSize(600, 400); // 크기 설정
                            client.setLocationRelativeTo(null); // 화면 중앙에 위치
                            client.setVisible(true);
                            frame.setVisible(false);
                            return;
                        }
                    }
                    if (P.getSize() < size) {

                        if(!eatingF) {
                            playEatingSound("/sounds/eatingSound.wav");
                          //  System.out.println("EAT");
                            size += P.getSize();
                            SendMessage(String.format("/PE %d %d %d", size, UserId,P.getID()));
                            playerIterator.remove();
                        }
                       
                         Players.rmPlayer(P);
                         eatingF = true;
                    }
                }

                if (x > X - 300 - s / 20 && x < X + 300 + s / 20 && y > Y - 200 - s / 20 && y < Y + 200 + s / 20) {
                    g.fillOval(300 - (X - x) - s / 20, 200 - (Y - y) - s / 20, s / 10, s / 10);
                    g.setColor(Color.WHITE);
                    g.drawOval(300 - (X - x) - s / 20, 200 - (Y - y) - s / 20, s / 10, s / 10);

                    g.setColor(Color.lightGray);
                    g.setFont(new Font("Arial", Font.BOLD, 14));
                    g.drawString(P.getName(),  300 - (X - x) - P.getName().length()*(4)+2, 200 - (Y - y) - s / 20 -6);
                 
                }
            }
        }
        // **스코어 보드 표시 추가**
        drawScoreBoard(g);
    }

    // 스코어 보드 그리기 메서드 추가
    private void drawScoreBoard(Graphics g) {
        g.setColor(Color.lightGray);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Score Board", 10, 20);

        int yPosition = 40;
        for (Player player : Players.getPlayers()) {
            g.drawString(player.getID() +". "+ player.getName() + ": " + player.getSize()*10, 10, yPosition);
            yPosition += 20;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (!isGameOver) {
            updatePlayerPosition();
            if (previousX != X || previousY != Y) {
                SendMessage(String.format("/PM %d %d %d", X, Y, UserId));
            }
            previousX = X;
            previousY = Y;
            repaint();
        }
    }

    private void updatePlayerPosition() {
        if (keys[KeyEvent.VK_LEFT] && X > 0) X -= SPEED;
        if (keys[KeyEvent.VK_RIGHT] && X < MaxMapSize) X += SPEED;
        if (keys[KeyEvent.VK_UP] && Y > 0) Y -= SPEED;
        if (keys[KeyEvent.VK_DOWN] && Y < MaxMapSize) Y += SPEED;
    }

    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
    
        if (e.getKeyCode() == KeyEvent.VK_T) {
           // System.out.println("T key pressed");
            overlayVisible = !overlayVisible;
            overlayPanel.setVisible(overlayVisible);
            chatInput.requestFocusInWindow();
            revalidate();
            repaint();
            chatInput.setText("");
        }
    }

    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    public void create_game() {
        frame = new JFrame("Agar.Io");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
    
        // JLayeredPane 초기화
        layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(600, 400));
        frame.add(layeredPane);
    
        // MainGame 패널 추가
        this.setBounds(0, 0, 600, 400);
        layeredPane.add(this, JLayeredPane.DEFAULT_LAYER);
    
        // Overlay panel 초기화
        overlayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(128, 128, 128, 128)); // 반투명 회색
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        overlayPanel.setBounds(0, 0, 600, 400);
        overlayPanel.setOpaque(false); // 패널을 투명하게 설정
        overlayPanel.setVisible(false); // 초기에는 보이지 않도록 설정
        overlayPanel.setLayout(new BorderLayout());
    
        // 채팅 영역 추가
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setOpaque(false); // JTextPane을 투명하게 설정
        chatPane.setBackground(new Color(0, 0, 0, 0)); // 배경색을 투명하게 설정
        chatPane.setForeground(Color.WHITE); // 텍스트 색상을 흰색으로 설정
        chatPane.setFont(new Font("Arial", Font.BOLD, 20));
        StyledDocument doc = chatPane.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(attrs, 0.1f); // 줄 간격 설정
        doc.setParagraphAttributes(0, doc.getLength(), attrs, false);
        JScrollPane chatScrollPane = new JScrollPane(chatPane);
        chatScrollPane.setOpaque(false); // JScrollPane을 투명하게 설정
        chatScrollPane.getViewport().setOpaque(false); // 뷰포트를 투명하게 설정
        overlayPanel.add(chatScrollPane, BorderLayout.CENTER);
    
        // 채팅 입력 필드 추가
        chatInput = new JTextField();
        chatInput.setOpaque(false); // JTextField를 투명하게 설정
        chatInput.setBackground(new Color(0, 0, 0, 0)); // 배경색을 투명하게 설정
        chatInput.setForeground(Color.WHITE); // 텍스트 색상을 흰색으로 설정
        chatInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = chatInput.getText();
                chatInput.setText("");
                SendMessage(String.format("/CH %s %s", username, message));
            }
        });
        chatInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    overlayVisible = !overlayVisible;
                    overlayPanel.setVisible(overlayVisible);
                    MainGame.this.requestFocusInWindow(); // 포커스를 MainGame 패널로 다시 설정
                }
            }
        });
        chatPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    overlayVisible = !overlayVisible;
                    overlayPanel.setVisible(overlayVisible);
                    MainGame.this.requestFocusInWindow(); // 포커스를 MainGame 패널로 다시 설정
                }
            }
        });
        overlayPanel.add(chatInput, BorderLayout.SOUTH);
    
        layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
    
        frame.pack();
        frame.setVisible(true);
        frame.setLocation(point);
        this.requestFocusInWindow();
    }
    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'keyTyped'");
    }
    
    private void playBackgroundMusic(String filePath) {
        try (InputStream audioSrc = getClass().getResourceAsStream(filePath);
             InputStream bufferedIn = new BufferedInputStream(audioSrc)) {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(audioInputStream);
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playEatingSound(String filePath) {
        try (InputStream audioSrc = getClass().getResourceAsStream(filePath);
             InputStream bufferedIn = new BufferedInputStream(audioSrc)) {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
            eatingClip = AudioSystem.getClip();
            eatingClip.open(audioInputStream);
            eatingClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
