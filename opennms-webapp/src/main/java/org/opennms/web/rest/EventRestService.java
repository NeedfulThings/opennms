//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2006 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
// OpenNMS Licensing       <license@opennms.org>
//     http://www.opennms.org/
//     http://www.opennms.com/
//

package org.opennms.web.rest;

import java.text.ParseException;
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.hibernate.criterion.Order;
import org.opennms.netmgt.dao.EventDao;
import org.opennms.netmgt.model.OnmsCriteria;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.model.OnmsEventCollection;
import org.opennms.netmgt.model.OnmsOutage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sun.jersey.spi.resource.PerRequest;

@Component
/**
 * <p>EventRestService class.</p>
 *
 * @author ranger
 * @version $Id: $
 * @since 1.8.1
 */
@PerRequest
@Scope("prototype")
@Path("events")
public class EventRestService extends OnmsRestService {

	@Autowired
	private EventDao m_eventDao;

	@Context
	UriInfo m_uriInfo;

	@Context
	HttpHeaders m_headers;

	@Context
	SecurityContext m_securityContext;

	/**
	 * <p>getEvent</p>
	 *
	 * @param eventId a {@link java.lang.String} object.
	 * @return a {@link org.opennms.netmgt.model.OnmsEvent} object.
	 */
	@GET
	@Produces( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	@Path("{eventId}")
	@Transactional
	public OnmsEvent getEvent(@PathParam("eventId")
	String eventId) {
		OnmsEvent result = m_eventDao.get(new Integer(eventId));
		return result;
	}

	/**
	 * returns a plaintext string being the number of events
	 *
	 * @return a {@link java.lang.String} object.
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("count")
	@Transactional
	public String getCount() {
		return Integer.toString(m_eventDao.countAll());
	}

	/**
	 * Returns all the events which match the filter/query in the query parameters
	 *
	 * @return Collection of OnmsEvents (ready to be XML-ified)
	 * @throws java.text.ParseException if any.
	 */
	@GET
	@Produces( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	@Transactional
	public OnmsEventCollection getEvents() throws ParseException {
		OnmsEventCollection coll = new OnmsEventCollection(m_eventDao.findMatching(getQueryFilters(m_uriInfo.getQueryParameters())));
		
		//For getting totalCount
		OnmsCriteria crit = new OnmsCriteria(OnmsEvent.class);
		addFiltersToCriteria(m_uriInfo.getQueryParameters(), crit, OnmsEvent.class);
		coll.setTotalCount(m_eventDao.countMatching(crit));
		
		return coll;
	}

	/**
	 * Updates the event with id "eventid"
	 * If the "ack" parameter is "true", then acks the events as the current logged in user, otherwise unacks the events
	 *
	 * @param eventId a {@link java.lang.String} object.
	 * @param ack a {@link java.lang.Boolean} object.
	 */
	@PUT
	@Path("{eventId}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Transactional
	public void updateEvent(@PathParam("eventId")
	String eventId, @FormParam("ack")
	Boolean ack) {
		OnmsEvent event = m_eventDao.get(new Integer(eventId));
		if (ack == null) {
			throw new IllegalArgumentException(
					"Must supply the 'ack' parameter, set to either 'true' or 'false'");
		}
		processEventAck(event, ack);
	}

	/**
	 * Updates all the events that match any filter/query supplied in the form.
	 * If the "ack" parameter is "true", then acks the events as the current logged in user, otherwise unacks the events
	 *
	 * @param formProperties Map of the parameters passed in by form encoding
	 */
	@PUT
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Transactional
	public void updateEvents(MultivaluedMapImpl formProperties) {
		Boolean ack=false;
		if(formProperties.containsKey("ack")) {
			ack="true".equals(formProperties.getFirst("ack"));
			formProperties.remove("ack");
		}
		
		for (OnmsEvent event : m_eventDao.findMatching(getQueryFilters(formProperties))) {
			processEventAck(event, ack);
		}
	}

	private void processEventAck(OnmsEvent event, Boolean ack) {
		if (ack) {
			event.setEventAckTime(new Date());
			event.setEventAckUser(m_securityContext.getUserPrincipal()
					.getName());
		} else {
			event.setEventAckTime(null);
			event.setEventAckUser(null);
		}
		m_eventDao.save(event);
	}

	private OnmsCriteria getQueryFilters(MultivaluedMap<String,String> params) {
	    OnmsCriteria criteria = new OnmsCriteria(OnmsEvent.class);

	    setLimitOffset(params, criteria, DEFAULT_LIMIT, false);
	    addOrdering(params, criteria, false);
	    // Set default ordering
	    criteria.addOrder(Order.desc("eventTime"));
	    addFiltersToCriteria(params, criteria, OnmsEvent.class);

	    return getDistinctIdCriteria(OnmsOutage.class, criteria);
	}
}
