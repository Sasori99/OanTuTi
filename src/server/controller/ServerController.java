/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import model.Message;
import model.User;
import server.view.ServerView;

/**
 *
 * @author Okami
 */
public class ServerController {

    private static final int port = 8000;
    private UserDAO userDAO;

    HashMap<User, Socket> listSocket;
    HashMap<User, ObjectInputStream> listOis;
    HashMap<User, ObjectOutputStream> listOos;

    public ServerController() {
        listSocket = new HashMap<>();
        listOis = new HashMap<>();
        listOos = new HashMap<>();
        this.userDAO = new UserDAO();
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        ServerView.log("Server is running...");
        while (true) {
            Socket socket = serverSocket.accept();
            ServerView.log("Connect success to: " + socket);
            RequestListener requestListener = new RequestListener(socket);
            requestListener.start();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerController serverController = new ServerController();
        serverController.start();
    }

    class RequestListener extends Thread {
        private Socket socket;
        private User currentUser;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;

        public RequestListener(Socket socket) throws IOException {
            System.out.println("Start listen....");
            this.socket = socket;
            ois = new ObjectInputStream(socket.getInputStream());
            oos = new ObjectOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            while (true) {
                try {
                    System.out.println(socket);
                    Message request = (Message) ois.readObject();
                    switch (request.getMessage()) {
                        case "Login": {
                            User user = (User) request.getObject();
                            ServerView.log("Request login username: " + user.getUsername());
                            ServerView.log("Request login password: " + user.getPassword());
                            Message response = new Message();
                            User result = userDAO.checkLogin(user);

                            if (result != null) {
                                if (listSocket.get(result) != null) {
                                    response.setMessage("Login fail");
                                    response.setObject("Tài khoản đang đăng nhập ở nơi khác!!!");
                                } else {
                                    result.setUserStatus(1);
                                    listSocket.put(result, socket);
                                    listOis.put(result, ois);
                                    listOos.put(result, oos);
                                    this.currentUser = result;
                                    userDAO.updateStatus(result);
                                    response.setMessage("Login success");
                                    response.setObject(result);
                                }
                            } else {
                                response.setMessage("Login fail");
                                response.setObject("Tài khoản hoặc mật khẩu không chính xác!!!");
                            }
                            oos.writeObject(response);
                            oos.flush();
                            break;
                        }
                        case "Get list user online": {
                            ServerView.log(this.currentUser + " request get list user online");
                            ArrayList<User> listUserOnline = new ArrayList<>(listSocket.keySet());
                            ServerView.log("Have " + listUserOnline.size() + " user online");
                            oos.writeObject(new Message("List user online", listUserOnline));
                            break;
                        }
                        case "Challenge to": {
                            User competitor = (User) request.getObject();
                            ServerView.log(this.currentUser.getUsername() + " challenge to " + competitor.getUsername());
                            listOos.get(competitor).writeObject(new Message("Challenge from", currentUser));
                            break;
                        }
                        case "Accept": {
                            User competitor = (User) request.getObject();
                            ServerView.log(this.currentUser.getUsername() + " accept challenge to " + competitor.getUsername());
                            request.setObject(this.currentUser);
                            listOos.get(competitor).writeObject(request);
                            break;
                        }
                        case "Decline": {
                            User competitor = (User) request.getObject();
                            ServerView.log(this.currentUser.getUsername() + " decline challenge to " + competitor.getUsername());
                            request.setObject(this.currentUser);
                            listOos.get(competitor).writeObject(request);
                            break;
                        }
                    }
                } catch (Exception e) {
                    listSocket.remove(currentUser);
                    listOis.remove(currentUser);
                    listOos.remove(currentUser);
                    currentUser.setUserStatus(0);
                    userDAO.updateStatus(currentUser);
                    ServerView.log(this.currentUser.getUsername() + " disconnected");
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}
