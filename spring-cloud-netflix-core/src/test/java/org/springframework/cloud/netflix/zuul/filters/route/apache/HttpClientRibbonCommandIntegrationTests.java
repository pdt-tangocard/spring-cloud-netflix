/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.springframework.cloud.netflix.zuul.filters.route.apache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.support.ZuulProxyTestBase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = HttpClientRibbonCommandIntegrationTests.TestConfig.class)
@WebIntegrationTest(randomPort = true, value = {
		"zuul.routes.other: /test/**=http://localhost:7777/local",
		"zuul.routes.another: /another/twolevel/**", "zuul.routes.simple: /simple/**" })
@DirtiesContext
public class HttpClientRibbonCommandIntegrationTests extends ZuulProxyTestBase {

	@Override
	protected boolean supportsPatch() {
		return true;
	}

	@Before
	public void init() {
		super.setTestRequestcontext();
	}

	@Test
	public void patchOnSelfViaRibbonRoutingFilter() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/local/1", HttpMethod.PATCH,
				new HttpEntity<>("TestPatch"), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Patched 1!", result.getBody());
	}

	@Test
	public void postOnSelfViaRibbonRoutingFilter() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/local/1", HttpMethod.POST,
				new HttpEntity<>("TestPost"), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted 1!", result.getBody());
	}

	@Test
	public void deleteOnSelfViaRibbonRoutingFilter() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/local/1", HttpMethod.DELETE,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Deleted 1!", result.getBody());
	}

	@Test
	public void ribbonCommandFactoryOverridden() {
		assertTrue("ribbonCommandFactory not a HttpClientRibbonCommandFactory",
				this.ribbonCommandFactory instanceof HttpClientRibbonCommandFactory);
	}
	// Don't use @SpringBootApplication because we don't want to component scan
	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableZuulProxy
	@RibbonClients({
			@RibbonClient(name = "simple", configuration = ZuulProxyTestBase.SimpleRibbonClientConfiguration.class),
			@RibbonClient(name = "another", configuration = ZuulProxyTestBase.AnotherRibbonClientConfiguration.class) })
	static class TestConfig extends ZuulProxyTestBase.AbstractZuulProxyApplication {

		@RequestMapping(value = "/local/{id}", method = RequestMethod.PATCH)
		public String patch(@PathVariable final String id, @RequestBody final String body) {
			return "Patched " + id + "!";
		}

		@Bean
		public RibbonCommandFactory<?> ribbonCommandFactory(
				final SpringClientFactory clientFactory) {
			return new HttpClientRibbonCommandFactory(clientFactory);
		}

		@Bean
		public ZuulProxyTestBase.MyErrorController myErrorController(ErrorAttributes errorAttributes) {
			return new ZuulProxyTestBase.MyErrorController(errorAttributes);
		}
	}
}
