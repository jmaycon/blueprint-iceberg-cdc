package edu.jmaycon.cdcapp.source;

import edu.playground.avro.FlightTicketAvro;

public class IcebergRowViewAdapter {
    public IcebergRowView fromTicket(FlightTicketAvro ticket) {
        return new IcebergRowView(ticket);
    }
}
