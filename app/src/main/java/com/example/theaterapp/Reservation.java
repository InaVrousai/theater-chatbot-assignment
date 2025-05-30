package com.example.theaterapp;

import java.util.List;

public class Reservation {
    public String id;
    public String showName;
    public String showTime;
    public int ticketCount;
    public List<String> seats;
    public String fullName;

    public Reservation(String id, String showName, String showTime,
                       int ticketCount, List<String> seats, String fullName) {
        this.id          = id;
        this.showName    = showName;
        this.showTime    = showTime;
        this.ticketCount = ticketCount;
        this.seats       = seats;
        this.fullName    = fullName;
    }
}