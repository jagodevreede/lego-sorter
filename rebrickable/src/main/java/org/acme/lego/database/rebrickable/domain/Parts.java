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
public class Parts extends PanacheEntityBase {
    @Id
    @Column(length = 20)
    private String partNum;

    @Column(length = 250)
    private String name;

    private Short partCategoryId;
}
