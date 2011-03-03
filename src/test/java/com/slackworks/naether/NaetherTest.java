package com.slackworks.naether;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
   *
 * http://www.apache.org/licenses/LICENSE-2.0
   *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Java SE
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

// JUnit
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

// Sonatype Aether
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import com.slackworks.naether.Bootstrap;
import com.slackworks.naether.MavenProject;
import com.slackworks.naether.Naether;
import com.slackworks.naether.Notation;


/**
 * Test for {@link Naether}
 * 
 * @author Michael Guymon
 *
 */
public class NaetherTest {
	
	private static Logger log = LoggerFactory.getLogger(NaetherTest.class);
	private Naether naether;
	
	@Before
	public void createNaether() {
		naether = new Naether();
		naether.setLocalRepoPath( "target/test-repo" );
		
		File dir = new File("target/test-repo");
		if ( dir.exists() ) {
			log.info( "Removing test maven repo: target/test-repo" );
			dir.delete();
		}
	}

	@Test
	public void addRemoteRepository() throws MalformedURLException {
		assertEquals( "central", naether.getRemoteRepositories().get(0).getId() );
		
		naether.addRemoteRepository( "http://test.net/hamster:7011" );
		assertEquals( "test.net-hamster-7011", naether.getRemoteRepositories().get(1).getId() );
		
		naether.addRemoteRepository( "test-id", "test-type", "http://test.net" );
		assertEquals( "test-id", naether.getRemoteRepositories().get(2).getId() );
	}
	
	@Test
	public void getDependenciesNotation() {
		Dependency dependency =
            new Dependency( new DefaultArtifact( "junit:junit:jar:4.8.2" ), "compile" );
        naether.addDependency(dependency);
        
        assertEquals( "junit:junit:jar:4.8.2", naether.getDependenciesNotation().get(0) );
	}
	
	@Test
	public void resolveDepedenciesAndDownloadArtifacts() throws Exception {
		Dependency dependency =
            new Dependency( new DefaultArtifact( "junit:junit:jar:4.8.2" ), "compile" );
        naether.addDependency(dependency);
        naether.resolveDependencies();
        String classpath = (new File( "target/test-repo/junit/junit/4.8.2/junit-4.8.2.jar")).getAbsolutePath();
        assertEquals( classpath, naether.getResolvedClassPath() );
        assertTrue( (new File( classpath ).exists()) );
	}
	
	@Test
	public void resolveDepedenciesWithoutDownloadingArtifacts() throws Exception {
		Dependency dependency =
            new Dependency( new DefaultArtifact( "org.testng:testng:jar:5.14" ), "compile" );
        naether.addDependency(dependency);
        naether.resolveDependencies(false);
        assertEquals( "", naether.getResolvedClassPath() );
        
        String jarPath = "target/test-repo/org.testng/testng/5.14/testng-5.14.jar";
        assertFalse( (new File( jarPath ).exists()) );
	}
	
	@Test
	public void resolveNaetherDependencies() throws Exception {
		MavenProject mavenProject = new MavenProject("pom.xml");
		for( org.apache.maven.model.Dependency mavenDep : mavenProject.getDependencies() ) {
			String notation = Notation.generate( mavenDep );
			
			Dependency dependency =
	            new Dependency( new DefaultArtifact( notation ), mavenDep.getScope() );
			naether.addDependency( dependency );
		}
		
		naether.resolveDependencies();
		
		for( Dependency dependency : naether.getDependencies() ) {
			String notation = Notation.generate( dependency );
			log.debug( "Dependency: {} {}", notation, dependency.getScope() );
		}
		
		List<String> completeDeps = Bootstrap.dependencies;
		
		// Jars excluded from bootstrap dependencies
		completeDeps.add( "junit:junit:jar:3.8.2");
		completeDeps.add( "log4j:log4j:jar:1.2.12");
		completeDeps.add( "commons-logging:commons-logging-api:jar:1.1");
		completeDeps.add( "commons-logging:commons-logging:jar:1.1.1");
		completeDeps.add( "nekohtml:xercesMinimal:jar:1.9.6.2" );
		completeDeps.add( "nekohtml:nekohtml:jar:1.9.6.2" );
		
		List<String> missingDeps = new ArrayList<String>();
        for( String dep : naether.getDependenciesNotation() ) {
        	if ( completeDeps.indexOf( dep ) == -1 ) {
        		missingDeps.add( dep );
        	}
        }
        
        if ( missingDeps.size() > 0 ) {
        	fail( "Missing Dependencies: " + missingDeps );
        }
		
	}
	
	@Test
	public void resolveArtifactsNotInMainRepo() throws Exception {
		Dependency dependency =
            new Dependency( new DefaultArtifact( "org.springframework:org.springframework.orm:3.0.5.RELEASE" ), "compile" );
		naether.addRemoteRepository( "http://repository.springsource.com/maven/bundles/release" );
		naether.addRemoteRepository( "http://repository.springsource.com/maven/bundles/external" );
		naether.addDependency(dependency);
        naether.resolveDependencies(false);
        
        List<String> results = new ArrayList<String>();
        results.add("org.springframework:org.springframework.orm:jar:3.0.5.RELEASE");
        results.add("org.springframework:org.springframework.beans:jar:3.0.5.RELEASE");
        results.add("org.springframework:org.springframework.asm:jar:3.0.5.RELEASE");
        results.add("org.springframework:org.springframework.core:jar:3.0.5.RELEASE");
        results.add("org.springframework:org.springframework.jdbc:jar:3.0.5.RELEASE");
        results.add("org.springframework:org.springframework.transaction:jar:3.0.5.RELEASE");
        results.add("org.aopalliance:com.springsource.org.aopalliance:jar:1.0.0");
        results.add("org.springframework:org.springframework.aop:jar:3.0.5.RELEASE");
        results.add("org.springframework:org.springframework.context:jar:3.0.5.RELEASE");
        results.add("org.springframework:org.springframework.expression:jar:3.0.5.RELEASE");
        		
        assertEquals( results, naether.getDependenciesNotation() );
	}

}