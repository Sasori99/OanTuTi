/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.controller;

import client.view.Dashboard;
import client.view.Game;
import client.view.Login;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import model.Message;
import model.User;

/**
 *
 * @author Okami
 */
public class ClientController {
    private static  final int port = 8000;
    private static  final String hostname = "localhost";
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    
    private User currentUser;
    private User competitorUser;
    
    private Login loginView;
    private Dashboard dashboard;
    private Game game;
    
    
    public ClientController() {
        try {
            
            this.socket = new Socket(hostname, port);
            this.oos = new ObjectOutputStream(this.socket.getOutputStream());
            this.ois = new ObjectInputStream(this.socket.getInputStream());
            RequestListener requestListener = new RequestListener();
            requestListener.start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Không thể kết nối đến máy chủ");
            Logger.getLogger(ClientController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
            
    public ClientController(Login loginView) {
        try {
            this.loginView = loginView;
            this.socket = new Socket(hostname, port);
            this.oos = new ObjectOutputStream(this.socket.getOutputStream());
            this.ois = new ObjectInputStream(this.socket.getInputStream());
            RequestListener requestListener = new RequestListener();
            requestListener.start();
            this.loginView.setVisible(true);
            loginView.addLoginListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                User user = loginView.getUserFromInputs();
                Message request = new Message("Login", user);
                sendRequest(request);
            }
        });
            
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Không thể kết nối đến máy chủ");
            Logger.getLogger(ClientController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void getListUserOnline() {
        Message request = new Message("Get list user online", null);
        sendRequest(request);
    }
    
    public void sendChallengeMessage(User competitor) {
        Message request = new Message("Challenge to", competitor);
        sendRequest(request);
    }
    
    public void sendRequest(Message request) {
        try {
            oos.writeObject(request);
            oos.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Không thể kết nối đến máy chủ");
            Logger.getLogger(ClientController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Message receiveResponse() {
        try {
            synchronized(ois) {
                return (Message) ois.readObject();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Không thể kết nối đến máy chủ");
            Logger.getLogger(ClientController.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    class RequestListener extends Thread {
        @Override
        public void run() {
            while (true) {
                Message response = receiveResponse();
                System.out.println(response.getMessage());
                switch (response.getMessage()) {
                    case "Login fail": {
                        JOptionPane.showMessageDialog(null, response.getObject().toString());
                        break;
                    }
                    case "Login success": {
                        currentUser = (User) response.getObject();
                        dashboard = new Dashboard(currentUser);
                        dashboard.addRefreshLisener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                getListUserOnline();
                            }
                        });
                        dashboard.addChallengeLisener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                User competitor = dashboard.getSelectedUser();
                                if (competitor ==  null) {
                                    JOptionPane.showMessageDialog(null, "Vui lòng chọn người chơi để thách đấu!!!");
                                } else {
                                    sendChallengeMessage(competitor);
                                }
                            }
                        });
                        loginView.dispose();
                        dashboard.setVisible(true);
                        getListUserOnline();
                        break;
                    }
                    case "List user online": {
                        dashboard.setListUser((ArrayList<User>) response.getObject());
                        break;
                    }
                    case "Challenge from": {
                        User competitor = (User) response.getObject();
                        int choice = JOptionPane.showConfirmDialog(dashboard, competitor.getUsername() + " muốn thách đấu với bạn!!" , "Lời mời", JOptionPane.YES_NO_OPTION);
                        if (choice == JOptionPane.YES_OPTION) {
                            sendRequest(new Message("Accept", competitor));
                        } else {
                            sendRequest(new Message("Decline", competitor));
                        }
                        break;
                    }
                    case "Accept": {
                        User competitor = (User) response.getObject();
                        JOptionPane.showMessageDialog(dashboard, "Đối thủ đã chấp nhận yêu cầu thách đấu của bạn!!!");
                        
                        dashboard.setVisible(false);
                        game = new Game();
                        game.setVisible(true);
                        break;
                        
                    }
                    case "Decline": {
                        User competitor = (User) response.getObject();
                        JOptionPane.showMessageDialog(dashboard, "Đối thủ đã từ chối yêu cầu thách đấu của bạn!!!");
                        break;
                    }
                }
            }
        }
    }
    
    public static void main(String[] args) {
        Login login = new Login();
        ClientController clientController = new ClientController(login);
    }
}
