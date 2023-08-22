package com.apigee.mgmtapi.sdk.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.apigee.edge.config.utils.ServerProfile;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.GoogleCredentials;

public class MgmtAPIClient {
	
	private static final Logger logger = LogManager.getLogger(MgmtAPIClient.class);
	
	/**
	 * 
	 * @param proxyHost
	 * @param proxyPort
	 * @param proxyUsername
	 * @param proxyPassword
	 * @return
	 */
	public static HttpTransportFactory getHttpTransportFactory(String proxyHost, int proxyPort, String proxyUsername, String proxyPassword) {
	    HttpHost proxyHostDetails = new HttpHost(proxyHost, proxyPort);
	    HttpRoutePlanner httpRoutePlanner = new DefaultProxyRoutePlanner(proxyHostDetails);

	    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
	    credentialsProvider.setCredentials(
	        new AuthScope(proxyHostDetails.getHostName(), proxyHostDetails.getPort()),
	        new UsernamePasswordCredentials(proxyUsername, proxyPassword)
	    );

	    HttpClient httpClient = ApacheHttpTransport.newDefaultHttpClientBuilder()
	        .setRoutePlanner(httpRoutePlanner)
	        .setProxyAuthenticationStrategy(ProxyAuthenticationStrategy.INSTANCE)
	        .setDefaultCredentialsProvider(credentialsProvider)
	        .build();

	    final HttpTransport httpTransport = new ApacheHttpTransport(httpClient);
	    return new HttpTransportFactory() {
	      @Override
	      public HttpTransport create() {
	        return httpTransport;
	      }
	    };
	  }
	
	
	/**
	 * To get the Google Service Account Access Token
	 * 
	 * @param serviceAccountFilePath
	 * @param profile
	 * @return
	 * @throws Exception
	 */
	public String getGoogleAccessToken(File serviceAccountJSON, ServerProfile profile) throws Exception {
		GoogleCredentials credentials;
		try {
			if(profile.getHasProxy()) {
				logger.info("Using the proxy settings to generate the token");
				HttpTransportFactory httpTransportFactory = getHttpTransportFactory(
						profile.getProxyServer(), profile.getProxyPort(), profile.getProxyUsername(), profile.getProxyPassword());
				credentials = GoogleCredentials.fromStream(new FileInputStream(serviceAccountJSON), httpTransportFactory)
						.createScoped("https://www.googleapis.com/auth/cloud-platform");
			}
			else {
				credentials = GoogleCredentials.fromStream(new FileInputStream(serviceAccountJSON))
						.createScoped("https://www.googleapis.com/auth/cloud-platform");
			}
			credentials.refreshIfExpired();
			com.google.auth.oauth2.AccessToken token = credentials.getAccessToken();
			return token.getTokenValue();
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			throw e;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
}
