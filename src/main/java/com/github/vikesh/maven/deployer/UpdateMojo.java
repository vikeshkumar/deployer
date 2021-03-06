package com.github.vikesh.maven.deployer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * 
 * @author Vikesh
 */
@Mojo(name = "update", defaultPhase = LifecyclePhase.DEPLOY)
public class UpdateMojo extends AbstractMojo {

	public static final String TOMCAT6 = "tomcat6";
	public static final String TOMCAT7 = "tomcat7";
	public static final String HTTP = "http";
	public static final String HTTPS = "https";
	@Parameter(required = false, alias = "proxyPort")
	private int proxyPort = 80;
	@Parameter(required = false, alias = "proxyHost")
	private String proxyHost = null;
	@Parameter(required = false, alias = "tomcatURL", defaultValue = "127.0.0.1")
	private String tomcatURL;
	@Parameter(required = false, alias = "tomcatVersion", defaultValue = TOMCAT6)
	private String tomcatVersion;
	@Parameter(required = false, alias = "tomcatPort")
	private int tomcatPort = 8080;
	@Parameter(required = false, alias = "scheme", defaultValue = HTTP)
	private String scheme;
	@Parameter(required = true, alias = "scriptUser")
	private String scriptUser;
	@Parameter(required = true, alias = "scriptPassword")
	private String scriptPass;
	@Parameter(defaultValue = "${project.basedir}", alias = "projectDirectory")
	private File root;
	@Parameter(required = false, alias = "warFile", defaultValue = "target/${project.build.finalName}.war")
	private String warFile;
	@Parameter(defaultValue = "${project}")
	private org.apache.maven.project.MavenProject project;
	@Parameter(required = false, defaultValue = "${project.artifactId}")
	private String appName;

	public void execute() throws MojoExecutionException, MojoFailureException {
		// Using DefaultHttpClient to make any request to remote tomcat6/tomcat7
		DefaultHttpClient client = null;
		try {
			client = new DefaultHttpClient();
			HttpHost proxy;
			if (proxyHost != null) {
				proxy = new HttpHost(proxyHost, proxyPort);
				client.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY,
						proxy);
			}
			// Create target host for update, deployment. The default scheme is
			// HTTP
			HttpHost target = new HttpHost(tomcatURL, tomcatPort, scheme);
			// Configure HTTP Client with script username and password
			client.getCredentialsProvider().setCredentials(
					new AuthScope(target.getHostName(), target.getPort()),
					new UsernamePasswordCredentials(scriptUser, scriptPass));
			AuthCache authCache = new BasicAuthCache();

			// Tomcat uses digestAuth scheme for authentication on
			// manager-script role.
			DigestScheme digestAuth = new DigestScheme();
			digestAuth.overrideParamter("algorithm", "SHA");
			digestAuth.overrideParamter("realm", tomcatURL);
			digestAuth.overrideParamter("nonce",
					Long.toString(new Random().nextLong(), 36));
			digestAuth.overrideParamter("qop", "auth");
			digestAuth.overrideParamter("nc", "0");
			digestAuth.overrideParamter("cnonce", DigestScheme.createCnonce());
			authCache.put(target, digestAuth);

			// Create BasicHttpContext
			BasicHttpContext localcontext = new BasicHttpContext();
			localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
			// Specify war file that needs to be uploaded. The default is
			// /target/<generatedWarFile>
			File war = new File(root, warFile);
			System.out.println("WarFile: " + war.getAbsolutePath());
			System.out.println("Project: " + project.getArtifactId());
			System.out.println("Application Name: " + appName);
			// Create a get request for undeployment
			HttpGet get = null;
			// If version of tomcat is 6 then GET path is /manager/undeploy, for
			// tomcat 7 it is /manager/text/undeploy
			if (tomcatVersion.equalsIgnoreCase(TOMCAT6)) {
				get = new HttpGet("/manager/undeploy?path=/" + appName);
			} else if (tomcatVersion.equalsIgnoreCase(TOMCAT7)) {
				get = new HttpGet("/manager/text/undeploy?path=/" + appName);
			}
			if (war.exists() && get != null) {
				try {
					// Execute GET request to undeploy the application
					HttpResponse response = client.execute(target, get,
							localcontext);
					HttpEntity responseEntity = response.getEntity();
					InputStream is = responseEntity.getContent();
					int read;
					while ((read = is.read()) != -1) {
						System.out.print((char) read);
					}
					is.close();
				} catch (IOException ex) {
					Logger.getLogger(DeployerMojo.class.getName()).log(
							Level.SEVERE, ex.getMessage(), ex);
					throw new MojoFailureException(
							"Undeploying application failed");
				}
			} else {
				System.out.println("File not found");
				throw new MojoFailureException("Undeploying application failed");
			}
			// Make a PUT request with war file as content.
			HttpPut put = null;
			if (tomcatVersion.equalsIgnoreCase(TOMCAT6)) {
				put = new HttpPut("/manager/deploy?path=/" + appName);
			} else if (tomcatVersion.equalsIgnoreCase(TOMCAT7)) {
				put = new HttpPut("/manager/text/deploy?path=/" + appName);
			}
			if (war.exists() && put != null) {
				// MultipartEntity for PUT request to update the application
				MultipartEntity mulEntity = new MultipartEntity(
						HttpMultipartMode.STRICT);
				FileBody bin = new FileBody(war);
				mulEntity.addPart("file", bin);
				put.setEntity(mulEntity);
				try {
					HttpResponse response = client.execute(target, put,
							localcontext);
					HttpEntity responseEntity = response.getEntity();
					InputStream is = responseEntity.getContent();
					int read;
					// Print the response to console.
					while ((read = is.read()) != -1) {
						System.out.print((char) read);
					}
					is.close();
				} catch (IOException ex) {
					Logger.getLogger(DeployerMojo.class.getName()).log(
							Level.SEVERE, ex.getMessage(), ex);
					throw new MojoFailureException(
							"Undeploying application failed");
				}
			} else {
				System.out.println("File not found");
			}
		} finally {
			if (client != null) {
				client.getConnectionManager().shutdown();
			}
		}
	}
}
