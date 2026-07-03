package com.rideshare.platform.chat.repository;

import com.rideshare.platform.chat.entity.ChatThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatThreadRepository extends JpaRepository<ChatThread, Long> {
    Optional<ChatThread> findByRideIdAndPassengerId(Long rideId, Long passengerId);
}
