package com.iotracks.comsat.private_socket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iotracks.comsat.utils.Constants;
import com.iotracks.comsat.utils.LogUtil;
import com.iotracks.comsat.utils.Settings;
import com.iotracks.comsat.utils.SslManager;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;

public class PrivateSocket implements Runnable {

	public final String passCode;
	public final int port;
	public final int maxConnections;

	private Channel channel;
	private boolean stopListening = false;
	private PrivateSocket pairSocket;
	private static Map<Channel, Channel> channelsMapping;
	
	public List<Channel> connections = new ArrayList<>();
	
	public PrivateSocket(int port, String passCode, int maxConnections) {
		this.passCode = passCode;
		this.port = port;
		this.maxConnections = maxConnections;
		PrivateSocket.channelsMapping = new HashMap<>();
		this.pairSocket = null;
	}
	
	public void run() {
		while (!stopListening) {
//			EventLoopGroup bossGroup = new NioEventLoopGroup(1);
//			EventLoopGroup workerGroup = new NioEventLoopGroup(1);
			try {
				ServerBootstrap b = new ServerBootstrap();
				b.group(Constants.bossGroup, Constants.workerGroup)
					.channel(NioServerSocketChannel.class);
				SslContext sslCtx = null;
				try {
					sslCtx = SslManager.getSslContext();
				} catch (Exception e) {
					e.printStackTrace();
				}
				PrivateSocketInitializer channelInitializer = new PrivateSocketInitializer(sslCtx, this);
				b.childHandler(channelInitializer);

				channel = b.bind(port).sync().channel();
				channel.closeFuture().sync();
			} catch (Exception e) {
				LogUtil.warning(e.getMessage());
			}
//			finally {
//				bossGroup.shutdownGracefully();
//				workerGroup.shutdownGracefully();
//			}

			if (!stopListening) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					LogUtil.warning(e.getMessage());
				}
			}
		}
	}

	public void close() {
		if (channel != null) {
			this.stopListening = true;
			try {
				channel.close().sync();
				Settings.setPortAvailable(port);
				for (Channel ch: connections) {
					ch.close().sync();
				}
				if (pairSocket != null) {
					pairSocket.pairSocket = null;
					pairSocket.close();
				}
			} catch (Exception e) {
				LogUtil.warning(e.getMessage());
			}
		}
	}

	public Channel mapChannel(Channel publicChannel) {
		synchronized (PrivateSocket.class) {
			for (Channel ch: connections) {
				if (!channelsMapping.containsKey(ch)) {
					channelsMapping.put(ch, publicChannel);
					return ch;
				}
			}
		}
		return null;
	}

	public void releaseChannel(Channel privateChannel) {
		synchronized (PrivateSocket.class) {
			channelsMapping.remove(privateChannel);
		}
	}
	
	public Channel getPublicChannel(Channel privateChannel) {
		synchronized (PrivateSocket.class) {
			return channelsMapping.get(privateChannel);
		}
	}
	
	public void setPairSocket(PrivateSocket pairSocket) {
		this.pairSocket = pairSocket;
	}
	
	public PrivateSocket getPairSocket() {
		return this.pairSocket;
	}
	
	public boolean isPublic() {
		return this.pairSocket == null;
	}

	public Channel getPrivateChannel() {
		if (connections.size() > 0) 
			return connections.get(0);
		else
			return null;
	}
	
	public String getStatus() {
		if (channel.isActive())
			return "Active";
		else
			return "Inactive";
	}
}
