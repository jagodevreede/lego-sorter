package org.acme.lego.database.rebrickable.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

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
