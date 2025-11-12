package com.example.demo.services;

import com.example.demo.entities.LoanEntity;
import com.example.demo.entities.LoanItemEntity;
import com.example.demo.entities.ToolEntity;
import com.example.demo.entities.UserEntity;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.ToolRepository;
import com.example.demo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock LoanRepository loanRepository;
    @Mock ToolRepository toolRepository;
    @Mock UserRepository userRepository;
    @Mock ToolService toolService;
    @Mock UserService userService;
    @Mock SettingService settingService;

    @InjectMocks LoanService loanService;

    UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setRut("11.111.111-1");
        user.setActive(true);
        user.setAmountOfLoans(0);
    }

    // ---------- createLoan: éxito ----------
    @Test
    void createLoan_ok() {
        LocalDate res = LocalDate.of(2025, 10, 1);
        LocalDate ret = LocalDate.of(2025, 10, 04); // 3 días

        // user encontrado, y tras recompute sigue activo
        given(userRepository.findByRut("11.111.111-1"))
                .willReturn(user)        // 1ra consulta
                .willReturn(user);       // 2da consulta (refreshed)
        // no supera máximo de 5
        given(loanRepository.countByRutUserAndLateReturnDateIsNull("11.111.111-1")).willReturn(0L);
        // precio diario
        given(settingService.getDailyRentPrice()).willReturn(2000);

        // tool "Disponible" con stock suficiente
        ToolEntity disponible = tool(100L, "Taladro", "Elec", "Disponible", 3, 50000, true);
        given(toolRepository.findById(100L)).willReturn(Optional.of(disponible));
        // no hay “Prestada” del mismo nombre/categoría
        given(toolRepository.findIdsByNameCategoryAndState("Taladro", "Elec", "Prestada"))
                .willReturn(List.of());
        // update a Prestada devuelve la entidad en Prestada (id puede ser mismo o bucket distinto)
        ToolEntity prestada = tool(200L, "Taladro", "Elec", "Prestada", 3, 50000, false);
        given(toolService.updateTool(eq(100L), eq("Prestada"), isNull(), isNull(), org.mockito.ArgumentMatchers.any(UserEntity.class)))
                .willReturn(prestada);

        // save devuelve la misma entidad con id asignado
        ArgumentCaptor<LoanEntity> cap = ArgumentCaptor.forClass(LoanEntity.class);
        given(loanRepository.save(cap.capture())).willAnswer(inv -> {
            LoanEntity le = cap.getValue();
            le.setId(10L);
            return le;
        });

        LoanService.Item it = new LoanService.Item();
        it.toolId = 100L; it.quantity = 1;

        LoanEntity out = loanService.createLoan("11.111.111-1", res, ret, List.of(it));

        assertEquals(10L, out.getId());
        assertEquals("11.111.111-1", out.getRutUser());
        assertEquals(res, out.getReservationDate());
        assertEquals(ret, out.getReturnDate());
        // total = 3 días * 2000
        assertEquals(6000, out.getTotal());
        // item agregado con tool prestada
        assertThat(out.getItems(), hasSize(1));
        assertEquals(200L, out.getItems().get(0).getTool().getId());

        // incrementó contador de préstamos del usuario y recompute llamado
        verify(userRepository).save(argThat(u -> u.getAmountOfLoans() == 1));
        verify(userService, times(2)).recomputeActiveStatus("11.111.111-1");
    }

    // ---------- createLoan: validaciones ----------
    @Test
    void createLoan_fails_onNullDates() {
        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("rut", null, LocalDate.now(), List.of(new LoanService.Item())));
        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("rut", LocalDate.now(), null, List.of(new LoanService.Item())));
    }

    @Test
    void createLoan_fails_whenReturnBeforeReservation() {
        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("rut",
                        LocalDate.of(2025,1,2), LocalDate.of(2025,1,1), List.of(new LoanService.Item())));
    }

    @Test
    void createLoan_fails_onEmptyItems() {
        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("rut", LocalDate.now(), LocalDate.now().plusDays(1), List.of()));
    }

    @Test
    void createLoan_fails_userNotFound() {
        given(userRepository.findByRut("X")).willReturn(null);
        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("X", LocalDate.now(), LocalDate.now().plusDays(1),
                        List.of(new LoanService.Item())));
    }

    @Test
    void createLoan_fails_userInactiveAfterRecompute() {
        given(userRepository.findByRut("11.111.111-1")).willReturn(user).willReturn(inactiveUser());
        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("11.111.111-1", LocalDate.now(), LocalDate.now().plusDays(1),
                        List.of(oneItem(1L))));
    }

    @Test
    void createLoan_fails_whenAlready5Active() {
        given(userRepository.findByRut(anyString())).willReturn(user).willReturn(user);
        given(loanRepository.countByRutUserAndLateReturnDateIsNull(anyString())).willReturn(5L);
        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("11", LocalDate.now(), LocalDate.now().plusDays(1),
                        List.of(oneItem(1L))));
    }

    @Test
    void createLoan_fails_onRepeatedTool() {
        given(userRepository.findByRut(anyString())).willReturn(user).willReturn(user);
        given(loanRepository.countByRutUserAndLateReturnDateIsNull(anyString())).willReturn(0L);
        LoanService.Item a = oneItem(1L); LoanService.Item b = oneItem(1L);
        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("11", LocalDate.now(), LocalDate.now().plusDays(1),
                        List.of(a,b)));
    }

    @Test
    void createLoan_fails_onInvalidQuantity() {
        given(userRepository.findByRut(anyString())).willReturn(user).willReturn(user);
        given(loanRepository.countByRutUserAndLateReturnDateIsNull(anyString())).willReturn(0L);

        LoanService.Item x = new LoanService.Item(); x.toolId = 1L; x.quantity = 0;
        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("11", LocalDate.now(), LocalDate.now().plusDays(1), List.of(x)));

        LoanService.Item y = new LoanService.Item(); y.toolId = 1L; y.quantity = 2;
        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("11", LocalDate.now(), LocalDate.now().plusDays(1), List.of(y)));
    }

    @Test
    void createLoan_fails_toolNotFound_orWrongState_orNoStock() {
        given(userRepository.findByRut(anyString())).willReturn(user).willReturn(user);
        given(loanRepository.countByRutUserAndLateReturnDateIsNull(anyString())).willReturn(0L);

        // no encontrada
        given(toolRepository.findById(1L)).willReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("11", LocalDate.now(), LocalDate.now().plusDays(1),
                        List.of(oneItem(1L))));

        // encontrada pero no "Disponible"
        ToolEntity tWrong = tool(2L,"Taladro","Elec","Prestada",1,0,true);
        given(toolRepository.findById(2L)).willReturn(Optional.of(tWrong));
        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("11", LocalDate.now(), LocalDate.now().plusDays(1),
                        List.of(oneItem(2L))));

        // encontrada Disponible pero sin stock
        ToolEntity tNoStock = tool(3L,"Taladro","Elec","Disponible",0,0,true);
        given(toolRepository.findById(3L)).willReturn(Optional.of(tNoStock));
        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("11", LocalDate.now(), LocalDate.now().plusDays(1),
                        List.of(oneItem(3L))));
    }

    @Test
    void createLoan_fails_whenAlreadyActiveLoanOfSameTool() {
        given(userRepository.findByRut(anyString())).willReturn(user).willReturn(user);
        given(loanRepository.countByRutUserAndLateReturnDateIsNull(anyString())).willReturn(0L);

        ToolEntity disp = tool(4L,"Taladro","Elec","Disponible",2,0,true);
        given(toolRepository.findById(4L)).willReturn(Optional.of(disp));

        given(toolRepository.findIdsByNameCategoryAndState("Taladro","Elec","Prestada"))
                .willReturn(List.of(200L, 201L));
        given(loanRepository.existsActiveWithAnyToolId("11", List.of(200L,201L))).willReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan("11", LocalDate.now(), LocalDate.now().plusDays(1),
                        List.of(oneItem(4L))));
    }

    // ---------- returnLoan: éxito ----------
    @Test
    void returnLoan_ok_mixedStates_andFines() {
        // Loan con 3 items: irreparable (id=1, rep=1000), dañada (id=2, rep=5000), normal (id=3)
        LoanEntity loan = new LoanEntity();
        loan.setId(77L);
        loan.setRutUser("11.111.111-1");
        loan.setReservationDate(LocalDate.of(2025,10,1));
        loan.setReturnDate(LocalDate.of(2025,10,5)); // vence día 5
        loan.setItems(new ArrayList<>());

        ToolEntity t1 = tool(1L, "Taladro", "Elec", "Prestada", 0, 1000, false);
        ToolEntity t2 = tool(2L, "Sierra", "Man", "Prestada", 0, 5000, false);
        ToolEntity t3 = tool(3L, "Llave", "Man", "Prestada", 0, 0, false);

        loan.addItem(lineOf(t1));
        loan.addItem(lineOf(t2));
        loan.addItem(lineOf(t3));

        given(loanRepository.findById(77L)).willReturn(Optional.of(loan));
        given(loanRepository.save(org.mockito.ArgumentMatchers.any(LoanEntity.class))).willAnswer(inv -> inv.getArgument(0));
        // cliente para decrementar amountOfLoans
        UserEntity customer = new UserEntity(); customer.setRut("11.111.111-1"); customer.setAmountOfLoans(2);
        given(userRepository.findByRut("11.111.111-1")).willReturn(customer);

        Map<Long,Integer> repairCosts = Map.of(2L, 300, 99L, 9999); // 99 ignora
        LoanEntity out = loanService.returnLoan(
                77L,
                LocalDate.of(2025,10,7),            // 2 días tarde
                Set.of(2L),                          // dañada
                Set.of(1L),                          // irreparable
                500,                                 // multa diaria
                repairCosts
        );

        assertEquals(LocalDate.of(2025,10,7), out.getLateReturnDate());
        assertEquals(1000 + 300, out.getDamagePenalty());   // repuesto + reparación
        assertEquals(2 * 500, out.getLateFine());           // 2 días * 500

        // estados actualizados
        verify(toolService).updateTool(eq(1L), eq("Dada de baja"), isNull(), isNull(), org.mockito.ArgumentMatchers.any(UserEntity.class));
        verify(toolService).updateTool(eq(2L), eq("En reparación"), isNull(), isNull(), org.mockito.ArgumentMatchers.any(UserEntity.class));
        verify(toolService).updateTool(eq(3L), eq("Disponible"), isNull(), isNull(), org.mockito.ArgumentMatchers.any(UserEntity.class));

        // decremento de amountOfLoans y recompute
        verify(userRepository).save(argThat(u -> u.getAmountOfLoans() == 1));
        verify(userService).recomputeActiveStatus("11.111.111-1");
    }

    // ---------- returnLoan: validaciones ----------
    @Test
    void returnLoan_fails_onNullActualDate() {
        assertThrows(IllegalArgumentException.class,
                () -> loanService.returnLoan(1L, null, null, null, null, null));
    }

    @Test
    void returnLoan_fails_whenLoanNotFound() {
        given(loanRepository.findById(1L)).willReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> loanService.returnLoan(1L, LocalDate.now(), null, null, null, null));
    }

    @Test
    void returnLoan_fails_whenAlreadyReturned() {
        LoanEntity l = new LoanEntity(); l.setId(1L); l.setLateReturnDate(LocalDate.now());
        given(loanRepository.findById(1L)).willReturn(Optional.of(l));
        assertThrows(IllegalArgumentException.class,
                () -> loanService.returnLoan(1L, LocalDate.now(), null, null, null, null));
    }

    @Test
    void returnLoan_fails_onIntersectingSets() {
        LoanEntity l = basicLoanWithToolIds(1L, 2L);
        given(loanRepository.findById(9L)).willReturn(Optional.of(l));
        assertThrows(IllegalArgumentException.class,
                () -> loanService.returnLoan(9L, LocalDate.now(), Set.of(1L), Set.of(1L), null, null));
    }

    @Test
    void returnLoan_fails_onDamagedNotBelonging() {
        LoanEntity l = basicLoanWithToolIds(10L, 20L);
        given(loanRepository.findById(9L)).willReturn(Optional.of(l));
        assertThrows(IllegalArgumentException.class,
                () -> loanService.returnLoan(9L, LocalDate.now(), Set.of(999L), Set.of(), null, null));
    }

    @Test
    void returnLoan_fails_onIrreparableNotBelonging() {
        LoanEntity l = basicLoanWithToolIds(10L, 20L);
        given(loanRepository.findById(9L)).willReturn(Optional.of(l));
        assertThrows(IllegalArgumentException.class,
                () -> loanService.returnLoan(9L, LocalDate.now(), Set.of(), Set.of(999L), null, null));
    }

    // ---------- payFines ----------
    @Test
    void payFines_setsFlags_andRecomputes() {
        LoanEntity l = new LoanEntity();
        l.setId(5L); l.setRutUser("11.111.111-1");
        l.setLateFine(1000); l.setDamagePenalty(2000);
        given(loanRepository.findById(5L)).willReturn(Optional.of(l));
        given(loanRepository.save(org.mockito.ArgumentMatchers.any(LoanEntity.class))).willAnswer(inv -> inv.getArgument(0));

        LoanEntity out = loanService.payFines(5L, true, true);

        assertTrue(out.isLateFinePaid());
        assertTrue(out.isDamagePenaltyPaid());
        verify(userService).recomputeActiveStatus("11.111.111-1");
    }

    // ---------- listados auxiliares ----------
    @Test
    void listActiveLoans_ok() {
        given(loanRepository.findByRutUserAndLateReturnDateIsNull("11")).willReturn(List.of());
        assertEquals(0, loanService.listActiveLoans("11").size());
    }

    @Test
    void listAllActiveLoans_ok() {
        given(loanRepository.findByLateReturnDateIsNull()).willReturn(List.of());
        assertEquals(0, loanService.listAllActiveLoans().size());
    }

    @Test
    void listLoansWithUnpaidDebts_mapsParams() {
        Pageable pr = PageRequest.of(0, 10);
        Page<LoanEntity> pg = new PageImpl<>(List.of(), pr, 0);
        given(loanRepository.findLoansWithUnpaidDebts(any(), anyBoolean(), any(), anyBoolean(), any(), any()))
                .willReturn(pg);

        // rut "" -> null; sin fechas
        Page<LoanEntity> out = loanService.listLoansWithUnpaidDebts("", null, null, pr);
        assertSame(pg, out);

        // verify flags
        verify(loanRepository).findLoansWithUnpaidDebts(
                isNull(), eq(false), isNull(), eq(false), isNull(), eq(pr));
    }

    @Test
    void listOverdueLoans_withAndWithoutRut() {
        Pageable pr = PageRequest.of(0, 5, Sort.by("returnDate"));
        Page<LoanEntity> pg1 = new PageImpl<>(List.of(), pr, 0);
        Page<LoanEntity> pg2 = new PageImpl<>(List.of(), pr, 0);

        given(loanRepository.findByLateReturnDateIsNullAndReturnDateBefore(org.mockito.ArgumentMatchers.any(LocalDate.class), eq(pr)))
                .willReturn(pg1);
        given(loanRepository.findByRutUserAndLateReturnDateIsNullAndReturnDateBefore(eq("11"),
                org.mockito.ArgumentMatchers.any(LocalDate.class), eq(pr)))
                .willReturn(pg2);

        assertSame(pg1, loanService.listOverdueLoans(null, pr));
        assertSame(pg2, loanService.listOverdueLoans("11", pr));
        assertSame(pg1, loanService.listOverdueLoans(" ", pr)); // blank -> rama sin rut
    }

    // ───────────── helpers ─────────────

    private static LoanService.Item oneItem(Long toolId) {
        LoanService.Item i = new LoanService.Item();
        i.toolId = toolId; i.quantity = 1;
        return i;
    }

    private static ToolEntity tool(Long id, String name, String cat, String state,
                                   int amount, Integer rep, boolean available) {
        ToolEntity t = new ToolEntity();
        t.setId(id); t.setName(name); t.setCategory(cat);
        t.setInitialState(state); t.setAmount(amount);
        t.setRepositionValue(rep); t.setAvailable(available);
        return t;
    }

    private static LoanItemEntity lineOf(ToolEntity t) {
        LoanItemEntity li = new LoanItemEntity();
        li.setTool(t);
        li.setToolNameSnapshot(t.getName());
        return li;
    }

    private static UserEntity inactiveUser() {
        UserEntity u = new UserEntity();
        u.setRut("11.111.111-1");
        u.setActive(false);
        return u;
    }

    private static LoanEntity basicLoanWithToolIds(Long... ids) {
        LoanEntity l = new LoanEntity();
        l.setId(9L);
        l.setItems(new ArrayList<>());
        for (Long id : ids) {
            ToolEntity t = new ToolEntity(); t.setId(id);
            l.addItem(lineOf(t));
        }
        return l;
    }
}
