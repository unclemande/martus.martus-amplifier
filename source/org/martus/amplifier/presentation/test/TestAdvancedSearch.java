package org.martus.amplifier.presentation.test;

import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;
import org.martus.amplifier.common.MonthFields;
import org.martus.amplifier.presentation.AdvancedSearch;
import org.martus.common.test.TestCaseEnhanced;

public class TestAdvancedSearch extends TestCaseEnhanced
{
	public TestAdvancedSearch(String name)
	{
		super(name);
	}
	
	public void testBasics()
	{
		MockAmplifierRequest request = new MockAmplifierRequest();
		HttpServletResponse response = null;
		Context context = new MockContext();
		
		AdvancedSearch as = new AdvancedSearch();
		String templateName = as.selectTemplate(request, response, context);
		assertEquals("AdvancedSearch.vm", templateName);
	}
	
	public void testMonthFieldsDisplay()
	{
		MockAmplifierRequest request = new MockAmplifierRequest();
		HttpServletResponse response = null;
		Context context = new MockContext();
		
		AdvancedSearch as = new AdvancedSearch();
		String templateName = as.selectTemplate(request, response, context);
		assertEquals("AdvancedSearch.vm", templateName);

		assertEquals(2, MonthFields.getIndexOfMonth("February"));
		assertEquals(12, MonthFields.getIndexOfMonth("December"));		
	}
}