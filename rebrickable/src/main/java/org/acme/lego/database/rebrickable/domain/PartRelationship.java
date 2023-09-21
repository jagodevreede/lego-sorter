package org.acme.lego.database.rebrickable.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Builder
@Getter
@Setter
@ToString
@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class PartRelationship extends PanacheEntity {
    @Column(length = 1)
    private String relType;

    @Column(length = 250)
    private String childPartNum;

    @Column(length = 250)
    private String parentPartNum;
}
