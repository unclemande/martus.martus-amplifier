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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.servlet.VelocityServlet;

abstract public class AmplifierServlet extends VelocityServlet
{
	/**
	 *   Called by the VelocityServlet
	 *   init().  We want to set a set of properties
	 *   so that templates will be found in the webapp
	 *   root.  This makes this easier to work with as 
	 *   an example, so a new user doesn't have to worry
	 *   about config issues when first figuring things
	 *   out
	 */
	protected Properties loadConfiguration(ServletConfig config )
		throws IOException, FileNotFoundException
	{
		Properties p = new Properties();

		/*
		 *  first, we set the template path for the
		 *  FileResourceLoader to the root of the 
		 *  webapp.  This probably won't work under
		 *  in a WAR under WebLogic, but should 
		 *  under tomcat :)
		 */

		String path = config.getServletContext().getRealPath("/");

		if (path == null)
		{
			System.out.println(" SampleServlet.loadConfiguration() : unable to " 
							   + "get the current webapp root.  Using '/'. Please fix.");

			path = "/";
		}

		p.setProperty( Velocity.FILE_RESOURCE_LOADER_PATH,  path );

		/**
		 *  and the same for the log file
		 */

		p.setProperty( "runtime.log", path + "velocity.log" );

		return p;
	}

	abstract public String selectTemplate(AmplifierServletRequest request,
			HttpServletResponse response, Context context) throws Exception;
			
			
	public Template handleRequest(HttpServletRequest request,
								HttpServletResponse response, 
								Context context)
	{
		try
		{
			AmplifierServletRequest ampRequest = new WrappedServletRequest(request);
			String templateName = selectTemplate(ampRequest, response, context);
			return getTemplate(templateName);
		}
		catch( ParseErrorException e )
		{
			displayError("parse error for template", e);
		}
		catch( ResourceNotFoundException e )
		{
			displayError("template not found", e);
		}
		catch( Exception e )
		{
			displayError("Unknown error", e);
		}
		return null;
	}
    
	protected void displayError(String message, Exception e)
	{
		System.out.println(getClass().getName() + ": " + message + " " + e);
	}


}