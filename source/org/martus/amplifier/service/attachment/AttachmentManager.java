package org.martus.amplifier.service.attachment;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Logger;

import org.martus.amplifier.common.bulletin.UniversalBulletinId;

import com.sleepycat.db.Db;
import com.sleepycat.db.DbException;
import com.sleepycat.db.Dbc;

public class AttachmentManager implements IAttachmentConstants
{
	protected AttachmentManager()
	{
		super();
	}
	
	public static AttachmentManager getInstance()
	{
		return instance;
	}
	
	public void createDatabase()
	{
        // Remove the previous database.
        new File(DATABASE_FILENAME).delete();
		try
        {
        	table = new Db(null, 0);
        	table.set_error_stream(System.err);
        	table.set_errpfx("AttachmentManager");
        	table.open(null, DATABASE_FILENAME, null, Db.DB_BTREE, Db.DB_CREATE, 0644);		
        }
        catch(DbException de)
        {
        	logger.severe("Unable to create attachment database: " + de.getMessage());
        }
        catch(FileNotFoundException fnfe)
        {
            logger.severe("Unable to create find attachment database file: " + fnfe.getMessage());
        }
        
	}
	
	public void putString(UniversalBulletinId universalId, String attachmentName)
	{
		UniversalIdDbt key = new UniversalIdDbt(universalId);
		StringDbt value = new StringDbt(attachmentName);
		int error = 0;
		try
		{
			error = table.put(null, key, value, Db.DB_NOOVERWRITE);
		}
		catch(DbException de)
		{
			logger.severe("Unable to add attachment to database: " + de.getMessage());
		}
	}

	public String getString(UniversalBulletinId UniversalBulletinId)
	{
		UniversalIdDbt key = new UniversalIdDbt(UniversalBulletinId);
		StringDbt returnValue = new StringDbt();
		File result = null;
		int error = 0;
		try
		{
			error = table.get(null, key, returnValue, 0);
			if(error != 0)
				throw new DbException("Problems finding attachment.");
		}
		catch(DbException de)
		{
			logger.severe("Unable to get attachment from database: " + de);
		}
		return returnValue.getString();
	}

	public void putAttachment(UniversalBulletinId universalId, File attachment)
	{
		UniversalIdDbt key = new UniversalIdDbt(universalId);
		AttachmentDbt value = new AttachmentDbt(attachment);
		int error = 0;
		try
		{
			error = table.put(null, key, value, Db.DB_NOOVERWRITE);
		}
		catch(DbException de)
		{
			logger.severe("Unable to add attachment to database: " + de.getMessage());
		}
	}
	
	public File getAttachment(UniversalBulletinId UniversalBulletinId)
	{
		UniversalIdDbt key = new UniversalIdDbt(UniversalBulletinId);
		AttachmentDbt returnValue = new AttachmentDbt();
		File result = null;
		int error = 0;
		try
		{
			error = table.get(null, key, returnValue, 0);
			if(error != 0)
				throw new DbException("Problems finding attachment.");
		}
		catch(DbException de)
		{
			logger.severe("Unable to get attachment from database: " + de);
		}
		return returnValue.getFile();
	}
		
	private static AttachmentManager instance = new AttachmentManager();
	private Db table =  null;
	private static final String DATABASE_FILENAME = "attachments.db";
	private Logger logger = Logger.getLogger(ATTACHMENT_LOGGER);
}
