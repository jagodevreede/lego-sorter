package org.acme.lego.database.rebrickable.domain;

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
public class Colors extends PanacheEntityBase {
    @Id
    private short id;

    @Column(length = 200)
    private String name;

    @Column(length = 6)
    private String rgb;

    private boolean isTrans;
}
