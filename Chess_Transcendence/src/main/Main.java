package main;
import javax.swing.*;

public class Main {
    public static void main(String[] args){

        JFrame window = new JFrame("Simple Chess");
        window.setDefaultCloseOperation((JFrame.EXIT_ON_CLOSE));
        window.setResizable(false);//true para hacerlo modificable el tamaño;
        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        gamePanel.launchGame();

    }
}
