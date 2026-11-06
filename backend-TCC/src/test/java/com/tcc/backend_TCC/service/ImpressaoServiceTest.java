package com.tcc.backend_TCC.service;

import com.tcc.backend_TCC.enuns.StatusOS;
import com.tcc.backend_TCC.model.*;
import com.tcc.backend_TCC.repository.OrdemServicoMecanicoRepository;
import com.tcc.backend_TCC.repository.OrdemServicoRepository;
import com.tcc.backend_TCC.repository.PessoaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImpressaoServiceTest {

    @Mock
    private OrdemServicoRepository osRepository;

    @Mock
    private PessoaRepository pessoaRepository;

    @Mock
    private OrdemServicoMecanicoRepository mecanicoRepository;

    @InjectMocks
    private ImpressaoService impressaoService;

    @Test
    void gerarPdfOs_comSucesso_retornaBytes() throws Exception {
        Pessoa cliente = new Pessoa();
        cliente.setId(1L);
        cliente.setNome("João Cliente");
        cliente.setCpf("123.456.789-00");
        cliente.setTelefone("(11) 99999-9999");
        cliente.setLogradouro("Rua das Flores, 123");

        Pessoa mecanico = new Pessoa();
        mecanico.setId(2L);
        mecanico.setNome("Carlos Mecânico");

        OrdemServico os = new OrdemServico();
        os.setId(1L);
        os.setCliente(cliente);
        os.setVeiculo("Fiat Uno 2015");
        os.setQuilometragem(120.0);
        os.setValorKm(BigDecimal.valueOf(1.50));
        os.setValorTotal(BigDecimal.valueOf(500.00));
        os.setStatus(StatusOS.ABERTA);
        os.setObservacao("Trocar pastilha de freio.");
        os.setDataRegisto(LocalDate.now());

        ItemServico item = new ItemServico();
        item.setId(1L);
        item.setNomeServico("Troca de Pastilha");

        OrdemServicoItem osItem = new OrdemServicoItem();
        osItem.setId(1L);
        osItem.setItemServico(item);
        osItem.setPrecoCobrado(BigDecimal.valueOf(320.00));
        os.setItensServico(List.of(osItem));

        OrdemServicoMecanico osMec = new OrdemServicoMecanico();
        osMec.setId(1L);
        osMec.setMecanico(mecanico);
        osMec.setValorAtribuido(BigDecimal.valueOf(50.00));
        os.setMecanicos(List.of(osMec));

        when(osRepository.findById(1L)).thenReturn(Optional.of(os));

        byte[] pdfBytes = impressaoService.gerarPdfOs(1L);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        verify(osRepository).findById(1L);
    }

    @Test
    void gerarPdfComissao_comSucesso_retornaBytes() throws Exception {
        Pessoa func = new Pessoa();
        func.setId(2L);
        func.setNome("Carlos Mecânico");
        func.setCargo("Mecânico Líder");
        func.setSalarioBase(2500.00);

        OrdemServico os = new OrdemServico();
        os.setId(10L);
        os.setDataRegisto(LocalDate.of(2026, 6, 1));
        
        Pessoa cliente = new Pessoa();
        cliente.setNome("Maria Cliente");
        os.setCliente(cliente);

        OrdemServicoMecanico participacao = new OrdemServicoMecanico();
        participacao.setId(1L);
        participacao.setOrdemServico(os);
        participacao.setMecanico(func);
        participacao.setValorAtribuido(BigDecimal.valueOf(150.00));

        when(pessoaRepository.findById(2L)).thenReturn(Optional.of(func));
        when(mecanicoRepository.buscarComissoesMes(eq(2L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(participacao));

        byte[] pdfBytes = impressaoService.gerarPdfComissao(2L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        verify(pessoaRepository).findById(2L);
        verify(mecanicoRepository).buscarComissoesMes(eq(2L), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void gerarPdfRelatorioGeral_comSucesso_retornaBytes() throws Exception {
        java.util.Map<String, Object> faturamento = java.util.Map.of(
                "totalFaturado", BigDecimal.valueOf(12500.00),
                "totalOS", 15L,
                "concluidas", 12L,
                "abertas", 2L,
                "canceladas", 1L
        );

        java.util.Map<String, Object> func1 = new java.util.HashMap<>();
        func1.put("id", 2L);
        func1.put("nome", "Carlos Mecânico");
        func1.put("cargo", "Mecânico Líder");
        func1.put("percentualComissao", 10.0);
        func1.put("salarioBase", BigDecimal.valueOf(2500.00));
        func1.put("totalComissao", BigDecimal.valueOf(850.00));
        func1.put("totalReceber", BigDecimal.valueOf(3350.00));
        func1.put("quantidadeOS", 8L);

        byte[] pdfBytes = impressaoService.gerarPdfRelatorioGeral(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                faturamento,
                List.of(func1)
        );

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
