import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Server extends JFrame {
    private int sendingFrame = 2;
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextArea textArea;
    private JTextField txtPortNumber;
    private ArrayList<Food> Foods = new ArrayList<>();
    private Players Players = new Players();
    private int MaxMapSize = 1600;
    private ServerSocket socket;
    private Vector<UserService> UserVec = new Vector<>();
    private static final int BUF_LEN = 128;
    private Datas data;
    private int PlayerCounter = 1;
    private boolean DiedPlayer = false;
    private boolean sendText = false;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                Server frame = new Server();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    
    public void removeUser(UserService u){
        UserVec.remove(u);
    }

    public Server() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 338, 386);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(12, 10, 300, 244);
        contentPane.add(scrollPane);

        textArea = new JTextArea();
        textArea.setEditable(false);
        scrollPane.setViewportView(textArea);

        JLabel lblNewLabel = new JLabel("Port Number");
        lblNewLabel.setBounds(12, 264, 87, 26);
        contentPane.add(lblNewLabel);

        txtPortNumber = new JTextField();
        txtPortNumber.setHorizontalAlignment(SwingConstants.CENTER);
        txtPortNumber.setText("30000");
        txtPortNumber.setBounds(111, 264, 199, 26);
        contentPane.add(txtPortNumber);
        txtPortNumber.setColumns(10);

        JButton btnServerStart = new JButton("Server Start");
        btnServerStart.addActionListener(e -> {
            try {
                socket = new ServerSocket(Integer.parseInt(txtPortNumber.getText()));
                AppendText("Chat Server Running..");
                btnServerStart.setText("Chat Server Running..");
                btnServerStart.setEnabled(false);
                txtPortNumber.setEnabled(false);
                new AcceptServer().start();
            } catch (NumberFormatException | IOException e1) {
                e1.printStackTrace();
            }
        });
        btnServerStart.setBounds(12, 300, 300, 35);
        contentPane.add(btnServerStart);

        for (int i = 0; i < 100; i++) {
            Foods.add(new Food(MaxMapSize, i));
        }
        data = new Datas(Foods, Players);
    }

    public void AppendText(String str) {
        textArea.append(str + "\n");
        textArea.setCaretPosition(textArea.getText().length());
    }

    class AcceptServer extends Thread {
        public void run() {
            while (true) {
                try {
                    AppendText("Waiting clients ...");
                    Socket client_socket = socket.accept();
                    AppendText("새로운 참가자 from " + client_socket);
                    UserService new_user = new UserService(client_socket);
                    synchronized (UserVec) {
                        UserVec.add(new_user);
                    }
                    AppendText("사용자 입장. 현재 참가자 수 " + UserVec.size());
                    new_user.start();
                } catch (IOException e) {
                    AppendText("accept 에러 발생");
                }
            }
        }
    }

    class UserService extends Thread {
        private DataInputStream dis;
        private DataOutputStream dos;
        private ObjectOutputStream oos;
        private Socket client_socket;
        private boolean isRunning = true; // 클라이언트 연결 상태
        private int Id;
        private Player Me;
        private int chageCounter=sendingFrame;


        public UserService(Socket client_socket) {
            this.client_socket = client_socket;
            try {
                dis = new DataInputStream(client_socket.getInputStream());
                dos = new DataOutputStream(client_socket.getOutputStream());
                oos = new ObjectOutputStream(dos);
                String line1 = dis.readUTF();
                String[] msg = line1.split(" ");
                Me = new Player(Integer.parseInt(msg[1]), Integer.parseInt(msg[2]), Integer.parseInt(msg[3]), PlayerCounter, msg[4]);
                synchronized (Players) {
                    Players.addPlayer(Me);
                }
                Id = PlayerCounter;
                dos.writeUTF("/PID " + PlayerCounter++);
                //WriteAll(Players);
            } catch (IOException e) {
                AppendText("UserService 초기화 에러");
                cleanup();
            }
        }

        public void WriteOne(Datas datas) {
            if (!isRunning) return; // 클라이언트가 연결된 경우에만 전송
            try {
                oos.reset();
                oos.writeObject(datas);
                oos.flush();
            } catch (IOException e) {
                AppendText("클라이언트로 데이터 전송 중 에러 발생");
                cleanup();
            }
        }


        public void WriteAll(Datas datas) {
            synchronized (UserVec) {
                Iterator<UserService> iterator = UserVec.iterator();
                while (iterator.hasNext()) {
                    UserService user = iterator.next();
                    if (user.isRunning) {
                        user.WriteOne(datas);
                    } else {
                        UserVec.remove(user);
                        AppendText("연결오류");
                    }
                }
            }
        }

        public void WriteOne(ChFoods datas) {
            if (!isRunning) return; // 클라이언트가 연결된 경우에만 전송
            try {
                oos.reset();
                oos.writeObject(datas);
                oos.flush();
            } catch (IOException e) {
                AppendText("클라이언트로 데이터 전송 중 에러 발생");
                cleanup();
            }
        }


        public void WriteAll(ChFoods datas) {
            synchronized (UserVec) {
                Iterator<UserService> iterator = UserVec.iterator();
                while (iterator.hasNext()) {
                    UserService user = iterator.next();
                    if (user.isRunning) {
                        user.WriteOne(datas);
                    } else {
                        UserVec.remove(user);
                        AppendText("연결오류");
                    }
                }
            }
        }

        public void WriteOne(Players datas) {
            if (!isRunning) return; // 클라이언트가 연결된 경우에만 전송
            try {
                oos.reset();
                oos.writeObject(datas);
                oos.flush();
            } catch (IOException e) {
                AppendText("클라이언트로 데이터 전송 중 에러 발생");
                cleanup();
            }
        }


        public void WriteAll(Players datas) {
            synchronized (UserVec) {
                Iterator<UserService> iterator = UserVec.iterator();
                while (iterator.hasNext()) {
                    UserService user = iterator.next();
                    if (user.isRunning) {
                        user.WriteOne(datas);
                    } else {
                        UserVec.remove(user);
                        AppendText("연결오류");
                    }
                }
            }
        }
    
        public void WriteOneM(Massage datas) {
            if (!isRunning) return; // 클라이언트가 연결된 경우에만 전송
            try {
                oos.reset();
                oos.writeObject(datas);
                oos.flush();
            } catch (IOException e) {
                AppendText("클라이언트로 데이터 전송 중 에러 발생");
                cleanup();
            }
        }


        public void WriteAllM(Massage datas) {
            synchronized (UserVec) {
                Iterator<UserService> iterator = UserVec.iterator();
                while (iterator.hasNext()) {
                    UserService user = iterator.next();
                    if (user.isRunning) {
                        user.WriteOneM(datas);
                    } else {
                        UserVec.remove(user);
                        AppendText("연결오류");
                    }
                }
            }
        }

        public void run() {
            try {
                while (isRunning) {
                    String msg = dis.readUTF();
                    
                    synchronized (data) {
                        if (fixData(msg)) {
                            data.setFoods(Foods);
                            data.setPlayers(Players);

                            WriteAll(Players);
                        }
                        else if(sendText){      
                            String[] cutedMsg = msg.split(" ",3); 
                            Massage massage = new Massage(cutedMsg[1],cutedMsg[2]);
                            WriteAllM(massage);
                            sendText = false;
                        }
                       
                        // if (chageCounter == sendingFrame) {
                        //     chageCounter = 0;
                        //     WriteAll(Players);
                        // } else {
                        //     chageCounter++;
                        // }
                    }
                }
            } catch (IOException e) {
                this.isRunning = false;
                AppendText("클라이언트 연결 종료: " + e.getMessage());
                cleanup(); 
            }
        }

        public boolean fixData(String msg) {
            //AppendText(msg);
            String[] cutedMsg = msg.split(" ");
            switch (cutedMsg[0]) {
                case "start":
                    WriteAll(data);
                    return true;
                case "/PM":
                    synchronized (Players) {
                            if (cutedMsg[3].equals(String.valueOf(Id))) {
                                Me.setX(Integer.parseInt(cutedMsg[1]));
                                Me.setY(Integer.parseInt(cutedMsg[2]));
                                
                                return true;
                            }
                        }
                    break;
                case "/FD":
                    synchronized (Foods) {
                        Iterator<Food> foodIterator = Foods.iterator();
                        while (foodIterator.hasNext()) {
                            Food f = foodIterator.next();
                            if (cutedMsg[1].equals(String.valueOf(f.getId()))) {
                                try {
                                    Food nF = new Food(MaxMapSize, f.getId());
                                    ChFoods Chf = new ChFoods(f, nF);
                                    foodIterator.remove();
                                    Foods.add(nF);
                                    WriteAll(Chf);
                                   // AppendText("CHF: " + f.getId() + " -> " + nF.getId());

                                    if (cutedMsg[3].equals(String.valueOf(Id))) {
                                        Me.setSize(Integer.parseInt(cutedMsg[2]));
                                        return true;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    break;

                case "/PE":
                    if (cutedMsg[2].equals(String.valueOf(Id))) {
                        ArrayList<Player> P = Players.getPlayers();
                        Me.setSize(Integer.parseInt(cutedMsg[1]));  // size 갱신
                        for (Player p: P){
                            if(cutedMsg[3].equals(String.valueOf(p.getID()))){
                                P.remove(p);
                                Players.setPlayers(P);
                                return true;
                            }
                        }
                    }
                    break;

                case "/CH":
                    sendText = true;
                    return false;
                default:
                    return false;
            }
            return false;
        }

        private void cleanup() {
            //if (isRunning) return;
            try {
                if (oos != null) oos.close();
                if (dos != null) dos.close();
                if (dis != null) dis.close();
                if (client_socket != null && !client_socket.isClosed()) client_socket.close();
                
                synchronized (UserVec) {
                    UserVec.remove(this);
                    AppendText("UserVec에서 사용자 제거: " + this);
                }

                synchronized (Players) {
                    ArrayList<Player> P = Players.getPlayers();
                    for (Player p : P) {
                        if (p.getID() == Me.getID()) {
                            P.remove(p);
                            Players.setPlayers(P);
                            AppendText("Players에서 사용자 제거: " + p);
                            break;
                        }
                    }
                }
                AppendText("CleanUp 완료. 남은 참가자 수: " + UserVec.size());
            } catch (IOException e) {
                AppendText("cleanup 에러 발생: " + e.getMessage());
            }
        }
    }
}
