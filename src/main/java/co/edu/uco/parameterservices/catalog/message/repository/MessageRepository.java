package co.edu.uco.parameterservices.catalog.message.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uco.parameterservices.catalog.message.domain.Message;

public interface MessageRepository extends JpaRepository<Message, String> {}
