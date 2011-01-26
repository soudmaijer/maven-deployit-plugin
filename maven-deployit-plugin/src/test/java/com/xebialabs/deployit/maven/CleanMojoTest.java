package com.xebialabs.deployit.maven;

import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: bmoussaud
 * Date: 24/01/11
 * Time: 18:11
 * To change this template use File | Settings | File Templates.
 */
public class CleanMojoTest {
	@Test
	public void start() throws Exception {
		CleanMojo mojo = new CleanMojo();
		mojo.execute();
	}
}
