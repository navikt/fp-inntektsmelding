package no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity(name = "KontaktpersonEntitet")
@Table(name = "KONTAKTPERSON")
public class KontaktpersonEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "inntektsmelding_id", nullable = false, updatable = false)
    private InntektsmeldingEntitet inntektsmelding;

    @Column(name = "telefonnummer")
    private String telefonnummer;

    @Column(name = "navn")
    private String navn;

    public KontaktpersonEntitet() {
        // Hibernate
    }

    public KontaktpersonEntitet(String navn, String telefonnummer) {
        this.telefonnummer = telefonnummer;
        this.navn = navn;
    }

    void setInntektsmelding(InntektsmeldingEntitet inntektsmeldingEntitet) {
        this.inntektsmelding = inntektsmeldingEntitet;
    }

    public String getTelefonnummer() {
        return telefonnummer;
    }

    public String getNavn() {
        return navn;
    }
}
