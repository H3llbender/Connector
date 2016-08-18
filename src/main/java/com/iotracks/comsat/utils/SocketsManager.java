package com.iotracks.comsat.utils;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.iotracks.comsat.config.Configuration;
import com.iotracks.comsat.private_socket.PrivateSocket;
import com.iotracks.comsat.public_socket.PublicSocket;

public class SocketsManager {
	private static Map<String, PublicSocket> publicSockets = new HashMap<>();
	private static Map<String, PrivateSocket> privateSockets = new HashMap<>();

	public synchronized void openPort(Configuration cfg) {
    	PublicSocket publicSocket = null;
        PrivateSocket privateSocket = new PrivateSocket(cfg.getPort1(), cfg.getPassCode1(), cfg.getMaxConnections1());

        if (cfg.getPassCode2().equals("")) {
        	publicSocket = new PublicSocket(cfg.getPort2(), privateSocket);
            new Thread(publicSocket).start();
            publicSockets.put(cfg.getId(), publicSocket);
        } else {
            PrivateSocket privateSocket2 = new PrivateSocket(cfg.getPort2(), cfg.getPassCode2(), cfg.getMaxConnections2());
            privateSocket.setPairSocket(privateSocket2);
            privateSocket2.setPairSocket(privateSocket);
            new Thread(privateSocket2).start();
        }

        new Thread(privateSocket).start();

        privateSockets.put(cfg.getId(), privateSocket);

        Settings.setPortInUse(cfg.getPort2());
        Settings.setPortInUse(cfg.getPort1());
	}
	
	public void closePorts() {
    	for (PrivateSocket privateSocket: privateSockets.values())
    		privateSocket.close();
    	
    	for (PublicSocket publicSocket: publicSockets.values()) 
    		publicSocket.close();
	}
	
	public synchronized void closePort(String id) {
		if (publicSockets.containsKey(id)) {
			publicSockets.get(id).close();
			publicSockets.remove(id);
		}
		
		if (privateSockets.containsKey(id)) {
			privateSockets.get(id).close();
			privateSockets.remove(id);
		}
	}
	
	public PrivateSocket getPrivateSocket(String id) {
		return privateSockets.get(id);
	}
	
	public PublicSocket getPublicSocket(String id) {
		return publicSockets.get(id);
	}
	
	public boolean isPortInUse(int portNumber) {
        boolean result;

        try {

            Socket s = new Socket("127.0.0.1", portNumber);
            s.close();
            result = true;

        }
        catch(Exception e) {
            result = false;
        }

        return(result);
	}
	
}
