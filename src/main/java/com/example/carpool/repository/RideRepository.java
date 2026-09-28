package com.example.carpool.repository;

import com.example.carpool.model.Ride;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    Page<Ride> findByOriginIgnoreCaseAndDestinationIgnoreCaseAndAvailableSeatsGreaterThanEqual(
        String origin, String destination, Integer seats, Pageable pageable
    );

    // INTHA METHOD-AH ADD PANNUM:
    List<Ride> findByDriverId(Long driverId);
}