package test.java.framework;

public class User {

    private String password;
    private String firstName;
    private String lastName;
    private String email;

    public User(String email, String password, String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    public String setFirstName(String firstname) {
        return this.firstName = firstname;
    }

    public String setLastName(String lastname) {
        return this.lastName = lastname;
    }

    public String setEmail(String email) {
        return this.email = email;
    }

    public String setPassword(String password) {
        return this.password = password;
    }

    @Override
    public String toString() {
        return String.format("%s, '%s', %s, %s", getEmail(), getPassword(), getFirstName(), getLastName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            User userToCompare = (User) obj;
            return userToCompare.email.equals(this.email);
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode() +
                (firstName == null ? 0 : firstName.hashCode()) +
                (lastName == null ? 0 : lastName.hashCode()) +
                (email == null ? 0 : email.hashCode());
    }
}
