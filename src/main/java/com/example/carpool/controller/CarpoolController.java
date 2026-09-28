package com.example.carpool.controller;

import com.example.carpool.model.Ride;
import com.example.carpool.model.RideRequest;
import com.example.carpool.model.User;
import com.example.carpool.repository.UserRepository;
import com.example.carpool.repository.RideRequestRepository;
import com.example.carpool.service.CarpoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carpool")
@CrossOrigin(origins = "*")
public class CarpoolController {

    @Autowired
    private CarpoolService carpoolService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideRequestRepository rideRequestRepository;

    // 1. Register User
    @PostMapping("/users")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        User savedUser = userRepository.save(user);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    // 2. Login by Phone Number
    @GetMapping("/users/login")
    public ResponseEntity<User> loginByPhone(@RequestParam String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("User not found with this mobile number!"));
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    // 3. Publish a new ride
    @PostMapping("/rides")
    public ResponseEntity<Ride> publishRide(@RequestParam Long driverId, @RequestBody Ride ride) {
        Ride publishedRide = carpoolService.publishRide(driverId, ride);
        return new ResponseEntity<>(publishedRide, HttpStatus.CREATED);
    }

    @GetMapping("/drivers/{driverId}/rides")
    public ResponseEntity<List<Ride>> getDriverRides(@PathVariable Long driverId) {
        return new ResponseEntity<>(carpoolService.getDriverRides(driverId), HttpStatus.OK);
    }

    @PutMapping("/rides/{rideId}")
    public ResponseEntity<Ride> updateRide(
            @PathVariable Long rideId,
            @RequestParam Long driverId,
            @RequestBody Ride ride) {
        return new ResponseEntity<>(carpoolService.updateRide(driverId, rideId, ride), HttpStatus.OK);
    }

    @DeleteMapping("/rides/{rideId}")
    public ResponseEntity<Void> deleteRide(@PathVariable Long rideId, @RequestParam Long driverId) {
        carpoolService.deleteRide(driverId, rideId);
        return ResponseEntity.noContent().build();
    }

    // 4. Search rides with pagination
    @GetMapping("/rides/search")
    public ResponseEntity<Page<Ride>> searchRides(
            @RequestParam String origin,
            @RequestParam String destination,
            Pageable pageable) {
        Page<Ride> rides = carpoolService.searchRides(origin, destination, pageable);
        return new ResponseEntity<>(rides, HttpStatus.OK);
    }

    // 5. Request a seat (Rider)
    @PostMapping("/rides/{rideId}/request/{riderId}")
    public ResponseEntity<RideRequest> requestSeat(@PathVariable Long rideId, @PathVariable Long riderId) {
        RideRequest request = carpoolService.requestSeat(rideId, riderId);
        return new ResponseEntity<>(request, HttpStatus.CREATED);
    }

    // 6. Get all ride requests for a specific driver (THIS WAS MISSING!)
    @GetMapping("/drivers/{driverId}/requests")
    public ResponseEntity<List<RideRequest>> getDriverRequests(@PathVariable Long driverId) {
        List<RideRequest> requests = rideRequestRepository.findByRideDriverId(driverId);
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }

    // 7. Approve or Reject a ride request (Driver)
    @PutMapping("/requests/{requestId}/status")
    public ResponseEntity<RideRequest> updateRequestStatus(
            @PathVariable Long requestId,
            @RequestParam RideRequest.RequestStatus status) {
        RideRequest updatedRequest = carpoolService.updateRequestStatus(requestId, status);
        return new ResponseEntity<>(updatedRequest, HttpStatus.OK);
    }
}