package com.example.booking.report;

public record RoomUtilization(Long roomId, String roomName, long bookedMinutes, double utilizationPercent) {
}
