/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2001-2003, Beneficent
Technology, Inc. (Benetech).

Martus is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later
version with the additions and exceptions described in the
accompanying Martus license file entitled "license.txt".

It is distributed WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, including warranties of fitness of purpose or
merchantability.  See the accompanying Martus License and
GPL license for more details on the required license terms
for this software.

You should have received a copy of the GNU General Public
License along with this program; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA 02111-1307, USA.

*/

package org.martus.amplifier.lucene.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.martus.amplifier.attachment.FileSystemAttachmentManager;
import org.martus.amplifier.common.DateUtilities;
import org.martus.amplifier.common.SearchParameters;
import org.martus.amplifier.common.SearchResultConstants;
import org.martus.amplifier.main.MartusAmplifier;
import org.martus.amplifier.presentation.SearchResults;
import org.martus.amplifier.search.AttachmentInfo;
import org.martus.amplifier.search.BulletinField;
import org.martus.amplifier.search.BulletinIndexException;
import org.martus.amplifier.search.BulletinIndexer;
import org.martus.amplifier.search.BulletinInfo;
import org.martus.amplifier.search.BulletinSearcher;
import org.martus.amplifier.search.Results;
import org.martus.amplifier.search.SearchConstants;
import org.martus.amplifier.test.AbstractAmplifierTestCase;
import org.martus.common.FieldSpec;
import org.martus.common.bulletin.AttachmentProxy;
import org.martus.common.crypto.MockMartusSecurity;
import org.martus.common.packet.FieldDataPacket;
import org.martus.common.packet.UniversalId;
import org.martus.util.DirectoryTreeRemover;
import org.martus.util.MartusFlexidate;

public abstract class AbstractSearchTestCase 
	extends AbstractAmplifierTestCase implements SearchConstants, SearchResultConstants
{
	protected AbstractSearchTestCase(String name) 
	{
		super(name);
	}

	public void setUp() throws Exception
	{
		super.setUp();
		MartusAmplifier.security = new MockMartusSecurity();
		MartusAmplifier.security.createKeyPair();
		MartusAmplifier.attachmentManager = new FileSystemAttachmentManager(getTestBasePath());
	}
	
	public void tearDown() throws Exception
	{
		super.tearDown();
		MartusAmplifier.attachmentManager.clearAllAttachments();
		DirectoryTreeRemover.deleteEntireDirectoryTree(new File(basePath));
	}
	
	public void testClearIndex() throws Exception
	{
		UniversalId bulletinId = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp = generateFieldDataPacket(bulletinId);
		BulletinIndexer indexer = openBulletinIndexer();
		try {
			indexer.indexFieldData(bulletinId, fdp);
		} finally {
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		try {
			Assert.assertNotNull(
				"Didn't find indexed bulletin", 
				searcher.lookup(bulletinId));
		} finally {
			searcher.close();
		}
		
		indexer = openBulletinIndexer();
		try {
			indexer.clearIndex();
		} finally {
			indexer.close();
		}
		
		searcher = openBulletinSearcher();
		try {
			Assert.assertNull(
				"Found an indexed bulletin after clearing!", 
				searcher.lookup(bulletinId));
		} finally {
			searcher.close();
		} 		
	}
	
	public void testFindBulletin() throws Exception
	{
		UniversalId bulletinId = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp = generateFieldDataPacket(bulletinId);
		BulletinIndexer indexer = openBulletinIndexer();
		try {
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId, fdp);
		} finally {
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		try {
			Assert.assertNotNull(
				"Didn't find indexed bulletin", 
				searcher.lookup(bulletinId));
		} finally {
			searcher.close();
		}
	}
	
	public void testIndexAndSearch() throws Exception
	{
		UniversalId bulletinId = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp = generateSampleData(bulletinId);		
		BulletinIndexer indexer = openBulletinIndexer();
		try {
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId, fdp);
		} finally {
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		try {
			BulletinInfo found = searcher.lookup(bulletinId);
			Assert.assertNotNull("Didn't find indexed bulletin", found);
				
			HashMap fields = new HashMap();			
			fields.put(RESULT_BASIC_QUERY_KEY, fdp.get(SEARCH_AUTHOR_INDEX_FIELD) );
			Assert.assertEquals(1, searcher.search(fields).getCount());
			
			fields = new HashMap();	
			fields.put(RESULT_BASIC_QUERY_KEY, fdp.get(SEARCH_KEYWORDS_INDEX_FIELD));
			Assert.assertEquals(1,searcher.search(fields).getCount());
			
			fields = new HashMap();
			fields.put(RESULT_BASIC_QUERY_KEY, fdp.get(SEARCH_DETAILS_INDEX_FIELD));											
			Assert.assertEquals(1, searcher.search(fields).getCount());
		} finally {
			searcher.close();
		}
		
	}
	
	public void testReconstructFieldDataPacket()  throws Exception
	{
		UniversalId bulletinId = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp = generateSampleData(bulletinId);		
		BulletinIndexer indexer = openBulletinIndexer();
		try {
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId, fdp);
		} finally {
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		try {
			BulletinInfo found = searcher.lookup(bulletinId);
			Assert.assertNotNull(
				"Didn't find indexed bulletin", 
				found);
			
			AttachmentProxy[] origProxies = fdp.getAttachments();
			List foundAttachments = found.getAttachments();
			Assert.assertEquals(
				origProxies.length, foundAttachments.size());
			for (int i = 0; i < origProxies.length; i++) {
				Assert.assertEquals(
					origProxies[i].getUniversalId().getLocalId(), 
					((AttachmentInfo) foundAttachments.get(i)).getLocalId());	
				Assert.assertEquals(
					origProxies[i].getLabel(), 
					((AttachmentInfo) foundAttachments.get(i)).getLabel());
			}
			
			Assert.assertEquals(
				bulletinId, found.getBulletinId());
			Collection fields = BulletinField.getSearchableFields();
			for (Iterator iter = fields.iterator(); iter.hasNext();) 
			{
				BulletinField field = (BulletinField) iter.next();
				if(field.isDateRangeField())
					Assert.assertEquals(
						fdp.get(field.getXmlId()), 
						found.get(field.getIndexId()+"-start"));
					
				else
					Assert.assertEquals(
						fdp.get(field.getXmlId()), 
						found.get(field.getIndexId()));
			}
		} 
		finally 
		{
			searcher.close();
		}
	}
	
	public void testInterleavedAccess()  throws Exception
	{
		BulletinIndexer indexer = null;
		BulletinSearcher searcher = null;
		BulletinIndexException closeException = null;
		
		try {
			indexer = openBulletinIndexer();
			searcher = openBulletinSearcher();
		} finally {
			if (indexer != null) {
				try {
					indexer.close();
				} catch (BulletinIndexException e) {
					closeException = e;
				}
			}
			if (searcher != null) {
				try {
					searcher.close();
				} catch (BulletinIndexException e) {
					closeException = e;
				}
			}
		}
		if (closeException != null) {
			throw closeException;
		}
			
		
	}
	
	public void testSearchResultsAfterClose() throws Exception
	{
		UniversalId bulletinId 	= UniversalId.createDummyUniversalId();
		FieldDataPacket fdp 	= generateSampleData(bulletinId);		
		BulletinIndexer indexer = openBulletinIndexer();
		try {
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId, fdp);
		} finally {
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;
		try {
			HashMap fields = new HashMap();			
			fields.put(RESULT_BASIC_QUERY_KEY, fdp.get(SEARCH_AUTHOR_INDEX_FIELD) );
			results = searcher.search(fields);
		} finally {
			searcher.close();
		}
		
		try {
			results.getBulletinInfo(0);
			Assert.fail(
				"Accessing results after closing searcher should have failed.");
		} catch (BulletinIndexException expected) {
		}
	}
	
	public void testSearchAllFields() throws Exception
	{
		UniversalId bulletinId 	= UniversalId.createDummyUniversalId();
		FieldDataPacket fdp 	= generateSampleData(bulletinId);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId, fdp);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;
		try 
		{
			HashMap fields = new HashMap();
			fields.put(SEARCH_AUTHOR_INDEX_FIELD, fdp.get(BulletinField.SEARCH_AUTHOR_INDEX_FIELD));				
			fields.put(SEARCH_DETAILS_INDEX_FIELD, fdp.get(BulletinField.SEARCH_DETAILS_INDEX_FIELD));
			fields.put(SEARCH_KEYWORDS_INDEX_FIELD, fdp.get(BulletinField.SEARCH_KEYWORDS_INDEX_FIELD));
			fields.put(SEARCH_LOCATION_INDEX_FIELD, fdp.get(BulletinField.SEARCH_LOCATION_INDEX_FIELD));
			fields.put(SEARCH_SUMMARY_INDEX_FIELD, fdp.get(BulletinField.SEARCH_SUMMARY_INDEX_FIELD));
			fields.put(SEARCH_TITLE_INDEX_FIELD, fdp.get(BulletinField.SEARCH_TITLE_INDEX_FIELD));
			
			fields.put(RESULT_BASIC_QUERY_KEY, fdp.get(BulletinField.SEARCH_AUTHOR_INDEX_FIELD));								
			results = searcher.search(fields);							
			assertEquals("Should have found a result for author", 1, results.getCount());
			
			fields.remove(RESULT_BASIC_QUERY_KEY);
			fields.put(RESULT_BASIC_QUERY_KEY, fdp.get(SEARCH_DETAILS_INDEX_FIELD));								
			results = searcher.search( fields);							
			assertEquals("Should have found a result for details", 1, results.getCount());
						
			fields.remove(RESULT_BASIC_QUERY_KEY);
			fields.put(RESULT_BASIC_QUERY_KEY, fdp.get(SEARCH_KEYWORDS_INDEX_FIELD));								
			results = searcher.search( fields);	
			assertEquals("Should have found a result for keyword", 1, results.getCount());
			
			fields.remove(RESULT_BASIC_QUERY_KEY);
			fields.put(RESULT_BASIC_QUERY_KEY, fdp.get(SEARCH_LOCATION_INDEX_FIELD));								
			results = searcher.search(fields);				
			assertEquals("Should have found a result for location", 1, results.getCount());
			
			
			fields.remove(RESULT_BASIC_QUERY_KEY);
			fields.put(RESULT_BASIC_QUERY_KEY, fdp.get(SEARCH_SUMMARY_INDEX_FIELD));								
			results = searcher.search( fields);				
			assertEquals("Should have found a result for summary", 1, results.getCount());
			
			fields.remove(RESULT_BASIC_QUERY_KEY);
			fields.put(RESULT_BASIC_QUERY_KEY, fdp.get(SEARCH_TITLE_INDEX_FIELD));								
			results = searcher.search( fields);		
			assertEquals("Should have found a result for title", 1, results.getCount());
			
			
			fields.remove(RESULT_BASIC_QUERY_KEY);
			fields.put(RESULT_BASIC_QUERY_KEY, "Lunch");								
			results = searcher.search( fields);			
			assertEquals("Should have found a result for the word Lunch", 1, results.getCount());
			
			fields.remove(RESULT_BASIC_QUERY_KEY);
			fields.put(RESULT_BASIC_QUERY_KEY, "Luch");								
			results = searcher.search( fields);	
			assertEquals("Should not have found a result for a word 'Luch' not in the bulletin", 0, results.getCount());
			
		} 
		finally 
		{
			searcher.close();
		}
	}

	public void testSearchForStopWords() throws Exception
	{
		UniversalId bulletinId 	= UniversalId.createDummyUniversalId();
		FieldDataPacket fdp 	= generateSampleData(bulletinId);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId, fdp);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;
		try 
		{			
			HashMap fields = new HashMap();
			fields.put(SEARCH_AUTHOR_INDEX_FIELD, fdp.get(BulletinField.SEARCH_AUTHOR_INDEX_FIELD));				
			fields.put(SEARCH_DETAILS_INDEX_FIELD, fdp.get(BulletinField.SEARCH_DETAILS_INDEX_FIELD));
			fields.put(SEARCH_KEYWORDS_INDEX_FIELD, fdp.get(BulletinField.SEARCH_KEYWORDS_INDEX_FIELD));
			fields.put(SEARCH_LOCATION_INDEX_FIELD, fdp.get(BulletinField.SEARCH_LOCATION_INDEX_FIELD));
			fields.put(SEARCH_SUMMARY_INDEX_FIELD, fdp.get(BulletinField.SEARCH_SUMMARY_INDEX_FIELD));
			fields.put(SEARCH_TITLE_INDEX_FIELD, fdp.get(BulletinField.SEARCH_TITLE_INDEX_FIELD));
		
			fields.put(RESULT_BASIC_QUERY_KEY, "for");								
					
			results = searcher.search(fields);
			assertEquals("Should have found 1 result for stopword 'for'", 1, results.getCount());
		} 
		finally 
		{
			searcher.close();
		}
	}

	public void testSearchForWildCards() throws Exception
	{
		UniversalId bulletinId1 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp1 	= generateSampleData(bulletinId1);		
		UniversalId bulletinId2 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp2 	= generateSampleFlexiData(bulletinId2);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId1, fdp1);
			indexer.indexFieldData(bulletinId2, fdp2);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;
		try 
		{
			HashMap fields = new HashMap();
			fields.put(SEARCH_AUTHOR_INDEX_FIELD, fdp1.get(BulletinField.SEARCH_AUTHOR_INDEX_FIELD));				
			fields.put(SEARCH_DETAILS_INDEX_FIELD, fdp1.get(BulletinField.SEARCH_DETAILS_INDEX_FIELD));
			fields.put(SEARCH_KEYWORDS_INDEX_FIELD, fdp1.get(BulletinField.SEARCH_KEYWORDS_INDEX_FIELD));
			fields.put(SEARCH_LOCATION_INDEX_FIELD, fdp1.get(BulletinField.SEARCH_LOCATION_INDEX_FIELD));
			fields.put(SEARCH_SUMMARY_INDEX_FIELD, fdp1.get(BulletinField.SEARCH_SUMMARY_INDEX_FIELD));
			fields.put(SEARCH_TITLE_INDEX_FIELD, fdp1.get(BulletinField.SEARCH_TITLE_INDEX_FIELD));
		
			fields.put(RESULT_BASIC_QUERY_KEY, "lun??");						
			results = searcher.search(fields);
			assertEquals("Should have found 2 result lun??", 2, results.getCount());
			
			
			fields.remove(RESULT_BASIC_QUERY_KEY);
			fields.put(RESULT_BASIC_QUERY_KEY, "sal*");	
			results = searcher.search(fields);
			assertEquals("Should have found 2 result sal* salad and salad2", 2, results.getCount());
			
			
			fields.remove(RESULT_BASIC_QUERY_KEY);
			fields.put(RESULT_BASIC_QUERY_KEY, "sa?ad");	
			results = searcher.search(fields);		
			assertEquals("Should have found 1 result sa?ad just salad", 1 , results.getCount());
			
			
/*			results = searcher.search(null, "");
			assertEquals("Should have found 2 result for nothing entered", 2, results.getCount());
			results = searcher.search(null, null);
			assertEquals("Should have found 2 result for null entered", 2, results.getCount());
			results = searcher.search(null, "*");
			assertEquals("Should have found 2 result for * entered", 2, results.getCount());
			results = searcher.search(null, "?");
			assertEquals("Should have found 2 result for ? entered", 2, results.getCount());
*/		}
		finally 
		{
			searcher.close();
		}
	}

	public void testSearchForLanguageReturned() throws Exception
	{
		UniversalId bulletinId1 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp1 	= generateSampleData(bulletinId1);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId1, fdp1);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;
		try 
		{
			HashMap fields = new HashMap();
			fields.put(SEARCH_KEYWORDS_INDEX_FIELD, fdp1.get(BulletinField.SEARCH_KEYWORDS_INDEX_FIELD));
			fields.put(RESULT_BASIC_QUERY_KEY, "ate");						
			results = searcher.search(fields);
			assertEquals("Should have found 1 result en", 1, results.getCount());
			BulletinInfo info = results.getBulletinInfo(0);
			assertEquals("The Language returned not correct?", "en", info.get(SEARCH_LANGUAGE_INDEX_FIELD));
		}
		finally 
		{
			searcher.close();
		}
	}

	public void testSearchEmptyField() throws Exception
	{
		UniversalId bulletinId 	= UniversalId.createDummyUniversalId();
		FieldDataPacket fdp 	= generateSampleFlexiData(bulletinId);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId, fdp);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;
		try 
		{
			HashMap fields = new HashMap();
			fields.put(SEARCH_AUTHOR_INDEX_FIELD, fdp.get(BulletinField.SEARCH_AUTHOR_INDEX_FIELD));				
			fields.put(SEARCH_DETAILS_INDEX_FIELD, fdp.get(BulletinField.SEARCH_DETAILS_INDEX_FIELD));
			fields.put(SEARCH_KEYWORDS_INDEX_FIELD, fdp.get(BulletinField.SEARCH_KEYWORDS_INDEX_FIELD));
			fields.put(SEARCH_LOCATION_INDEX_FIELD, fdp.get(BulletinField.SEARCH_LOCATION_INDEX_FIELD));
			fields.put(SEARCH_SUMMARY_INDEX_FIELD, fdp.get(BulletinField.SEARCH_SUMMARY_INDEX_FIELD));
			fields.put(SEARCH_TITLE_INDEX_FIELD, fdp.get(BulletinField.SEARCH_TITLE_INDEX_FIELD));
		
			fields.put(RESULT_BASIC_QUERY_KEY, "Chuck");		
			
			results = searcher.search(fields);
			assertEquals("Should have found 1 result Chuck", 1, results.getCount());
			
			
			BulletinInfo info =results.getBulletinInfo(0);
			assertNotNull("Bulletin Info null?", info);
			assertNotNull("Sumary should not be null",info.get(SEARCH_SUMMARY_INDEX_FIELD));
			assertNotNull("Location should not be  null",info.get(SEARCH_LOCATION_INDEX_FIELD));
			assertEquals("Location should be ''", "", info.get(SEARCH_LOCATION_INDEX_FIELD));
		}
		finally 
		{
			searcher.close();
		}
	}
	
	public void testLuceneSearchQueries() throws Exception
	{
		UniversalId bulletinId1 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp1 	= generateSampleData(bulletinId1);		
		UniversalId bulletinId2 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp2 	= generateSampleFlexiData(bulletinId2);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId1, fdp1);
			indexer.indexFieldData(bulletinId2, fdp2);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;				
		
		try 
		{		
			String query = "-(lunch)"+ " AND \"What's for\"";
			assertEquals("-(lunch) AND \"What's for\"", query);	
			results = searcher.search(SEARCH_TITLE_INDEX_FIELD, query);
			assertEquals("Combine without these words and exactphrase? ", 0, results.getCount());	
			
			query = "+(lunch)"+ " AND \"What's for\"";
			assertEquals("+(lunch) AND \"What's for\"", query);	
			results = searcher.search(SEARCH_TITLE_INDEX_FIELD, query);
			assertEquals("Combine witt these words and exactphrase? ", 1, results.getCount());	
		}
		finally 
		{
			searcher.close();
		}
	}
	
	public void testAdvancedSearchEventDateOnly() throws Exception
	{
		UniversalId bulletinId1 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp1 	= generateSampleData(bulletinId1);		
		UniversalId bulletinId2 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp2 	= generateSampleFlexiData(bulletinId2);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId1, fdp1);
			indexer.indexFieldData(bulletinId2, fdp2);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;				
		
		try 
		{
			String startDate 	= "2003-08-01";
			String endDate 	= "2003-08-25";
		
			HashMap fields = new HashMap();
			fields.put(BulletinField.SEARCH_EVENT_START_DATE_INDEX_FIELD, startDate);
			fields.put(BulletinField.SEARCH_EVENT_END_DATE_INDEX_FIELD, endDate);
			
			results = searcher.search(fields);
			assertEquals("Should have found 1 match? ", 1, results.getCount());			
		}
		finally 
		{
			searcher.close();
		}
	}	
	
	public void testAdvancedSearchCombineEventDateAndBulletineField() throws Exception
	{
		UniversalId bulletinId1 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp1 = generateSampleData(bulletinId1);		
		UniversalId bulletinId2 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp2 = generateSampleFlexiData(bulletinId2);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId1, fdp1);
			indexer.indexFieldData(bulletinId2, fdp2);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;				
		
		try 
		{
			String startDate 	= "2003-08-01";
			String endDate 	= "2003-08-22";
		
			HashMap fields = new HashMap();
			fields.put(BulletinField.SEARCH_EVENT_START_DATE_INDEX_FIELD, startDate);
			fields.put(BulletinField.SEARCH_EVENT_END_DATE_INDEX_FIELD, endDate);
			fields.put(SearchResultConstants.RESULT_FIELDS_KEY, BulletinField.SEARCH_TITLE_INDEX_FIELD);
			fields.put(ANYWORD_TAG, "lunch");
			
			results = searcher.search(fields);
			assertEquals("Combine search for eventdate and field? ", 1, results.getCount());
		}
		finally 
		{
			searcher.close();
		}
	}
	
	public void testAdvancedSearchCombineEventDateAndEntryDate() throws Exception
	{
		UniversalId bulletinId1 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp1 	= generateSampleData(bulletinId1);		
		UniversalId bulletinId2 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp2 	= generateSampleFlexiData(bulletinId2);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId1, fdp1);
			indexer.indexFieldData(bulletinId2, fdp2);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;				
		
		try 
		{
			String startDate		= "2003-08-01";
			String endDate 		= "2003-08-22";
			String defaultDate 	= "1970-01-01";			
			String entryStartDate = "2003-05-22";
			String nearToday = "2003-10-03";		
		
			HashMap fields = new HashMap();
			fields.put(BulletinField.SEARCH_EVENT_START_DATE_INDEX_FIELD, defaultDate);
			fields.put(BulletinField.SEARCH_EVENT_END_DATE_INDEX_FIELD, nearToday);
			fields.put(BulletinField.SEARCH_ENTRY_DATE_INDEX_FIELD, entryStartDate);
			
			results = searcher.search(fields);
			assertEquals("search for entry date only? ", 1, results.getCount());
			
			fields.remove(SEARCH_EVENT_START_DATE_INDEX_FIELD);
			fields.remove(SEARCH_EVENT_END_DATE_INDEX_FIELD);
			fields.put(BulletinField.SEARCH_EVENT_START_DATE_INDEX_FIELD, startDate);
			fields.put(BulletinField.SEARCH_EVENT_END_DATE_INDEX_FIELD, endDate);			
			
			results = searcher.search(fields);
			assertEquals("Combine search for eventdate and entry date? ", 1, results.getCount());
		}
		finally 
		{
			searcher.close();
		}
	}
	
	public void testAdvancedSearchCombineEventDateAndBulletineFieldAndLanguage() throws Exception
	{
		UniversalId bulletinId1 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp1 	= generateSampleData(bulletinId1);		
		UniversalId bulletinId2 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp2 	= generateSampleFlexiData(bulletinId2);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId1, fdp1);
			indexer.indexFieldData(bulletinId2, fdp2);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;				
		
		try 
		{
			String startDate 			= "2003-08-01";
			String endDate 			= "2003-08-22";			
			String defaultStartDate	= "1970-01-01";
			String defaultEndDate		= "2003-09-24";
				
			HashMap fields = new HashMap();
			fields.put(BulletinField.SEARCH_EVENT_START_DATE_INDEX_FIELD, defaultStartDate);
			fields.put(BulletinField.SEARCH_EVENT_END_DATE_INDEX_FIELD, defaultEndDate);		
		
			fields.put(BulletinField.SEARCH_LANGUAGE_INDEX_FIELD, "es");		
			results = searcher.search(fields);			
			assertEquals("search laguage with default event date? ", 1, results.getCount());
				
			fields = new HashMap();			
			fields.put(BulletinField.SEARCH_EVENT_START_DATE_INDEX_FIELD, startDate);
			fields.put(BulletinField.SEARCH_EVENT_END_DATE_INDEX_FIELD, endDate);
			fields.put(SearchResultConstants.RESULT_FIELDS_KEY, BulletinField.SEARCH_TITLE_INDEX_FIELD);
			fields.put(BulletinField.SEARCH_LANGUAGE_INDEX_FIELD, "en");
			fields.put(ANYWORD_TAG, "lunch");
			
			results = searcher.search(fields);
			assertEquals("Combine search for eventdate, field, and laguage? ", 0, results.getCount());
			
			fields.remove(SEARCH_LANGUAGE_INDEX_FIELD);
			fields.put(BulletinField.SEARCH_LANGUAGE_INDEX_FIELD, "fr");
			results = searcher.search(fields);
			assertEquals("Combine search for eventdate, bulletin field, and language (not match)? ", 0, results.getCount());			
						
		}
		finally 
		{
			searcher.close();
		}
	}
	
	public void testAdvancedSearchCombineEventDateAndBulletineFieldAndLanguageAndEntryDate() throws Exception
	{
		UniversalId bulletinId1 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp1 = generateSampleData(bulletinId1);		
		UniversalId bulletinId2 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp2 = generateSampleFlexiData(bulletinId2);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId1, fdp1);
			indexer.indexFieldData(bulletinId2, fdp2);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;				
		
		try 
		{
			String startDate 		= "2003-08-01";
			String endDate 			= "2003-08-22";			
			String defaultStartDate = "1970-01-01";		
			String todayDate 		= "2003-09-24";
			
			String pastWeek 	= SearchParameters.getEntryDate(ENTRY_PAST_WEEK_DAYS_TAG);
			String pastMonth 	= SearchParameters.getEntryDate(ENTRY_PAST_MONTH_DAYS_TAG);
			String past3Month = SearchParameters.getEntryDate(ENTRY_PAST_3_MONTH_DAYS_TAG);
			String past6Month = SearchParameters.getEntryDate(ENTRY_PAST_6_MONTH_DAYS_TAG);
			String pastYear 	= SearchParameters.getEntryDate(ENTRY_PAST_YEAR_DAYS_TAG);
		
			HashMap fields = new HashMap();
			fields.put(BulletinField.SEARCH_EVENT_START_DATE_INDEX_FIELD, defaultStartDate);
			fields.put(BulletinField.SEARCH_EVENT_END_DATE_INDEX_FIELD, todayDate);		
			
			//2003-05-11 and 2003-08-30
			fields.put(BulletinField.SEARCH_ENTRY_DATE_INDEX_FIELD, pastWeek);		
			results = searcher.search(fields);			
			assertEquals("search for entry date submitted in past 1 week? ", 0, results.getCount());
						
			fields.remove(SEARCH_ENTRY_DATE_INDEX_FIELD);
			fields.put(SEARCH_ENTRY_DATE_INDEX_FIELD, pastMonth);
			results = searcher.search(fields);			
			assertEquals("search for entry date submitted in past 1 month? ", 1, results.getCount());
			
			fields.remove(SEARCH_ENTRY_DATE_INDEX_FIELD);
			fields.put(SEARCH_ENTRY_DATE_INDEX_FIELD, past3Month);
			results = searcher.search(fields);			
			assertEquals("search for entry date submitted in past 3 month? ", 1, results.getCount());
			
			fields.remove(SEARCH_ENTRY_DATE_INDEX_FIELD);
			fields.put(SEARCH_ENTRY_DATE_INDEX_FIELD, past6Month);
			results = searcher.search(fields);			
			assertEquals("search for entry date submitted in past 6 month? ", 2, results.getCount());
			
			fields.remove(SEARCH_ENTRY_DATE_INDEX_FIELD);
			fields.put(SEARCH_ENTRY_DATE_INDEX_FIELD, pastYear);
			results = searcher.search(fields);			
			assertEquals("search for entry date submitted in past 1 year? ", 2, results.getCount());
			
									
			fields = new HashMap();			
			fields.put(BulletinField.SEARCH_EVENT_START_DATE_INDEX_FIELD, startDate);
			fields.put(BulletinField.SEARCH_EVENT_END_DATE_INDEX_FIELD, endDate);
			fields.put(SearchResultConstants.RESULT_FIELDS_KEY, BulletinField.SEARCH_TITLE_INDEX_FIELD);			
			fields.put(BulletinField.SEARCH_LANGUAGE_INDEX_FIELD, "es");
			fields.put(SEARCH_ENTRY_DATE_INDEX_FIELD, past3Month);
			fields.put(ANYWORD_TAG, "lunch");
			
			results = searcher.search(fields);
			assertEquals("Combine search for eventdate, field, laguage, and event date? ", 1, results.getCount());
								
		}
		finally 
		{
			searcher.close();
		}
	}
	
	public void testAdvancedSearchCombineEventDateAndFilterWords() throws Exception
	{
		UniversalId bulletinId1 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp1 	= generateSampleData(bulletinId1);		
		UniversalId bulletinId2 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp2 	= generateSampleFlexiData(bulletinId2);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId1, fdp1);
			indexer.indexFieldData(bulletinId2, fdp2);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;				
		
		try 
		{
			String defaultDate 	= "1970-01-01";
			String defaultEndDate = "2004-01-01";			
		
			HashMap fields = new HashMap();
			fields.put(BulletinField.SEARCH_EVENT_START_DATE_INDEX_FIELD, defaultDate);
			fields.put(BulletinField.SEARCH_EVENT_END_DATE_INDEX_FIELD, defaultEndDate);
			fields.put(RESULT_FIELDS_KEY, IN_ALL_FIELDS);
			
			String query = SearchParameters.convertToQueryString("root sandwich", THESE_WORD_TAG);						
			fields.put(THESE_WORD_TAG, query);
			results = searcher.search(fields);
			assertEquals("search for all of these words? ", 2, results.getCount());
						
			query = SearchParameters.convertToQueryString("Paul", THESE_WORD_TAG);	
			clear4Fields(fields);
			fields.put(THESE_WORD_TAG, query);
			results = searcher.search(fields);
			assertEquals("search for all of these words? ", 1, results.getCount());
										
			query = SearchParameters.convertToQueryString("egg salad sandwich", EXACTPHRASE_TAG);
			clear4Fields(fields);		
			fields.put(EXACTPHRASE_TAG, query);			
			results = searcher.search(fields);
			assertEquals("search for exact phrase? ", 1, results.getCount());
						
			clear4Fields(fields);
			query = SearchParameters.convertToQueryString("for lunch.", EXACTPHRASE_TAG);		
			fields.put(EXACTPHRASE_TAG, query);
			
			results = searcher.search(fields);
			assertEquals("search for exact phrase? ", 2, results.getCount());
			
			clear4Fields(fields);				
			query = SearchParameters.convertToQueryString("salad2", WITHOUTWORDS_TAG);			
			fields.put(WITHOUTWORDS_TAG, query);			
			results = searcher.search(fields);
//			assertEquals("search for without of those words? ", 1, results.getCount());
			
			clear4Fields(fields);
			query = SearchParameters.convertToQueryString("Paul", WITHOUTWORDS_TAG);			
			fields.put(WITHOUTWORDS_TAG, query);			
			results = searcher.search(fields);
//			assertEquals("search for without of those words? ", 1, results.getCount());
										
		}
		finally 
		{
			searcher.close();
		}
	}
	
	public void test4FieldsQuery() throws Exception
	{
		UniversalId bulletinId1 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp1 	= generateSampleData(bulletinId1);		
		UniversalId bulletinId2 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp2 	= generateSampleFlexiData(bulletinId2);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId1, fdp1);
			indexer.indexFieldData(bulletinId2, fdp2);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;				
		
		try 
		{
			String defaultDate 	= "1970-01-01";
			String defaultEndDate = "2004-01-01";			
		
			HashMap fields = new HashMap();
			fields.put(BulletinField.SEARCH_EVENT_START_DATE_INDEX_FIELD, defaultDate);
			fields.put(BulletinField.SEARCH_EVENT_END_DATE_INDEX_FIELD, defaultEndDate);
			fields.put(RESULT_FIELDS_KEY, IN_ALL_FIELDS);
			
			//combined these words and exactphrase
			String query = SearchParameters.convertToQueryString("root sandwich", THESE_WORD_TAG);						
			fields.put(THESE_WORD_TAG, query);		
						
			query = SearchParameters.convertToQueryString("Paul", EXACTPHRASE_TAG);				
			fields.put(EXACTPHRASE_TAG, query);
			
			results = searcher.search(fields);
			assertEquals("search for these words and exactphrase? ", 1, results.getCount());		
			
			clear4Fields(fields);
			//test again with all match
			query = SearchParameters.convertToQueryString("root sandwich", THESE_WORD_TAG);						
			fields.put(THESE_WORD_TAG, query);		
						
			query = SearchParameters.convertToQueryString("Today", EXACTPHRASE_TAG);				
			fields.put(EXACTPHRASE_TAG, query);
			
			results = searcher.search(fields);
			assertEquals("search for these words and exactphrase? ", 2, results.getCount());											
										
		}
		finally 
		{
			searcher.close();
		}
	}
	
	private void clear4Fields(HashMap fields)
	{
		fields.remove(ANYWORD_TAG);
		fields.remove(EXACTPHRASE_TAG);	
		fields.remove(THESE_WORD_TAG);
		fields.remove(WITHOUTWORDS_TAG);			
	}
	
	public void testAdvancedSearchSortByTitle() throws Exception
	{
		UniversalId bulletinId1 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp1 	= generateSampleData(bulletinId1);		
		UniversalId bulletinId2 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp2 	= generateSampleFlexiData(bulletinId2);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId1, fdp1);
			indexer.indexFieldData(bulletinId2, fdp2);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;				
		
		try 
		{
			String defaultDate 	= "1970-01-01";
			String defaultEndDate = "2004-01-01";
			
			HashMap fields = new HashMap();			
			fields.put(BulletinField.SEARCH_EVENT_START_DATE_INDEX_FIELD, defaultDate);
			fields.put(BulletinField.SEARCH_EVENT_END_DATE_INDEX_FIELD, defaultEndDate);
			fields.put(RESULT_FIELDS_KEY, IN_ALL_FIELDS);
			fields.put(RESULT_SORTBY_KEY, SEARCH_TITLE_INDEX_FIELD);			
			fields.put(ANYWORD_TAG, "lunch");			
			
			results = searcher.search(fields);
			assertEquals("Should have found 2 matches? ", 2, results.getCount());
			
			int count = results.getCount();												
			ArrayList list = new ArrayList();
			for (int i = 0; i < count; i++)
			{
				BulletinInfo bulletin = results.getBulletinInfo(i);					
				list.add(bulletin);
			}

			SearchResults.sortBulletins(list, SEARCH_TITLE_INDEX_FIELD);
			
			String title1 = ((BulletinInfo)list.get(0)).get(SEARCH_TITLE_INDEX_FIELD);
			String title2 = ((BulletinInfo)list.get(1)).get(SEARCH_TITLE_INDEX_FIELD);
						
			assertEquals(fdp2.get(BulletinField.SEARCH_TITLE_INDEX_FIELD), title1);
			assertEquals(fdp1.get(BulletinField.SEARCH_TITLE_INDEX_FIELD), title2);
												
		}
		finally 
		{
			searcher.close();
		}
	}				
		
	public void testAdvancedSearchSortByEventDate() throws Exception
	{
		UniversalId bulletinId1 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp1 	= generateSampleData(bulletinId1);		
		UniversalId bulletinId2 = UniversalId.createDummyUniversalId();
		FieldDataPacket fdp2 	= generateSampleFlexiData(bulletinId2);		
		BulletinIndexer indexer = openBulletinIndexer();
		try 
		{
			indexer.clearIndex();
			indexer.indexFieldData(bulletinId2, fdp2);
			indexer.indexFieldData(bulletinId1, fdp1);
		} 
		finally 
		{
			indexer.close();
		}
		
		BulletinSearcher searcher = openBulletinSearcher();
		Results results = null;				
		
		try 
		{
			String defaultDate 	= "1970-01-01";
			String defaultEndDate = "2004-01-01";
			
			HashMap fields = new HashMap();			
			fields.put(BulletinField.SEARCH_EVENT_START_DATE_INDEX_FIELD, defaultDate);
			fields.put(BulletinField.SEARCH_EVENT_END_DATE_INDEX_FIELD, defaultEndDate);
			fields.put(RESULT_FIELDS_KEY, IN_ALL_FIELDS);
			fields.put(RESULT_SORTBY_KEY, SEARCH_EVENT_DATE_INDEX_FIELD);			
			fields.put(ANYWORD_TAG, "lunch");			
			
			results = searcher.search(fields);
			assertEquals("Should have found 2 matches? ", 2, results.getCount());
			
			int count = results.getCount();												
			ArrayList list = new ArrayList();
			for (int i = 0; i < count; i++)
			{
				BulletinInfo bulletin = results.getBulletinInfo(i);					
				list.add(bulletin);
			}
			
			SearchResults.sortBulletins(list, SEARCH_EVENT_DATE_INDEX_FIELD);
		
			String eventStartDate1 = ((BulletinInfo)list.get(0)).get(SEARCH_EVENT_DATE_INDEX_FIELD+"-start");
			String eventStartDate2 = ((BulletinInfo)list.get(1)).get(SEARCH_EVENT_DATE_INDEX_FIELD+"-start");
						
			assertEquals(fdp1.get(BulletinField.SEARCH_EVENT_DATE_INDEX_FIELD), eventStartDate1);
			String startDate2 = DateUtilities.getStartDateRange(fdp2.get(BulletinField.SEARCH_EVENT_DATE_INDEX_FIELD));
			assertEquals(startDate2, eventStartDate2);
												
		}
		finally 
		{
			searcher.close();
		}
	}				

	protected FieldDataPacket generateSampleData(UniversalId bulletinId)
	{
		String author = "Paul";
		String keyword = "ate";
		String keywords = keyword + " egg salad root beer";
		String title = "ZZZ for Lunch?";
		String eventdate = "2003-04-10";
		String entrydate = "2003-05-11";
		String publicInfo = "menu";
		String language = "en";
		String organization = "test sample";
		String summary = 
			"Today Paul ate an egg salad sandwich and a root beer " +
			"for lunch.";
		String location = "San Francisco, CA";
		
		String attachment1LocalId = "att1Id";
		String attachment1Label = "Eggs.gif";
		String attachment2LocalId = "att2Id";
		String attachment2Label = "Recipe.txt";
		
		FieldDataPacket fdp = createFieldDataPacket(bulletinId, author, keywords, title, eventdate, entrydate, publicInfo, summary, location, attachment1LocalId, attachment1Label, attachment2LocalId, attachment2Label, language, organization);
		return fdp;
	}

	protected FieldDataPacket generateSampleFlexiData(UniversalId bulletinId)
	{
		String author = "Chuck";	
		String keywords = "2003-08-20";
		String title = "What's for Lunch?";
		long tenDaysOfMillis = 10*24*60*60*1000L;
		Date tenDaysAgo = new Date(System.currentTimeMillis() - tenDaysOfMillis);
		String entrydate= MartusFlexidate.toStoredDateFormat(tenDaysAgo);
		String eventdate = "2003-08-20,20030820+3";
		String publicInfo = "menu3";
		String language = "es";
		String organization = "test complex";
		String summary = 
			"Today Chuck ate an egg2 salad2 sandwich and a root beer2 " +
			"for lunch.";
		//String location = "San Francisco, CA";
		
		String attachment1LocalId = "att1Id";
		String attachment1Label = "Eggs.gif";
		String attachment2LocalId = "att2Id";
		String attachment2Label = "Recipe.txt";
		
		FieldDataPacket fdp = createFieldDataPacket(bulletinId, author, keywords, title, eventdate, entrydate, publicInfo, summary, null, attachment1LocalId, attachment1Label, attachment2LocalId, attachment2Label, language, organization);
		return fdp;
	}
	
	private FieldDataPacket createFieldDataPacket(UniversalId bulletinId, String author, String keywords, String title, String eventdate, String entrydate, String publicInfo, String summary, String location, String attachment1LocalId, String attachment1Label, String attachment2LocalId, String attachment2Label, String language, String organization)
	{
		FieldDataPacket fdp = generateFieldDataPacket(
			bulletinId, new String[] { 
				SEARCH_AUTHOR_INDEX_FIELD, author, 
				SEARCH_KEYWORDS_INDEX_FIELD, keywords, 
				SEARCH_TITLE_INDEX_FIELD, title,
				SEARCH_ENTRY_DATE_INDEX_FIELD, entrydate, 
				SEARCH_EVENT_DATE_INDEX_FIELD, eventdate,
				SEARCH_DETAILS_INDEX_FIELD, publicInfo, 
				SEARCH_SUMMARY_INDEX_FIELD, summary,
				SEARCH_LOCATION_INDEX_FIELD, location,
				SEARCH_LANGUAGE_INDEX_FIELD, language,
				SEARCH_ORGANIZATION_INDEX_FIELD, organization
			}, new String[] {
				attachment1LocalId, attachment1Label, 
				attachment2LocalId, attachment2Label
			});
		return fdp;
	}

	protected FieldDataPacket generateFieldDataPacket(UniversalId bulletinId)
	{
		return generateFieldDataPacket(bulletinId, new String[0]);
	}
	
	protected FieldDataPacket generateFieldDataPacket(
		UniversalId bulletinId, String[] fieldsAssocList)
	{
		return generateFieldDataPacket(
			bulletinId, fieldsAssocList, new String[0]);
	}
	
	
	protected FieldDataPacket generateFieldDataPacket(
		UniversalId bulletinId, String[] fieldsAssocList,
		String[] attachmentsAssocList)
	{
		FieldSpec[] fieldSpecs = BulletinField.getDefaultSearchFieldSpecs();
		UniversalId fieldUid = UniversalId.createFromAccountAndLocalId(
			bulletinId.getAccountId(), "TestField");
		
		FieldDataPacket fdp = new FieldDataPacket(fieldUid, fieldSpecs);
		Assert.assertEquals(
			"Uneven assoc list: " + Arrays.asList(fieldsAssocList), 
			0, fieldsAssocList.length % 2);
		for (int i = 0; i < fieldsAssocList.length; i += 2) {
			fdp.set(fieldsAssocList[i], fieldsAssocList[i + 1]);
		}
		Assert.assertEquals(
			"Uneven assoc list: " + Arrays.asList(attachmentsAssocList), 
			0, attachmentsAssocList.length % 2);
		for (int i = 0; i < attachmentsAssocList.length; i += 2) {
			fdp.addAttachment(new AttachmentProxy(
				UniversalId.createFromAccountAndLocalId(
					bulletinId.getAccountId(), attachmentsAssocList[i]),
				attachmentsAssocList[i + 1],
				null));
		}
					
		return fdp;
	}
	
	protected abstract BulletinIndexer openBulletinIndexer()
		throws BulletinIndexException;
	protected abstract BulletinSearcher openBulletinSearcher() throws Exception;
}