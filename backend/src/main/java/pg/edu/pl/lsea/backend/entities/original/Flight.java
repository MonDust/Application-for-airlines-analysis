package pg.edu.pl.lsea.backend.entities.original;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pg.edu.pl.lsea.backend.entities.Route;

import java.util.Comparator;
import java.util.Objects;

/**
 * A class representing a tracked flight of an aircraft
 */
@Setter
@Getter
@Entity
@Table(
        name = "flights",
        uniqueConstraints = @UniqueConstraint(columnNames = {"icao24", "first_seen"})
)
public class Flight extends Trackable implements Cloneable {
    /**
     * This value is generated automatically during object creation
     * and is added by the JPA (Java Persistence API).
     */
    @Id
    @GeneratedValue
    private Long id;
    /**
     * Unix timestamp of the first record of the aircraft of the flight in seconds.
     */
    @Column(name = "first_seen")
    private int firstSeen;
    /**
     * Unix timestamp of the last record of the aircraft of the flight in seconds.
     */
    @Column(name = "last_seen")
    private int lastSeen;

//    /**
//     * Airport of departure
//     */
//    @ManyToOne()
//    @JoinColumn(name = "departureAirport_id", referencedColumnName = "id")
//    private Airport departureAirport;
//
//    /**
//     * Airport of arrival
//     */
//    @ManyToOne()
//    @JoinColumn(name = "arrivalAirport_id", referencedColumnName = "id")
//    private Airport arrivalAirport;

    /**
     * Many-to-one relation: one flight can have only one aircraft
     * aircraft_id is a Foreign Key
     */
    @ManyToOne
    @JoinColumn(name = "aircraft_id", referencedColumnName = "id")
    private Aircraft aircraft;

    @ManyToOne
    @JoinColumn(name = "route_id")
    private Route route;

    /**
     * Creates a flight object.
     * @param icao24 A string representing the 6-character hexadecimal icao24 code of the trackable entity.
     * @param firstSeen An integer representing the unix timestamp of the first record of the aircraft of the flight in seconds.
     * @param lastSeen An integer representing the unix timestamp of the last record of the aircraft of the flight in seconds.
     */
    public Flight(String icao24, int firstSeen, int lastSeen) {
        setIcao24(icao24);
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.route = null;
    }

    /**
     * Empty constructor needed for cloning
     */
    public Flight () {
        setIcao24("");
        firstSeen = 0;
        lastSeen = 0;
        route = null;
    }

   /**
     * Class for comparing flights based on firstSeen timestamp, lastSeen timestamp and departure airport.
     */
    public static class FlightComparator implements Comparator<Flight> {
       /**
        * Compares flights by firstSeen,
        * if firstSeen is the same compares by lastSeen,
        * if lastSeen is the same compares by departureAirport.
        * @param a The first flight to be compared.
        * @param b The second flight to be compared.
        * @return Negative integer if first object is smaller by comparison of firstSeen, lastSeen and departureAirport,
        * positive integer if first object is greater,
        * zero if both have the same firstSeen, lastSeen and departureAirport.
        */
        @Override
        public int compare(Flight a, Flight b) {
            int firstSeenCompare = a.firstSeen -b.firstSeen;
            int lastSeenCompare = a.lastSeen -b.lastSeen;
            int departureAirportCompare = a.route.compareTo(b.route);

            return (firstSeenCompare == 0)
                ? (lastSeenCompare == 0)
                    ? departureAirportCompare
                    : lastSeenCompare
                : firstSeenCompare;
        }
    }


    /**
     * Printing flights.
     * @return Flight with all fields in a string format.
     */
    @Override
    public String toString() {
        return "Flight{" +
               "icao24='" + getIcao24() + "'" +
               ", firstseen=" + firstSeen +
               ", lastseen=" + lastSeen +
               ", route='" + route + "'" +
               "}";
    }



    @Override
    public Flight clone() {
        Flight newFlight = new Flight();
        newFlight.setIcao24(getIcao24());
        newFlight.setFirstSeen(firstSeen);
        newFlight.setLastSeen(lastSeen);
        newFlight.setRoute(route);
        return newFlight;
    }

    /**
     * A method that compares a flight object to another object and determines if they are equal based on icao24 and firstSeen values.
     * @param o the objects to compare the flight to
     * @return true if the compared objects are considered equal, false if they are different
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flight flight = (Flight) o;
        // checking flight uniqueness by firstseen and icao24
        return firstSeen == flight.firstSeen && Objects.equals(getIcao24(), flight.getIcao24());
    }

    /**
     * Calculates a hash code for flight objects based on icao24 and firstSeen values to ensure objects
     * with the same values of these fields are considered equal and have the same hash code
     * @return hash code value for the flight object
     */
    @Override
    public int hashCode() {
        return Objects.hash(getIcao24(), firstSeen);
    }
}

