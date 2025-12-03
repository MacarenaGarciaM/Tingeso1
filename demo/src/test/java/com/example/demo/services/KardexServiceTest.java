package com.example.demo.services;

import com.example.demo.entities.KardexEntity;
import com.example.demo.repositories.KardexRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KardexServiceTest {

    @Mock
    KardexRepository kardexRepository;

    @InjectMocks
    KardexService kardexService;

    @Test
    void search_mapsParams_allPresent() {
        // given
        Long toolId = 5L;
        String rut = "11.111.111-1";
        String type = "LOAN";
        LocalDate start = LocalDate.of(2025, 10, 1);
        LocalDate end   = LocalDate.of(2025, 10, 31);
        String name = "Taladro";
        String category = "Eléctricas";
        Pageable pr = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "movementDate"));

        Page<KardexEntity> expected = new PageImpl<>(List.of(), pr, 0);
        given(kardexRepository.search(
                any(), any(), any(),
                anyBoolean(), any(), anyBoolean(), any(),
                any(), any(), any()))
                .willReturn(expected);

        // when
        Page<KardexEntity> out = kardexService.search(
                toolId, rut, type, start, end, name, category, pr);

        // then: retorna lo que entrega el repo
        assertSame(expected, out);

        // capture and verify mapping/normalization
        ArgumentCaptor<Long> aToolId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> aRut = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aTypeLower = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> aHasFrom = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<LocalDate> aStart = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<Boolean> aHasTo = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<LocalDate> aEnd = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<String> aNamePat = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aCatPat = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> aPageable = ArgumentCaptor.forClass(Pageable.class);

        verify(kardexRepository).search(
                aToolId.capture(), aRut.capture(), aTypeLower.capture(),
                aHasFrom.capture(), aStart.capture(),
                aHasTo.capture(), aEnd.capture(),
                aNamePat.capture(), aCatPat.capture(),
                aPageable.capture()
        );

        assertEquals(toolId, aToolId.getValue());
        assertEquals(rut, aRut.getValue());
        assertEquals("loan", aTypeLower.getValue());           // lower-case
        assertTrue(aHasFrom.getValue());
        assertEquals(start, aStart.getValue());
        assertTrue(aHasTo.getValue());
        assertEquals(end, aEnd.getValue());
        assertEquals("%taladro%", aNamePat.getValue());        // pattern with %
        assertEquals("%eléctricas%", aCatPat.getValue());
        assertEquals(pr, aPageable.getValue());
    }

    @Test
    void search_mapsParams_nullsAndBlanks() {
        // given: blank or null type/name/category; no dates
        Pageable pr = PageRequest.of(1, 10);
        Page<KardexEntity> expected = new PageImpl<>(List.of(), pr, 0);

        given(kardexRepository.search(
                any(), any(), any(),
                anyBoolean(), any(), anyBoolean(), any(),
                any(), any(), any()))
                .willReturn(expected);

        // when
        Page<KardexEntity> out = kardexService.search(
                null, null, "   ",
                null, null,
                "", null,
                pr
        );

        assertSame(expected, out);

        // verify normalization
        ArgumentCaptor<String> aTypeLower = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> aHasFrom = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Boolean> aHasTo = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> aNamePat = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aCatPat = ArgumentCaptor.forClass(String.class);

        verify(kardexRepository).search(
                any(), any(), aTypeLower.capture(),
                aHasFrom.capture(), any(),
                aHasTo.capture(), any(),
                aNamePat.capture(), aCatPat.capture(),
                any()
        );

        assertEquals("", aTypeLower.getValue());
        assertFalse(aHasFrom.getValue());
        assertFalse(aHasTo.getValue());
        assertEquals("", aNamePat.getValue());
        assertEquals("", aCatPat.getValue());
    }
}
