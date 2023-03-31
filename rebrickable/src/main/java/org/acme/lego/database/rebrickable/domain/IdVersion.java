package org.acme.lego.database.rebrickable.domain;

import lombok.*;

import javax.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Embeddable
public class IdVersion implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int id;
    private short version;
}
