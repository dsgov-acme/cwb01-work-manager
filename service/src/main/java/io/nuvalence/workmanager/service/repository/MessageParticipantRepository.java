package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.securemessaging.AgencyMessageParticipant;
import io.nuvalence.workmanager.service.domain.securemessaging.EmployerMessageParticipant;
import io.nuvalence.workmanager.service.domain.securemessaging.IndividualMessageParticipant;
import io.nuvalence.workmanager.service.domain.securemessaging.MessageParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MessageParticipantRepository
        extends JpaRepository<MessageParticipant, UUID>,
                JpaSpecificationExecutor<MessageParticipant> {

    @Query("SELECT amp FROM AgencyMessageParticipant amp WHERE amp.userId = :userId")
    List<AgencyMessageParticipant> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT amp FROM EmployerMessageParticipant amp WHERE amp.employer = :employer")
    List<EmployerMessageParticipant> findAllByEmployer(@Param("employer") Employer employer);

    @Query("SELECT amp FROM IndividualMessageParticipant amp WHERE amp.individual = :individual")
    List<IndividualMessageParticipant> findAllByIndividual(
            @Param("individual") Individual individual);
}
