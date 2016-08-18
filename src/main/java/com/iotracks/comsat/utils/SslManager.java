package com.iotracks.comsat.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

public class SslManager {
	public static SslContext getSslContext() throws Exception {
		SslContext sslCtx = SslContextBuilder.forServer(
				new File(Constants.CERTITICATE_FILENAME),
				new File(Constants.KEY_FILENAME))
				.build();
		
		return sslCtx;
	}
	
	public static SSLContext getSSLContext() throws Exception {        
	    byte[] certBytes = fileToByte(Constants.CERTITICATE_FILENAME);
	    
	    X509Certificate cert = generateCertificateFromDER(certBytes);
	    
	    TrustManagerFactory tmf = TrustManagerFactory
	    	    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
	    	KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
	    	ks.load(null); // You don't need the KeyStore instance to come from a file.
	    	ks.setCertificateEntry("caCert", cert);

	    	tmf.init(ks);

	    	SSLContext sslContext = SSLContext.getInstance("TLS");
	    	sslContext.init(null, tmf.getTrustManagers(), null);
	    return sslContext;
	}
	
	protected static X509Certificate generateCertificateFromDER(byte[] certBytes) throws Exception {
	    CertificateFactory factory = CertificateFactory.getInstance("X.509");

	    return (X509Certificate)factory.generateCertificate(new ByteArrayInputStream(certBytes));      
	}
	
	private static byte[] fileToByte(String fileName) {
		FileInputStream fileInputStream = null;

        File file = new File(fileName);

        byte[] bFile = new byte[(int) file.length()];

	    try {
			fileInputStream = new FileInputStream(file);
		    fileInputStream.read(bFile);
		    fileInputStream.close();
		    return bFile;
		} catch (Exception e) {
			return null;
		}
	}

}
