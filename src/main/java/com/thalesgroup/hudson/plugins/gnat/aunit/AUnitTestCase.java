package com.thalesgroup.hudson.plugins.gnat.aunit;

public class AUnitTestCase {

	private String classname;
	
	private String name;
	
	private int time;

	private boolean failed;
	
	private String failedMessage;
	
	private String failedBody;
	
	public String getClassname() {
		return classname;
	}

	public void setClassname(String classname) {
		this.classname = classname;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public boolean isFailed() {
		return failed;
	}

	public void setFailed(boolean failed) {
		this.failed = failed;
	}

	public String getFailedMessage() {
		return failedMessage;
	}

	public void setFailedMessage(String failedMessage) {
		this.failedMessage = failedMessage;
	}

	public String getFailedBody() {
		return failedBody;
	}

	public void setFailedBody(String failedBody) {
		this.failedBody = failedBody;
	}
	
	
	
}
