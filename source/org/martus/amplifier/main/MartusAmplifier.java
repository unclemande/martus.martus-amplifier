package org.martus.amplifier.main;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Logger;

import org.martus.amplifier.attachment.AttachmentManager;
import org.martus.amplifier.attachment.AttachmentStorageException;
import org.martus.amplifier.attachment.FileSystemAttachmentManager;
import org.martus.amplifier.common.AmplifierConfiguration;
import org.martus.amplifier.datasynch.BackupServerInfo;
import org.martus.amplifier.datasynch.DataSynchManager;
import org.martus.amplifier.lucene.LuceneBulletinIndexer;
import org.martus.amplifier.network.AmplifierClientSideNetworkHandlerUsingXMLRPC.SSLSocketSetupException;
import org.martus.amplifier.search.BulletinIndexException;
import org.martus.amplifier.search.BulletinIndexer;
import org.martus.common.LoggerInterface;
import org.martus.common.LoggerToConsole;
import org.martus.common.MartusUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.mortbay.http.HttpContext;
import org.mortbay.jetty.Server;

public class MartusAmplifier
{
	public static void main(String[] args) throws Exception
	{
		MartusAmplifier amp = new MartusAmplifier(new LoggerToConsole());
		amp.start();
	}
	
	public MartusAmplifier(LoggerInterface loggerToUse)
	{
		logger = loggerToUse;
	}

	void start() throws Exception
	{
		deleteLuceneLockFile();
		
		security = new MartusSecurity();
		log("Creating key pair...");
		security.createKeyPair();
		
		File configDirectory = new File(AmplifierConfiguration.getInstance().getBasePath());
		File backupServersDirectory = new File(configDirectory, "serversWhoWeCall");
		backupServersList = loadServersWeWillCall(backupServersDirectory, security);
		
		//Code.setDebug(true);
		Server server = new Server("jettyConfiguration.xml");
		server.addWebApplication("/","presentation/");
		
		addPasswordAuthentication(server);
		
		server.start();
		timer.scheduleAtFixedRate(timedTask, IMMEDIATELY, dataSynchIntervalMillis);
		
		while(! isShutdownRequested() )
		{
		}
	}

	void deleteLuceneLockFile()
	{
		File indexDirectory = new File(AmplifierConfiguration.getInstance().getBasePath(), "index");
		File lockFile = new File(indexDirectory, "write.lock");
		if(lockFile.exists())
		{
			log("Deleting lucene lock file: " + lockFile.getPath());
			lockFile.delete();
		}
	}

	void addPasswordAuthentication(Server server)
	{
		PasswordAuthenticationHandler handler = new PasswordAuthenticationHandler();
		HttpContext context = server.getContext("/");
		context.addHandler(handler);
	}
	
	
	
	boolean isShutdownRequested()
	{
		File ampDir = getWorkingDirectory();
		File shutdownFile = new File(ampDir, "shutdown");
		boolean doShutdown = false;
		if(shutdownFile.exists() && ! isAmplifierSyncing() )
		{
			shutdownFile.delete();
			doShutdown = true;
		}
		return doShutdown;
	}
	
	MartusSecurity getSecurity()
	{
		return security;
	}

	private File getWorkingDirectory()
	{
		return new File(AmplifierConfiguration.getInstance().getWorkingPath());
	}
	
	public boolean isAmplifierSyncing()
	{
		return isSyncing;
	}
	
	public void startSynch()
	{
		isSyncing = true;
	}
	
	public void endSynch()
	{
		isSyncing = false;
	}

	public void pullNewBulletinsFromServers(List backupServersList) 
	{
		Logger fancyLogger = Logger.getLogger("MainTask");
		
		if(backupServersList.size() == 0)
			return;
				
		BulletinIndexer indexer = null;
		AttachmentManager attachmentManager = null;
		try
		{
			DataSynchManager dataManager = new DataSynchManager(backupServersList, getSecurity());
			AmplifierConfiguration config = 
				AmplifierConfiguration.getInstance();
			indexer = new LuceneBulletinIndexer(
				config.getBasePath());
			attachmentManager = new FileSystemAttachmentManager(
				config.getBasePath());
	
			dataManager.getAllNewBulletins(attachmentManager, indexer);
		}
		catch(Exception e)
		{
			fancyLogger.severe("MartusAmplifierDataSynch.execute(): " + e.getMessage());
			e.printStackTrace();
		} 
		finally
		{
			if (indexer != null) {
				try {
					indexer.close();
				} catch (BulletinIndexException e) {
					fancyLogger.severe(
						"Unable to close the indexer: " + e.getMessage());
				}
			}
			
			if (attachmentManager != null) {
				try {
					attachmentManager.close();
				} catch (AttachmentStorageException e) {
					fancyLogger.severe(
						"Unable to close the attachment manager: " +
						e.getMessage());
				}
			}
		}
	}

	public List loadServersWeWillCall(File directory, MartusCrypto security) throws 
			IOException, MartusUtilities.InvalidPublicKeyFileException, MartusUtilities.PublicInformationInvalidException, SSLSocketSetupException
	{
		List serversWeWillCall = new Vector();
	
		File[] toCallFiles = directory.listFiles();
		if(toCallFiles != null)
		{
			for (int i = 0; i < toCallFiles.length; i++)
			{
				File toCallFile = toCallFiles[i];
				if(!toCallFile.isDirectory())
				{
					serversWeWillCall.add(getServerToCall(toCallFile, security));
					log("We will call: " + toCallFile.getName());
				}
			}
		}

		if(serversWeWillCall.size() > 1)
		{
			log("ERROR: Can only call one server. Aborting.");
			System.exit(55);
		}
		log("Configured to call " + serversWeWillCall.size() + " servers");
		return serversWeWillCall;
	}

	BackupServerInfo getServerToCall(File publicKeyFile, MartusCrypto security) throws
			IOException, 
			MartusUtilities.InvalidPublicKeyFileException, 
			MartusUtilities.PublicInformationInvalidException, 
			SSLSocketSetupException
	{
		String ip = MartusUtilities.extractIpFromFileName(publicKeyFile.getName());
		int port = 985;
		Vector publicInfo = MartusUtilities.importServerPublicKeyFromFile(publicKeyFile, security);
		String publicKey = (String)publicInfo.get(0);
	
		return new BackupServerInfo(ip, ip, port, publicKey);		
	}
	
	void log(String message)
	{
		logger.log(message);
	}
	
	class UpdateFromServerTask extends TimerTask
	{	
		public void run()
		{
			if(! isAmplifierSyncing() )
			{
				startSynch();
				//System.out.println("Scheduled Task started " + System.currentTimeMillis());

				pullNewBulletinsFromServers(backupServersList);
				
				//System.out.println("Scheduled Task finished " + System.currentTimeMillis() + "\n");
				endSynch();
			}
		}
	}

	MartusSecurity security;
	static final long IMMEDIATELY = 0;
	static final long dataSynchIntervalMillis = 100000;

	Timer timer = new Timer(true);
	TimerTask timedTask = new UpdateFromServerTask();
	boolean isSyncing;
	
	List backupServersList;
	
	LoggerInterface logger;
}
