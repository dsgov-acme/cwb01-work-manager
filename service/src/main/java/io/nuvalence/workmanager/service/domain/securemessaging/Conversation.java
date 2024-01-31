package io.nuvalence.workmanager.service.domain.securemessaging;

import io.nuvalence.auth.access.AccessResource;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntity;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntityEventListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Defines the structure and behavior of a record.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@AccessResource("conversation")
@Table(name = "conversation")
@EntityListeners(UpdateTrackedEntityEventListener.class)
public class Conversation implements UpdateTrackedEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subject", nullable = false)
    private String subject;

    // TODO: SecureMessaging. To be removed soon. (participants)
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "conversation_participant",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "participant_id"))
    private List<MessageParticipant> participants = new ArrayList<>();

    @OneToMany(
            mappedBy = "conversation",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Message> replies;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "entity_reference_id", nullable = false)
    private EntityReference entityReference;

    @Setter
    @Column(name = "created_by", length = 36, nullable = false)
    private String createdBy;

    @Setter
    @Column(name = "created_timestamp", nullable = false)
    private OffsetDateTime createdTimestamp;

    @Setter
    @Column(name = "last_updated_by", length = 36, nullable = false)
    private String lastUpdatedBy;

    @Setter
    @Column(name = "last_updated_timestamp", nullable = false)
    private OffsetDateTime lastUpdatedTimestamp;

    public void setReplies(List<Message> replies) {
        this.replies = replies;
        if (replies != null) {
            replies.forEach(reply -> reply.setConversation(this));
        }
    }

    public void addReply(Message reply) {
        if (replies == null) {
            replies = new ArrayList<>();
        }
        reply.setConversation(this);
        replies.add(reply);
    }

    public List<Message> getRepliesSortBy(String sortBy, String sortOrder) {

        if (sortBy.equalsIgnoreCase("timestamp") && sortOrder.equalsIgnoreCase("desc")) {
            replies.sort(Comparator.comparing(Message::getTimestamp).reversed());
        } else if (sortBy.equalsIgnoreCase("timestamp") && sortOrder.equalsIgnoreCase("asc")) {
            replies.sort(Comparator.comparing(Message::getTimestamp));
        }
        return replies;
    }
}
