package org.martus.amplifier.service.datasynch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.martus.amplifier.common.configuration.AmplifierConfiguration;
import org.martus.amplifier.common.datasynch.AmplifierBulletinRetrieverGatewayInterface;
import org.martus.amplifier.common.datasynch.AmplifierClientSideNetworkGateway;
import org.martus.amplifier.common.datasynch.AmplifierClientSideNetworkHandlerUsingXMLRPC;
import org.martus.amplifier.common.datasynch.AmplifierNetworkInterface;
import org.martus.amplifier.common.datasynch.AmplifierClientSideNetworkHandlerUsingXMLRPC.SSLSocketSetupException;
import org.martus.amplifier.service.attachment.AttachmentManager;
import org.martus.amplifier.service.attachment.AttachmentStorageException;
import org.martus.amplifier.service.search.BulletinIndexException;
import org.martus.amplifier.service.search.BulletinIndexer;
import org.martus.common.MartusUtilities.ServerErrorException;
import org.martus.common.bulletin.BulletinZipUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.crypto.MartusCrypto.DecryptionException;
import org.martus.common.crypto.MartusCrypto.NoKeyPairException;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.common.packet.UniversalId;
import org.martus.common.packet.Packet.InvalidPacketException;
import org.martus.common.packet.Packet.SignatureVerificationException;
import org.martus.common.packet.Packet.WrongPacketTypeException;
import org.martus.util.Base64.InvalidBase64Exception;

public class AmplifierNetworkGateway implements IDataSynchConstants
{
	
	protected AmplifierNetworkGateway()
	{
		super();
	
		bulletinWorkingDirectory = AmplifierConfiguration.getInstance().getBulletinPath();
		attachmentWorkingDirectory = AmplifierConfiguration.getInstance().getAttachmentPath();
		serverInfoList = BackupServerManager.getInstance().getBackupServersList();
		gateway = getCurrentNetworkInterfaceGateway();
		try
		{
			security = new MartusSecurity();
			security.createKeyPair();
		}
		catch(Exception e)
		{
			logger.severe("CryptoInitialization Exception " + e.getMessage());		
		}
		
	}
	
	public static AmplifierNetworkGateway getInstance()
	{
		if(instance == null)
		instance = new AmplifierNetworkGateway();
		return instance;
	}
	
	
	public Vector getAllAccountIds() //throws ServerErrorException
	{
		Vector result = new Vector();
		try
		{
			NetworkResponse response = gateway.getAccountIds(security);
			String resultCode = response.getResultCode();
			if(!resultCode.equals(NetworkInterfaceConstants.OK))
				throw new ServerErrorException(resultCode);
			result= response.getResultVector();
		}
		catch(IOException e)
		{
			logger.info("No server available");
		}
		catch(Exception e)
		{
			logger.severe("AmplifierNetworkGateway.getAllAccountIds(): Unable to retrieve AccountIds: " + e.getMessage());
		}
		return result;
	}
	
	
	public Vector getAccountUniversalIds(String accountId) //throws ServerErrorException
	{
		Vector result = new Vector();
		try
		{
			NetworkResponse response = gateway.getPublicBulletinUniversalIds(security, accountId);
			String resultCode = response.getResultCode();
			if( !resultCode.equals(NetworkInterfaceConstants.OK) )	
					throw new ServerErrorException(resultCode);
			result = response.getResultVector();		
		}	
		catch(Exception e)
		{
			logger.severe("AmplifierNetworkGateway.getAccountUniversalIds(): unable to retrieve UniversalIds for AccountID = "+accountId);
		}
		return result;
	}
	

	public void retrieveAndManageBulletin(
		UniversalId uid, BulletinExtractor bulletinExtractor) 
		throws WrongPacketTypeException, IOException, DecryptionException, 
			InvalidPacketException, BulletinIndexException, 
			NoKeyPairException, SignatureVerificationException, 
			AttachmentStorageException, InvalidBase64Exception
	{
		File bulletinFile = getBulletin(uid);
		bulletinExtractor.extractAndStoreBulletin(bulletinFile);	
	}
	
	
	public File getBulletin(UniversalId uid)
	{
		File tempFile = null;
		FileOutputStream out = null;
		int chunkSize = NetworkInterfaceConstants.MAX_CHUNK_SIZE;
		int totalLength =0;
		try 
		{
			tempFile = File.createTempFile("$$$TempFile", null);
			tempFile.deleteOnExit();
        	out = new FileOutputStream(tempFile);		
		    try
		 	{	
				totalLength = BulletinZipUtilities.retrieveBulletinZipToStream
									(uid, out, chunkSize, gateway, security, null);
			}
			catch(Exception e)
			{
				logger.severe("Unable to retrieve bulletin: " + e.getMessage());
			}
		out.close();
		}
		catch(FileNotFoundException fe)
		{
			logger.severe("File not found : " + fe.getMessage());	
		}
		catch(IOException ie)
		{
			logger.severe("IO Exception could not create tempfile : " + ie.getMessage());	
		}

		if(tempFile.length() != totalLength)
		{
			System.out.println("file=" + tempFile.length() + ", returned=" + totalLength);
			logger.severe("Error" + new ServerErrorException("totalSize didn't match data length") );
		}
		return tempFile;
	}
	
	public BulletinExtractor createBulletinExtractor(
		AttachmentManager attachmentManager, BulletinIndexer indexer)
	{
		return new BulletinExtractor(attachmentManager, indexer, security);
	}
	
	
//methods copied/modified from MartusAPP	
	private AmplifierClientSideNetworkGateway getCurrentNetworkInterfaceGateway()
	{
		if(currentNetworkInterfaceGateway == null)
		{
			currentNetworkInterfaceGateway = new AmplifierClientSideNetworkGateway(getCurrentNetworkInterfaceHandler());
		}
		
		return currentNetworkInterfaceGateway;
	}
	
	private AmplifierNetworkInterface getCurrentNetworkInterfaceHandler()
	{
		if(currentNetworkInterfaceHandler == null)
		{
			currentNetworkInterfaceHandler = createXmlRpcNetworkInterfaceHandler();
		}

		return currentNetworkInterfaceHandler;
	}

	private AmplifierNetworkInterface createXmlRpcNetworkInterfaceHandler() 
	{
		int index = 0;
		BackupServerInfo serverInfo = (BackupServerInfo) serverInfoList.get(index);
		String ourServer = serverInfo.getName();
//		int ourPort = NetworkInterfaceXmlRpcConstants.MARTUS_PORT_FOR_SSL;
		int ourPort = serverInfo.getPort();
		try 
		{
			AmplifierClientSideNetworkHandlerUsingXMLRPC handler = new AmplifierClientSideNetworkHandlerUsingXMLRPC(ourServer, ourPort);
		//	handler.getSimpleX509TrustManager().setExpectedPublicKey(getConfigInfo().getServerPublicKey());
		    handler.getSimpleX509TrustManager().setExpectedPublicKey(serverInfo.getServerPublicKey());
			return handler;
		} 
		catch (SSLSocketSetupException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	private static AmplifierNetworkGateway instance = null;
	private AmplifierBulletinRetrieverGatewayInterface gateway;
	private MartusCrypto security;
	private Logger logger = Logger.getLogger(DATASYNC_LOGGER);
	private List serverInfoList = null;
	private String bulletinWorkingDirectory = "";
	private String attachmentWorkingDirectory= "";
	private AmplifierNetworkInterface currentNetworkInterfaceHandler = null;
	private AmplifierClientSideNetworkGateway currentNetworkInterfaceGateway = null;
	
	
}
