package com.example.demo.services;

import com.example.demo.entities.KardexEntity;
import com.example.demo.entities.ToolEntity;
import com.example.demo.entities.UserEntity;
import com.example.demo.repositories.KardexRepository;
import com.example.demo.repositories.ToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    @Mock ToolRepository toolRepository;
    @Mock KardexRepository kardexRepository;

    @InjectMocks ToolService toolService;

    private UserEntity user;

    @BeforeEach
    void init() {
        user = new UserEntity();
        user.setRut("11.111.111-1");
    }

    // ───────────────────────── saveTool ─────────────────────────

    @Test
    void saveTool_createsNewBucket_andWritesKardex() {
        ToolEntity input = tool(null, "Taladro", "Elec", "Disponible", 50000, true, 3);

        // no hay bucket existente con mismo name-category-state
        given(toolRepository.findByNameAndCategoryAndInitialState("Taladro", "Elec", "Disponible"))
                .willReturn(List.of());

        // guardará un NEW ToolEntity; simulamos que JPA asigna id=101
        given(toolRepository.save(any(ToolEntity.class))).willAnswer(inv -> {
            ToolEntity t = inv.getArgument(0);
            if (t.getId() == null) t.setId(101L);
            return t;
        });

        ToolEntity out = toolService.saveTool(input, user);

        assertEquals(101L, out.getId());
        assertEquals("Taladro", out.getName());
        assertEquals("Disponible", out.getInitialState());
        // kardex escrito con la cantidad ingresada (3)
        verify(kardexRepository).save(argThat(k ->
                k.getTool().getId().equals(101L)
                        && "Ingreso".equals(k.getType())
                        && k.getStock() == 3
                        && "11.111.111-1".equals(k.getRutUser())
        ));
    }

    @Test
    void saveTool_mergesIntoExistingBucket_andWritesKardex() {
        // existe bucket Disponible Taladro/Elec con amount=5
        ToolEntity existing = tool(10L, "Taladro", "Elec", "Disponible", 30000, true, 5);

        given(toolRepository.findByNameAndCategoryAndInitialState("Taladro", "Elec", "Disponible"))
                .willReturn(List.of(existing));

        // input agrega 2 unidades y actualiza repositionValue
        ToolEntity input = tool(null, "Taladro", "Elec", "Disponible", 50000, true, 2);

        given(toolRepository.save(existing)).willAnswer(inv -> inv.getArgument(0));

        ToolEntity out = toolService.saveTool(input, user);

        assertEquals(10L, out.getId());
        assertEquals(7, out.getAmount());              // 5 + 2
        assertEquals(50000, out.getRepositionValue()); // actualizado

        verify(kardexRepository).save(argThat(k ->
                k.getTool().getId().equals(10L)
                        && "Ingreso".equals(k.getType())
                        && k.getStock() == 2         // SOLO ingresado
        ));
    }

    @Test
    void saveTool_validations_fail() {
        // name null
        ToolEntity t1 = tool(null, null, "Cat", "Disponible", 10, true, 1);
        assertThrows(IllegalArgumentException.class, () -> toolService.saveTool(t1, user));

        // category null
        ToolEntity t2 = tool(null, "Name", null, "Disponible", 10, true, 1);
        assertThrows(IllegalArgumentException.class, () -> toolService.saveTool(t2, user));

        // repositionValue <= 0
        ToolEntity t3 = tool(null, "Name", "Cat", "Disponible", 0, true, 1);
        assertThrows(IllegalArgumentException.class, () -> toolService.saveTool(t3, user));

        // amount <= 0
        ToolEntity t4 = tool(null, "Name", "Cat", "Disponible", 10, true, 0);
        assertThrows(IllegalArgumentException.class, () -> toolService.saveTool(t4, user));

        // initialState null
        ToolEntity t5 = tool(null, "Name", "Cat", null, 10, true, 1);
        assertThrows(IllegalArgumentException.class, () -> toolService.saveTool(t5, user));

        // initialState inválido
        ToolEntity t6 = tool(null, "Name", "Cat", "INVALID", 10, true, 1);
        assertThrows(IllegalArgumentException.class, () -> toolService.saveTool(t6, user));
    }


    // ──────────────────────── updateTool ────────────────────────

    @Test
    void updateTool_fails_whenNotFound() {
        given(toolRepository.findById(9L)).willReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> toolService.updateTool(9L, "Prestada", null, null, user));
    }

    @Test
    void updateTool_disponibleToOther_createsOrMerges_andWritesKardex() {
        // Origen: bucket Disponible con amount=2
        ToolEntity origen = tool(1L, "Taladro", "Elec", "Disponible", 50000, true, 2);
        given(toolRepository.findById(1L)).willReturn(Optional.of(origen));

        // repository para buscar todos con el mismo name/category
        ToolEntity yaPrestada = tool(20L, "Taladro", "Elec", "Prestada", 50000, false, 4);
        given(toolRepository.findByNameAndCategory("Taladro", "Elec"))
                .willReturn(List.of(origen, yaPrestada));

        // save para origen (resta 1) y para target (suma 1)
        given(toolRepository.save(origen)).willAnswer(inv -> inv.getArgument(0));
        given(toolRepository.save(yaPrestada)).willAnswer(inv -> inv.getArgument(0));

        ToolEntity out = toolService.updateTool(1L, "Prestada", null, null, user);

        // Origen -1
        assertEquals(1, origen.getAmount());
        // Destino +1
        assertEquals(5, yaPrestada.getAmount());
        // Retorno es el destino
        assertEquals(20L, out.getId());

        verify(kardexRepository).save(argThat(k ->
                "Cambio de estado: Prestada".equals(k.getType())
                        && k.getTool().getId().equals(20L)
                        && k.getStock() == 5
        ));
    }

    @Test
    void updateTool_disponibleToOther_createsNewTarget_whenMissing() {
        ToolEntity origen = tool(1L, "Taladro", "Elec", "Disponible", 50000, true, 1);
        given(toolRepository.findById(1L)).willReturn(Optional.of(origen));

        // no existe bucket Prestada aún
        given(toolRepository.findByNameAndCategory("Taladro", "Elec"))
                .willReturn(List.of(origen));

        // guardará origen y luego creará destino
        given(toolRepository.save(any(ToolEntity.class))).willAnswer(inv -> {
            ToolEntity t = inv.getArgument(0);
            if (t.getId() == null) t.setId(99L); // id del nuevo bucket
            return t;
        });

        ToolEntity out = toolService.updateTool(1L, "Prestada", null, null, user);
        assertEquals(99L, out.getId()); // nuevo bucket
        verify(kardexRepository).save(any(KardexEntity.class));
    }

    @Test
    void updateTool_otherToDisponible_mergesOrCreates_andWritesKardex() {
        ToolEntity origen = tool(5L, "Sierra", "Manual", "Prestada", 20000, false, 3);
        given(toolRepository.findById(5L)).willReturn(Optional.of(origen));

        ToolEntity disponible = tool(8L, "Sierra", "Manual", "Disponible", 20000, true, 10);

        given(toolRepository.findByNameAndCategory("Sierra", "Manual"))
                .willReturn(List.of(origen, disponible));

        given(toolRepository.save(origen)).willAnswer(inv -> inv.getArgument(0));
        given(toolRepository.save(disponible)).willAnswer(inv -> inv.getArgument(0));

        ToolEntity out = toolService.updateTool(5L, "Disponible", null, null, user);

        assertEquals(2, origen.getAmount());     // -1
        assertEquals(11, disponible.getAmount()); // +1
        assertEquals(8L, out.getId());

        verify(kardexRepository).save(argThat(k ->
                "Cambio de estado: Disponible".equals(k.getType())
                        && k.getTool().getId().equals(8L)
                        && k.getStock() == 11
        ));
    }

    @Test
    void updateTool_otherToOther_usesFindFirstTarget_andWritesKardex() {
        ToolEntity origen = tool(50L, "Llave", "Manual", "En reparación", 15000, false, 2);
        given(toolRepository.findById(50L)).willReturn(Optional.of(origen));

        // destino (Dada de baja) no existe aún al consultar findFirst...
        given(toolRepository.findFirstByNameAndCategoryAndInitialState("Llave", "Manual", "Dada de baja"))
                .willReturn(Optional.empty());

        // guardado de origen y destino nuevo
        given(toolRepository.save(any(ToolEntity.class))).willAnswer(inv -> {
            ToolEntity t = inv.getArgument(0);
            if (t.getId() == null) t.setId(77L);
            return t;
        });

        ToolEntity out = toolService.updateTool(50L, "Dada de baja", null, null, user);

        // origen -1
        assertEquals(1, origen.getAmount());
        // destino nuevo con +1
        assertEquals(77L, out.getId());
        assertEquals("Dada de baja", out.getInitialState());
        assertEquals(1, out.getAmount());

        verify(kardexRepository).save(argThat(k ->
                "Cambio de estado: Dada de baja".equals(k.getType())
                        && k.getTool().getId().equals(77L)
                        && k.getStock() == 1
        ));
    }

    @Test
    void updateTool_invalidState_throws() {
        ToolEntity origen = tool(1L, "Taladro", "Elec", "Disponible", 50000, true, 1);
        given(toolRepository.findById(1L)).willReturn(Optional.of(origen));

        assertThrows(IllegalArgumentException.class,
                () -> toolService.updateTool(1L, "XXX", null, null, user));
    }

    @Test
    void updateTool_setsNewAmount_andNewRepositionValue() {
        ToolEntity t = tool(1L, "Taladro", "Elec", "Disponible", 10000, true, 5);
        given(toolRepository.findById(1L)).willReturn(Optional.of(t));
        given(toolRepository.save(t)).willAnswer(inv -> inv.getArgument(0));

        ToolEntity out = toolService.updateTool(1L, null, 7, 20000, user);

        assertEquals(7, out.getAmount());
        assertEquals(20000, out.getRepositionValue());
    }

    @Test
    void updateTool_negativeAmountOrReposition_throws() {
        ToolEntity t = tool(1L, "Taladro", "Elec", "Disponible", 10000, true, 5);
        given(toolRepository.findById(1L)).willReturn(Optional.of(t));

        assertThrows(IllegalArgumentException.class,
                () -> toolService.updateTool(1L, null, -1, null, user));

        assertThrows(IllegalArgumentException.class,
                () -> toolService.updateTool(1L, null, null, -1, user));
    }

    @Test
    void updateTool_disponibleToOther_withoutStock_throws() {
        ToolEntity t = tool(1L, "Taladro", "Elec", "Disponible", 10000, true, 0);
        given(toolRepository.findById(1L)).willReturn(Optional.of(t));

        assertThrows(IllegalArgumentException.class,
                () -> toolService.updateTool(1L, "Prestada", null, null, user));
    }

    @Test
    void updateTool_otherToDisponible_withoutStock_throws() {
        ToolEntity t = tool(2L, "Taladro", "Elec", "Prestada", 10000, false, 0);
        given(toolRepository.findById(2L)).willReturn(Optional.of(t));

        assertThrows(IllegalArgumentException.class,
                () -> toolService.updateTool(2L, "Disponible", null, null, user));
    }

    @Test
    void updateTool_otherToOther_withoutStock_throws() {
        ToolEntity t = tool(3L, "Taladro", "Elec", "En reparación", 10000, false, 0);
        given(toolRepository.findById(3L)).willReturn(Optional.of(t));

        assertThrows(IllegalArgumentException.class,
                () -> toolService.updateTool(3L, "Dada de baja", null, null, user));
    }

    // ───────────────────────── getToolByName ─────────────────────────

    @Test
    void getToolByName_ok() {
        ToolEntity a = tool(1L, "Martillo", "Man", "Disponible", 1, true, 2);
        given(toolRepository.findByName("Martillo")).willReturn(List.of(a));

        ToolEntity out = toolService.getToolByName("   Martillo  ");
        assertEquals(1L, out.getId());
    }

    @Test
    void getToolByName_fails_whenBlank_orNotFound() {
        assertThrows(IllegalArgumentException.class, () -> toolService.getToolByName("  "));

        given(toolRepository.findByName("Inexistente")).willReturn(List.of());
        assertThrows(IllegalArgumentException.class, () -> toolService.getToolByName("Inexistente"));
    }

    // ──────────────────────── getAllNamesWithCategory ────────────────────────

    @Test
    void getAllNamesWithCategory_uniqueAndStableOrder() {
        ToolEntity a = tool(1L, "Taladro", "Elec", "Disponible", 1, true, 1);
        ToolEntity b = tool(2L, "Taladro", "Elec", "Prestada", 1, false, 1); // duplicado por name+cat
        ToolEntity c = tool(3L, "Sierra", "Manual", "Disponible", 1, true, 1);
        ToolEntity d = tool(4L, null, "Manual", "Disponible", 1, true, 1);  // ignorar nulls
        ToolEntity e = tool(5L, "Llave", null, "Disponible", 1, true, 1);   // ignorar nulls

        given(toolRepository.findAll()).willReturn(List.of(a,b,c,d,e));

        List<ToolService.NameCategory> out = toolService.getAllNamesWithCategory();

        assertEquals(2, out.size());
        assertEquals("Taladro", out.get(0).getName());
        assertEquals("Elec", out.get(0).getCategory());
        assertEquals("Sierra", out.get(1).getName());
        assertEquals("Manual", out.get(1).getCategory());
    }

    // ──────────────────────── listAvailable / listByState ────────────────────────

    @Test
    void listAvailable_delegatesCorrectly() {
        given(toolRepository.findAllByInitialStateIgnoreCaseAndAmountGreaterThan("Disponible", 0))
                .willReturn(List.of());
        assertNotNull(toolService.listAvailable());
        verify(toolRepository).findAllByInitialStateIgnoreCaseAndAmountGreaterThan("Disponible", 0);
    }

    @Test
    void listByState_requiresState_andBranches() {
        assertThrows(IllegalArgumentException.class, () -> toolService.listByState(null));
        assertThrows(IllegalArgumentException.class, () -> toolService.listByState("  "));

        given(toolRepository.findAllByInitialStateIgnoreCaseAndAmountGreaterThan("Disponible", 0))
                .willReturn(List.of());
        toolService.listByState("Disponible");
        verify(toolRepository).findAllByInitialStateIgnoreCaseAndAmountGreaterThan("Disponible", 0);

        given(toolRepository.findAllByInitialStateIgnoreCase("Prestada")).willReturn(List.of());
        toolService.listByState("Prestada");
        verify(toolRepository).findAllByInitialStateIgnoreCase("Prestada");
    }

    // ───────────────────────── helpers ─────────────────────────

    private static ToolEntity tool(Long id, String name, String cat, String state,
                                   Integer repValue, boolean available, int amount) {
        ToolEntity t = new ToolEntity();
        t.setId(id);
        t.setName(name);
        t.setCategory(cat);
        t.setInitialState(state);
        t.setRepositionValue(repValue);
        t.setAvailable(available);
        t.setAmount(amount);
        return t;
    }
}
