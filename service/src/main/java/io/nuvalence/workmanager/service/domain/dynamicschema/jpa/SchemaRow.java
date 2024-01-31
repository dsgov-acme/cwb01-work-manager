package io.nuvalence.workmanager.service.domain.dynamicschema.jpa;

import io.nuvalence.workmanager.service.domain.UpdateTrackedEntity;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntityEventListener;
import io.nuvalence.workmanager.service.domain.VersionedEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * Represents a single row in the dynamic_schema table.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@EntityListeners(UpdateTrackedEntityEventListener.class)
@NoArgsConstructor
@Entity
@Table(name = "dynamic_schema")
@ToString
@SuppressWarnings("ClassFanOutComplexity")
public class SchemaRow extends VersionedEntity implements UpdateTrackedEntity {

    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "`key`", length = 1024, nullable = false)
    private String key;

    @Column(name = "name", length = 1024, nullable = false)
    private String name;

    @Column(name = "description", length = 1024)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_json", nullable = false, columnDefinition = "json")
    private String schemaJson;

    @Cascade(CascadeType.SAVE_UPDATE)
    @ManyToMany
    @JoinTable(
            name = "parent_child_schema",
            joinColumns = @JoinColumn(name = "parent_id"),
            inverseJoinColumns = @JoinColumn(name = "child_id"))
    private Set<SchemaRow> children;

    @Column(name = "created_by", length = 36, updatable = false, nullable = false)
    private String createdBy;

    @Column(name = "last_updated_by", length = 36, nullable = false)
    private String lastUpdatedBy;

    @Column(name = "created_timestamp", updatable = false, nullable = false)
    private OffsetDateTime createdTimestamp;

    @Column(name = "last_updated_timestamp", nullable = false)
    private OffsetDateTime lastUpdatedTimestamp;
}
