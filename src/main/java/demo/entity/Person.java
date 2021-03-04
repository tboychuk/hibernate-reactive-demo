package demo.entity;


import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Person {
    @Id
    @GeneratedValue
    private Long id;
    private String firstName;

    @Setter(AccessLevel.PRIVATE)
    @OneToMany(mappedBy = "person", cascade = CascadeType.PERSIST)
    private List<Note> notes = new ArrayList<>();

    public Person(String firstName) {
        this.firstName = firstName;
    }

    public Person addNote(Note note) {
        note.setPerson(this);
        notes.add(note);
        return this;
    }
}
