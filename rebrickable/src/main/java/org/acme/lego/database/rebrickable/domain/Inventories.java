package org.acme.lego.database.rebrickable.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.*;

@Builder
@Getter
@Setter
@ToString
@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Inventories extends PanacheEntityBase {
    @EmbeddedId
    private IdVersion idVersion;

    @Column(length = 20)
    private String setNum;
}
