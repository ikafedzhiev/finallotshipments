package com.melexis.viiper.finallotshipments;


import java.util.Map;

import com.melexis.foundation.util.IO;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.language.SimpleExpression;


public class FinalLotShipments extends RouteBuilder{
	
	
	private Processor PrepareQueryFinalLotsShipped = new Processor()	{
		@Override
		public void process(Exchange exchange) throws Exception {
			final Message in = exchange.getIn();
			in.setBody(in.getHeader("LOTNAME", String.class));
			final String query = IO.resourceAsString(FinalLotShipments.class, "sql/finallotshipments.sql");
			final String evaluated = (String) new SimpleExpression(query).evaluate(exchange);
			in.setBody(evaluated);
		}
	};	

	private Processor MapMessageToHeaders = new Processor()	{
		@Override	
    	public void process(Exchange exchange) throws Exception {
        	final Message in = exchange.getIn();
        	final Map<String, String> row = in.getBody(Map.class);
        	for (final Map.Entry<String, String> c : row.entrySet()) {
        		in.setHeader(c.getKey(), c.getValue());
        	}        
    	}
	};
	
	private Processor LotNameAsBody = new Processor()	{
		@Override	
    	public void process(Exchange exchange) throws Exception {
        	final Message in = exchange.getIn();
        	final Map<String, String> row = in.getBody(Map.class);
        	for (final Map.Entry<String, String> c : row.entrySet()) {
        		in.setHeader(c.getKey(), c.getValue());
        	}
        	in.setBody(in.getHeader("LOTNAME", String.class));
    	}
	};	
	@Override
	public void configure() throws Exception {
		
		errorHandler(
				deadLetterChannel("properties:{{exceptions.to}}")
				.maximumRedeliveries(180) 
				.redeliveryDelay(60000)  
				.asyncDelayedRedelivery()
				.retryAttemptedLogLevel(LoggingLevel.WARN));

		from("timer:oracle?fixedRate=true&period=60000")
			.setBody(constant("select distinct lot_number as LOTNAME  from apps.wip_discrete_jobs where lot_number like 'A42295%'"))
			.to("jdbc:viiper-ds")
			.split().body()
			.process(MapMessageToHeaders)
			.to("activemq:queue:customerdeliveries");
		
//		from("properties:{{customerdeliveries.from}}")
		from("activemq:queue:customerdeliveries")
			.routeId("ViiperFinalLotShipments")
			.log("New Customer delivery: ${in.body} ")
			.process(PrepareQueryFinalLotsShipped)
//			.log("Check in Viiper if the lot had its last shipment : ${in.body}")
			.to("jdbc:viiper-ds")
            .split().body()
            .process(LotNameAsBody)
			.log("Submit lot \"${body}\" to Final Lot Shipments Topic.")
			.to("properties:{{finallotshipments.to}}");

	}

}
