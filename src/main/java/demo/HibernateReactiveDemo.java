package demo;

import demo.entity.Note;
import demo.entity.Person;
import org.hibernate.reactive.stage.Stage;
import org.hibernate.reactive.stage.Stage.SessionFactory;

import javax.persistence.Persistence;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class HibernateReactiveDemo {
    private static SessionFactory sessionFactory;

    public static void main(String[] args) {
        showExamples(() -> {

            savePersonWithANote("Andrii", "Hello everyone!")
                    .thenCompose(
                            person -> addNoteByPersonId(person.getId(), "The second note :)")
                                    .thenApply(v -> person)
                    )
                    .thenCompose(
                            person -> getPersonWithNotesByIdUsingLazyRelation(person.getId())
                                    .thenAccept(System.out::println)
                    )
                    .thenCompose(v -> makeAllPersonsReactive())
                    .thenCompose(
                            v -> getAllPersonWithNotesUsingJoinFetch()
                                    .thenAccept(HibernateReactiveDemo::printNumberOfNotesPerPerson)
                    )
                    .toCompletableFuture()
                    .join();
        });
    }

    private static void showExamples(Runnable runnable) {
        sessionFactory = Persistence.createEntityManagerFactory("default")
                .unwrap(SessionFactory.class);
        try {
            runnable.run();
        } finally {
            sessionFactory.close();
        }
    }

    private static CompletionStage<Person> savePersonWithANote(String personName, String note) {
        return sessionFactory.withTransaction(
                (session, transaction) -> {
                    var person = new Person(personName);
                    return session.persist(person.addNote(new Note(note)))
                            .thenApply(v -> person);
                }
        );
    }

    private static CompletionStage<Person> getPersonById(Long id) {
        return sessionFactory.withSession(
                session -> session.find(Person.class, id)
        );
    }

    private static CompletionStage<Person> getPersonWithNotesByIdUsingLazyRelation(Long id) {
        return sessionFactory.withSession(
                session -> session.find(Person.class, id)
                        .thenCompose(
                                person -> Stage.fetch(person.getNotes())
                                        .thenApply(notes -> person)
                        )
        );
    }

    private static CompletionStage<List<Person>> getAllPersonWithNotesUsingJoinFetch() {
        return sessionFactory.withSession(
                session -> session.createQuery(
                        "select distinct p from Person p left join fetch p.notes",
                        Person.class
                )
                        .getResultList()
        );
    }

    private static CompletionStage<Void> addNoteByPersonId(Long personId, String noteText) {
        return sessionFactory.withTransaction(
                (session, tx) -> {
                    var note = new Note(noteText);
                    var personProxy = session.getReference(Person.class, personId);
                    note.setPerson(personProxy);
                    return session.persist(note);
                }
        );
    }

    private static CompletionStage<Void> makeAllPersonsReactive() {
        return sessionFactory.withTransaction(
                (session, tx) -> session.createQuery(
                        "select p from Person p left join fetch p.notes",
                        Person.class
                )
                        .getResultList()
                        .thenAccept(HibernateReactiveDemo::addReactivePrefix)
        );
    }

    private static void addReactivePrefix(List<Person> personList) {
        personList.stream()
                .filter(p -> !p.getFirstName().startsWith("Reactive"))
                .forEach(p -> p.setFirstName("Reactive " + p.getFirstName()));
    }

    private static CompletionStage<Void> removeNoteById(Long id) {
        return sessionFactory.withTransaction(
                (session, tx) -> session.find(Note.class, id)
                        .thenAccept(session::remove)
        );
    }

    private static void printNumberOfNotesPerPerson(List<Person> personList) {
        personList
                .forEach(p -> System.out.println(p.getFirstName() + " has " + p.getNotes().size() + " notes"));
    }

}
