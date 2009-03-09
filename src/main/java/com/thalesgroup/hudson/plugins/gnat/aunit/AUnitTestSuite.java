/*******************************************************************************
* Copyright (c) 2009 Thales Corporate Services SAS                             *
* Author : Gregory Boissinot                                                   *
*                                                                              *
* Permission is hereby granted, free of charge, to any person obtaining a copy *
* of this software and associated documentation files (the "Software"), to deal*
* in the Software without restriction, including without limitation the rights *
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
* copies of the Software, and to permit persons to whom the Software is        *
* furnished to do so, subject to the following conditions:                     *
*                                                                              *
* The above copyright notice and this permission notice shall be included in   *
* all copies or substantial portions of the Software.                          *
*                                                                              *
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
* THE SOFTWARE.                                                                *
*******************************************************************************/

package com.thalesgroup.hudson.plugins.gnat.aunit;

import java.util.ArrayList;
import java.util.List;

public class AUnitTestSuite {

	private int nbErrors;
	
	private int nbFailure;
	
	private int nbTests;
	
	private String name;

	private List<AUnitTestCase> testcases = new ArrayList<AUnitTestCase>();
	
	
	public void addTestCase(AUnitTestCase testcase){
		testcases.add(testcase);
	}
	
	
	public int getNbErrors() {
		return nbErrors;
	}

	public void setNbErrors(int nbErrors) {
		this.nbErrors = nbErrors;
	}

	public int getNbFailure() {
		return nbFailure;
	}

	public void setNbFailure(int nbFailure) {
		this.nbFailure = nbFailure;
	}

	public int getNbTests() {
		return nbTests;
	}

	public void setNbTests(int nbTests) {
		this.nbTests = nbTests;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	public String generateXML(){
		
		StringBuilder builder=new StringBuilder();
		builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		builder.append("<testsuite errors=\"")
			  .append(nbErrors)
			  .append("\" failures=\"")
			  .append(nbFailure)
			  .append("\" tests=\"")
			  .append(nbTests)
			  .append("\" name=\"\">\n");
		
		
		for (AUnitTestCase testcase:testcases){
			builder.append("<testcase classname=\"")
					.append(testcase.getClassname())
					.append("\" name=\"")
					.append(testcase.getName())
				    .append("\" time=\"")
				    .append(testcase.getTime())
				    .append("\">\n");
			
			
			if (testcase.isFailed()){				
				builder.append("<failure message=\"")
				.append(testcase.getFailedMessage())
				.append("\">");

				if (testcase.getFailedBody()!=null)
					builder.append(testcase.getFailedBody());
					
			    builder.append("</failure>\n");				
			}
			
			if (testcase.isError()){				
				builder.append("<error message=\"")
				.append(testcase.getErrorMessage())
				.append("\">");

				if (testcase.getErrorBody()!=null)
					builder.append(testcase.getErrorBody());
					
			    builder.append("</error>\n");				
			}
			
			
			builder.append("</testcase>\n");
		}
		
		builder.append("</testsuite>");
		
		return builder.toString();

	}
	
	
}
