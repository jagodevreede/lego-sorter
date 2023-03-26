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
public class Elements extends PanacheEntityBase {
    @Id
    @Column(length = 40)
    public String id;

    @Column(length = 20)
    private String partNum;

    private short colorId;
}
