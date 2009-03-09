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
			
			builder.append("</testcase>\n");
		}
		
		builder.append("</testsuite>");
		
		return builder.toString();

	}
	
	
}
