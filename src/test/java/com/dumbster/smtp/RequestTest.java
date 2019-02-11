/*
 * Dumbster - a dummy SMTP server
 * Copyright 2016 Joachim Nicolay
 * Copyright 2004 Jason Paul Kitchen
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dumbster.smtp;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RequestTest {

	@Test
	public void testUnrecognizedCommandConnectState() {
		Request request = new Request(Action.UNRECOG, null, State.CONNECT);
		Response response = request.execute();
		assertThat(response.getCode(), is(500));
	}

	@Test
	public void testUnrecognizedCommandGreetState() {
		Request request = new Request(Action.UNRECOG, null, State.GREET);
		Response response = request.execute();
		assertThat(response.getCode(), is(500));
	}

	@Test
	public void testUnrecognizedCommandMailState() {
		Request request = new Request(Action.UNRECOG, null, State.MAIL);
		Response response = request.execute();
		assertThat(response.getCode(), is(500));
	}

	@Test
	public void testUnrecognizedCommandQuitState() {
		Request request = new Request(Action.UNRECOG, null, State.QUIT);
		Response response = request.execute();
		assertThat(response.getCode(), is(500));
	}

	@Test
	public void testUnrecognizedCommandRcptState() {
		Request request = new Request(Action.UNRECOG, null, State.RCPT);
		Response response = request.execute();
		assertThat(response.getCode(), is(500));
	}

	@Test
	public void testUnrecognizedCommandDataBodyState() {
		Request request = new Request(Action.UNRECOG, null, State.DATA_BODY);
		Response response = request.execute();
		assertThat(response.getCode(), is(-1));
	}

	@Test
	public void testUnrecognizedCommandDataHdrState() {
		Request request = new Request(Action.UNRECOG, null, State.DATA_HDR);
		Response response = request.execute();
		assertThat(response.getCode(), is(-1));
	}


}
