package org.martus.amplifier.common.datasynch;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.martus.common.*;

public class AmplifierSimpleX509TrustManager implements X509TrustManager 
{

	public AmplifierSimpleX509TrustManager() 
	{
		super();
	}

	public void checkClientTrusted(X509Certificate[] chain, String authType)
		throws CertificateException 
	{
	}

	public void checkServerTrusted(X509Certificate[] chain, String authType)
		throws CertificateException 
	{
		if(!authType.equals("RSA"))
			throw new CertificateException("Only RSA supported");
		if(chain.length != 3)
			throw new CertificateException("Need three certificates");
		X509Certificate cert0 = chain[0];
		X509Certificate cert1 = chain[1];
		X509Certificate cert2 = chain[2];
		try 
		{
			cert0.verify(cert0.getPublicKey());
			cert1.verify(cert2.getPublicKey());
			cert2.verify(cert2.getPublicKey());

			PublicKey tryPublicKey = expectedPublicKey;
			if(tryPublicKey == null)
			{
				String certPublicKeyString = MartusSecurity.getKeyString(cert2.getPublicKey());
				String certPublicCode = MartusUtilities.computePublicCode(certPublicKeyString);
				if(expectedPublicCode.equals(certPublicCode))
					tryPublicKey = cert2.getPublicKey();
			}
			cert1.verify(tryPublicKey);
			setExpectedPublicKey(MartusSecurity.getKeyString(tryPublicKey));
		} 
		catch (Exception e) 
		{
			throw new CertificateException(e.toString());
		}
	}

	public X509Certificate[] getAcceptedIssuers() 
	{
		return null;
	}

	public void setExpectedPublicCode(String expectedPublicCodeToUse) 
	{
		expectedPublicCode = expectedPublicCodeToUse;
		expectedPublicKey = null;
	}

	public void setExpectedPublicKey(String expectedPublicKeyToUse) 
	{
		expectedPublicKey = MartusSecurity.extractPublicKey(expectedPublicKeyToUse);
		expectedPublicCode = null;
	}
	
	private PublicKey expectedPublicKey;
	private String expectedPublicCode;

}