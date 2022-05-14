package org.acme.lego.database.rebrickable.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

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
