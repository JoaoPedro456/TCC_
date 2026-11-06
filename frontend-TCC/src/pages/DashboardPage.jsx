import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { Users, ClipboardList, ArrowUpRight, CheckCircle2, Calendar, Activity, Folder, FileText, Clock, ChevronRight, AlertCircle, Bell, PlusCircle, DollarSign } from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { formatarMoeda } from '../utils/formatters';
// --- Hook para Animação dos Números (CountUp) ---
function AnimatedNumber({ value, prefix = '', suffix = '', isCurrency = false }) {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const endValue = Number(value) || 0;
    if (endValue === 0) return;
    
    let startTimestamp = null;
    const duration = 1500; // 1.5 segundos

    const step = (timestamp) => {
      if (!startTimestamp) startTimestamp = timestamp;
      const progress = Math.min((timestamp - startTimestamp) / duration, 1);
      // Ease out expo
      const easeProgress = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);
      setCount(easeProgress * endValue);
      
      if (progress < 1) {
        window.requestAnimationFrame(step);
      } else {
        setCount(endValue);
      }
    };
    window.requestAnimationFrame(step);
  }, [value]);

  if (isCurrency) {
    return <span>{prefix}{formatarMoeda(count)}{suffix}</span>;
  }
  return <span>{prefix}{Math.floor(count)}{suffix}</span>;
}

// Os dados agora vêm do backend!

export function DashboardPage() {
  const [nomeUsuario, setNomeUsuario] = useState('');
  const [stats, setStats] = useState({
    clientes: 0, 
    funcionarios: 0, 
    faturamentoMes: 0, 
    osMes: 0,
    osConcluidasMes: 0,
    ordensHojeTotal: 0,
    hojeAbertas: 0,
    hojeConcluidas: 0,
    hojeCanceladas: 0
  });
  
  const [contasAPagar, setContasAPagar] = useState({ vencendoHoje: [], vencendoAmanha: [], atrasados: [] });
  const [abaAvisos, setAbaAvisos] = useState('atrasados'); // 'atrasados' | 'hoje' | 'amanha'
  const [historicoFaturamento, setHistoricoFaturamento] = useState([]);
  const [ultimasOrdens, setUltimasOrdens] = useState([]);
  const [loading, setLoading] = useState(true);
  const [mesFormatado, setMesFormatado] = useState('');

  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        setNomeUsuario(payload.sub || 'Usuário');
      } catch {
        setNomeUsuario('Usuário');
      }
    }

    const d = new Date();
    const year = d.getFullYear();
    const nomeMes = d.toLocaleString('pt-BR', { month: 'long' });
    setMesFormatado(nomeMes.charAt(0).toUpperCase() + nomeMes.slice(1) + ' de ' + year);

    const carregar = async () => {
      try {
        const [resDash, resOrdens, resContas] = await Promise.all([
          api.get('/relatorios/dashboard'),
          api.get('/ordens', { params: { size: 4, sort: 'id,desc' } }),
          // Se falhar (ex.: backend ainda não reiniciado), não derruba o resto do dashboard
          api.get('/relatorios/contas-apagar-dashboard').catch(() => ({ data: {} }))
        ]);

        if (resDash.data.historicoFaturamento) {
          setHistoricoFaturamento(resDash.data.historicoFaturamento);
        }

        setStats({
          clientes: resDash.data.totalClientes || 0,
          funcionarios: resDash.data.totalFuncionarios || 0,
          faturamentoMes: resDash.data.faturamentoMes || 0,
          osMes: resDash.data.osMes || 0,
          osConcluidasMes: resDash.data.osConcluidasMes || 0,
          ordensHojeTotal: resDash.data.osHoje || 0,
          hojeAbertas: resDash.data.hojeAbertas || 0,
          hojeConcluidas: resDash.data.hojeConcluidas || 0,
          hojeCanceladas: resDash.data.hojeCanceladas || 0,
        });

        const atrasados = resContas.data.atrasados || [];
        const vencendoHoje = resContas.data.vencendoHoje || [];
        const vencendoAmanha = resContas.data.vencendoAmanha || [];

        setUltimasOrdens(resOrdens.data.content || []);
        setContasAPagar({
          vencendoHoje,
          vencendoAmanha,
          atrasados
        });

        if (atrasados.length > 0) {
          setAbaAvisos('atrasados');
        } else if (vencendoHoje.length > 0) {
          setAbaAvisos('hoje');
        } else if (vencendoAmanha.length > 0) {
          setAbaAvisos('amanha');
        }
      } catch (err) {
        // Erro silencioso
      } finally {
        setLoading(false);
      }
    };
    carregar();
  }, []);

  // --- Lógica para calcular o crescimento real em % ---
  let percentualCrescimento = 0;
  if (historicoFaturamento.length >= 2) {
    const atual = historicoFaturamento[historicoFaturamento.length - 1].faturamento;
    const anterior = historicoFaturamento[historicoFaturamento.length - 2].faturamento;
    if (anterior > 0) {
      percentualCrescimento = ((atual - anterior) / anterior) * 100;
    } else if (atual > 0) {
      percentualCrescimento = 100;
    }
  }
  
  const textoCrescimento = percentualCrescimento > 0 
    ? `+${percentualCrescimento.toFixed(1)}%` 
    : `${percentualCrescimento.toFixed(1)}%`;
    
  const corCrescimento = percentualCrescimento >= 0 
    ? "text-emerald-600 bg-emerald-50" 
    : "text-red-600 bg-red-50";
  // ----------------------------------------------------

  if (loading) return (
    <div className="flex flex-col items-center justify-center h-[70vh] gap-4">
      <div className="w-10 h-10 border-4 border-blue-100 border-t-blue-600 rounded-full animate-spin"></div>
      <p className="text-slate-400 font-medium tracking-wide">Iniciando sistema...</p>
    </div>
  );

  return (
    <div className="max-w-7xl mx-auto">
      
      {/* Header Compacto */}
      <div className="mb-5 flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div>
          <h2 className="text-2xl font-black text-slate-900 tracking-tight">
            Olá, {nomeUsuario}!
          </h2>
          <p className="text-slate-500 text-sm mt-0.5">
            Resumo de atividades em <span className="text-blue-600 font-medium">{mesFormatado}</span>
          </p>
        </div>
        <div className="flex items-center gap-2 bg-white px-3.5 py-2 rounded-xl border border-slate-200 shadow-sm self-start md:self-auto">
           <Calendar size={16} className="text-slate-400" />
           <span className="text-slate-600 text-xs font-medium">{new Date().toLocaleDateString('pt-BR')}</span>
        </div>
      </div>

      {/* Linha Superior: Faturamento Atual (3 cols) + Avisos de Vencimento (5 cols) + Últimas OS (4 cols) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 mb-5 items-stretch">
        
        {/* Card: Faturamento Atual */}
        <div className="lg:col-span-3 bg-white rounded-2xl border border-slate-100 p-5 flex flex-col justify-between shadow-[0_2px_10px_-3px_rgba(6,81,237,0.08)] h-[225px]">
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2.5 min-w-0">
              <div className="p-2 rounded-xl bg-blue-50 text-blue-600 shrink-0">
                <Activity size={18} />
              </div>
              <div className="min-w-0">
                <p className="text-xs text-slate-500 font-semibold truncate">Faturamento Atual</p>
                <p className="text-[11px] text-slate-400 truncate">{mesFormatado}</p>
              </div>
            </div>
            <span className={`text-[11px] font-semibold px-2 py-0.5 rounded-lg shrink-0 ${corCrescimento}`}>
              {textoCrescimento}
            </span>
          </div>

          <div className="my-auto pt-1">
            <p className="text-2xl xl:text-3xl font-black text-slate-900 tracking-tight truncate">
              <AnimatedNumber value={stats.faturamentoMes} prefix="R$ " isCurrency={true} />
            </p>
          </div>

          <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-[11px] text-slate-400">
            <span className="truncate">OS concluídas</span>
            <button
              onClick={() => navigate('/faturamento')}
              className="text-blue-600 font-semibold hover:text-blue-700 cursor-pointer shrink-0"
            >
              Detalhes →
            </button>
          </div>
        </div>

        {/* Avisos de Vencimento */}
        <div className="lg:col-span-5 bg-white rounded-2xl border border-slate-100 p-5 shadow-sm flex flex-col h-[225px]">
          <div className="flex flex-wrap items-center justify-between gap-2 mb-3 shrink-0">
            <h3 className="font-bold text-slate-800 tracking-tight text-sm flex items-center gap-1.5">
              <Bell size={15} className="text-amber-500" /> Contas a Pagar (Avisos)
            </h3>

            {/* Abas: Vencidos | Hoje | Amanhã */}
            <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-xl">
              <button
                type="button"
                onClick={() => setAbaAvisos('atrasados')}
                className={`py-1 px-2 rounded-lg text-[11px] font-bold transition-all flex items-center gap-1 cursor-pointer ${
                  abaAvisos === 'atrasados'
                    ? 'bg-white text-red-600 shadow-xs'
                    : 'text-slate-500 hover:text-slate-700'
                }`}
              >
                <span>Vencidos</span>
                <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${
                  contasAPagar.atrasados.length > 0
                    ? 'bg-red-100 text-red-700'
                    : 'bg-slate-200 text-slate-600'
                }`}>
                  {contasAPagar.atrasados.length}
                </span>
              </button>

              <button
                type="button"
                onClick={() => setAbaAvisos('hoje')}
                className={`py-1 px-2 rounded-lg text-[11px] font-bold transition-all flex items-center gap-1 cursor-pointer ${
                  abaAvisos === 'hoje'
                    ? 'bg-white text-amber-600 shadow-xs'
                    : 'text-slate-500 hover:text-slate-700'
                }`}
              >
                <span>Hoje</span>
                <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${
                  contasAPagar.vencendoHoje.length > 0
                    ? 'bg-amber-100 text-amber-700'
                    : 'bg-slate-200 text-slate-600'
                }`}>
                  {contasAPagar.vencendoHoje.length}
                </span>
              </button>

              <button
                type="button"
                onClick={() => setAbaAvisos('amanha')}
                className={`py-1 px-2 rounded-lg text-[11px] font-bold transition-all flex items-center gap-1 cursor-pointer ${
                  abaAvisos === 'amanha'
                    ? 'bg-white text-blue-600 shadow-xs'
                    : 'text-slate-500 hover:text-slate-700'
                }`}
              >
                <span>Amanhã</span>
                <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${
                  contasAPagar.vencendoAmanha.length > 0
                    ? 'bg-blue-100 text-blue-700'
                    : 'bg-slate-200 text-slate-600'
                }`}>
                  {contasAPagar.vencendoAmanha.length}
                </span>
              </button>
            </div>
          </div>

          {/* Lista da aba selecionada */}
          {(() => {
            const listaAtiva =
              abaAvisos === 'atrasados'
                ? contasAPagar.atrasados
                : abaAvisos === 'hoje'
                ? contasAPagar.vencendoHoje
                : contasAPagar.vencendoAmanha;

            const textoVazio =
              abaAvisos === 'atrasados'
                ? 'Nenhum pagamento vencido.'
                : abaAvisos === 'hoje'
                ? 'Nada vencendo hoje.'
                : 'Nada vencendo amanhã.';

            const corBordaItem =
              abaAvisos === 'atrasados'
                ? 'border-red-100 bg-red-50/40 hover:bg-red-50/80'
                : abaAvisos === 'hoje'
                ? 'border-amber-100 bg-amber-50/40 hover:bg-amber-50/80'
                : 'border-blue-100 bg-blue-50/40 hover:bg-blue-50/80';

            const corValor =
              abaAvisos === 'atrasados'
                ? 'text-red-600'
                : abaAvisos === 'hoje'
                ? 'text-amber-700'
                : 'text-blue-600';

            if (listaAtiva.length === 0) {
              return (
                <div className="flex-1 flex flex-col items-center justify-center text-center">
                  <CheckCircle2 size={24} className="text-emerald-500 mb-1 opacity-80" />
                  <p className="text-slate-400 text-xs">{textoVazio}</p>
                </div>
              );
            }

            return (
              <div className="flex flex-col gap-2 flex-1 overflow-y-auto pr-1">
                {listaAtiva.map((conta) => {
                  const dataFmt = conta.vencimento
                    ? String(conta.vencimento).split('-').reverse().join('/')
                    : '';
                  return (
                    <div
                      key={conta.id}
                      onClick={() => navigate('/faturamento')}
                      className={`p-2.5 rounded-xl border transition-colors cursor-pointer flex items-start justify-between gap-2.5 ${corBordaItem}`}
                    >
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5">
                          <AlertCircle size={13} className={`shrink-0 ${corValor}`} />
                          <p className="text-xs font-bold text-slate-800 truncate">
                            {conta.descricao}
                          </p>
                        </div>
                        {conta.envolvido && (
                          <p className="text-[11px] text-slate-600 truncate mt-0.5">
                            {conta.envolvido}
                          </p>
                        )}
                        <p className="text-[10px] text-slate-400 font-medium mt-0.5">
                          Venc.: {dataFmt}
                        </p>
                      </div>
                      <div className="text-right shrink-0">
                        <p className={`text-xs font-black ${corValor}`}>
                          R$ {formatarMoeda(Number(conta.valor) || 0)}
                        </p>
                        <span className="text-[10px] font-semibold text-slate-500">
                          {conta.tipo === 'PAGAR' ? 'A Pagar' : 'A Receber'}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            );
          })()}
        </div>

        {/* Feed de Últimas OS (Ao lado dos Avisos de Vencimento) */}
        <div className="lg:col-span-4 bg-white rounded-2xl border border-slate-100 p-5 shadow-sm flex flex-col h-[225px]">
          <div className="flex justify-between items-center mb-3 shrink-0">
            <h3 className="font-bold text-slate-800 tracking-tight text-sm flex items-center gap-1.5">
              <ClipboardList size={15} className="text-blue-600" /> Últimas OS
            </h3>
            <button onClick={() => navigate('/ordens')} className="text-blue-600 text-xs font-semibold hover:text-blue-700 cursor-pointer">
              Ver todas
            </button>
          </div>
          
          <div className="flex flex-col gap-2.5 flex-1 overflow-y-auto pr-1">
            {ultimasOrdens.length === 0 ? (
              <div className="flex-1 flex items-center justify-center">
                <p className="text-slate-400 text-xs text-center">Nenhuma ordem recente.</p>
              </div>
            ) : (
              ultimasOrdens.map(os => (
                <div key={os.id} className="flex gap-2.5 group cursor-pointer" onClick={() => navigate('/ordens')}>
                  <div className="w-2 h-2 mt-1.5 rounded-full bg-blue-500 shrink-0"></div>
                  <div className="flex-1 min-w-0 border-b border-slate-50 pb-2 group-hover:border-blue-100 transition-colors flex items-center justify-between gap-2">
                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-slate-800 truncate">{os.cliente?.nome || 'Cliente'}</p>
                      <p className="text-[11px] text-slate-500 truncate">{os.veiculo || 'Serviços gerais'}</p>
                    </div>
                    <span className="text-[10px] text-slate-400 font-mono shrink-0 bg-slate-50 px-2 py-0.5 rounded-md border border-slate-100">
                      #{String(os.id).padStart(4, '0')}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Linha Inferior: Gráfico Grande (8 cols) + Atalhos Rápidos (4 cols) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-stretch">
        
        {/* Gráfico de Evolução do Faturamento Grande */}
        <div className="lg:col-span-8 bg-white rounded-2xl border border-slate-100 p-5 shadow-[0_2px_10px_-3px_rgba(6,81,237,0.1)] flex flex-col justify-between h-[300px]">
          <div className="flex justify-between items-center mb-3">
            <h3 className="font-bold text-slate-800 tracking-tight text-base">Evolução do Faturamento</h3>
            <span className={`text-xs font-medium px-2.5 py-1 rounded-md ${corCrescimento}`}>
              {textoCrescimento} vs último mês
            </span>
          </div>
          <div className="flex-1 w-full min-h-0">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={historicoFaturamento} margin={{ top: 10, right: 10, left: -15, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorFaturamento" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.22}/>
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="mes" axisLine={false} tickLine={false} tick={{fill: '#94a3b8', fontSize: 12}} dy={8} />
                <YAxis axisLine={false} tickLine={false} tick={{fill: '#94a3b8', fontSize: 12}} />
                <Tooltip 
                  contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}
                  formatter={(value) => [`R$ ${value}`, 'Faturamento']}
                />
                <Area type="monotone" dataKey="faturamento" stroke="#3b82f6" strokeWidth={3} fillOpacity={1} fill="url(#colorFaturamento)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Atalhos Rápidos: Criar OS e Registrar Pagamento */}
        <div className="lg:col-span-4 bg-white rounded-2xl border border-slate-100 p-5 shadow-sm flex flex-col justify-between h-[300px]">
          <div>
            <h3 className="font-bold text-slate-800 tracking-tight text-base mb-1">Atalhos Rápidos</h3>
            <p className="text-xs text-slate-400 mb-4">Acesso direto às principais operações do dia</p>
          </div>

          <div className="flex flex-col gap-3 my-auto">
            <button
              onClick={() => navigate('/ordens')}
              className="w-full p-4 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-semibold text-sm flex items-center justify-between transition-all shadow-sm hover:shadow-md cursor-pointer group"
            >
              <div className="flex items-center gap-3 text-left">
                <div className="p-2 rounded-lg bg-white/15 text-white">
                  <PlusCircle size={20} />
                </div>
                <div>
                  <p className="font-bold text-sm leading-tight">Criar Nova OS</p>
                  <p className="text-[11px] text-blue-100 font-normal mt-0.5">Abrir ordem de serviço</p>
                </div>
              </div>
              <ChevronRight size={18} className="text-blue-200 group-hover:translate-x-0.5 transition-transform" />
            </button>

            <button
              onClick={() => navigate('/faturamento')}
              className="w-full p-4 rounded-xl bg-emerald-50 hover:bg-emerald-100/80 border border-emerald-200/70 text-emerald-900 font-semibold text-sm flex items-center justify-between transition-all cursor-pointer group"
            >
              <div className="flex items-center gap-3 text-left">
                <div className="p-2 rounded-lg bg-emerald-500 text-white">
                  <DollarSign size={20} />
                </div>
                <div>
                  <p className="font-bold text-sm leading-tight text-emerald-950">Registrar Pagamento</p>
                  <p className="text-[11px] text-emerald-700 font-normal mt-0.5">Dar baixa ou lançar no financeiro</p>
                </div>
              </div>
              <ChevronRight size={18} className="text-emerald-600 group-hover:translate-x-0.5 transition-transform" />
            </button>
          </div>  
        </div>
      </div>
    </div>
  );
}