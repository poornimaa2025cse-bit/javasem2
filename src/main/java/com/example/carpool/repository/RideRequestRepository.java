package com.example.carpool.repository;

import com.example.carpool.model.RideRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {
    List<RideRequest> findByRideDriverId(Long driverId);
    void deleteByRideId(Long rideId);
}