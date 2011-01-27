package com.xebialabs.deployit.maven;

import org.junit.Test;

/**
 *
 */
public class CleanTestMojo {
	@Test
	public void start() throws Exception {
		CleanMojo mojo = new CleanMojo();
		mojo.execute();
	}
}
