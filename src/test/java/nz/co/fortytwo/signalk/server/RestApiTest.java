/*
 *
 * Copyright (C) 2012-2014 R T Huitema. All Rights Reserved.
 * Web: www.42.co.nz
 * Email: robert@42.co.nz
 * Author: R T Huitema
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package nz.co.fortytwo.signalk.server;

import static nz.co.fortytwo.signalk.util.JsonConstants.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import mjson.Json;
import nz.co.fortytwo.signalk.model.SignalKModel;
import nz.co.fortytwo.signalk.model.impl.SignalKModelFactory;
import nz.co.fortytwo.signalk.util.JsonConstants;

import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.websocket.SignalkWebsocketComponent;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.client.cookie.Cookie;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

public class RestApiTest extends SignalKCamelTestSupport {
 
    private static Logger logger = Logger.getLogger(RestApiTest.class);
	String jsonDiff = null;
	
	@Produce(uri = "direct:input")
    protected ProducerTemplate template;
	
	public RestApiTest(){
		try {
			jsonDiff = FileUtils.readFileToString(new File("src/test/resources/samples/testUpdate.json"));
			jsonDiff=jsonDiff.replaceAll("SELF", SELF);
		} catch (IOException e) {
			logger.error(e);
			fail();
		}
	}

	@Test
    public void shouldGetJsonResponse() throws Exception {
		
	    final CountDownLatch latch = new CountDownLatch(1);
	    
        final AsyncHttpClient c = new AsyncHttpClient();
        
        template.sendBody(RouteManager.SEDA_INPUT,jsonDiff);
        latch.await(2,TimeUnit.SECONDS);
        //get a sessionid
        Response r1 = c.prepareGet("http://localhost:"+restPort+SIGNALK_AUTH+"/demo/pass").execute().get();
        //latch2.await(3, TimeUnit.SECONDS);
        assertEquals(200, r1.getStatusCode());
        Response reponse = c.prepareGet("http://localhost:"+restPort+SIGNALK_API+"/vessels/"+SELF).setCookies(r1.getCookies()).execute().get();
        //latch.await(3, TimeUnit.SECONDS);
        logger.debug(reponse.getResponseBody());
        assertEquals(200, reponse.getStatusCode());
        
        Json resp = Json.read(reponse.getResponseBody());
        assertEquals(172.9 , resp.at(VESSELS).at(SELF).at(nav).at("courseOverGroundTrue").at("value").asFloat(),0.001);
     
        reponse = c.prepareGet("http://localhost:"+restPort+SIGNALK_API+"/vessels/"+SELF+"/navigation").setCookies(r1.getCookies()).execute().get();
        //latch.await(3, TimeUnit.SECONDS);
        logger.debug(reponse.getResponseBody());
        assertEquals(200, reponse.getStatusCode());
        			//{\"updates\":[{\"values\":[{\"value\":172.9,\"path\":\"navigation.courseOverGroundTrue\"}],\"source\":{\"timestamp\":\"2014-08-15T16:00:00.081Z\",\"source\":\"/dev/actisense-N2K-115-128267\"}}],\"context\":\"vessels.self\"}
        
        resp = Json.read(reponse.getResponseBody());
        assertEquals(172.9 , resp.at(VESSELS).at(SELF).at(nav).at("courseOverGroundTrue").at("value").asFloat(),0.001);
        c.close();
    }


	@Override
	public void configureRouteBuilder(RouteBuilder routeBuilder) {
		// TODO Auto-generated method stub
		try {
			((RouteManager)routeBuilder).configure0();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e);
			fail();
		}
	}

}
