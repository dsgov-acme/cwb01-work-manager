package io.nuvalence.workmanager.service.domain.securemessaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Entity
@Table(name = "agency_message_participant")
public class AgencyMessageParticipant extends MessageParticipant {
    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
