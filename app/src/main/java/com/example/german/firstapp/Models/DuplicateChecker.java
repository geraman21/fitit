package com.example.german.firstapp.Models;

public class DuplicateChecker {
   private String duplicate = "";

    public DuplicateChecker(String duplicate) {
        this.duplicate = duplicate;
    }

    public String getDuplicate() {
        return duplicate;
    }

    public void setDuplicate(String duplicate) {
        this.duplicate = duplicate;
    }
}
