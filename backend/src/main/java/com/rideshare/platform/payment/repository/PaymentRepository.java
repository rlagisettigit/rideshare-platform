package com.rideshare.platform.payment.repository;

import com.rideshare.platform.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /** Payment.bookingId is a plain FK (no JPA relation to Booking), so this joins via a subquery. */
    @Query("select p from Payment p where p.bookingId in " +
            "(select b.id from Booking b where b.passenger.id = :passengerId) order by p.createdAt desc")
    List<Payment> findByPassengerId(Long passengerId);
}
