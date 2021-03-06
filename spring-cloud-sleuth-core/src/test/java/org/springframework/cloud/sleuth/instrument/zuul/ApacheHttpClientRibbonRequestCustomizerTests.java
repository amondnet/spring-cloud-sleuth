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
 */

package org.springframework.cloud.sleuth.instrument.zuul;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class ApacheHttpClientRibbonRequestCustomizerTests {

	@Mock Tracer tracer;
	@InjectMocks ApacheHttpClientRibbonRequestCustomizer customizer;
	Span span = Span.builder().name("name").spanId(1L).traceId(2L).parent(3L)
			.processId("processId").build();

	@Test
	public void should_accept_customizer_when_apache_http_client_is_passed() throws Exception {
		then(this.customizer.accepts(String.class)).isFalse();
		then(this.customizer.accepts(RequestBuilder.class)).isTrue();
	}

	@Test
	public void should_set_not_sampled_on_the_context_when_there_is_no_span() throws Exception {
		RequestBuilder requestBuilder = RequestBuilder.create("GET");

		this.customizer.inject(null, this.customizer.toSpanTextMap(requestBuilder));

		HttpUriRequest request = requestBuilder.build();
		Header header = request.getFirstHeader(Span.SAMPLED_NAME);
		then(header.getName()).isEqualTo(Span.SAMPLED_NAME);
		then(header.getValue()).isEqualTo(Span.SPAN_NOT_SAMPLED);
	}

	@Test
	public void should_set_tracing_headers_on_the_context_when_there_is_a_span() throws Exception {
		RequestBuilder requestBuilder = RequestBuilder.create("GET");

		this.customizer.inject(this.span, this.customizer.toSpanTextMap(requestBuilder));

		HttpUriRequest request = requestBuilder.build();
		thenThereIsAHeaderWithNameAndValue(request, Span.SPAN_ID_NAME, "1");
		thenThereIsAHeaderWithNameAndValue(request, Span.TRACE_ID_NAME, "2");
		thenThereIsAHeaderWithNameAndValue(request, Span.PARENT_ID_NAME, "3");
		thenThereIsAHeaderWithNameAndValue(request, Span.PROCESS_ID_NAME, "processId");
	}

	private void thenThereIsAHeaderWithNameAndValue(HttpUriRequest request, String name, String value) {
		Header header = request.getFirstHeader(name);
		then(header.getName()).isEqualTo(name);
		then(header.getValue()).isEqualTo(value);
	}
}