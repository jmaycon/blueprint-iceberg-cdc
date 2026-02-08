package edu.jmaycon.cdcapp.source;

import edu.playground.avro.FlightTicketAvro;

public record IcebergRowView(FlightTicketAvro ticket) {}
