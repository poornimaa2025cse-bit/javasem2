package com.example.carpool.service;

import com.example.carpool.model.Ride;
import com.example.carpool.model.RideRequest;
import com.example.carpool.model.User;
import com.example.carpool.repository.RideRepository;
import com.example.carpool.repository.RideRequestRepository;
import com.example.carpool.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CarpoolService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private RideRequestRepository rideRequestRepository;

    @Autowired
    private WhatsAppNotificationService whatsAppNotificationService;

    // 1. Publish a new ride
    public Ride publishRide(Long driverId, Ride rideDetails) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + driverId));
        
        rideDetails.setDriver(driver);
        rideDetails.setAvailableSeats(rideDetails.getTotalSeats());
        return rideRepository.save(rideDetails);
    }

    public List<Ride> getDriverRides(Long driverId) {
        return rideRepository.findByDriverId(driverId);
    }

    // 2. Search rides with Pagination & Sorting
    public Page<Ride> searchRides(String origin, String destination, Pageable pageable) {
        return rideRepository.findByOriginIgnoreCaseAndDestinationIgnoreCaseAndAvailableSeatsGreaterThanEqual(
                origin, destination, 1, pageable
        );
    }

    // 3. Request a seat (With Self-Booking check & WhatsApp notification to Driver)
    public RideRequest requestSeat(Long rideId, Long riderId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        
        User rider = userRepository.findById(riderId)
                .orElseThrow(() -> new RuntimeException("Rider not found"));

        // RULE 1: Self-booking check
        if (ride.getDriver().getId().equals(riderId)) {
            throw new IllegalArgumentException("You cannot request a seat on your own published ride!");
        }

        // Check if seats are available
        if (ride.getAvailableSeats() <= 0) {
            throw new IllegalStateException("No seats remaining for this ride.");
        }

        RideRequest request = new RideRequest();
        request.setRide(ride);
        request.setRider(rider);
        request.setStatus(RideRequest.RequestStatus.PENDING);

        RideRequest savedRequest = rideRequestRepository.save(request);

        // Send WhatsApp notification to the DRIVER about the new seat request
        String driverPhone = ride.getDriver().getPhoneNumber();
        String driverMsg = "Hi " + ride.getDriver().getName() + ", " + rider.getName() 
                         + " has requested a seat for your ride from " + ride.getOrigin() 
                         + " to " + ride.getDestination() + ". Please check your dashboard to approve! 🚗";
        whatsAppNotificationService.sendWhatsAppMessage(driverPhone, driverMsg);

        return savedRequest;
    }

    // 4. Approve or Reject a Ride Request (With Seat decrement & WhatsApp trigger to Rider)
    @Transactional
    public RideRequest updateRequestStatus(Long requestId, RideRequest.RequestStatus newStatus) {
        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Ride Request not found"));

        Ride ride = request.getRide();

        if (newStatus == RideRequest.RequestStatus.APPROVED) {
            // RULE 2: Zero seats check
            if (ride.getAvailableSeats() <= 0) {
                throw new IllegalStateException("Cannot approve request: No seats remaining!");
            }

            // Decrement available seats
            ride.setAvailableSeats(ride.getAvailableSeats() - 1);
            rideRepository.save(ride);

            request.setStatus(RideRequest.RequestStatus.APPROVED);

            // Send WhatsApp message confirmation to the rider
            String phoneNumber = request.getRider().getPhoneNumber();
            String msg = "Hi " + request.getRider().getName() + ", your ride request from " 
                       + ride.getOrigin() + " to " + ride.getDestination() + " has been APPROVED! 🚗";
            whatsAppNotificationService.sendWhatsAppMessage(phoneNumber, msg);

        } else if (newStatus == RideRequest.RequestStatus.REJECTED) {
            request.setStatus(RideRequest.RequestStatus.REJECTED);
            
            String phoneNumber = request.getRider().getPhoneNumber();
            String msg = "Hi " + request.getRider().getName() + ", unfortunately your ride request from " 
                       + ride.getOrigin() + " to " + ride.getDestination() + " was rejected.";
            whatsAppNotificationService.sendWhatsAppMessage(phoneNumber, msg);
        }

        return rideRequestRepository.save(request);
    }

    // 5. Edit/Update Ride
    @Transactional
    public Ride updateRide(Long driverId, Long rideId, Ride updatedRideDetails) {
        Ride ride = getDriverOwnedRide(driverId, rideId);

        Integer updatedTotalSeats = updatedRideDetails.getTotalSeats();
        if (updatedTotalSeats == null || updatedTotalSeats < 1) {
            throw new IllegalArgumentException("Total seats must be at least one.");
        }

        int bookedSeats = ride.getTotalSeats() - ride.getAvailableSeats();
        if (updatedTotalSeats < bookedSeats) {
            throw new IllegalArgumentException("Total seats cannot be less than the number of already booked seats.");
        }
        
        ride.setOrigin(updatedRideDetails.getOrigin());
        ride.setDestination(updatedRideDetails.getDestination());
        ride.setDepartureTime(updatedRideDetails.getDepartureTime());
        ride.setTotalSeats(updatedTotalSeats);
        ride.setAvailableSeats(updatedTotalSeats - bookedSeats);
        ride.setFare(updatedRideDetails.getFare());
        
        return rideRepository.save(ride);
    }

    // 6. Delete Ride
    @Transactional
    public void deleteRide(Long driverId, Long rideId) {
        Ride ride = getDriverOwnedRide(driverId, rideId);
        rideRequestRepository.deleteByRideId(rideId);
        rideRepository.delete(ride);
    }

    private Ride getDriverOwnedRide(Long driverId, Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found with id: " + rideId));
        if (ride.getDriver() == null || !driverId.equals(ride.getDriver().getId())) {
            throw new IllegalArgumentException("You can only manage your own rides.");
        }
        return ride;
    }
}