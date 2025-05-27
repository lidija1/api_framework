package models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private Integer id;
    private String email;
    private String firstName;
    private String lastName;
    private String avatar;
    private String job;
    private String policyType;
    private Double premium;
}
