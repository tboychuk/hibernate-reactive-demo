package demo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Data
@ToString(exclude = "person")
@NoArgsConstructor
public class Note {
    @Id
    @GeneratedValue
    private Long id;

    private String body;

    @ManyToOne
    @JoinColumn(name = "person_id")
    private Person person;

    public Note(String body) {
        this.body = body;
    }
}
