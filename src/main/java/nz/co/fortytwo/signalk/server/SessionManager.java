/*
 *
 * Copyright (C) 2012-2014 R T Huitema. All Rights Reserved.
 * Web: www.42.co.nz
 * Email: robert@42.co.nz
 * Author: R T Huitema
 *
 * This file is part of the signalk-server-java project
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nz.co.fortytwo.signalk.server.util.JsonConstants;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Track and manage the sessionId's and corresponding webSocket identifiers for a consumer
 * @author robert
 *
 */
public class SessionManager {
	//hold sessionid <> wsSessionId
	BiMap<String, String> wsSessionMap = HashBiMap.create();
	//wsSessionId>Subscription
	List<Subscription> subscriptions = new ArrayList<Subscription>();
	
	public void addSubscription(Subscription sub){
		if(!subscriptions.contains(sub)){
			subscriptions.add(sub);
			//create a new route if we have too
			if(sub.isActive()){
				RouteManager routeManager = RouteManagerFactory.getInstance(null);
				SignalkRouteFactory.configureSubscribeTimer(routeManager, sub);
			}
		}
		
	}
	
	public void removeSubscription(Subscription sub){
			subscriptions.remove(sub);
			if(sub.isActive()){
				RouteManager routeManager = RouteManagerFactory.getInstance(null);
				SignalkRouteFactory.removeSubscribeTimer(routeManager, sub);
			}
	}
	
	public List<Subscription> getSubscriptions(String wsSession){
		List<Subscription> subs = new ArrayList<Subscription>();
		for (Subscription s: subscriptions){
			if(s.getWsSession().equals(wsSession)){
				subs.add(s);
			}
		}
		return subs;
	}
	
	/**
	 * Returns the wsSessionId for the sessionId if it exists
	 * Returns the sessionId if not. This allows for subscriptions to occur before wsSocket starts
	 * @param sessionId
	 * @return
	 */
	public String getWsSession(String sessionId){
		if(!wsSessionMap.containsKey(sessionId))return sessionId;
		return wsSessionMap.get(sessionId);
	}
	public String getSessionId(String wsSession){
		return wsSessionMap.inverse().get(wsSession);
	}
	
	/**
	 * Inserts the sessionId, wsSession pair.
	 * Swaps the wsSessionId for any any inactive sessions that have been entered with sessionId, sessionId
	 * @param sessionId
	 * @param wsSession
	 */
	public void add(String sessionId, String wsSession){
		wsSessionMap.put(sessionId, wsSession);
		//now update any subscriptions for sessionId
		List<Subscription> subs = new ArrayList<Subscription>();
		for (Subscription s: subscriptions){
			if(s.getWsSession().equals(sessionId)){
				subs.add(s);
			}
		}
		for (Subscription s: subs){
			if(s.getWsSession().equals(sessionId)){
				subscriptions.remove(s);
				s.setWsSession(wsSession);
				s.setActive(true);
				subscriptions.add(s);
				RouteManager routeManager = RouteManagerFactory.getInstance(null);
				SignalkRouteFactory.configureSubscribeTimer(routeManager, s);
			}
		}
	}
	public void removeSessionId(String sessionId){
		String wsSession = wsSessionMap.get(sessionId);
		wsSessionMap.remove(sessionId);
		//remove all subscriptions
		RouteManager routeManager = RouteManagerFactory.getInstance(null);
		SignalkRouteFactory.removeSubscribeTimers(routeManager, getSubscriptions(wsSession));
		subscriptions.removeAll(getSubscriptions(wsSession));
		subscriptions.removeAll(getSubscriptions(sessionId));
		
		
	}
	public void removeWsSession(String wsSession){
		wsSessionMap.inverse().remove(wsSession);
		//remove all subscriptions
		RouteManager routeManager = RouteManagerFactory.getInstance(null);
		SignalkRouteFactory.removeSubscribeTimers(routeManager, getSubscriptions(wsSession));
		subscriptions.removeAll(getSubscriptions(wsSession));
	}

	/**
	 * Returns a Set of all the current sessionIds.
	 * 
	 * @return
	 */
	public Set<String> getSessionKeys() {
		return wsSessionMap.keySet();
	}
	
	/**
	 * Gets a Set of all the current wsSessions
	 * @return
	 */
	public Set<String> getWsSessionKeys() {
		return wsSessionMap.inverse().keySet();
	}

	public boolean isValid(String sessionId) {
		if(wsSessionMap.containsKey(sessionId))return true;
		return false;
	}
	
}