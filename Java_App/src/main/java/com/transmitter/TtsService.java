package com.transmitter;
// text to speak service, using powershell in built Tts
public class TtsService{
    private static final String psCommand = "Add-Type -AssemblyName System.Speech; " +
            "$s = (New-Object System.Speech.Synthesis.SpeechSynthesizer); " +
            "$s.SelectVoice('%s'); " +
            "$s.Speak('%s')";



    public void speak(String message, String voice){
        if(message == null || message.isBlank()) return;
        String command = String.format(psCommand,sanitize(voice), sanitize(message)); 
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-Command", command);
            pb.redirectErrorStream(true);
            pb.start(); // fire process
        } catch (Exception e) {
            System.err.println("[TTS] Error " + e.getMessage());
        }
    }

    private String sanitize(String input){
        return input.replaceAll("'", ""); // replace quotes with blanks
    }
}