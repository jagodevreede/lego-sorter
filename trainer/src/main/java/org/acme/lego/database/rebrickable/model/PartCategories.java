package org.acme.lego.database.rebrickable.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Builder
@Getter
@Setter
@ToString
@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class PartCategories extends PanacheEntityBase {
    @Id
    private short id;

    @Column(length = 200)
    private String name;
}
