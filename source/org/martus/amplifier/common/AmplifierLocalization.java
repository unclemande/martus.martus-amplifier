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

package org.martus.amplifier.common;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.martus.common.MartusUtilities;
import org.martus.util.UnicodeReader;

public class AmplifierLocalization
{
	public static String getLanguageString(String code)
	{
		File languageFile = getEnglishLanguageTranslationFile();
		HashMap languages = AmplifierLocalization.buildLanguageMap(languageFile);
		if(!languages.containsKey(code))
			return null;
		return (String)languages.get(code);		
	}

	public static File getEnglishLanguageTranslationFile()
	{
		URL url = AmplifierLocalization.class.getResource("LanguageNames_en.txt");
		return new File(url.getFile());
	}

	public static HashMap buildLanguageMap(File languageFile)
	{
		HashMap languages = new HashMap();
		if(!languageFile.exists())
		{
			languages.put(SearchResultConstants.LANGUAGE_ANYLANGUAGE_LABEL, SearchResultConstants.LANGUAGE_ANYLANGUAGE_LABEL);
			return languages;				
		}
		
		try
		{
			UnicodeReader reader = new UnicodeReader(languageFile);
			Vector localizedLanguages = MartusUtilities.loadListFromFile(reader);
			for (Iterator iter = localizedLanguages.iterator(); iter.hasNext();)
			{
				String data = (String) iter.next();
				String[] idAndName = data.split("=");
				languages.put(idAndName[0], idAndName[1]);
			}
			reader.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return languages;
	}

}
