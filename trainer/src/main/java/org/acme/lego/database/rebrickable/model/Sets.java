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
public class Sets extends PanacheEntityBase {
    @Id
    @Column(length = 20)
    private String setNum;

    @Column(length = 250)
    private String name;

    private Short year;

    private Short themeId;

    private Short numParts;

}
