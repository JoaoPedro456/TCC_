package com.tcc.backend_TCC.service;

import com.tcc.backend_TCC.exception.RecursoNaoEncontradoException;
import com.tcc.backend_TCC.model.OrdemServico;
import com.tcc.backend_TCC.model.Pessoa;
import com.tcc.backend_TCC.model.Lancamento;
import com.tcc.backend_TCC.enuns.TipoPessoa;
import com.tcc.backend_TCC.enuns.StatusLancamento;
import com.tcc.backend_TCC.enuns.TipoLancamento;
import com.tcc.backend_TCC.repository.LancamentoRepository;
import com.tcc.backend_TCC.repository.OrdemServicoMecanicoRepository;
import com.tcc.backend_TCC.repository.OrdemServicoRepository;
import com.tcc.backend_TCC.repository.PessoaRepository;
import com.tcc.backend_TCC.service.LancamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private LancamentoService lancamentoService;

    @Autowired
    private PessoaRepository personRepository;

    @Autowired
    private OrdemServicoRepository ordemServicoRepository;

    @Autowired
    private OrdemServicoMecanicoRepository mecanicoRepository;

    // Helper to avoid null values from DB
    private BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    public Map<String, Object> relatorioFuncionario(Long id, LocalDate inicio, LocalDate fim) {
        Pessoa funcionario = personRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado"));
        BigDecimal totalComissao = zeroSeNulo(mecanicoRepository.totalComissaoPorMecanicoEPeriodo(id, inicio, fim));
        BigDecimal salarioBase = funcionario.getSalarioBase() != null ? BigDecimal.valueOf(funcionario.getSalarioBase()) : BigDecimal.ZERO;
        long quantidadeOS = mecanicoRepository.contarOSPormecanicoEPeriodo(id, inicio, fim);
        double percentual = funcionario.getPercentualComissao() != null ? funcionario.getPercentualComissao() : 0;
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("id", id);
        resultado.put("nome", funcionario.getNome());
        resultado.put("cargo", funcionario.getCargo() != null ? funcionario.getCargo() : "Sem cargo");
        resultado.put("percentualComissao", percentual);
        resultado.put("salarioBase", salarioBase);
        resultado.put("totalComissao", totalComissao);
        resultado.put("totalReceber", salarioBase.add(totalComissao));
        resultado.put("quantidadeOS", quantidadeOS);
        return resultado;
    }

    public List<Map<String, Object>> relatorioTodosFuncionarios(LocalDate inicio, LocalDate fim) {
        return personRepository.findByTipo(TipoPessoa.FUNCIONARIO)
                .stream()
                .map(f -> relatorioFuncionario(f.getId(), inicio, fim))
                .sorted((a, b) -> ((BigDecimal) b.get("totalReceber")).compareTo((BigDecimal) a.get("totalReceber")))
                .collect(Collectors.toList());
    }

    public Map<String, Object> faturamentoPorPeriodo(LocalDate inicio, LocalDate fim) {
        BigDecimal total = zeroSeNulo(ordemServicoRepository.totalFaturadoPorPeriodo(inicio, fim));
        List<OrdemServico> lista = ordemServicoRepository.findByPeriodoReferencia(inicio, fim);
        long concluidas = contarPorStatus(lista, "CONCLUIDA");
        long abertas = contarPorStatus(lista, "ABERTA");
        long canceladas = contarPorStatus(lista, "CANCELADA");
        return Map.of(
                "periodo", inicio.toString() + " até " + fim.toString(),
                "totalFaturado", total,
                "totalOS", (long) lista.size(),
                "concluidas", concluidas,
                "abertas", abertas,
                "canceladas", canceladas
        );
    }

    /**
     * Retorna os avisos de vencimento para o dashboard (OSs a receber e contas a pagar em aberto):
     * - vencendoHoje: lançamentos cujo vencimento é hoje e ainda não pagos.
     * - vencendoAmanha: lançamentos cujo vencimento é amanhã e ainda não pagos.
     * - atrasados: lançamentos cujo vencimento já passou e ainda não foram pagos.
     */
    private Map<String,Object> lancamentoToMap(Lancamento l) {
        Map<String,Object> m = new HashMap<>();
        m.put("id", l.getId());
        m.put("descricao", l.getDescricao());
        m.put("envolvido", l.getEnvolvido());
        m.put("valor", l.getValor());
        m.put("vencimento", l.getVencimento());
        m.put("status", l.getStatus());
        m.put("tipo", l.getTipo());
        m.put("ordemServicoId", l.getOrdemServicoId());
        return m;
    }

    public Map<String, Object> contasAPagarDashboard() {
        LocalDate hoje = LocalDate.now();
        LocalDate amanha = hoje.plusDays(1);
        List<Lancamento> contas = lancamentoRepository.findByTipoOrderByVencimentoAsc(TipoLancamento.PAGAR);
        List<Map<String,Object>> hojeList = new ArrayList<>();
        List<Map<String,Object>> amanhaList = new ArrayList<>();
        List<Map<String,Object>> atrasadosList = new ArrayList<>();
        for (Lancamento c : contas) {
            if (c.getStatus() == StatusLancamento.PAGO) {
                continue;
            }
            LocalDate v = c.getVencimento();
            if (v != null) {
                if (v.isBefore(hoje)) {
                    if (c.getStatus() != StatusLancamento.ATRASADO) {
                        c.setStatus(StatusLancamento.ATRASADO);
                        lancamentoRepository.save(c);
                    }
                    atrasadosList.add(lancamentoToMap(c));
                } else {
                    if (c.getStatus() == StatusLancamento.ATRASADO) {
                        c.setStatus(StatusLancamento.PENDENTE);
                        lancamentoRepository.save(c);
                    }
                    if (v.isEqual(hoje)) {
                        hojeList.add(lancamentoToMap(c));
                    } else if (v.isEqual(amanha)) {
                        amanhaList.add(lancamentoToMap(c));
                    }
                }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("vencendoHoje", hojeList);
        result.put("vencendoAmanha", amanhaList);
        result.put("atrasados", atrasadosList);
        return result;
    }

    public Map<String, Object> dashboard() {
        LocalDate hoje = LocalDate.now();
        LocalDate mesAtual = hoje.withDayOfMonth(1);
        // O faturamento e as métricas do período consideram a data de realização/conclusão da OS
        List<OrdemServico> lista = ordemServicoRepository.findByPeriodoReferencia(mesAtual, hoje);
        BigDecimal faturamentoMes = lista.stream()
                .filter(os -> os.getStatus() != null && "CONCLUIDA".equals(os.getStatus().name()))
                .map(os -> zeroSeNulo(os.getValorTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalClientes = personRepository.countByTipoAndAtivoTrue(TipoPessoa.CLIENTE);
        long totalFuncionarios = personRepository.countByTipoAndAtivoTrue(TipoPessoa.FUNCIONARIO);
        long osAbertas = contarPorStatus(lista, "ABERTA");
        long osConcluidasMes = contarPorStatus(lista, "CONCLUIDA");
        long osHoje = lista.stream().filter(os -> {
            LocalDate ref = os.getDataRealizacao() != null ? os.getDataRealizacao() : os.getDataRegisto();
            return hoje.equals(ref);
        }).count();
        long hojeAbertas = lista.stream().filter(os -> {
            LocalDate ref = os.getDataRealizacao() != null ? os.getDataRealizacao() : os.getDataRegisto();
            return hoje.equals(ref) && os.getStatus() != null && "ABERTA".equals(os.getStatus().name());
        }).count();
        long hojeConcluidas = lista.stream().filter(os -> {
            LocalDate ref = os.getDataRealizacao() != null ? os.getDataRealizacao() : os.getDataRegisto();
            return hoje.equals(ref) && os.getStatus() != null && "CONCLUIDA".equals(os.getStatus().name());
        }).count();
        long hojeCanceladas = lista.stream().filter(os -> {
            LocalDate ref = os.getDataRealizacao() != null ? os.getDataRealizacao() : os.getDataRegisto();
            return hoje.equals(ref) && os.getStatus() != null && "CANCELADA".equals(os.getStatus().name());
        }).count();
        BigDecimal ticketMedio = osConcluidasMes > 0
                ? faturamentoMes.divide(BigDecimal.valueOf(osConcluidasMes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Map<String, Object> dash = new HashMap<>();
        dash.put("dataGeracao", hoje.toString());
        dash.put("mesReferencia", mesAtual.getMonth().name() + "/" + hoje.getYear());
        dash.put("faturamentoMes", faturamentoMes);
        dash.put("osMes", (long) lista.size());
        dash.put("osAbertasMes", osAbertas);
        dash.put("osConcluidasMes", osConcluidasMes);
        dash.put("osHoje", osHoje);
        dash.put("hojeAbertas", hojeAbertas);
        dash.put("hojeConcluidas", hojeConcluidas);
        dash.put("hojeCanceladas", hojeCanceladas);
        dash.put("osEmServico", contarPorStatus(lista, "EM_SERVICO"));
        dash.put("osAguardandoPeca", contarPorStatus(lista, "AGUARDANDO_PECA"));
        dash.put("osCanceladas", contarPorStatus(lista, "CANCELADA"));
        dash.put("ticketMedio", ticketMedio);
        dash.put("totalClientes", totalClientes);
        dash.put("totalFuncionarios", totalFuncionarios);
        // Histórico de faturamento dos últimos 6 meses
        LocalDate inicioPeriodo = hoje.minusMonths(5).withDayOfMonth(1);
        List<Object[]> faturamentoAgrupado = ordemServicoRepository.faturamentoMensalAgrupado(inicioPeriodo, hoje);
        Map<String, BigDecimal> faturamentoMap = new HashMap<>();
        for (Object[] row : faturamentoAgrupado) {
            Integer ano = (Integer) row[0];
            Integer mes = (Integer) row[1];
            BigDecimal total = (BigDecimal) row[2];
            faturamentoMap.put(ano + "-" + mes, total);
        }
        List<Map<String, Object>> historicoFaturamento = new ArrayList<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM", new Locale("pt", "BR"));
        for (int i = 5; i >= 0; i--) {
            LocalDate inicioMes = hoje.minusMonths(i).withDayOfMonth(1);
            String chave = inicioMes.getYear() + "-" + inicioMes.getMonthValue();
            BigDecimal fatMes = zeroSeNulo(faturamentoMap.get(chave));
            Map<String, Object> dadosMes = new HashMap<>();
            String nomeMes = inicioMes.format(formatter);
            nomeMes = nomeMes.substring(0, 1).toUpperCase() + nomeMes.substring(1);
            dadosMes.put("mes", nomeMes);
            dadosMes.put("faturamento", fatMes);
            historicoFaturamento.add(dadosMes);
        }
        dash.put("historicoFaturamento", historicoFaturamento);
        return dash;
    }

    // Helper to count by status
    private long contarPorStatus(List<OrdemServico> lista, String statusNome) {
        return lista.stream()
                .filter(os -> os.getStatus() != null && os.getStatus().name().equals(statusNome))
                .count();
    }
}
