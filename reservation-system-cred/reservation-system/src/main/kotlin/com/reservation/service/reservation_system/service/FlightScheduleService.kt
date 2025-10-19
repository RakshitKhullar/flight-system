package com.reservation.service.reservation_system.service

import com.reservation.service.reservation_system.dto.*
import com.reservation.service.reservation_system.repository.entity.cassandra.*
import com.reservation.service.reservation_system.store.TravelScheduleStore
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@Service
class FlightScheduleService(
    private val travelScheduleStore: TravelScheduleStore
) {

    fun createFlightSchedule(flightId: String, scheduleItems: List<ScheduleItem>): TravelSchedule {
        val travelSchedule = TravelSchedule(
            vehicleId = flightId,
            schedule = scheduleItems
        )
        return travelScheduleStore.save(travelSchedule)
    }

    fun getFlightSchedules(flightId: String): List<TravelSchedule> {
        return travelScheduleStore.findByFlightId(flightId)
    }

    fun updateSeatStatus(scheduleId: UUID, seatId: UUID, newStatus: SeatStatus): TravelSchedule? {
        val schedule = travelScheduleStore.findById(scheduleId) ?: return null
        
        val updatedSchedule = schedule.copy(
            schedule = schedule.schedule.map { scheduleItem ->
                scheduleItem.copy(
                    seats = scheduleItem.seats.map { seatInfo ->
                        if (seatInfo.seatId == seatId) {
                            seatInfo.copy(seatStatus = newStatus)
                        } else {
                            seatInfo
                        }
                    }
                )
            }
        )
        
        return travelScheduleStore.save(updatedSchedule)
    }
    
    fun updateSeatStatusByFlightId(flightId: String, seatId: UUID, newStatus: SeatStatus): TravelSchedule? {
        val schedules = travelScheduleStore.findByFlightId(flightId)
        
        // Find the schedule that contains the seat
        val targetSchedule = schedules.firstOrNull { schedule ->
            schedule.schedule.any { scheduleItem ->
                scheduleItem.seats.any { seatInfo -> seatInfo.seatId == seatId }
            }
        } ?: return null
        
        // Update the seat status
        val updatedSchedule = targetSchedule.copy(
            schedule = targetSchedule.schedule.map { scheduleItem ->
                scheduleItem.copy(
                    seats = scheduleItem.seats.map { seatInfo ->
                        if (seatInfo.seatId == seatId) {
                            seatInfo.copy(seatStatus = newStatus)
                        } else {
                            seatInfo
                        }
                    }
                )
            }
        )
        
        return travelScheduleStore.save(updatedSchedule)
    }

    fun confirmBlockedSeatByFlightId(flightId: String, seatId: UUID): TravelSchedule? {
        val schedules = travelScheduleStore.findByFlightId(flightId)
        
        // Find the schedule that contains the blocked seat
        val targetSchedule = schedules.firstOrNull { schedule ->
            schedule.schedule.any { scheduleItem ->
                scheduleItem.seats.any { seatInfo -> 
                    seatInfo.seatId == seatId && seatInfo.seatStatus == SeatStatus.BLOCKED 
                }
            }
        } ?: throw IllegalStateException("Seat not found or not in BLOCKED status")
        
        // Update seat status from BLOCKED to BOOKED
        val updatedSchedule = targetSchedule.copy(
            schedule = targetSchedule.schedule.map { scheduleItem ->
                scheduleItem.copy(
                    seats = scheduleItem.seats.map { seatInfo ->
                        if (seatInfo.seatId == seatId && seatInfo.seatStatus == SeatStatus.BLOCKED) {
                            seatInfo.copy(seatStatus = SeatStatus.BOOKED)
                        } else {
                            seatInfo
                        }
                    }
                )
            }
        )
        
        return travelScheduleStore.save(updatedSchedule)
    }
    
    fun getAvailableSeats(flightId: String, date: String?): List<SeatInfo> {
        return travelScheduleStore.findByFlightId(flightId)
            .flatMap { travelSchedule ->
                travelSchedule.schedule
                    .flatMap { scheduleItem -> scheduleItem.seats }
                    .filter { seatInfo -> seatInfo.seatStatus == SeatStatus.AVAILABLE }
            }
    }

    fun getBlockedSeats(flightId: String): List<SeatInfo> {
        return travelScheduleStore.findByFlightId(flightId)
            .flatMap { travelSchedule ->
                travelSchedule.schedule
                    .flatMap { scheduleItem -> scheduleItem.seats }
                    .filter { seatInfo -> seatInfo.seatStatus == SeatStatus.BLOCKED }
            }
    }

    fun getAllFlightSchedules(): List<TravelSchedule> {
        return travelScheduleStore.findAll()
    }

    fun deleteFlightSchedule(id: UUID): Boolean {
        return if (travelScheduleStore.existsById(id)) {
            travelScheduleStore.deleteById(id)
            true
        } else {
            false
        }
    }

    // New methods for flight creation and management
    fun createFlight(request: FlightCreationRequest, adminId: UUID): FlightCreationResponse {
        // Convert stops
        val stops = request.stops.map { stopRequest ->
            FlightStop(
                airportCode = stopRequest.airportCode,
                airportName = stopRequest.airportName,
                city = stopRequest.city,
                arrivalTime = stopRequest.arrivalTime,
                departureTime = stopRequest.departureTime,
                layoverDuration = stopRequest.layoverDuration,
                stopSequence = stopRequest.stopSequence
            )
        }

        // Convert seats
        val seats = request.seats.map { seatRequest ->
            SeatInfo(
                seatNumber = seatRequest.seatNumber,
                seatClass = seatRequest.seatClass,
                amount = seatRequest.amount,
                seatStatus = SeatStatus.AVAILABLE
            )
        }

        // Create all possible schedule segments for multi-stop flights
        val scheduleItems = createFlightSegments(
            flightNumber = request.flightNumber,
            date = request.date,
            sourceCode = request.sourceCode,
            destinationCode = request.destinationCode,
            travelStartTime = request.travelStartTime,
            travelEndTime = request.travelEndTime,
            stops = stops,
            seats = seats
        )

        // Create travel schedule with all segments
        val travelSchedule = TravelSchedule(
            vehicleId = request.flightNumber,
            schedule = scheduleItems
        )

        // Save to store
        val savedSchedule = travelScheduleStore.save(travelSchedule)
        val mainScheduleItem = savedSchedule.schedule.first { 
            it.sourceCode == request.sourceCode && it.destinationCode == request.destinationCode 
        }

        // Return response
        return FlightCreationResponse(
            scheduleId = mainScheduleItem.scheduleId,
            flightNumber = mainScheduleItem.flightNumber,
            date = mainScheduleItem.date,
            sourceCode = mainScheduleItem.sourceCode,
            destinationCode = mainScheduleItem.destinationCode,
            numberOfStops = mainScheduleItem.numberOfStops,
            isDirect = mainScheduleItem.isDirect,
            totalSeats = mainScheduleItem.totalSeats,
            availableSeats = mainScheduleItem.availableSeats,
            stops = mainScheduleItem.stops.map { stop ->
                FlightStopResponse(
                    stopId = stop.stopId,
                    airportCode = stop.airportCode,
                    airportName = stop.airportName,
                    city = stop.city,
                    arrivalTime = stop.arrivalTime,
                    departureTime = stop.departureTime,
                    layoverDuration = stop.layoverDuration,
                    stopSequence = stop.stopSequence
                )
            },
            message = "Flight created successfully with ${scheduleItems.size} segments"
        )
    }

    private fun createFlightSegments(
        flightNumber: String,
        date: LocalDate,
        sourceCode: String,
        destinationCode: String,
        travelStartTime: LocalTime,
        travelEndTime: LocalTime,
        stops: List<FlightStop>,
        seats: List<SeatInfo>
    ): List<ScheduleItem> {
        val scheduleItems = mutableListOf<ScheduleItem>()
        
        // Create a list of all airports in order: source -> stops -> destination
        val allAirports = mutableListOf<String>()
        allAirports.add(sourceCode)
        allAirports.addAll(stops.sortedBy { it.stopSequence }.map { it.airportCode })
        allAirports.add(destinationCode)

        // Create all possible segments
        for (i in allAirports.indices) {
            for (j in i + 1 until allAirports.size) {
                val segmentSource = allAirports[i]
                val segmentDestination = allAirports[j]
                
                // Calculate segment stops (stops between segmentSource and segmentDestination)
                val segmentStops = if (i == 0 && j == allAirports.size - 1) {
                    // Full route: include all stops
                    stops
                } else {
                    // Partial route: include only relevant stops
                    stops.filter { stop ->
                        val stopIndex = allAirports.indexOf(stop.airportCode)
                        stopIndex > i && stopIndex < j
                    }.map { stop ->
                        // Adjust stop sequence for this segment
                        val newSequence = stops.filter { s ->
                            val sIndex = allAirports.indexOf(s.airportCode)
                            sIndex > i && sIndex < j
                        }.sortedBy { it.stopSequence }.indexOf(stop) + 1
                        stop.copy(stopSequence = newSequence)
                    }
                }

                // Calculate segment times
                val segmentStartTime = when (i) {
                    0 -> travelStartTime // Original start time
                    else -> {
                        val stopAirport = allAirports[i]
                        stops.find { it.airportCode == stopAirport }?.departureTime ?: travelStartTime
                    }
                }

                val segmentEndTime = when (j) {
                    allAirports.size - 1 -> travelEndTime // Original end time
                    else -> {
                        val stopAirport = allAirports[j]
                        stops.find { it.airportCode == stopAirport }?.arrivalTime ?: travelEndTime
                    }
                }

                // Create schedule item for this segment
                val scheduleItem = ScheduleItem(
                    date = date,
                    flightNumber = flightNumber,
                    sourceCode = segmentSource,
                    destinationCode = segmentDestination,
                    travelStartTime = segmentStartTime,
                    travelEndTime = segmentEndTime,
                    seats = seats.map { it.copy() }, // Copy seats for each segment
                    totalSeats = seats.size,
                    availableSeats = seats.size,
                    numberOfStops = segmentStops.size,
                    stops = segmentStops,
                    isDirect = segmentStops.isEmpty()
                )

                scheduleItems.add(scheduleItem)
            }
        }

        return scheduleItems
    }

    // New method to handle segment booking
    fun bookSeatForSegment(
        flightNumber: String,
        date: LocalDate,
        seatId: UUID,
        sourceCode: String,
        destinationCode: String
    ): Boolean {
        val schedules = travelScheduleStore.findByFlightId(flightNumber)
        
        // Find all segments that overlap with the requested booking
        val overlappingSegments = schedules.flatMap { schedule ->
            schedule.schedule.filter { scheduleItem ->
                scheduleItem.date == date &&
                scheduleItem.flightNumber == flightNumber &&
                isSegmentOverlapping(scheduleItem, sourceCode, destinationCode)
            }
        }

        if (overlappingSegments.isEmpty()) {
            return false
        }

        // Check if seat is available in all overlapping segments
        val seatAvailableInAllSegments = overlappingSegments.all { segment ->
            segment.seats.any { seat -> 
                seat.seatId == seatId && seat.seatStatus == SeatStatus.AVAILABLE 
            }
        }

        if (!seatAvailableInAllSegments) {
            return false
        }

        // Book the seat in all overlapping segments
        schedules.forEach { schedule ->
            val updatedSchedule = schedule.copy(
                schedule = schedule.schedule.map { scheduleItem ->
                    if (scheduleItem.date == date && 
                        scheduleItem.flightNumber == flightNumber &&
                        isSegmentOverlapping(scheduleItem, sourceCode, destinationCode)) {
                        
                        scheduleItem.copy(
                            seats = scheduleItem.seats.map { seat ->
                                if (seat.seatId == seatId) {
                                    seat.copy(seatStatus = SeatStatus.BOOKED)
                                } else {
                                    seat
                                }
                            },
                            availableSeats = scheduleItem.availableSeats - 1
                        )
                    } else {
                        scheduleItem
                    }
                }
            )
            travelScheduleStore.save(updatedSchedule)
        }

        return true
    }

    private fun isSegmentOverlapping(
        scheduleItem: ScheduleItem,
        requestedSource: String,
        requestedDestination: String
    ): Boolean {
        // Get all airports in the schedule item route
        val scheduleAirports = mutableListOf<String>()
        scheduleAirports.add(scheduleItem.sourceCode)
        scheduleAirports.addAll(scheduleItem.stops.sortedBy { it.stopSequence }.map { it.airportCode })
        scheduleAirports.add(scheduleItem.destinationCode)

        // Get all airports in the requested route
        val requestedAirports = listOf(requestedSource, requestedDestination)

        // Check if there's any overlap
        val requestedSourceIndex = scheduleAirports.indexOf(requestedSource)
        val requestedDestinationIndex = scheduleAirports.indexOf(requestedDestination)
        val scheduleSourceIndex = scheduleAirports.indexOf(scheduleItem.sourceCode)
        val scheduleDestinationIndex = scheduleAirports.indexOf(scheduleItem.destinationCode)

        // If either airport is not found, no overlap
        if (requestedSourceIndex == -1 || requestedDestinationIndex == -1 ||
            scheduleSourceIndex == -1 || scheduleDestinationIndex == -1) {
            return false
        }

        // Check if the segments overlap
        return !(requestedDestinationIndex <= scheduleSourceIndex || 
                 requestedSourceIndex >= scheduleDestinationIndex)
    }

    fun searchFlights(
        sourceCode: String,
        destinationCode: String,
        date: LocalDate,
        maxStops: Int? = null,
        directOnly: Boolean = false
    ): List<FlightSearchResponse> {
        val allSchedules = travelScheduleStore.findAll()
        
        return allSchedules.flatMap { travelSchedule ->
            travelSchedule.schedule
                .filter { scheduleItem ->
                    scheduleItem.date == date &&
                    scheduleItem.sourceCode.equals(sourceCode, ignoreCase = true) &&
                    scheduleItem.destinationCode.equals(destinationCode, ignoreCase = true) &&
                    (if (directOnly) scheduleItem.isDirect else true) &&
                    (maxStops?.let { scheduleItem.numberOfStops <= it } ?: true)
                }
                .map { scheduleItem ->
                    val availableSeats = scheduleItem.seats.filter { it.seatStatus == SeatStatus.AVAILABLE }
                    val prices = availableSeats.map { it.amount }
                    
                    FlightSearchResponse(
                        scheduleId = scheduleItem.scheduleId,
                        flightNumber = scheduleItem.flightNumber,
                        date = scheduleItem.date,
                        sourceCode = scheduleItem.sourceCode,
                        destinationCode = scheduleItem.destinationCode,
                        travelStartTime = scheduleItem.travelStartTime,
                        travelEndTime = scheduleItem.travelEndTime,
                        numberOfStops = scheduleItem.numberOfStops,
                        isDirect = scheduleItem.isDirect,
                        flightType = scheduleItem.getFlightType(),
                        stops = scheduleItem.stops.map { stop ->
                            FlightStopResponse(
                                stopId = stop.stopId,
                                airportCode = stop.airportCode,
                                airportName = stop.airportName,
                                city = stop.city,
                                arrivalTime = stop.arrivalTime,
                                departureTime = stop.departureTime,
                                layoverDuration = stop.layoverDuration,
                                stopSequence = stop.stopSequence
                            )
                        },
                        totalSeats = scheduleItem.totalSeats,
                        availableSeats = availableSeats.size,
                        minPrice = prices.minOrNull() ?: BigDecimal.ZERO,
                        maxPrice = prices.maxOrNull() ?: BigDecimal.ZERO,
                        availableSeatClasses = availableSeats.map { it.seatClass }.distinct(),
                        totalTravelTime = scheduleItem.getTotalTravelTime(),
                        totalLayoverTime = scheduleItem.getTotalLayoverTime()
                    )
                }
        }
    }

    fun getFlightDetails(flightNumber: String, date: LocalDate): FlightDetailsResponse? {
        val schedules = travelScheduleStore.findByFlightId(flightNumber)
        val scheduleItem = schedules.flatMap { it.schedule }
            .firstOrNull { it.flightNumber == flightNumber && it.date == date }
            ?: return null

        return FlightDetailsResponse(
            scheduleId = scheduleItem.scheduleId,
            flightNumber = scheduleItem.flightNumber,
            date = scheduleItem.date,
            sourceCode = scheduleItem.sourceCode,
            destinationCode = scheduleItem.destinationCode,
            travelStartTime = scheduleItem.travelStartTime,
            travelEndTime = scheduleItem.travelEndTime,
            numberOfStops = scheduleItem.numberOfStops,
            isDirect = scheduleItem.isDirect,
            flightType = scheduleItem.getFlightType(),
            stops = scheduleItem.stops.map { stop ->
                FlightStopResponse(
                    stopId = stop.stopId,
                    airportCode = stop.airportCode,
                    airportName = stop.airportName,
                    city = stop.city,
                    arrivalTime = stop.arrivalTime,
                    departureTime = stop.departureTime,
                    layoverDuration = stop.layoverDuration,
                    stopSequence = stop.stopSequence
                )
            },
            seats = scheduleItem.seats.map { seat ->
                SeatDetailsResponse(
                    seatId = seat.seatId,
                    seatNumber = seat.seatNumber,
                    seatClass = seat.seatClass,
                    amount = seat.amount,
                    seatStatus = seat.seatStatus.name,
                    isAvailable = seat.seatStatus == SeatStatus.AVAILABLE
                )
            },
            totalSeats = scheduleItem.totalSeats,
            availableSeats = scheduleItem.seats.count { it.seatStatus == SeatStatus.AVAILABLE },
            totalTravelTime = scheduleItem.getTotalTravelTime(),
            totalLayoverTime = scheduleItem.getTotalLayoverTime()
        )
    }

    fun updateSeatStatus(
        flightNumber: String,
        date: LocalDate,
        seatId: UUID,
        newStatus: String,
        adminId: UUID
    ): Boolean {
        val seatStatus = try {
            SeatStatus.valueOf(newStatus.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid seat status: $newStatus")
        }

        val schedules = travelScheduleStore.findByFlightId(flightNumber)
        val targetSchedule = schedules.firstOrNull { schedule ->
            schedule.schedule.any { scheduleItem ->
                scheduleItem.date == date && 
                scheduleItem.seats.any { seatInfo -> seatInfo.seatId == seatId }
            }
        } ?: return false

        val updatedSchedule = targetSchedule.copy(
            schedule = targetSchedule.schedule.map { scheduleItem ->
                if (scheduleItem.date == date) {
                    scheduleItem.copy(
                        seats = scheduleItem.seats.map { seatInfo ->
                            if (seatInfo.seatId == seatId) {
                                seatInfo.copy(seatStatus = seatStatus)
                            } else {
                                seatInfo
                            }
                        }
                    )
                } else {
                    scheduleItem
                }
            }
        )

        travelScheduleStore.save(updatedSchedule)
        return true
    }

    fun cancelFlight(
        flightNumber: String,
        date: LocalDate,
        adminId: UUID,
        reason: String?
    ): Boolean {
        val schedules = travelScheduleStore.findByFlightId(flightNumber)
        val targetSchedule = schedules.firstOrNull { schedule ->
            schedule.schedule.any { it.date == date && it.flightNumber == flightNumber }
        } ?: return false

        // Mark all seats as maintenance to indicate flight cancellation
        val updatedSchedule = targetSchedule.copy(
            schedule = targetSchedule.schedule.map { scheduleItem ->
                if (scheduleItem.date == date && scheduleItem.flightNumber == flightNumber) {
                    scheduleItem.copy(
                        seats = scheduleItem.seats.map { seatInfo ->
                            seatInfo.copy(seatStatus = SeatStatus.MAINTENANCE)
                        }
                    )
                } else {
                    scheduleItem
                }
            }
        )

        travelScheduleStore.save(updatedSchedule)
        return true
    }

    // Get available seats for a specific segment
    fun getAvailableSeatsForSegment(
        flightNumber: String,
        date: LocalDate,
        sourceCode: String,
        destinationCode: String
    ): List<SeatInfo> {
        val schedules = travelScheduleStore.findByFlightId(flightNumber)
        
        // Find the specific segment
        val segmentSchedule = schedules.flatMap { it.schedule }
            .firstOrNull { scheduleItem ->
                scheduleItem.date == date &&
                scheduleItem.flightNumber == flightNumber &&
                scheduleItem.sourceCode == sourceCode &&
                scheduleItem.destinationCode == destinationCode
            }
        
        return segmentSchedule?.seats?.filter { it.seatStatus == SeatStatus.AVAILABLE } ?: emptyList()
    }

    // Check if a seat is available for a specific segment
    fun isSeatAvailableForSegment(
        flightNumber: String,
        date: LocalDate,
        seatId: UUID,
        sourceCode: String,
        destinationCode: String
    ): Boolean {
        val schedules = travelScheduleStore.findByFlightId(flightNumber)
        
        // Find all segments that overlap with the requested booking
        val overlappingSegments = schedules.flatMap { schedule ->
            schedule.schedule.filter { scheduleItem ->
                scheduleItem.date == date &&
                scheduleItem.flightNumber == flightNumber &&
                isSegmentOverlapping(scheduleItem, sourceCode, destinationCode)
            }
        }

        // Check if seat is available in all overlapping segments
        return overlappingSegments.isNotEmpty() && overlappingSegments.all { segment ->
            segment.seats.any { seat -> 
                seat.seatId == seatId && seat.seatStatus == SeatStatus.AVAILABLE 
            }
        }
    }
}
