package io.nuvalence.workmanager.service.domain.securemessaging;

import io.nuvalence.workmanager.service.domain.profile.Individual;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "individual_message_participant")
public class IndividualMessageParticipant extends MessageParticipant {
    @OneToOne
    @JoinColumn(name = "individual_id", nullable = false)
    private Individual individual;
}
