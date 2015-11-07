/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springside.examples.bootservice.functional;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;
import org.springside.examples.bootapi.BootApiApplication;
import org.springside.examples.bootapi.dto.BookDto;
import org.springside.modules.test.data.RandomData;

import com.google.common.collect.Maps;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BootApiApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
@DirtiesContext
public class BookEndpointTest {

	@Value("${local.server.port}")
	private int port;

	private RestTemplate restTemplate;
	private String resourceUrl;
	private String loginUrl;
	private String logoutUrl;

	@Before
	public void setup() {
		restTemplate = new TestRestTemplate();
		resourceUrl = "http://localhost:" + port + "/api/books";
		loginUrl = "http://localhost:" + port + "/api/accounts/login";
		logoutUrl = "http://localhost:" + port + "/api/accounts/logout";
	}

	@Test
	public void listBook() {
		BookList tasks = restTemplate.getForObject(resourceUrl, BookList.class);
		assertThat(tasks).hasSize(3);
		BookDto firstBook = tasks.get(0);

		assertThat(firstBook.title).isEqualTo("Big Data日知录");
		assertThat(firstBook.owner.name).isEqualTo("Calvin");
	}

	@Test
	public void getTask() {
		BookDto book = restTemplate.getForObject(resourceUrl + "/{id}", BookDto.class, 1L);
		assertThat(book.title).isEqualTo("Big Data日知录");
		assertThat(book.owner.name).isEqualTo("Calvin");
	}

	@Test
	public void applyRequest() {
		String token = login();
		HttpEntity request = buildRequest(token);

		ResponseEntity response = restTemplate.exchange(resourceUrl + "/{id}/request", HttpMethod.GET, request,
				String.class, 3L);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		logout(token);

	}

	@Test
	public void applyRequestError() {
		// 未设置token
		ResponseEntity response = restTemplate.getForEntity(resourceUrl + "/{id}/request", String.class, 1L);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		// 设置错误token
		HttpEntity request = buildRequest("abc");
		response = restTemplate.exchange(resourceUrl + "/{id}/request", HttpMethod.GET, request, String.class, 1L);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		// 自己借自己的书
		String token = login();
		request = buildRequest(token);

		response = restTemplate.exchange(resourceUrl + "/{id}/request", HttpMethod.GET, request, String.class, 1L);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	private String login() {
		Map<String, String> map = Maps.newHashMap();
		map.put("email", "calvin.xiao@vipshop.com");
		map.put("password", "springside");

		ResponseEntity<String> response = restTemplate.getForEntity(loginUrl + "?email={email}&password={password}",
				String.class, "calvin.xiao@vipshop.com", "springside");
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		return response.getBody();
	}

	private HttpEntity buildRequest(String token) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.add("token", token);
		HttpEntity request = new HttpEntity(requestHeaders);
		return request;
	}

	public void logout(String token) {
		HttpEntity request = buildRequest(token);
		restTemplate.exchange(logoutUrl, HttpMethod.GET, request, String.class);
	}

	private static BookDto randomBook() {
		BookDto book = new BookDto();
		book.title = RandomData.randomName("Book");

		return book;
	}

	// ArrayList<Task>在RestTemplate转换时不好表示，创建一个类来表达它是最简单的。
	private static class BookList extends ArrayList<BookDto> {
	}

}