package com.transmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class SerialBackend{
    // constants
    private final int baudRate = 115200;
    private final int numDataBits = 8;

    // port object to connect 
    private SerialPort port;
    
    public ArrayList<String> getAvailablePorts(){
        // list with com ports
        ArrayList <String> comPorts = new ArrayList<>(); 

        try {
            for(SerialPort port: SerialPort.getCommPorts()){
                comPorts.add(port.getSystemPortName());
            }
        } catch (UnsatisfiedLinkError | ExceptionInInitializerError e) {
            System.err.println("jSerialComm native library not available: " + e.getMessage());
            // Return empty list if jSerialComm can't be loaded
        }
        return comPorts;
    }
    public boolean connect(String portName, int baudRate){
        // get port by name
        port = SerialPort.getCommPort(portName); 

        // config
        port.setBaudRate(baudRate);
        port.setNumDataBits(numDataBits);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        
        // open port
        return port.openPort(); 

    }

    public boolean disconnect(){
        if (port!= null && port.isOpen()){
            port.closePort(); //close port
            return true;
        }
        return false;
    }

    public boolean isConnected(){
        return (port!= null && port.isOpen());
     }

    public int getBaudRate(){
        return this.baudRate;
    }

    
    // pass the function received from controller -> data arrives -> function gets called with the data
    public void startListening(Consumer<String> onMessageReceived){ 
        //onMessageReceived = message -> {lambda function to be implemented based on message}

        // add dataListener to port
        port.addDataListener(new SerialPortDataListener() {

            // implement getListeningEvents()
            @Override   
            public int getListeningEvents(){ 
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; // fire when data is available
            }

            // implement serialEvent() - jserial calls it automatically when data arrives
            @Override
            public void serialEvent(SerialPortEvent event){
                if(event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                    return; // ignore other events
            
                int available = port.bytesAvailable(); // available bytes
                if(available<=0) return; // nothing to read

                byte[] buffer = new byte [available];
                // read bytes
                port.readBytes(buffer, buffer.length); 

                String message = new String (buffer,java.nio.charset.StandardCharsets.UTF_8);

                // call the function received from the controller, giving it a message
                onMessageReceived.accept(message);
            }
        });
    }
    public void stopListening(){
        port.removeDataListener();
    }

    public List<String> getAvailableVoices() {
    List<String> voices = new ArrayList<>();
    try {
        String ps = "Add-Type -AssemblyName System.Speech; " +
                    "(New-Object System.Speech.Synthesis.SpeechSynthesizer).GetInstalledVoices() | " +
                    "% { $_.VoiceInfo.Name }";
        Process p = new ProcessBuilder("powershell", "-NoProfile", "-Command", ps).start();
        new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))
            .lines().forEach(voices::add);
        p.waitFor();
    } catch (Exception e) {
        System.err.println("[TTS] Could not fetch voices: " + e.getMessage());
    }
    return voices;
}
}
