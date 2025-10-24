package kopo.userservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "managers")
public class ManagerDocument {
    @Id
    private String id;
    private String pw;
    private String email;
    private String name;
    private List<String> patientIds;
    private String _class;

    // getters/setters
    public String getId() { return id; }
    public String getManagerId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPw() { return pw; }
    public void setPw(String pw) { this.pw = pw; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getPatientIds() { return patientIds; }
    public void setPatientIds(List<String> patientIds) { this.patientIds = patientIds; }
    public String get_class() { return _class; }
    public void set_class(String _class) { this._class = _class; }
}
