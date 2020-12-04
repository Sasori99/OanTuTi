/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.controller;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.User;

/**
 *
 * @author Okami
 */
public class UserDAO extends DAO{
    
    private final String checkLoginSQL = "SELECT * FROM user WHERE username = ? AND password = ?" ;
    private final String updateStatusSQL = "UPDATE user SET userStatus = ? WHERE id = ?" ;

    public UserDAO() {
        super();
    }
    
    public User checkLogin(User user) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement(checkLoginSQL);
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getPassword());
            ResultSet res = preparedStatement.executeQuery();
            while (res.next()) {
                User rs = new User(res.getInt("id"), res.getString("username"),res.getString("password"),res.getString("fullName"));
                return rs;
            }
        } catch (SQLException ex) {
            Logger.getLogger(UserDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    public boolean updateStatus(User user) {
        try {
            PreparedStatement preparedStatement = con.prepareStatement(updateStatusSQL);
            preparedStatement.setInt(1, user.getUserStatus());
            preparedStatement.setInt(2, user.getId());
            boolean res = preparedStatement.execute();
            return res;
        } catch (SQLException ex) {
            Logger.getLogger(UserDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
}
