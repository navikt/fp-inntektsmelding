package no.nav.foreldrepenger.inntektsmelding.typer.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.EndringsårsakType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

class KodeverkMapperTest {

    @ParameterizedTest
    @EnumSource(NaturalytelsetypeDto.class)
    void skal_mappe_alle_naturalytelsetype_dto_til_domene(NaturalytelsetypeDto dto) {
        var resultat = KodeverkMapper.mapNaturalytelseTilDomene(dto);
        assertThat(resultat).isNotNull();
        assertThat(resultat.name()).isEqualTo(dto.name());
    }

    @ParameterizedTest
    @EnumSource(NaturalytelseType.class)
    void skal_ha_dto_for_alle_naturalytelsetype_domene_verdier(NaturalytelseType domene) {
        var dto = NaturalytelsetypeDto.valueOf(domene.name());
        assertThat(KodeverkMapper.mapNaturalytelseTilDomene(dto)).isEqualTo(domene);
    }

    @ParameterizedTest
    @EnumSource(YtelseTypeDto.class)
    void skal_mappe_ytelsetype_dto_til_domene(YtelseTypeDto dto) {
        var resultat = KodeverkMapper.mapYtelsetype(dto);
        assertThat(resultat).isNotNull();
        assertThat(resultat.name()).isEqualTo(dto.name());
    }

    @ParameterizedTest
    @EnumSource(Ytelsetype.class)
    void skal_mappe_ytelsetype_domene_til_dto(Ytelsetype domene) {
        var resultat = KodeverkMapper.mapYtelsetype(domene);
        assertThat(resultat).isNotNull();
        assertThat(resultat.name()).isEqualTo(domene.name());
    }

    @ParameterizedTest
    @EnumSource(EndringsårsakDto.class)
    void skal_mappe_endringsårsak_dto_til_domene(EndringsårsakDto dto) {
        var resultat = KodeverkMapper.mapEndringsårsak(dto);
        assertThat(resultat).isNotNull();
        assertThat(resultat.name()).isEqualTo(dto.name());
    }

    @ParameterizedTest
    @EnumSource(EndringsårsakType.class)
    void skal_mappe_endringsårsak_domene_til_dto(EndringsårsakType domene) {
        var resultat = KodeverkMapper.mapEndringsårsak(domene);
        assertThat(resultat).isNotNull();
        assertThat(resultat.name()).isEqualTo(domene.name());
    }

    @ParameterizedTest
    @EnumSource(ForespørselStatusDto.class)
    void skal_mappe_forespørselstatus_dto_til_domene(ForespørselStatusDto dto) {
        var resultat = KodeverkMapper.mapForespørselStatus(dto);
        assertThat(resultat).isNotNull();
        assertThat(resultat.name()).isEqualTo(dto.name());
    }

    @ParameterizedTest
    @EnumSource(ForespørselStatus.class)
    void skal_mappe_forespørselstatus_domene_til_dto(ForespørselStatus domene) {
        var resultat = KodeverkMapper.mapForespørselStatus(domene);
        assertThat(resultat).isNotNull();
        assertThat(resultat.name()).isEqualTo(domene.name());
    }

    @ParameterizedTest
    @EnumSource(ArbeidsgiverinitiertÅrsakDto.class)
    void skal_mappe_arbeidsgiverinitiert_årsak_dto_til_domene(ArbeidsgiverinitiertÅrsakDto dto) {
        var resultat = KodeverkMapper.mapArbeidsgiverinitiertÅrsak(dto);
        assertThat(resultat).isNotNull();
        assertThat(resultat.name()).isEqualTo(dto.name());
    }

    @Test
    void skal_kaste_npe_ved_null_arbeidsgiverinitiert_årsak() {
        assertThatNullPointerException()
            .isThrownBy(() -> KodeverkMapper.mapArbeidsgiverinitiertÅrsak(null))
            .withMessageContaining("Mangler årsak");
    }
}
