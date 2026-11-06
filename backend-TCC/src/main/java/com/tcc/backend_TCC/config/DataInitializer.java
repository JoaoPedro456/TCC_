package com.tcc.backend_TCC.config;

import com.tcc.backend_TCC.enuns.StatusLancamento;
import com.tcc.backend_TCC.model.Lancamento;
import com.tcc.backend_TCC.model.Usuario;
import com.tcc.backend_TCC.repository.LancamentoRepository;
import com.tcc.backend_TCC.repository.OrdemServicoRepository;
import com.tcc.backend_TCC.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired(required = false)
    private LancamentoRepository lancamentoRepository;

    @Autowired(required = false)
    private OrdemServicoRepository ordemServicoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.core.env.Environment environment;

    @Value("${admin.default-login:admin}")
    private String defaultAdminLogin;

    @Value("${admin.default-password:#{null}}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) {
        inicializarAdmin();
        recalcularVencimentosOsAtrasadas();
    }

    private void inicializarAdmin() {
        if (usuarioRepository.count() == 0) {
            String senha = defaultAdminPassword;
            if (senha == null || senha.trim().isEmpty()) {
                senha = java.util.UUID.randomUUID().toString().substring(0, 10);
                if (environment != null && environment.matchesProfiles("dev")) {
                    log.warn("Nenhuma senha padrao fornecida via variavel de ambiente.");
                    log.warn("Senha gerada aleatoriamente para o primeiro acesso (dev): {}", senha);
                } else {
                    log.warn("Admin inicial criado com senha gerada. Configure ADMIN_DEFAULT_PASSWORD no servidor.");
                }
            }

            Usuario admin = new Usuario();
            admin.setLogin(defaultAdminLogin);
            admin.setSenha(passwordEncoder.encode(senha));
            admin.setRole("ADMIN");
            usuarioRepository.save(admin);
            log.info("Usuario administrador inicial configurado: {}", defaultAdminLogin);
        }
    }

    /**
     * Recalcula o vencimento para lançamentos de OS que estavam marcados como ATRASADOS
     * alinhando com a nova regra de negócio: Vencimento = Data de Realização + 30 dias.
     * Mantém intocados registros já pagos ou pendentes que não estavam atrasados.
     */
    private void recalcularVencimentosOsAtrasadas() {
        if (lancamentoRepository == null || ordemServicoRepository == null) {
            return;
        }
        try {
            LocalDate hoje = LocalDate.now();
            List<Lancamento> atrasados = lancamentoRepository.findAll().stream()
                    .filter(l -> l.getOrdemServicoId() != null && l.getStatus() == StatusLancamento.ATRASADO)
                    .toList();

            for (Lancamento lanc : atrasados) {
                ordemServicoRepository.findById(lanc.getOrdemServicoId()).ifPresent(os -> {
                    LocalDate dataBase = os.getDataRealizacao() != null ? os.getDataRealizacao() : os.getDataRegisto();
                    if (dataBase != null) {
                        LocalDate novoVencimento = dataBase.plusDays(30);
                        lanc.setVencimento(novoVencimento);
                        if (novoVencimento.isBefore(hoje)) {
                            lanc.setStatus(StatusLancamento.ATRASADO);
                        } else {
                            lanc.setStatus(StatusLancamento.PENDENTE);
                        }
                        lancamentoRepository.save(lanc);
                        log.info("Lançamento da OS #{} recalculado: Novo Vencimento {}, Status {}", os.getId(), novoVencimento, lanc.getStatus());
                    }
                });
            }
        } catch (Exception e) {
            log.error("Erro ao recalcular vencimento das OSs atrasadas: {}", e.getMessage());
        }
    }
}
