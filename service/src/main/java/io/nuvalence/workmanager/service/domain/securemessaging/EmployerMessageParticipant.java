package io.nuvalence.workmanager.service.domain.securemessaging;

import io.nuvalence.workmanager.service.domain.profile.Employer;
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
@Table(name = "employer_message_participant")
public class EmployerMessageParticipant extends MessageParticipant {

    @OneToOne
    @JoinColumn(name = "employer_id", nullable = false)
    private Employer employer;
}
