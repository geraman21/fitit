package com.example.german.firstapp.Models;

public class ChatModel {
    public String message;
    String time;
    String sender;
    String userLogin;
    String publishGroups;

    public ChatModel(String message, String time, String sender, String userLogin, String publishGroups) {
        this.message = message;
        this.time = time;
        this.sender = sender;
        this.userLogin = userLogin;
        this.publishGroups = publishGroups;
    }

    public String getTime() {
        return time;
    }


    public ChatModel(){}

    public String getPublishGroups() {return publishGroups;}

    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }

    public String getUserLogin() {
        return userLogin;
    }
}
