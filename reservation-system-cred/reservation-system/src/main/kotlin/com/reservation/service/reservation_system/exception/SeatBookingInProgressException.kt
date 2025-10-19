package com.reservation.service.reservation_system.exception

class SeatBookingInProgressException(
    message: String = "Seat booking in progress, kindly select another seat"
) : RuntimeException(message)
