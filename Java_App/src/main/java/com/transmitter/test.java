package com.transmitter;
import java.util.Scanner;

public class test {
    private static final Scanner s = new Scanner(System.in);
    public static void main(String[] args) {
        TtsService tts = new TtsService();
        System.out.println("Enter a message to speak: ");
        String message = s.nextLine();
        tts.speak(message,"Microsoft Zira Desktop"); // other assistant
    }
}
